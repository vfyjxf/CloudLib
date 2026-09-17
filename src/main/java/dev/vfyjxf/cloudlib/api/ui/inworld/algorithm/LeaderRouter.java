package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leader-line routing with the standard annotation hierarchy (§3.0):
 * <ul>
 *   <li><strong>s-leader</strong> — the straight segment from feature anchor
 *       to label; the default, always preferred when it crosses nothing</li>
 *   <li><strong>po-leader</strong> — the parallel-orthogonal two-segment
 *       elbow (one horizontal, one vertical segment; every leader uses the
 *       same {@link Elbow} order, which is what makes them parallel and
 *       crossing-unfriendly); a leader upgrades to this when its straight
 *       segment crosses another leader's</li>
 *   <li><strong>hyperleader</strong> — the shared trunk of a cluster: members
 *       route anchor → trunk → label, where the trunk is the centroid of the
 *       cluster's anchors, so every member's trunk→label segment has the same
 *       endpoints and renders as one shared line</li>
 * </ul>
 * <p>
 * Upgrades and downgrades pass through a per-leader {@link SwitchGate} — the
 * metric is the leader's crossing count, the band is in crossings — so a
 * boundary that jitters between "one crossing" and "no crossings" cannot
 * flip the style back and forth. Crossings are always measured on the
 * <em>straight</em> baselines (anchor → label), regardless of the currently
 * routed style: the trigger signal is geometric, the gate adds the temporal
 * hysteresis. Leaders sharing a cluster never count against each other (they
 * converge on the trunk by design).
 * <p>
 * One {@link #route} call is one decision epoch; determinism holds because
 * everything iterates the caller's leader list in order.
 */
public final class LeaderRouter {

    /** The routing style a leader ended up with. */
    public enum Style {
        sLeader,
        poLeader,
        hyperLeader
    }

    /**
     * Which segment leaves the anchor first on a po-leader.
     * {@code horizontalFirst} routes {@code anchor → (labelX, anchorY) →
     * label}; {@code verticalFirst} routes {@code anchor → (anchorX, labelY)
     * → label}.
     */
    public enum Elbow {
        horizontalFirst,
        verticalFirst
    }

    /**
     * @param elbow the shared po-leader elbow order (shared = parallel)
     * @param band the hysteresis band, in crossing-count units — a style
     *        switch needs the crossing count to have moved this far from its
     *        value at the last commit; positive
     * @param dwellEpochs how many consecutive epochs a candidate style must
     *        survive before committing; at least 1
     */
    public record Config(Elbow elbow, double band, int dwellEpochs) {

        public Config {
            if (!Double.isFinite(band) || band <= 0) {
                throw new IllegalArgumentException("band must be finite and positive: " + band);
            }
            if (dwellEpochs < 1) {
                throw new IllegalArgumentException("dwellEpochs must be at least 1: " + dwellEpochs);
            }
        }

        public static Config of(Elbow elbow, double band, int dwellEpochs) {
            return new Config(elbow, band, dwellEpochs);
        }
    }

    /** One leader to route: the feature anchor and the label attach point. */
    public record Leader(String id, double anchorX, double anchorY, double labelX, double labelY) {}

    /**
     * One routed leader: the polyline points in drawing order. Cluster
     * members carry their {@code clusterId} and share the trunk point as
     * their second vertex.
     */
    public record Route(String id, Style style, List<FloatPos> points, @Nullable String clusterId) {

        public Route {
            points = List.copyOf(points);
        }
    }

    private static final double epsilon = 1.0e-9;

    private final Config config;
    private final Map<String, SwitchGate<Style>> gates = new HashMap<>();

    public LeaderRouter(Config config) {
        this.config = config;
    }

    /**
     * Routes one epoch's leaders.
     *
     * @param leaders the leaders, in the caller's canonical order
     * @param clusters leader id → cluster id; leaders sharing a cluster id
     *        (two or more) route as hyperleaders through their shared trunk,
     *        leaders absent from the map route individually
     *
     * @throws IllegalArgumentException if leader ids are duplicated
     */
    public List<Route> route(List<Leader> leaders, Map<String, String> clusters) {
        Map<String, Leader> byId = new LinkedHashMap<>();
        for (Leader leader : leaders) {
            if (byId.put(leader.id(), leader) != null) {
                throw new IllegalArgumentException("duplicate leader id: " + leader.id());
            }
        }

        Map<String, List<Leader>> clusterGroups = groupClusters(leaders, clusters);
        Map<String, Integer> crossings = countCrossings(leaders, clusterGroups);

        gates.keySet().retainAll(byId.keySet());

        List<Route> routes = new ArrayList<>(leaders.size());
        for (Leader leader : leaders) {
            String clusterId = clusters.get(leader.id());
            List<Leader> group = clusterId == null ? null : clusterGroups.get(clusterId);
            if (group != null && group.size() >= 2) {
                FloatPos trunk = trunkOf(group);
                routes.add(new Route(
                        leader.id(),
                        Style.hyperLeader,
                        List.of(
                                new FloatPos(leader.anchorX(), leader.anchorY()),
                                trunk,
                                new FloatPos(leader.labelX(), leader.labelY())),
                        clusterId));
                continue;
            }
            Style desired = crossings.get(leader.id()) > 0 ? Style.poLeader : Style.sLeader;
            SwitchGate<Style> gate =
                    gates.computeIfAbsent(leader.id(), id -> new SwitchGate<>(gateConfig(), Style.sLeader, 0.0));
            gate.propose(desired, crossings.get(leader.id()));
            routes.add(new Route(leader.id(), gate.current(), polyline(leader, gate.current()), null));
        }
        return routes;
    }

    /** Drops all per-leader gate state (a new scene, a teleport). */
    public void reset() {
        gates.clear();
    }

    private Map<String, List<Leader>> groupClusters(List<Leader> leaders, Map<String, String> clusters) {
        Map<String, List<Leader>> groups = new LinkedHashMap<>();
        for (Leader leader : leaders) {
            String clusterId = clusters.get(leader.id());
            if (clusterId != null) {
                groups.computeIfAbsent(clusterId, id -> new ArrayList<>()).add(leader);
            }
        }
        return groups;
    }

    private static FloatPos trunkOf(List<Leader> group) {
        double x = 0;
        double y = 0;
        for (Leader leader : group) {
            x += leader.anchorX();
            y += leader.anchorY();
        }
        return new FloatPos(x / group.size(), y / group.size());
    }

    /**
     * Crossing counts per leader, measured on the straight baselines; pairs
     * inside the same cluster are exempt (they meet at the trunk by design).
     */
    private static Map<String, Integer> countCrossings(List<Leader> leaders, Map<String, List<Leader>> clusterGroups) {
        Map<String, Integer> counts = new HashMap<>();
        for (Leader leader : leaders) {
            counts.put(leader.id(), 0);
        }
        for (int i = 0; i < leaders.size(); i++) {
            for (int j = i + 1; j < leaders.size(); j++) {
                Leader a = leaders.get(i);
                Leader b = leaders.get(j);
                if (sameCluster(a, b, clusterGroups)) {
                    continue;
                }
                if (segmentsCross(
                        new FloatPos(a.anchorX(), a.anchorY()),
                        new FloatPos(a.labelX(), a.labelY()),
                        new FloatPos(b.anchorX(), b.anchorY()),
                        new FloatPos(b.labelX(), b.labelY()))) {
                    counts.merge(a.id(), 1, Integer::sum);
                    counts.merge(b.id(), 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private static boolean sameCluster(Leader a, Leader b, Map<String, List<Leader>> clusterGroups) {
        for (List<Leader> group : clusterGroups.values()) {
            if (group.size() < 2) {
                continue;
            }
            boolean hasA = false;
            boolean hasB = false;
            for (Leader leader : group) {
                hasA |= leader.id().equals(a.id());
                hasB |= leader.id().equals(b.id());
            }
            if (hasA && hasB) {
                return true;
            }
        }
        return false;
    }

    private List<FloatPos> polyline(Leader leader, Style style) {
        FloatPos anchor = new FloatPos(leader.anchorX(), leader.anchorY());
        FloatPos label = new FloatPos(leader.labelX(), leader.labelY());
        if (style == Style.sLeader) {
            return List.of(anchor, label);
        }
        FloatPos elbow = config.elbow() == Elbow.horizontalFirst
                ? new FloatPos(leader.labelX(), leader.anchorY())
                : new FloatPos(leader.anchorX(), leader.labelY());
        return List.of(anchor, elbow, label);
    }

    private SwitchGate.Config gateConfig() {
        return new SwitchGate.Config(config.band(), config.dwellEpochs(), 0.0, 0);
    }

    /**
     * Whether segments {@code a1→a2} and {@code b1→b2} properly cross: strict
     * intersection under an orientation test, or collinear overlap. Shared or
     * touching endpoints are not crossings.
     */
    private static boolean segmentsCross(FloatPos a1, FloatPos a2, FloatPos b1, FloatPos b2) {
        if (distance(a1, a2) < epsilon || distance(b1, b2) < epsilon) {
            return false;
        }
        if (distance(a1, b1) < epsilon
                || distance(a1, b2) < epsilon
                || distance(a2, b1) < epsilon
                || distance(a2, b2) < epsilon) {
            return false;
        }
        double d1 = cross(b2.x() - b1.x(), b2.y() - b1.y(), a1.x() - b1.x(), a1.y() - b1.y());
        double d2 = cross(b2.x() - b1.x(), b2.y() - b1.y(), a2.x() - b1.x(), a2.y() - b1.y());
        double d3 = cross(a2.x() - a1.x(), a2.y() - a1.y(), b1.x() - a1.x(), b1.y() - a1.y());
        double d4 = cross(a2.x() - a1.x(), a2.y() - a1.y(), b2.x() - a1.x(), b2.y() - a1.y());
        if (((d1 > epsilon && d2 < -epsilon) || (d1 < -epsilon && d2 > epsilon))
                && ((d3 > epsilon && d4 < -epsilon) || (d3 < -epsilon && d4 > epsilon))) {
            return true;
        }
        return collinearOverlap(a1, a2, b1, b2, d1, d2, d3, d4);
    }

    private static boolean collinearOverlap(
            FloatPos a1, FloatPos a2, FloatPos b1, FloatPos b2, double... orientations) {
        for (double orientation : orientations) {
            if (Math.abs(orientation) > epsilon) {
                return false;
            }
        }
        return Math.max(a1.x(), a2.x()) > Math.min(b1.x(), b2.x()) - epsilon
                && Math.max(b1.x(), b2.x()) > Math.min(a1.x(), a2.x()) - epsilon
                && Math.max(a1.y(), a2.y()) > Math.min(b1.y(), b2.y()) - epsilon
                && Math.max(b1.y(), b2.y()) > Math.min(a1.y(), a2.y()) - epsilon;
    }

    private static double cross(double ax, double ay, double bx, double by) {
        return ax * by - ay * bx;
    }

    private static double distance(FloatPos a, FloatPos b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
