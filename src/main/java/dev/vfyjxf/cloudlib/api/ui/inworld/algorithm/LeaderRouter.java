package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
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
 *   <li><strong>po-leader</strong> — the orthogonal route produced by
 *       {@link LeaderGridRouter}: one or two bends around rectangle
 *       obstacles, first segment off the anchor along its stub, last segment
 *       into the port along the port's outward normal</li>
 *   <li><strong>hyperleader</strong> — the shared trunk of a cluster: members
 *       route anchor → trunk → label, where the trunk is the centroid of the
 *       cluster's anchors, so every member's trunk→label segment renders as
 *       one shared line</li>
 * </ul>
 * <p>
 * <strong>When a leader upgrades to po.</strong> The trigger is geometric and
 * measured on the straight baselines (anchor → port), regardless of the
 * currently routed style: a baseline crossing another leader's baseline
 * <em>or</em> running within {@link Config#proximityPx} of it (dense
 * parallel straights read as chaotic — the user-study finding behind the
 * proximity trigger) <em>or</em> cutting through an inflated obstacle. Each
 * unit feeds the per-leader {@link SwitchGate} metric, so upgrades and
 * downgrades need the sustained movement the gate's band and dwell demand —
 * a boundary that jitters cannot flip the style back and forth. Leaders
 * sharing a cluster never count against each other (they converge on the
 * trunk by design).
 * <p>
 * <strong>The three-layer anti-flash hysteresis</strong> on every po route:
 * <ol>
 *   <li><strong>Anchor quantization.</strong> The anchor, port and obstacle
 *       set are quantized to {@link Config#anchorQuantPx} (12 px) into a
 *       reuse key; while the key is unchanged the committed interior
 *       <em>lanes</em> are reused verbatim and only the exact endpoints
 *       move — each re-joined to its frozen lane by a joint that slides
 *       along the lane, so every stretched segment stays axis-aligned:
 *       no re-route, no flicker, no per-frame diagonal.</li>
 *   <li><strong>Switch cost.</strong> When the key does change, the fresh
 *       route replaces the committed one only if it is at least
 *       {@link Config#switchCostPx} shorter than the currently stretched
 *       committed route, or the committed route has become invalid (an
 *       obstacle now blocks it, or the port's exit axis changed). Equal-ish
 *       alternatives never flap.</li>
 *   <li><strong>Exit-side deadzone.</strong> The port's face cannot change
 *       at all until {@link AttachPointResolver}'s 55°/35° band and 48 px
 *       exit deadzone have committed the change — the router inherits the
 *       resolver's decision and re-keys on it.</li>
 * </ol>
 * <p>
 * <strong>Zero crossings are a hard constraint, not a preference.</strong>
 * After routing, actual-geometry crossings are counted pairwise; every
 * crossing po-leader is re-routed once with the polylines it crosses as
 * additional thin obstacles. Whatever crossings remain are reported in
 * {@link Route#crossings} — the caller trims (Nimbus's leader budget already
 * does).
 * <p>
 * <strong>Degeneracies.</strong> A panel whose port lies within
 * {@link Config#leaderTolerancePx} of the anchor draws no line at all (empty
 * points). A leader that never gets close draws one whose final segment is
 * exactly the routing config's {@code lastSegmentPx}, stopping
 * {@code arrivalGapPx} short of the border. When the grid A* finds no path —
 * a sealed-in anchor, an overlapping obstacle field — a simple elbow is
 * drawn instead: never a blank frame.
 * <p>
 * One {@link #route} call is one decision epoch; determinism holds because
 * everything iterates the caller's leader list in order and the grid search
 * itself is tie-broken. No wall clock — the perimeter slide's dt is consumed
 * by the {@link AttachPointResolver} the caller drives.
 */
public final class LeaderRouter {

    /** The routing style a leader ended up with. */
    public enum Style {
        sLeader,
        poLeader,
        hyperLeader
    }

    /**
     * The epoch-level knobs; the geometric routing knobs live in the nested
     * {@link LeaderGridRouter.Config}.
     *
     * @param band the style-switch hysteresis band, in trigger units
     *        (crossings + proximity pairs + obstacle hits); positive
     * @param dwellEpochs how many consecutive epochs a candidate style must
     *        survive before committing; at least 1
     * @param leaderTolerancePx anchors closer than this to their port draw no
     *        leader at all; positive
     * @param anchorQuantPx the quantization cell of the po-route reuse key;
     *        positive
     * @param switchCostPx the length a fresh route must save over the
     *        stretched committed route before replacing it; non-negative
     * @param proximityPx how close two baselines may run before the pair
     *        counts as a po trigger; positive
     * @param routing the grid router configuration
     */
    public record Config(
            double band,
            int dwellEpochs,
            double leaderTolerancePx,
            double anchorQuantPx,
            double switchCostPx,
            double proximityPx,
            LeaderGridRouter.Config routing) {

        public Config {
            if (!Double.isFinite(band) || band <= 0) {
                throw new IllegalArgumentException("band must be finite and positive: " + band);
            }
            if (dwellEpochs < 1) {
                throw new IllegalArgumentException("dwellEpochs must be at least 1: " + dwellEpochs);
            }
            if (!Double.isFinite(leaderTolerancePx) || leaderTolerancePx <= 0) {
                throw new IllegalArgumentException(
                        "leaderTolerancePx must be finite and positive: " + leaderTolerancePx);
            }
            if (!Double.isFinite(anchorQuantPx) || anchorQuantPx <= 0) {
                throw new IllegalArgumentException("anchorQuantPx must be finite and positive: " + anchorQuantPx);
            }
            if (!Double.isFinite(switchCostPx) || switchCostPx < 0) {
                throw new IllegalArgumentException("switchCostPx must be finite and non-negative: " + switchCostPx);
            }
            if (!Double.isFinite(proximityPx) || proximityPx <= 0) {
                throw new IllegalArgumentException("proximityPx must be finite and positive: " + proximityPx);
            }
        }

        /** Band/dwell on the survey defaults: {@code 1, 2, 80, 12, 20, 6} + routing defaults. */
        public static Config of(double band, int dwellEpochs) {
            return new Config(band, dwellEpochs, 80.0, 12.0, 20.0, 6.0, LeaderGridRouter.Config.ofDefaults());
        }
    }

    /**
     * One leader to route: the feature anchor and the label-side port the
     * {@link AttachPointResolver} resolved. Use {@link #toPoint} when no
     * panel rect is at hand — a bare label point with the arrival normal
     * inferred from the anchor's side.
     */
    public record Leader(String id, double anchorX, double anchorY, AttachPointResolver.Port port) {

        public Leader {
            if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY)) {
                throw new IllegalArgumentException("anchor must be finite: (" + anchorX + ", " + anchorY + ")");
            }
        }

        /** A leader whose label end is a bare point; the port normal points toward the anchor's dominant axis. */
        public static Leader toPoint(String id, double anchorX, double anchorY, double labelX, double labelY) {
            double dx = anchorX - labelX;
            double dy = anchorY - labelY;
            double normalX = Math.abs(dx) >= Math.abs(dy) ? Math.signum(dx) : 0.0;
            double normalY = normalX == 0.0 ? Math.signum(dy) : 0.0;
            if (normalX == 0.0 && normalY == 0.0) {
                normalX = 1.0;
            }
            AttachPointResolver.Face face = AttachPointResolver.faceOfNormal(normalX, normalY);
            return new Leader(
                    id,
                    anchorX,
                    anchorY,
                    new AttachPointResolver.Port(face, new FloatPos(labelX, labelY), normalX, normalY));
        }
    }

    /**
     * One routed leader. {@code points} is the drawn polyline in order —
     * empty when the leader tolerance suppressed the line. {@code crossings}
     * counts this polyline's crossings against the epoch's other routed
     * polylines after the zero-crossing pass; a positive count is the
     * caller's cue to trim. Cluster members carry their {@code clusterId}
     * and share the trunk point as their second vertex.
     *
     * @param shapeEpoch the topology token: it changes only when this
     *        leader's committed route is replaced (a fresh adoption, a
     *        crossing re-route) or its style flips — never when the same
     *        route merely stretches under a sliding endpoint. The caller
     *        keys its shape-settle animation off it, so the settle plays for
     *        real topology changes and endpoint slides stay unhitched
     */
    public record Route(
            String id, Style style, List<FloatPos> points, @Nullable String clusterId, int crossings, long shapeEpoch) {

        public Route {
            points = List.copyOf(points);
        }
    }

    private static final double epsilon = 1.0e-9;

    private final Config config;
    private final Map<String, SwitchGate<Style>> gates = new HashMap<>();
    private final Map<String, Committed> committed = new HashMap<>();
    private long epochClock;

    public LeaderRouter(Config config) {
        this.config = config;
    }

    /**
     * Routes one epoch's leaders with no obstacles.
     *
     * @see #route(List, Map, List, FloatRect)
     */
    public List<Route> route(List<Leader> leaders, Map<String, String> clusters) {
        return route(leaders, clusters, List.of(), null);
    }

    /**
     * Routes one epoch's leaders.
     *
     * @see #route(List, Map, List, FloatRect)
     */
    public List<Route> route(List<Leader> leaders, Map<String, String> clusters, List<FloatRect> obstacles) {
        return route(leaders, clusters, obstacles, null);
    }

    /**
     * Routes one epoch's leaders.
     *
     * @param leaders the leaders, in the caller's canonical order
     * @param clusters leader id → cluster id; leaders sharing a cluster id
     *        (two or more) route as hyperleaders through their shared trunk,
     *        leaders absent from the map route individually
     * @param obstacles the rects to route around — other panels and HUD
     *        areas as a plain rectangle list (never one monolithic HUD
     *        block), in the caller's canonical order
     * @param viewport the drawable bounds every routed segment must stay
     *        inside — the gui-px screen rect for the overlay pass; stretched
     *        or committed routes that would leave it are re-routed, and the
     *        elbow fallback clamps onto its edge. {@code null} bounds
     *        nothing
     * @return one {@link Route} per leader, in the leaders' order
     *
     * @throws IllegalArgumentException if leader ids are duplicated
     */
    public List<Route> route(
            List<Leader> leaders,
            Map<String, String> clusters,
            List<FloatRect> obstacles,
            @Nullable FloatRect viewport) {
        Map<String, Leader> byId = new LinkedHashMap<>();
        for (Leader leader : leaders) {
            if (byId.put(leader.id(), leader) != null) {
                throw new IllegalArgumentException("duplicate leader id: " + leader.id());
            }
        }

        Map<String, List<Leader>> clusterGroups = groupClusters(leaders, clusters);
        Map<String, Integer> triggers = countTriggers(leaders, clusterGroups, obstacles, viewport);

        gates.keySet().retainAll(byId.keySet());
        committed.keySet().retainAll(byId.keySet());

        List<Working> work = new ArrayList<>(leaders.size());
        for (Leader leader : leaders) {
            String clusterId = clusters.get(leader.id());
            List<Leader> group = clusterId == null ? null : clusterGroups.get(clusterId);
            work.add(routeOne(leader, group, clusterId, triggers.get(leader.id()), obstacles, viewport));
        }
        reduceCrossings(work, clusterGroups, obstacles, viewport);
        int[] counts = countCrossings(work, clusterGroups);
        List<Route> routes = new ArrayList<>(work.size());
        for (int i = 0; i < work.size(); i++) {
            Working w = work.get(i);
            routes.add(new Route(w.id, w.style, w.points, w.clusterId, counts[i], w.shapeEpoch));
        }
        return routes;
    }

    /** Drops all per-leader state (a new scene, a teleport). */
    public void reset() {
        gates.clear();
        committed.clear();
    }

    // region per-leader routing

    private static final class Committed {
        String key;
        List<FloatPos> points;
        double dirX;
        double dirY;
        /** the port's local edge normal the route was committed against */
        double exitX;

        double exitY;
        double cost;
        /**
         * The commit's topology token — bumped on every adoption (fresh
         * route or crossing re-route); stretches reuse the value untouched,
         * which is what tells the caller's settle a slide is not a re-route.
         */
        long epoch;
    }

    private static final class Working {
        final String id;
        final @Nullable String clusterId;
        Style style;
        List<FloatPos> points = List.of();
        long shapeEpoch;
        final Leader leader;

        Working(Leader leader, @Nullable String clusterId, Style style) {
            this.id = leader.id();
            this.clusterId = clusterId;
            this.leader = leader;
            this.style = style;
        }
    }

    private Working routeOne(
            Leader leader,
            @Nullable List<Leader> group,
            @Nullable String clusterId,
            int trigger,
            List<FloatRect> obstacles,
            @Nullable FloatRect viewport) {
        Style desired = trigger > 0 ? Style.poLeader : Style.sLeader;
        SwitchGate<Style> gate =
                gates.computeIfAbsent(leader.id(), id -> new SwitchGate<>(gateConfig(), Style.sLeader, 0.0));
        gate.propose(desired, trigger);
        Style gated = gate.current();

        FloatPos anchor = new FloatPos(leader.anchorX(), leader.anchorY());
        FloatPos portPoint = leader.port().point();
        double distance = Math.hypot(portPoint.x() - anchor.x(), portPoint.y() - anchor.y());
        Working working = new Working(leader, group != null && group.size() >= 2 ? clusterId : null, gated);
        working.shapeEpoch = styleShapeEpoch(gated);
        if (distance < config.leaderTolerancePx()) {
            return working; // within tolerance: no line at all
        }

        if (working.clusterId != null) {
            FloatPos trunk = trunkOf(group);
            working.points = List.of(anchor, trunk, LeaderGridRouter.clamped(drawnEnd(leader), viewport));
            working.style = Style.hyperLeader;
            working.shapeEpoch = styleShapeEpoch(Style.hyperLeader);
            return working;
        }

        boolean straightBlocked = LeaderGridRouter.polylineBlocked(
                List.of(anchor, portPoint),
                portPoint,
                obstacles,
                config.routing().clearancePx(),
                viewport);
        if (gated == Style.sLeader && !straightBlocked) {
            working.points = List.of(anchor, LeaderGridRouter.clamped(drawnEnd(leader), viewport));
            return working;
        }

        working.style = Style.poLeader;
        working.points = orthogonalRoute(leader, obstacles, viewport);
        Committed commit = committed.get(leader.id());
        working.shapeEpoch = commit != null ? commit.epoch : styleShapeEpoch(Style.poLeader);
        return working;
    }

    /**
     * The shape token of an uncommitted style — negative so no commit epoch
     * (positive, from {@link #epochClock}) ever collides; the point is only
     * that a style flip reads as a topology change while a slide does not.
     */
    private static long styleShapeEpoch(Style style) {
        return switch (style) {
            case sLeader -> -1L;
            case hyperLeader -> -2L;
            case poLeader -> -3L;
        };
    }

    private List<FloatPos> orthogonalRoute(Leader leader, List<FloatRect> obstacles, @Nullable FloatRect viewport) {
        FloatPos anchor = new FloatPos(leader.anchorX(), leader.anchorY());
        FloatPos portPoint = leader.port().point();
        String key = reuseKey(leader, obstacles);
        Committed prior = committed.get(leader.id());
        // a port that swung to another face (or slid around a corner) leaves
        // the committed approach pointing the wrong way: its re-jointed tail
        // would cut a diagonal across the panel, so that route is not reusable
        boolean exitChanged = prior == null
                || prior.exitX != leader.port().normalX()
                || prior.exitY != leader.port().normalY();

        if (!exitChanged && prior.key.equals(key)) {
            List<FloatPos> stretched = stretch(prior.points, anchor, leader.port(), prior, viewport);
            if (!LeaderGridRouter.polylineBlocked(
                    stretched, portPoint, obstacles, config.routing().clearancePx(), viewport)) {
                prior.points = stretched;
                prior.cost = pathLength(stretched);
                return stretched;
            }
        }

        double dx = portPoint.x() - anchor.x();
        double dy = portPoint.y() - anchor.y();
        double[] primary = axisUnit(dx, dy);
        // the perpendicular stub axis, signed toward the port — the 1-fold
        // route off a sideways port needs the stub heading at the target
        double[] secondary = Math.abs(primary[0]) > 0.5 ? axisUnit(0, dy) : axisUnit(dx, 0);
        List<FloatPos> fresh = null;
        double[] freshDir = primary;
        double freshCost = Double.POSITIVE_INFINITY;
        // both stub-axis candidates compete on the drawn cost model — the
        // perpendicular one often saves a whole bend on offset targets, the
        // dominant one wins the near-aligned case; ties keep the dominant
        for (double[] candidate : new double[][] {primary, secondary}) {
            List<FloatPos> path = LeaderGridRouter.route(
                    anchor,
                    candidate[0],
                    candidate[1],
                    portPoint,
                    leader.port().normalX(),
                    leader.port().normalY(),
                    obstacles,
                    config.routing(),
                    viewport);
            if (path == null) {
                continue;
            }
            double cost = candidateCost(path);
            if (cost < freshCost - epsilon) {
                freshCost = cost;
                fresh = path;
                freshDir = candidate;
            }
        }
        if (fresh == null) {
            fresh = elbowFallback(anchor, primary, leader.port(), viewport);
            freshDir = primary;
        }

        boolean adopt = prior == null;
        if (!adopt) {
            List<FloatPos> stretched = stretch(prior.points, anchor, leader.port(), prior, viewport);
            boolean priorValid = !LeaderGridRouter.polylineBlocked(
                    stretched, portPoint, obstacles, config.routing().clearancePx(), viewport);
            double stretchedCost = pathLength(stretched);
            adopt = exitChanged || !priorValid || stretchedCost - pathLength(fresh) >= config.switchCostPx();
            if (!adopt) {
                prior.points = stretched;
                prior.cost = stretchedCost;
                return stretched;
            }
        }
        Committed next = new Committed();
        next.key = key;
        next.points = fresh;
        next.dirX = freshDir[0];
        next.dirY = freshDir[1];
        next.exitX = leader.port().normalX();
        next.exitY = leader.port().normalY();
        next.cost = pathLength(fresh);
        next.epoch = ++epochClock;
        committed.put(leader.id(), next);
        return fresh;
    }

    /**
     * The committed interior re-joined to the exact endpoints as a rubber
     * band: the first committed <em>lane</em> (the run after the stub's bend)
     * and the last one (the run before the approach's bend) stay frozen while
     * a head joint rides the anchor's stub-axis line onto the first lane and
     * a tail joint rides the port-normal line onto the last lane. Every
     * vertex of the answer is axis-aligned by construction — an endpoint
     * sliding under a stable reuse key moves its joint <em>along</em> a lane,
     * never into a per-frame diagonal. The joints may overrun their lane
     * (a ≤ one quantization cell of back-jog while the key still holds) —
     * orthogonal and transient, and the blocked test on the stretch still
     * decides re-routes. A committed shape whose head or tail runs are not
     * the frozen axes this derives from (a viewport-truncated stub, an
     * unmerged fold) falls back to the exact elbow rather than risk a
     * diagonal. Fresh grid routes carry no stub vertex (the merge folds it
     * into the first run), and neither does the stretch: the lane itself is
     * what is committed.
     */
    private List<FloatPos> stretch(
            List<FloatPos> points,
            FloatPos anchor,
            AttachPointResolver.Port port,
            Committed prior,
            @Nullable FloatRect viewport) {
        FloatPos end = LeaderGridRouter.clamped(drawnEnd(port), viewport);
        if (points.size() <= 2) {
            return List.of(anchor, end);
        }
        if (points.size() == 3) {
            return elbowFallback(anchor, new double[] {prior.dirX, prior.dirY}, port, viewport);
        }
        FloatPos firstBend = points.get(1);
        FloatPos secondBend = points.get(2);
        FloatPos lastBend = points.get(points.size() - 2);
        FloatPos beforeLastBend = points.get(points.size() - 3);
        if (!runsAlong(points.get(0), firstBend, prior.dirX, prior.dirY)
                || !runsPerpendicular(firstBend, secondBend, prior.dirX, prior.dirY)
                || !runsPerpendicular(beforeLastBend, lastBend, port.normalX(), port.normalY())) {
            return elbowFallback(anchor, new double[] {prior.dirX, prior.dirY}, port, viewport);
        }
        List<FloatPos> out = new ArrayList<>(points.size());
        out.add(anchor);
        addIfNotNear(out, jointOnLane(anchor, firstBend, prior.dirX, prior.dirY));
        // the interior between the two joints — frozen lanes, verbatim
        for (int i = 2; i <= points.size() - 3; i++) {
            addIfNotNear(out, points.get(i));
        }
        addIfNotNear(out, jointOnLane(end, lastBend, port.normalX(), port.normalY()));
        addIfNotNear(out, end);
        return out;
    }

    /** Whether the segment a→b runs along the given axis unit (zero length counts). */
    private static boolean runsAlong(FloatPos a, FloatPos b, double ux, double uy) {
        return Math.abs(ux) > 0.5 ? Math.abs(a.y() - b.y()) < epsilon : Math.abs(a.x() - b.x()) < epsilon;
    }

    /** Whether the segment a→b runs along the axis perpendicular to the given axis unit. */
    private static boolean runsPerpendicular(FloatPos a, FloatPos b, double ux, double uy) {
        return Math.abs(ux) > 0.5 ? Math.abs(a.x() - b.x()) < epsilon : Math.abs(a.y() - b.y()) < epsilon;
    }

    /**
     * The sliding end's joint on the lane through {@code bend}: the lane is
     * the frozen run that met the bend (perpendicular to the end's axis, so
     * the bend's lane coordinate survives while the end's own coordinate
     * slides).
     */
    private static FloatPos jointOnLane(FloatPos end, FloatPos bend, double ux, double uy) {
        return Math.abs(ux) > 0.5 ? new FloatPos(bend.x(), end.y()) : new FloatPos(end.x(), bend.y());
    }

    private static void addIfNotNear(List<FloatPos> out, FloatPos point) {
        if (!out.isEmpty() && near(point, out.get(out.size() - 1))) {
            return;
        }
        out.add(point);
    }

    /**
     * The never-blank fallback: a simple elbow under the frozen directions,
     * obstacles ignored. With a viewport the approach and drawn end clamp
     * onto its edge — every interior vertex is a coordinate mix of those and
     * the anchor, so clamping the ends keeps the whole elbow on-screen.
     */
    private List<FloatPos> elbowFallback(
            FloatPos anchor, double[] dir, AttachPointResolver.Port port, @Nullable FloatRect viewport) {
        FloatPos end = LeaderGridRouter.clamped(drawnEnd(port), viewport);
        boolean dirHorizontal = Math.abs(dir[0]) > 0.5;
        boolean normalHorizontal = Math.abs(port.normalX()) > 0.5;
        if (dirHorizontal != normalHorizontal) {
            FloatPos bend = dirHorizontal ? new FloatPos(end.x(), anchor.y()) : new FloatPos(anchor.x(), end.y());
            if (near(bend, anchor) || near(bend, end)) {
                return List.of(anchor, end);
            }
            return List.of(anchor, bend, end);
        }
        if (Math.abs(end.y() - anchor.y()) < epsilon && dirHorizontal) {
            return List.of(anchor, end);
        }
        if (Math.abs(end.x() - anchor.x()) < epsilon && !dirHorizontal) {
            return List.of(anchor, end);
        }
        FloatPos approach = LeaderGridRouter.clamped(
                new FloatPos(
                        end.x() + port.normalX() * config.routing().lastSegmentPx(),
                        end.y() + port.normalY() * config.routing().lastSegmentPx()),
                viewport);
        if (dirHorizontal) {
            FloatPos jog = new FloatPos(approach.x(), anchor.y());
            if (near(jog, anchor) || near(jog, approach)) {
                return dedupNear(anchor, approach, end);
            }
            return dedupNear(anchor, jog, approach, end);
        }
        FloatPos jog = new FloatPos(anchor.x(), approach.y());
        if (near(jog, anchor) || near(jog, approach)) {
            return dedupNear(anchor, approach, end);
        }
        return dedupNear(anchor, jog, approach, end);
    }

    /** The elbow vertices minus consecutive duplicates a clamped approach/end can fold into. */
    private static List<FloatPos> dedupNear(FloatPos... points) {
        List<FloatPos> out = new ArrayList<>(points.length);
        for (FloatPos point : points) {
            if (!out.isEmpty() && near(point, out.get(out.size() - 1))) {
                continue;
            }
            out.add(point);
        }
        return out;
    }

    private static boolean near(FloatPos a, FloatPos b) {
        return Math.abs(a.x() - b.x()) < epsilon && Math.abs(a.y() - b.y()) < epsilon;
    }

    private FloatPos drawnEnd(AttachPointResolver.Port port) {
        return new FloatPos(
                port.point().x() + port.normalX() * config.routing().arrivalGapPx(),
                port.point().y() + port.normalY() * config.routing().arrivalGapPx());
    }

    private FloatPos drawnEnd(Leader leader) {
        return drawnEnd(leader.port());
    }

    // endregion

    // region triggers

    /**
     * Trigger units per leader on the straight baselines: crossings + close
     * parallels + obstacle cuts, one unit per pair/event. Same-cluster pairs
     * are exempt (they meet at the trunk by design).
     */
    private Map<String, Integer> countTriggers(
            List<Leader> leaders,
            Map<String, List<Leader>> clusterGroups,
            List<FloatRect> obstacles,
            @Nullable FloatRect viewport) {
        Map<String, Integer> counts = new LinkedHashMap<>();
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
                FloatPos aPort = a.port().point();
                FloatPos bPort = b.port().point();
                FloatPos aAnchor = new FloatPos(a.anchorX(), a.anchorY());
                FloatPos bAnchor = new FloatPos(b.anchorX(), b.anchorY());
                boolean cross = segmentsCross(aAnchor, aPort, bAnchor, bPort);
                boolean close = !cross && segmentDistance(aAnchor, aPort, bAnchor, bPort) <= config.proximityPx();
                if (cross || close) {
                    counts.merge(a.id(), 1, Integer::sum);
                    counts.merge(b.id(), 1, Integer::sum);
                }
            }
        }
        for (Leader leader : leaders) {
            if (!obstacles.isEmpty()
                    && LeaderGridRouter.polylineBlocked(
                            List.of(
                                    new FloatPos(leader.anchorX(), leader.anchorY()),
                                    leader.port().point()),
                            leader.port().point(),
                            obstacles,
                            config.routing().clearancePx(),
                            viewport)) {
                counts.merge(leader.id(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Map<String, List<Leader>> groupClusters(List<Leader> leaders, Map<String, String> clusters) {
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

    private SwitchGate.Config gateConfig() {
        // the metric is the per-leader trigger count (crossings + close
        // parallels + obstacle cuts), so the deadband stays at zero: a single
        // crossing must be able to move the reference and open the gate
        return SwitchGate.Config.of(config.band(), config.dwellEpochs(), 0.0, 0);
    }

    // endregion

    // region zero-crossing pass

    /**
     * Every crossing po-leader, in caller order, gets one re-route attempt
     * with the polylines it crosses as additional thin obstacles; a candidate
     * replaces the current route only if it strictly reduces that leader's
     * crossings against the same set. Remaining crossings are reported, not
     * hidden.
     */
    private void reduceCrossings(
            List<Working> work,
            Map<String, List<Leader>> clusterGroups,
            List<FloatRect> obstacles,
            @Nullable FloatRect viewport) {
        boolean[][] crossing = crossingMatrix(work, clusterGroups);
        for (int i = 0; i < work.size(); i++) {
            Working w = work.get(i);
            if (w.style != Style.poLeader || w.points.size() < 2) {
                continue;
            }
            int before = 0;
            for (int j = 0; j < work.size(); j++) {
                if (crossing[i][j]) before++;
            }
            if (before == 0) {
                continue;
            }
            List<FloatRect> thin = new ArrayList<>(obstacles);
            for (int j = 0; j < work.size(); j++) {
                if (!crossing[i][j]) continue;
                for (int s = 0; s + 1 < work.get(j).points.size(); s++) {
                    thin.add(thinRect(
                            work.get(j).points.get(s), work.get(j).points.get(s + 1)));
                }
            }
            FloatPos anchor = new FloatPos(w.leader.anchorX(), w.leader.anchorY());
            Committed prior = committed.get(w.id);
            double dirX = prior != null ? prior.dirX : 1.0;
            double dirY = prior != null ? prior.dirY : 0.0;
            List<FloatPos> alt = LeaderGridRouter.route(
                    anchor,
                    dirX,
                    dirY,
                    w.leader.port().point(),
                    w.leader.port().normalX(),
                    w.leader.port().normalY(),
                    thin,
                    config.routing(),
                    viewport);
            if (alt == null) {
                continue;
            }
            int after = 0;
            for (int j = 0; j < work.size(); j++) {
                if (!crossing[i][j]) continue;
                if (polylinesCross(alt, work.get(j).points)) {
                    after++;
                }
            }
            if (after < before && detourWithinBudget(w.leader, w.points, alt)) {
                w.points = alt;
                if (prior != null) {
                    prior.points = alt;
                    prior.cost = pathLength(alt);
                    // the detour is a topology change: the settle must see it
                    prior.epoch = ++epochClock;
                    w.shapeEpoch = prior.epoch;
                }
            }
        }
    }

    /**
     * A reroute may not balloon: its added length stays within one direct
     * anchor→port manhattan distance — enough to buy a real lane change,
     * never a lap around the screen.
     */
    private boolean detourWithinBudget(LeaderRouter.Leader leader, List<FloatPos> current, List<FloatPos> rerouted) {
        double direct = Math.abs(leader.port().point().x() - leader.anchorX())
                + Math.abs(leader.port().point().y() - leader.anchorY());
        return pathLength(rerouted) - pathLength(current) <= direct;
    }

    private FloatRect thinRect(FloatPos a, FloatPos b) {
        double x0 = Math.min(a.x(), b.x()) - config.routing().clearancePx();
        double y0 = Math.min(a.y(), b.y()) - config.routing().clearancePx();
        double x1 = Math.max(a.x(), b.x()) + config.routing().clearancePx();
        double y1 = Math.max(a.y(), b.y()) + config.routing().clearancePx();
        return new FloatRect(x0, y0, x1 - x0, y1 - y0);
    }

    private boolean[][] crossingMatrix(List<Working> work, Map<String, List<Leader>> clusterGroups) {
        boolean[][] crossing = new boolean[work.size()][work.size()];
        for (int i = 0; i < work.size(); i++) {
            for (int j = i + 1; j < work.size(); j++) {
                if (work.get(i).points.size() < 2 || work.get(j).points.size() < 2) {
                    continue;
                }
                if (sameCluster(work.get(i).leader, work.get(j).leader, clusterGroups)) {
                    continue;
                }
                if (polylinesCross(work.get(i).points, work.get(j).points)) {
                    crossing[i][j] = true;
                    crossing[j][i] = true;
                }
            }
        }
        return crossing;
    }

    private int[] countCrossings(List<Working> work, Map<String, List<Leader>> clusterGroups) {
        boolean[][] crossing = crossingMatrix(work, clusterGroups);
        int[] counts = new int[work.size()];
        for (int i = 0; i < work.size(); i++) {
            for (int j = 0; j < work.size(); j++) {
                if (crossing[i][j]) counts[i]++;
            }
        }
        return counts;
    }

    // endregion

    // region segment math

    private static boolean polylinesCross(List<FloatPos> a, List<FloatPos> b) {
        for (int i = 0; i + 1 < a.size(); i++) {
            for (int j = 0; j + 1 < b.size(); j++) {
                if (segmentsCross(a.get(i), a.get(i + 1), b.get(j), b.get(j + 1))) {
                    return true;
                }
            }
        }
        return false;
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

    /** The minimum distance between two segments (0 when they cross or touch). */
    private static double segmentDistance(FloatPos a1, FloatPos a2, FloatPos b1, FloatPos b2) {
        if (segmentsCross(a1, a2, b1, b2) || segmentsTouch(a1, a2, b1, b2)) {
            return 0.0;
        }
        return Math.min(
                Math.min(pointSegmentDistance(a1, b1, b2), pointSegmentDistance(a2, b1, b2)),
                Math.min(pointSegmentDistance(b1, a1, a2), pointSegmentDistance(b2, a1, a2)));
    }

    private static boolean segmentsTouch(FloatPos a1, FloatPos a2, FloatPos b1, FloatPos b2) {
        return pointSegmentDistance(a1, b1, b2) < epsilon
                || pointSegmentDistance(a2, b1, b2) < epsilon
                || pointSegmentDistance(b1, a1, a2) < epsilon
                || pointSegmentDistance(b2, a1, a2) < epsilon;
    }

    private static double pointSegmentDistance(FloatPos p, FloatPos a, FloatPos b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < epsilon) {
            return distance(p, a);
        }
        double t = ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / lengthSquared;
        t = Math.max(0.0, Math.min(1.0, t));
        return distance(p, new FloatPos(a.x() + t * dx, a.y() + t * dy));
    }

    private static double cross(double ax, double ay, double bx, double by) {
        return ax * by - ay * bx;
    }

    private static double distance(FloatPos a, FloatPos b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double pathLength(List<FloatPos> points) {
        double length = 0;
        for (int i = 0; i + 1 < points.size(); i++) {
            length += distance(points.get(i), points.get(i + 1));
        }
        return length;
    }

    /** The drawn cost of a candidate route: length plus one bend penalty per interior vertex. */
    private double candidateCost(List<FloatPos> points) {
        return pathLength(points)
                + Math.max(0, points.size() - 2) * config.routing().bendPenaltyPx();
    }

    private static double[] axisUnit(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            return new double[] {Math.signum(dx) == 0 ? 1.0 : Math.signum(dx), 0.0};
        }
        return new double[] {0.0, Math.signum(dy)};
    }

    // endregion

    // region reuse key

    private String reuseKey(Leader leader, List<FloatRect> obstacles) {
        double q = config.anchorQuantPx();
        StringBuilder key = new StringBuilder(leader.id());
        appendQuant(key, leader.anchorX(), q);
        appendQuant(key, leader.anchorY(), q);
        appendQuant(key, leader.port().point().x(), q);
        appendQuant(key, leader.port().point().y(), q);
        key.append(leader.port().face());
        for (FloatRect rect : obstacles) {
            appendQuant(key, rect.x(), q);
            appendQuant(key, rect.y(), q);
            appendQuant(key, rect.width(), q);
            appendQuant(key, rect.height(), q);
        }
        return key.toString();
    }

    private static void appendQuant(StringBuilder key, double value, double cell) {
        key.append(Math.round(value / cell)).append(',');
    }

    // endregion
}
