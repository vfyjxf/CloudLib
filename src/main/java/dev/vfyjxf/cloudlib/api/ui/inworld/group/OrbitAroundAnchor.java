package dev.vfyjxf.cloudlib.api.ui.inworld.group;

import java.util.Objects;

/**
 * {@link GroupStrategy} for ring-slot arrangements (status icon rows around a
 * nameplate, riders around a vehicle): members take slots on concentric
 * {@code OrbitRing}s around the group anchor, assigned sticky-greedy by the
 * {@code SlotAssigner} under a per-epoch <em>recourse budget</em> — at most
 * {@link #recourseBudget} deliberate relocations per arrange call, incumbents
 * otherwise kept, which is what keeps churn low.
 * <p>
 * Capacity degradation ladder (§3.7): inner rings fill → spill into outer
 * rings (the {@link #maxRings} bound) → surplus members aggregate into the
 * representative (the member closest to the anchor, carrying the "+N" count)
 * → beyond {@link #maxAggregated} the surplus hides (and lingers out through
 * the coordinator's retract path).
 *
 * @param baseRadius the first ring's radius in gui pixels
 * @param radiusStep the radius growth per ring
 * @param slotArcLength the desired arc distance between neighboring slots
 * @param maxRings the ring-count bound (the expansion limit)
 * @param recourseBudget the per-epoch deliberate-move budget K
 * @param switchPenalty the flat cost charged to a slot move
 * @param incumbentDiscount the incumbent-slot hysteresis discount in
 *        {@code [0, 1)}
 * @param maxAggregated how many surplus members the representative absorbs
 *        before the rest hide
 */
public record OrbitAroundAnchor(
    double baseRadius,
    double radiusStep,
    double slotArcLength,
    int maxRings,
    int recourseBudget,
    double switchPenalty,
    double incumbentDiscount,
    int maxAggregated
) implements GroupStrategy {

    public OrbitAroundAnchor {
        requirePositive("baseRadius", baseRadius);
        requirePositive("radiusStep", radiusStep);
        requirePositive("slotArcLength", slotArcLength);
        if (maxRings < 1) {
            throw new IllegalArgumentException("maxRings must be at least 1: " + maxRings);
        }
        if (recourseBudget < 0) {
            throw new IllegalArgumentException("recourseBudget must not be negative: " + recourseBudget);
        }
        if (!Double.isFinite(switchPenalty) || switchPenalty < 0) {
            throw new IllegalArgumentException("switchPenalty must be finite and non-negative: " + switchPenalty);
        }
        if (!Double.isFinite(incumbentDiscount) || incumbentDiscount < 0 || incumbentDiscount >= 1) {
            throw new IllegalArgumentException("incumbentDiscount must be in [0, 1): " + incumbentDiscount);
        }
        if (maxAggregated < 0) {
            throw new IllegalArgumentException("maxAggregated must not be negative: " + maxAggregated);
        }
    }

    /**
     * The nameplate-baseline preset: 28 px base ring, 14 px steps, roughly
     * 44 px of arc per slot, 4 rings, a recourse budget of 2 and up to 8
     * aggregated members.
     */
    public static OrbitAroundAnchor of() {
        return new OrbitAroundAnchor(28.0, 14.0, 44.0, 4, 2, 20.0, 0.15, 8);
    }

    private static void requirePositive(String name, double value) {
        Objects.requireNonNull(name, "name");
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + value);
        }
    }
}
