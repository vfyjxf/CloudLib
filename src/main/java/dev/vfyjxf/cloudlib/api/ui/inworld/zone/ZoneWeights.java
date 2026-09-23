package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

/**
 * The nine term weights of the unified cost function ({@link ZoneCost}).
 * Every normalized term lives on the same {@code [0, 1]} scale, so a weight
 * is directly comparable: weight 2 on {@code overlap} against weight 1 on
 * {@code anchor} means "a fully covered candidate is as bad as a candidate at
 * the screen-diagonal distance from its anchor". A zero weight disables the
 * term.
 * <p>
 * The defaults follow the structural-before-cosmetic ordering:
 * <ul>
 *   <li>{@code edge 4.0} — out-of-safe-rect is prohibitive (defensive; the
 *       candidate lattice pre-clamps, so this term is 0 on the happy path)</li>
 *   <li>{@code overlap 2.0} — covering an already-placed panel is the worst
 *       real outcome</li>
 *   <li>{@code hud 1.5} — covering a HUD exclusion is nearly as bad, but
 *       exclusions can be advisory</li>
 *   <li>{@code anchor 1.0} — the baseline proximity term every other weight
 *       is measured against</li>
 *   <li>{@code temporal 0.8} — frame-to-frame stability matters almost as
 *       much as proximity, but must not pin a panel to a bad spot</li>
 *   <li>{@code topology 0.7} — breaking a previous-frame neighbor relation is
 *       a visible shuffle, below hard constraints</li>
 *   <li>{@code attention 0.6} — keep the crosshair clear, but a collision at
 *       the crosshair is still worse than a clean center placement</li>
 *   <li>{@code crossing 0.5} — leader crossings are aesthetic noise</li>
 *   <li>{@code leader 0.4} — shorter leaders are preferred, weakly</li>
 * </ul>
 *
 * @param anchor proximity to the anchor
 * @param overlap overlap with already-placed rects
 * @param hud overlap with exclusion (HUD) rects
 * @param attention the center attention field's cost
 * @param edge out-of-safe-rect area (defensive)
 * @param leader estimated leader-line length
 * @param temporal distance from the previous frame's rect
 * @param crossing crossings with already-placed leader segments
 * @param topology broken previous-frame adjacency relations
 */
public record ZoneWeights(
    double anchor,
    double overlap,
    double hud,
    double attention,
    double edge,
    double leader,
    double temporal,
    double crossing,
    double topology
) {

    public ZoneWeights {
        requireNonNegative("anchor", anchor);
        requireNonNegative("overlap", overlap);
        requireNonNegative("hud", hud);
        requireNonNegative("attention", attention);
        requireNonNegative("edge", edge);
        requireNonNegative("leader", leader);
        requireNonNegative("temporal", temporal);
        requireNonNegative("crossing", crossing);
        requireNonNegative("topology", topology);
    }

    public static ZoneWeights defaults() {
        return new ZoneWeights(1.0, 2.0, 1.5, 0.6, 4.0, 0.4, 0.8, 0.5, 0.7);
    }

    public ZoneWeights withAnchor(double value) {
        return new ZoneWeights(value, overlap, hud, attention, edge, leader, temporal, crossing, topology);
    }

    public ZoneWeights withOverlap(double value) {
        return new ZoneWeights(anchor, value, hud, attention, edge, leader, temporal, crossing, topology);
    }

    public ZoneWeights withHud(double value) {
        return new ZoneWeights(anchor, overlap, value, attention, edge, leader, temporal, crossing, topology);
    }

    public ZoneWeights withAttention(double value) {
        return new ZoneWeights(anchor, overlap, hud, value, edge, leader, temporal, crossing, topology);
    }

    public ZoneWeights withEdge(double value) {
        return new ZoneWeights(anchor, overlap, hud, attention, value, leader, temporal, crossing, topology);
    }

    public ZoneWeights withLeader(double value) {
        return new ZoneWeights(anchor, overlap, hud, attention, edge, value, temporal, crossing, topology);
    }

    public ZoneWeights withTemporal(double value) {
        return new ZoneWeights(anchor, overlap, hud, attention, edge, leader, value, crossing, topology);
    }

    public ZoneWeights withCrossing(double value) {
        return new ZoneWeights(anchor, overlap, hud, attention, edge, leader, temporal, value, topology);
    }

    public ZoneWeights withTopology(double value) {
        return new ZoneWeights(anchor, overlap, hud, attention, edge, leader, temporal, crossing, value);
    }

    private static void requireNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
    }
}
