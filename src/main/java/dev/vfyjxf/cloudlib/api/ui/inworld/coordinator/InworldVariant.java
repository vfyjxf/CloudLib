package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;

import java.util.Objects;

/**
 * One rung of an element's {@link VariantLadder}: a concrete form the element
 * can take, combining how much space it asks for with how much information it
 * shows and how it behaves in space arbitration.
 *
 * @param level the ladder position this variant occupies; must equal its
 *        index in the ladder (0 = strongest form)
 * @param requestedSize the gui-pixel size this form wants to occupy; strictly
 *        non-increasing down a ladder
 * @param contentTier how much content this form renders; tiers never regress
 *        down a ladder
 * @param spacePolicy how this form participates in space resolution — the
 *        ladder can, for example, drop from {@code active} to {@code fixed}
 *        at its icon rung
 * @param allowsNudge whether the coordinator may shift the candidate rect out
 *        of an overlap (a small deterministic displacement) before rejecting
 * @param allowsClamp whether the coordinator may slide the candidate rect
 *        into the work area before rejecting it as out of bounds
 * @param minComfortableArea the smallest granted rect area, in px², this form
 *        is worth showing in; below it the coordinator rejects with
 *        {@link RejectionReason#insufficientArea}
 */
public record InworldVariant(
    int level,
    Size requestedSize,
    ContentTier contentTier,
    SpacePolicy spacePolicy,
    boolean allowsNudge,
    boolean allowsClamp,
    double minComfortableArea
) {

    public InworldVariant {
        Objects.requireNonNull(requestedSize, "requestedSize");
        Objects.requireNonNull(contentTier, "contentTier");
        Objects.requireNonNull(spacePolicy, "spacePolicy");
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative: " + level);
        }
        if (requestedSize.width() <= 0 || requestedSize.height() <= 0) {
            throw new IllegalArgumentException("requestedSize must be positive: " + requestedSize);
        }
        if (!Double.isFinite(minComfortableArea) || minComfortableArea < 0) {
            throw new IllegalArgumentException(
                "minComfortableArea must be finite and non-negative: " + minComfortableArea
            );
        }
    }

    /** The requested footprint's area in px². */
    public double requestedArea() {
        return (double) requestedSize.width() * requestedSize.height();
    }
}
