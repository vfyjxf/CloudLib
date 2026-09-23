package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
 *       alternatives never flap — and a crossing detour the zero-crossing
 *       pass bought is exempt from the cost rule entirely for as long as it
 *       stays valid and needed: its extra length is the crossing it avoids,
 *       which a shorter route cannot claim. It releases only through the
 *       pass itself, into a provably crossing-free route (see below).</li>
 *   <li><strong>Exit-side deadzone.</strong> The port's face cannot change
 *       at all until {@link AttachPointResolver}'s 55°/35° band and 48 px
 *       exit deadzone have committed the change — the router inherits the
 *       resolver's decision and re-keys on it.</li>
 * </ol>
 * <p>
 * <strong>Zero crossings are a hard constraint, not a preference.</strong>
 * After routing, actual-geometry crossings are counted pairwise; a crossing
 * po-leader is re-routed once with the polylines it crosses as additional
 * thin obstacles — but the whole pass is built not to oscillate:
 * <ul>
 *   <li>the attempt is dwelled: the crossing must have persisted
 *       {@link Config#dwellEpochs} consecutive epochs (counted from the
 *       straight baselines, so the dwell the style gate already served
 *       carries over) — a crossing that flickers in and out with camera
 *       motion buys no detour;</li>
 *   <li>the attempt measures its candidate against <em>every</em> other
 *       live polyline, not just the partners in the epoch's crossing row —
 *       an alt that dodges one leader by piercing another is not an
 *       improvement and is not adopted;</li>
 *   <li>at most one detour commits per epoch — the pass may not cascade
 *       through the field, re-keying partner after partner off geometry
 *       that its own earlier adoption invalidated;</li>
 *   <li>a bought detour is a commitment: the switch-cost rule may not undo
 *       it (see layer 2 above). It releases only after its need has lapsed
 *       {@link Config#dwellEpochs} consecutive epochs — no crossing, and no
 *       baseline crossing either — and only into a fresh route that crosses
 *       nothing live and still saves the switch cost; until then the leader
 *       keeps stretching the detour it bought.</li>
 * </ul>
 * Whatever crossings remain are reported in
 * {@link Route#crossings} — the caller trims (Nimbus's leader budget already
 * does).
 * <p>
 * <strong>Degeneracies — the near-distance tier ladder.</strong> A panel whose
 * port lies within {@link Config#tierFullPx} of its anchor does not get the
 * full routing: the frozen geometry of a po route (16 px stub + 20 px arrival
 * + 6 px gap = 42 px) already fills a short line, so a near leader steps down
 * a ladder whose rungs keep the pairing legible at every distance:
 * <ul>
 *   <li><strong>full</strong> (d ≥ {@link Config#tierFullPx}) — the routing
 *       above, untouched.</li>
 *   <li><strong>fold</strong> ({@link Config#tierFoldPx} ≤ d &lt; full) — one
 *       direct segment from the panel's nearest border candidate (4 edge
 *       midpoints + 4 corners) to the anchor, <em>both ends touching their
 *       target</em>: no stub, no arrival segment, no gap. A line more than
 *       60° off vertical leaves the anchor along a short vertical run first
 *       (vertical-first, like the full tier's stub). The segment fades in
 *       linearly, half ink at the fold threshold rising to full at the
 *       full threshold — never dimmer than half, the near end must stay
 *       legible.</li>
 *   <li><strong>attach</strong> (d &lt; {@link Config#tierFoldPx}) — no line
 *       at all (empty points, {@link Tier#attach}); the caller carries the
 *       pairing with marks instead, per the finding that connectivity is the
 *       strongest grouping cue and ink without connection is the worst
 *       combination.</li>
 * </ul>
 * Tier changes pass a {@link Config#tierHysteresisPx} Schmitt band (rising at
 * fold+hysteresis / full, falling at fold / full−hysteresis), so a boundary
 * that jitters cannot flap the form. A leader that never gets close draws one
 * whose final segment is exactly the routing config's {@code lastSegmentPx},
 * stopping {@code arrivalGapPx} short of the border. When the grid A* finds
 * no path — a sealed-in anchor, an overlapping obstacle field — a simple
 * elbow is drawn instead: never a blank frame.
 * <p>
 * One {@link #route} call is one decision epoch; determinism holds because
 * everything iterates the caller's leader list in order and the grid search
 * itself is tie-broken. No wall clock — the perimeter slide's dt is consumed
 * by the {@link AttachPointResolver} the caller drives.
 */
public final class LeaderRouter {

    /** The routing style a leader ended up with. */
    public enum Style {
        sLeader, poLeader, hyperLeader
    }

    /**
     * The near-distance tier a leader's form ended up in — orthogonal to
     * {@link Style}: the tier picks the <em>form</em> (full routing, fold
     * segment, attach marks), the style picks how a full-tier route was
     * produced. See the class docs for the ladder and its hysteresis.
     */
    public enum Tier {
        /** No line — the caller carries the pairing with panel-edge and anchor marks. */
        attach,
        /** One direct segment, both ends touching their target, alpha ramping in with distance. */
        fold,
        /** The full orthogonal/straight routing. */
        full
    }

    /**
     * The epoch-level knobs; the geometric routing knobs live in the nested
     * {@link LeaderGridRouter.Config}.
     *
     * @param band the style-switch hysteresis band, in trigger units
     *        (crossings + proximity pairs + obstacle hits); positive
     * @param dwellEpochs how many consecutive epochs a candidate style must
     *        survive before committing; at least 1
     * @param tierFoldPx the attach/fold boundary: port↔anchor distances below
     *        this draw no line; positive and below {@code tierFullPx}
     * @param tierFullPx the fold/full boundary: distances at or above this
     *        route fully
     * @param tierHysteresisPx the Schmitt band around both tier boundaries —
     *        a tier change must clear the boundary by this much; non-negative
     *        and small enough that the rising and falling bands cannot meet
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
        double tierFoldPx,
        double tierFullPx,
        double tierHysteresisPx,
        double anchorQuantPx,
        double switchCostPx,
        double proximityPx,
        LeaderGridRouter.Config routing
    ) {

        public Config {
            if (!Double.isFinite(band) || band <= 0) {
                throw new IllegalArgumentException("band must be finite and positive: " + band);
            }
            if (dwellEpochs < 1) {
                throw new IllegalArgumentException("dwellEpochs must be at least 1: " + dwellEpochs);
            }
            if (!Double.isFinite(tierFoldPx) || tierFoldPx <= 0) {
                throw new IllegalArgumentException("tierFoldPx must be finite and positive: " + tierFoldPx);
            }
            if (!Double.isFinite(tierFullPx) || tierFullPx <= tierFoldPx) {
                throw new IllegalArgumentException("tierFullPx must be finite and above tierFoldPx: " + tierFullPx);
            }
            if (!Double.isFinite(tierHysteresisPx) || tierHysteresisPx < 0) {
                throw new IllegalArgumentException(
                    "tierHysteresisPx must be finite and non-negative: " + tierHysteresisPx
                );
            }
            if (tierFullPx - tierFoldPx <= 2 * tierHysteresisPx) {
                throw new IllegalArgumentException(
                    "the tier bands must stay disjoint: tierFullPx - tierFoldPx > 2 * tierHysteresisPx"
                );
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

        /**
         * The survey defaults: {@code 1, 2} on the gate, the 42/84 px tier
         * ladder with an 8 px Schmitt band (42 px is the po form's frozen
         * geometry — stub + arrival + gap), {@code 12, 20, 6} + routing
         * defaults.
         */
        public static Config of(double band, int dwellEpochs) {
            return ofTiered(band, dwellEpochs, 42.0, 84.0, 8.0);
        }

        /** {@link #of} with the tier ladder's thresholds and Schmitt band. */
        public static Config ofTiered(
            double band,
            int dwellEpochs,
            double tierFoldPx,
            double tierFullPx,
            double tierHysteresisPx
        ) {
            return new Config(
                band,
                dwellEpochs,
                tierFoldPx,
                tierFullPx,
                tierHysteresisPx,
                12.0,
                20.0,
                6.0,
                LeaderGridRouter.Config.ofDefaults()
            );
        }
    }

    /**
     * One leader to route: the feature anchor and the label-side port the
     * {@link AttachPointResolver} resolved. Use {@link #toPoint} when no
     * panel rect is at hand — a bare label point with the arrival normal
     * inferred from the anchor's side. The {@code panel} rect, when present,
     * is what the fold tier's border candidates are picked from; without it
     * the fold segment lands on the port point itself.
     */
    public record Leader(
        String id,
        double anchorX,
        double anchorY,
        AttachPointResolver.Port port,
        @Nullable FloatRect panel
    ) {

        public Leader {
            if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY)) {
                throw new IllegalArgumentException("anchor must be finite: (" + anchorX + ", " + anchorY + ")");
            }
        }

        public Leader(String id, double anchorX, double anchorY, AttachPointResolver.Port port) {
            this(id, anchorX, anchorY, port, null);
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
                new AttachPointResolver.Port(face, new FloatPos(labelX, labelY), normalX, normalY),
                null
            );
        }
    }

    /**
     * One routed leader. {@code points} is the drawn polyline in order —
     * empty when the tier suppressed the line ({@link Tier#attach}).
     * {@code crossings} counts this polyline's crossings against the epoch's
     * other routed polylines after the zero-crossing pass; a positive count
     * is the caller's cue to trim. Cluster members carry their
     * {@code clusterId} and share the trunk point as their second vertex.
     * {@code tier} is the near-distance form and {@code alpha} its distance
     * fade (the fold tier's ramp; 1 for full, 0 for attach).
     *
     * @param shapeEpoch the topology token: it changes only when this
     *        leader's committed route is replaced (a fresh adoption, a
     *        crossing re-route), its style flips, or its tier form changes
     *        (a tier step, a fold candidate flip) — never when the same
     *        route merely stretches under a sliding endpoint. The caller
     *        keys its shape-settle animation off it, so the settle plays for
     *        real topology changes and endpoint slides stay unhitched
     * @param telemetry the epoch's routing debug surface — the trigger
     *        count that fed the style gate and the decision the router made
     *        for this leader this epoch. Carried on the route (not a side
     *        channel) so a recorded trace replays with its decisions intact
     */
    public record Route(
        String id,
        Style style,
        List<FloatPos> points,
        @Nullable String clusterId,
        int crossings,
        long shapeEpoch,
        Tier tier,
        double alpha,
        Telemetry telemetry
    ) {

        public Route {
            points = List.copyOf(points);
            if (tier == null) {
                throw new IllegalArgumentException("tier must not be null");
            }
            if (telemetry == null) {
                telemetry = new Telemetry(0, Decision.unchanged);
            }
        }
    }

    /**
     * What the router did for one leader on one epoch — the trace surface:
     * a re-routed line can be told apart from a merely stretched one without
     * diffing the polylines.
     */
    public enum Decision {
        /** No routing machinery ran: a straight, a fold, an attach, a hyper trunk. */
        unchanged,
        /** The committed route reused, its joints slid to the exact endpoints. */
        stretched,
        /** A fresh route replaced the commit (first route, invalid detour, or a cleared switch cost). */
        adopted,
        /** The zero-crossing pass bought a detour around the lines this leader crosses. */
        rerouted,
        /** A bought detour released back into a crossing-free direct route. */
        released
    }

    /** {@link Route}'s debug surface: the gate's trigger count and the epoch's {@link Decision}. */
    public record Telemetry(int trigger, Decision decision) {

        public Telemetry {
            if (decision == null) {
                decision = Decision.unchanged;
            }
        }
    }

    private static final double epsilon = 1.0e-9;

    private final Config config;
    private final Map<String, SwitchGate<Style>> gates = new HashMap<>();
    private final Map<String, Committed> committed = new HashMap<>();
    private final Map<String, Integer> crossingStreaks = new HashMap<>();
    private final Map<String, Tier> tiers = new HashMap<>();
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
        @Nullable FloatRect viewport
    ) {
        Map<String, Leader> byId = new LinkedHashMap<>();
        for (Leader leader : leaders) {
            if (byId.put(leader.id(), leader) != null) {
                throw new IllegalArgumentException("duplicate leader id: " + leader.id());
            }
        }

        Map<String, List<Leader>> clusterGroups = groupClusters(leaders, clusters);
        Triggers triggers = countTriggers(leaders, clusterGroups, obstacles, viewport);

        gates.keySet().retainAll(byId.keySet());
        committed.keySet().retainAll(byId.keySet());
        crossingStreaks.keySet().retainAll(byId.keySet());
        tiers.keySet().retainAll(byId.keySet());

        List<Working> work = new ArrayList<>(leaders.size());
        for (Leader leader : leaders) {
            String clusterId = clusters.get(leader.id());
            List<Leader> group = clusterId == null ? null : clusterGroups.get(clusterId);
            work.add(
                routeOne(
                    leader,
                    group,
                    clusterId,
                    Objects.requireNonNull(triggers.counts.get(leader.id()), "trigger count"),
                    obstacles,
                    viewport
                )
            );
        }
        reduceCrossings(work, clusterGroups, obstacles, viewport, triggers.baselineCrossing);
        int[] counts = countCrossings(work, clusterGroups);
        List<Route> routes = new ArrayList<>(work.size());
        for (int i = 0; i < work.size(); i++) {
            Working w = work.get(i);
            routes.add(
                new Route(
                    w.id,
                    w.style,
                    w.points,
                    w.clusterId,
                    counts[i],
                    w.shapeEpoch,
                    w.tier,
                    w.alpha,
                    new Telemetry(w.trigger, w.decision)
                )
            );
        }
        return routes;
    }

    /** Drops all per-leader state (a new scene, a teleport). */
    public void reset() {
        gates.clear();
        committed.clear();
        crossingStreaks.clear();
        tiers.clear();
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
        /**
         * True while this commit was bought by the zero-crossing pass: the
         * switch-cost rule may not undo it (its length is the crossing it
         * avoids) — only the pass's own release may, into a provably
         * crossing-free route.
         */
        boolean detour;
        /** Consecutive epochs this detour has not been needed (no routed and no baseline crossing). */
        int idle;

        Committed(String key, List<FloatPos> points) {
            this.key = key;
            this.points = points;
        }
    }

    private static final class Working {
        final String id;
        final @Nullable String clusterId;
        Style style;
        List<FloatPos> points = List.of();
        long shapeEpoch;
        Tier tier = Tier.full;
        double alpha = 1.0;
        int trigger;
        Decision decision = Decision.unchanged;
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
        @Nullable FloatRect viewport
    ) {
        Style desired = trigger > 0 ? Style.poLeader : Style.sLeader;
        SwitchGate<Style> gate = gates
                .computeIfAbsent(leader.id(), id -> new SwitchGate<>(gateConfig(), Style.sLeader, 0.0));
        gate.propose(desired, trigger);
        Style gated = gate.current();

        FloatPos anchor = new FloatPos(leader.anchorX(), leader.anchorY());
        FloatPos portPoint = leader.port().point();
        double distance = Math.hypot(portPoint.x() - anchor.x(), portPoint.y() - anchor.y());
        Working working = new Working(leader, group != null && group.size() >= 2 ? clusterId : null, gated);
        working.trigger = trigger;
        working.tier = tierOf(leader.id(), distance);
        tiers.put(leader.id(), working.tier);
        if (working.tier == Tier.attach) {
            working.alpha = 0.0;
            working.shapeEpoch = attachShapeEpoch;
            return working; // no line: the caller's marks carry the pairing
        }
        if (working.tier == Tier.fold) {
            FoldLine fold = foldRoute(anchor, leader.panel(), portPoint);
            working.alpha = foldAlpha(distance);
            working.points = fold.points();
            working.shapeEpoch = foldShapeEpoch(fold);
            return working;
        }
        working.alpha = 1.0;
        working.shapeEpoch = styleShapeEpoch(gated);

        if (working.clusterId != null) {
            FloatPos trunk = trunkOf(Objects.requireNonNull(group, "group"));
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
            viewport
        );
        if (gated == Style.sLeader && !straightBlocked) {
            working.points = List.of(anchor, LeaderGridRouter.clamped(drawnEnd(leader), viewport));
            return working;
        }

        working.style = Style.poLeader;
        orthogonalRoute(working, obstacles, viewport);
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

    /**
     * The fold form's shape token: the chosen border candidate and the
     * one-or-two segment shape both fold into the constant, so a candidate
     * flip (a discrete jump of the panel end) or a 60° branch flip reads as
     * a topology change while the endpoints slide freely under it. Candidate
     * slots 0–8 (0 = the bare-point port fallback, 1–8 the border
     * candidates) keep the values in {@code [-11, -4]} and {@code [-21, -13]},
     * clear of the attach token and the style tokens.
     */
    private static long foldShapeEpoch(FoldLine fold) {
        return -(4 + (fold.candidate() + 1) + (fold.points().size() == 3 ? 9 : 0));
    }

    /** The attach form's shape token — see {@link #foldShapeEpoch}. */
    private static final long attachShapeEpoch = -22L;

    /**
     * The epoch's tier under the Schmitt band: rising changes must clear
     * {@code boundary + hysteresis}, falling ones drop past
     * {@code boundary - hysteresis} — a distance that jitters around a
     * boundary cannot flap the form. A leader with no committed tier enters
     * at the plain boundaries.
     */
    private Tier tierOf(String id, double distance) {
        Tier current = tiers.getOrDefault(id, plainTier(distance));
        double foldOut = config.tierFoldPx();
        double foldIn = config.tierFoldPx() + config.tierHysteresisPx();
        double fullOut = config.tierFullPx() - config.tierHysteresisPx();
        double fullIn = config.tierFullPx();
        return switch (current) {
            case attach -> distance >= foldIn ? Tier.fold : Tier.attach;
            case fold -> distance >= fullIn ? Tier.full : distance < foldOut ? Tier.attach : Tier.fold;
            case full -> distance < fullOut ? Tier.fold : Tier.full;
        };
    }

    private Tier plainTier(double distance) {
        if (distance >= config.tierFullPx()) return Tier.full;
        if (distance >= config.tierFoldPx()) return Tier.fold;
        return Tier.attach;
    }

    /**
     * The fold tier's fade-in: half ink at the fold boundary rising linearly
     * to full at the full boundary — the near end stays legible (merely
     * dimmer than the full routing) instead of fading to nothing.
     */
    private double foldAlpha(double distance) {
        double span = config.tierFullPx() - config.tierFoldPx();
        double ramp = Math.max(0.0, Math.min(1.0, (distance - config.tierFoldPx()) / span));
        return 0.5 + 0.5 * ramp;
    }

    /**
     * The fold tier's connector, anchor-first: the panel's nearest border
     * candidate (4 edge midpoints then 4 corners, fixed order, first
     * strictly-nearest wins) joined to the anchor by one direct segment —
     * both ends land on their target, no stub, no arrival, no gap. A line
     * more than 60° off vertical instead leaves the anchor along a short
     * vertical run (half the vertical gap, the full stub's idiom) before
     * cutting diagonal, so a shallow pairing never grazes the anchor. A bare
     * label point (no panel rect) folds onto the port itself.
     */
    private static FoldLine foldRoute(FloatPos anchor, @Nullable FloatRect panel, FloatPos port) {
        FloatPos panelEnd = port;
        int candidate = -1;
        if (panel != null) {
            FloatPos[] border = borderCandidates(panel);
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < border.length; i++) {
                double d = Math.hypot(border[i].x() - anchor.x(), border[i].y() - anchor.y());
                if (d < best - epsilon) {
                    best = d;
                    panelEnd = border[i];
                    candidate = i;
                }
            }
        }
        double dx = panelEnd.x() - anchor.x();
        double dy = panelEnd.y() - anchor.y();
        double verticalRun = Math.abs(dy) * 0.5;
        boolean shallow = Math.atan2(Math.abs(dx), Math.abs(dy)) > Math.toRadians(60.0);
        if (shallow && verticalRun > 0.5) {
            FloatPos bend = new FloatPos(anchor.x(), anchor.y() + Math.signum(dy) * verticalRun);
            return new FoldLine(List.of(anchor, bend, panelEnd), candidate);
        }
        return new FoldLine(List.of(anchor, panelEnd), candidate);
    }

    /** The fold tier's border candidates: 4 edge midpoints then 4 corners, clockwise from top. */
    private static FloatPos[] borderCandidates(FloatRect rect) {
        double cx = rect.x() + rect.width() * 0.5;
        double cy = rect.y() + rect.height() * 0.5;
        double x0 = rect.x(), y0 = rect.y(), x1 = rect.right(), y1 = rect.bottom();
        return new FloatPos[]{new FloatPos(cx, y0), new FloatPos(x1, cy), new FloatPos(cx, y1), new FloatPos(x0, cy),
                new FloatPos(x0, y0), new FloatPos(x1, y0), new FloatPos(x1, y1), new FloatPos(x0, y1)};
    }

    /** {@link #foldRoute}'s answer: the polyline and the border candidate it landed on (−1 = the bare port). */
    private record FoldLine(List<FloatPos> points, int candidate) {}

    private void orthogonalRoute(Working w, List<FloatRect> obstacles, @Nullable FloatRect viewport) {
        Leader leader = w.leader;
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

        if (!exitChanged) {
            prior = Objects.requireNonNull(prior, "prior");
            if (prior.key.equals(key)) {
                List<FloatPos> stretched = stretch(prior.points, anchor, leader.port(), prior, viewport);
                if (!LeaderGridRouter
                        .polylineBlocked(stretched, portPoint, obstacles, config.routing().clearancePx(), viewport)) {
                    prior.points = stretched;
                    prior.cost = pathLength(stretched);
                    w.points = stretched;
                    w.decision = Decision.stretched;
                    return;
                }
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
        for (double[] candidate : new double[][]{primary, secondary}) {
            List<FloatPos> path = LeaderGridRouter.route(
                anchor,
                candidate[0],
                candidate[1],
                portPoint,
                leader.port().normalX(),
                leader.port().normalY(),
                obstacles,
                config.routing(),
                viewport
            );
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
            prior = Objects.requireNonNull(prior, "prior");
            List<FloatPos> stretched = stretch(prior.points, anchor, leader.port(), prior, viewport);
            boolean priorValid = !LeaderGridRouter
                    .polylineBlocked(stretched, portPoint, obstacles, config.routing().clearancePx(), viewport);
            if (priorValid && !exitChanged && prior.detour) {
                // a bought detour is a commitment: its extra length is the
                // crossing it avoids, which this shorter fresh route cannot
                // claim — the cost rule has no authority over it. It keeps
                // stretching until the zero-crossing pass releases it into
                // a provably crossing-free route (or it turns invalid, which
                // falls through to the normal rule below)
                prior.points = stretched;
                prior.cost = pathLength(stretched);
                w.points = stretched;
                w.decision = Decision.stretched;
                return;
            }
            double stretchedCost = pathLength(stretched);
            adopt = exitChanged || !priorValid || stretchedCost - pathLength(fresh) >= config.switchCostPx();
            if (!adopt) {
                prior.points = stretched;
                prior.cost = stretchedCost;
                w.points = stretched;
                w.decision = Decision.stretched;
                return;
            }
        }
        Committed next = new Committed(key, fresh);
        next.dirX = freshDir[0];
        next.dirY = freshDir[1];
        next.exitX = leader.port().normalX();
        next.exitY = leader.port().normalY();
        next.cost = pathLength(fresh);
        next.epoch = ++epochClock;
        committed.put(leader.id(), next);
        w.points = fresh;
        w.decision = Decision.adopted;
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
        @Nullable FloatRect viewport
    ) {
        FloatPos end = LeaderGridRouter.clamped(drawnEnd(port), viewport);
        if (points.size() <= 2) {
            return List.of(anchor, end);
        }
        if (points.size() == 3) {
            return elbowFallback(anchor, new double[]{prior.dirX, prior.dirY}, port, viewport);
        }
        FloatPos firstBend = points.get(1);
        FloatPos secondBend = points.get(2);
        FloatPos lastBend = points.get(points.size() - 2);
        FloatPos beforeLastBend = points.get(points.size() - 3);
        if (!runsAlong(points.get(0), firstBend, prior.dirX, prior.dirY)
                || !runsPerpendicular(firstBend, secondBend, prior.dirX, prior.dirY)
                || !runsPerpendicular(beforeLastBend, lastBend, port.normalX(), port.normalY())) {
            return elbowFallback(anchor, new double[]{prior.dirX, prior.dirY}, port, viewport);
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
        FloatPos anchor,
        double[] dir,
        AttachPointResolver.Port port,
        @Nullable FloatRect viewport
    ) {
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
                end.y() + port.normalY() * config.routing().lastSegmentPx()
            ),
            viewport
        );
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
            port.point().y() + port.normalY() * config.routing().arrivalGapPx()
        );
    }

    private FloatPos drawnEnd(Leader leader) {
        return drawnEnd(leader.port());
    }

    // endregion

    // region triggers

    /**
     * Trigger units per leader on the straight baselines: crossings + close
     * parallels + obstacle cuts, one unit per pair/event. Same-cluster pairs
     * are exempt (they meet at the trunk by design). The per-leader
     * {@code baselineCrossing} flag (a strict crossing pair, proximity
     * excluded) rides along — the zero-crossing pass counts its dwell from
     * it, so epochs served before the po upgrade carry over.
     */
    private Triggers countTriggers(
        List<Leader> leaders,
        Map<String, List<Leader>> clusterGroups,
        List<FloatRect> obstacles,
        @Nullable FloatRect viewport
    ) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Boolean> baselineCrossing = new LinkedHashMap<>();
        for (Leader leader : leaders) {
            counts.put(leader.id(), 0);
            baselineCrossing.put(leader.id(), false);
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
                if (cross) {
                    baselineCrossing.put(a.id(), true);
                    baselineCrossing.put(b.id(), true);
                }
                if (cross || close) {
                    counts.merge(a.id(), 1, Integer::sum);
                    counts.merge(b.id(), 1, Integer::sum);
                }
            }
        }
        for (Leader leader : leaders) {
            if (!obstacles.isEmpty()
                    && LeaderGridRouter.polylineBlocked(
                        List.of(new FloatPos(leader.anchorX(), leader.anchorY()), leader.port().point()),
                        leader.port().point(),
                        obstacles,
                        config.routing().clearancePx(),
                        viewport
                    )) {
                counts.merge(leader.id(), 1, Integer::sum);
            }
        }
        return new Triggers(counts, baselineCrossing);
    }

    /** {@link #countTriggers}'s answer: the trigger counts and the strict baseline-crossing flags. */
    private record Triggers(Map<String, Integer> counts, Map<String, Boolean> baselineCrossing) {}

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
     * The zero-crossing pass, built not to oscillate (the class docs list
     * the four guards). Two phases over the epoch's routed polylines:
     * <ol>
     *   <li><strong>Release.</strong> A committed detour whose need has
     *       lapsed — no routed crossing, no baseline crossing, for
     *       {@link Config#dwellEpochs} consecutive epochs — may return to a
     *       fresh route around the plain obstacles, but only one that
     *       crosses nothing live and saves the switch cost. This is the only
     *       way out of a bought detour short of invalidation.</li>
     *   <li><strong>Buy.</strong> A po-leader whose crossing survived the
     *       dwell gets one re-route attempt with its <em>live</em> crossing
     *       partners' polylines as additional thin obstacles; the candidate
     *       must cross strictly fewer of <em>all</em> the epoch's live
     *       polylines than the current route does — dodging one leader by
     *       piercing another is not an improvement. At most one buy commits
     *       per epoch, so the pass cannot cascade through the field.</li>
     * </ol>
     * Remaining crossings are reported, not hidden.
     */
    private void reduceCrossings(
        List<Working> work,
        Map<String, List<Leader>> clusterGroups,
        List<FloatRect> obstacles,
        @Nullable FloatRect viewport,
        Map<String, Boolean> baselineCrossing
    ) {
        boolean[][] crossing = crossingMatrix(work, clusterGroups);
        int[] crossings = new int[work.size()];
        for (int i = 0; i < work.size(); i++) {
            for (int j = 0; j < work.size(); j++) {
                if (crossing[i][j]) crossings[i]++;
            }
        }
        // the dwell streak: consecutive epochs this leader crossed another —
        // on its routed polyline or, before the po upgrade, on its baseline
        for (int i = 0; i < work.size(); i++) {
            Working w = work.get(i);
            boolean crossingNow = crossings[i] > 0 || baselineCrossing.getOrDefault(w.id, false);
            int streak = crossingNow ? crossingStreaks.getOrDefault(w.id, 0) + 1 : 0;
            crossingStreaks.put(w.id, streak);
        }

        for (int i = 0; i < work.size(); i++) {
            Working w = work.get(i);
            Committed prior = committed.get(w.id);
            if (prior == null || !prior.detour) {
                continue;
            }
            boolean needed = crossings[i] > 0 || baselineCrossing.getOrDefault(w.id, false);
            prior.idle = needed ? 0 : prior.idle + 1;
            if (needed || prior.idle < config.dwellEpochs()) {
                continue;
            }
            releaseDetour(w, prior, work, clusterGroups, obstacles, viewport);
        }

        boolean bought = false;
        for (int i = 0; i < work.size(); i++) {
            Working w = work.get(i);
            if (w.style != Style.poLeader || w.points.size() < 2) {
                continue;
            }
            int before = crossingsAgainstAll(w.points, work, i, clusterGroups);
            if (before == 0) {
                continue; // nothing live to dodge — a stale matrix row names ghosts
            }
            if (crossingStreaks.getOrDefault(w.id, 0) < config.dwellEpochs()) {
                continue;
            }
            if (bought) {
                continue; // one buy per epoch: the pass may not cascade
            }
            List<FloatRect> thin = new ArrayList<>(obstacles);
            for (int j = 0; j < work.size(); j++) {
                if (j == i || sameCluster(w.leader, work.get(j).leader, clusterGroups)) {
                    continue;
                }
                if (work.get(j).points.size() < 2 || !polylinesCross(w.points, work.get(j).points)) {
                    continue;
                }
                for (int s = 0; s + 1 < work.get(j).points.size(); s++) {
                    thin.add(thinRect(work.get(j).points.get(s), work.get(j).points.get(s + 1)));
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
                viewport
            );
            if (alt == null) {
                continue;
            }
            int after = crossingsAgainstAll(alt, work, i, clusterGroups);
            if (after >= before || alt.equals(w.points)) {
                // no real improvement against everyone — or the same polyline
                // back: committing it would only churn the settle
                continue;
            }
            if (!detourWithinBudget(w.leader, w.points, alt)) {
                continue;
            }
            w.points = alt;
            if (prior != null) {
                prior.points = alt;
                prior.cost = pathLength(alt);
                prior.detour = true;
                prior.idle = 0;
                // the detour is a topology change: the settle must see it
                prior.epoch = ++epochClock;
                w.shapeEpoch = prior.epoch;
            }
            w.decision = Decision.rerouted;
            bought = true;
        }
    }

    /**
     * The release: back to a fresh route around the plain obstacles, but
     * only one that crosses nothing live and saves the switch cost — the
     * guard that keeps a release from re-opening the crossing the detour
     * was bought for.
     */
    private void releaseDetour(
        Working w,
        Committed prior,
        List<Working> work,
        Map<String, List<Leader>> clusterGroups,
        List<FloatRect> obstacles,
        @Nullable FloatRect viewport
    ) {
        FloatPos anchor = new FloatPos(w.leader.anchorX(), w.leader.anchorY());
        List<FloatPos> direct = LeaderGridRouter.route(
            anchor,
            prior.dirX,
            prior.dirY,
            w.leader.port().point(),
            w.leader.port().normalX(),
            w.leader.port().normalY(),
            obstacles,
            config.routing(),
            viewport
        );
        if (direct == null) {
            return;
        }
        int self = work.indexOf(w);
        if (crossingsAgainstAll(direct, work, self, clusterGroups) > 0) {
            return; // the direct route would re-open a crossing — the detour stays
        }
        if (pathLength(w.points) - pathLength(direct) < config.switchCostPx()) {
            return; // not worth the settle
        }
        prior.points = direct;
        prior.cost = pathLength(direct);
        prior.detour = false;
        prior.idle = 0;
        prior.epoch = ++epochClock;
        w.points = direct;
        w.shapeEpoch = prior.epoch;
        w.decision = Decision.released;
    }

    /**
     * How many of the epoch's other live polylines {@code points} crosses —
     * the measure both the buy and the release compare against, so an alt
     * cannot "improve" by trading one victim for another.
     */
    private static int crossingsAgainstAll(
        List<FloatPos> points,
        List<Working> work,
        int self,
        Map<String, List<Leader>> clusterGroups
    ) {
        int count = 0;
        for (int j = 0; j < work.size(); j++) {
            if (j == self || work.get(j).points.size() < 2) {
                continue;
            }
            if (sameCluster(work.get(self).leader, work.get(j).leader, clusterGroups)) {
                continue;
            }
            if (polylinesCross(points, work.get(j).points)) {
                count++;
            }
        }
        return count;
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
        FloatPos a1,
        FloatPos a2,
        FloatPos b1,
        FloatPos b2,
        double... orientations
    ) {
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
            Math.min(pointSegmentDistance(b1, a1, a2), pointSegmentDistance(b2, a1, a2))
        );
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
        return pathLength(points) + Math.max(0, points.size() - 2) * config.routing().bendPenaltyPx();
    }

    private static double[] axisUnit(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            return new double[]{Math.signum(dx) == 0 ? 1.0 : Math.signum(dx), 0.0};
        }
        return new double[]{0.0, Math.signum(dy)};
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
