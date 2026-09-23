package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ContentTier;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldVariant;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.VariantLadder;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The degrade facet (G12): the element's {@link VariantLadder} — the ordered
 * forms it degrades through under rejection, index 0 the strongest. The
 * coordinator owns the walk down; this facet is the declaration of the
 * rungs.
 */
public record DegradeFacet(VariantLadder ladder) {

    public DegradeFacet {
        Objects.requireNonNull(ladder, "ladder");
    }

    /**
     * A ladder over the sizes with a shared policy and nudge/clamp flags;
     * content tiers walk the degradation order and the comfortable minimum
     * sits at 75% of each rung's area.
     */
    public static DegradeFacet of(SpacePolicy policy, boolean allowsNudge, boolean allowsClamp, Size... sizes) {
        if (sizes.length == 0) {
            throw new IllegalArgumentException("a ladder needs at least one rung");
        }
        ContentTier[] tiers = ContentTier.values();
        List<InworldVariant> rungs = new ArrayList<>(sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            Size size = sizes[i];
            rungs.add(
                new InworldVariant(
                    i,
                    size,
                    tiers[Math.min(i, tiers.length - 1)],
                    policy,
                    allowsNudge,
                    allowsClamp,
                    0.75 * size.width() * size.height()
                )
            );
        }
        return new DegradeFacet(VariantLadder.of(rungs));
    }

    /** An active ladder over the sizes: no nudge, clamping allowed. */
    public static DegradeFacet active(Size... sizes) {
        return of(SpacePolicy.active, false, true, sizes);
    }

    /** A fixed-policy ladder over the sizes: exemption at every rung. */
    public static DegradeFacet fixed(Size... sizes) {
        return of(SpacePolicy.fixed, false, true, sizes);
    }

    /** A ghost ladder over the sizes: participation in nothing. */
    public static DegradeFacet ghost(Size... sizes) {
        return of(SpacePolicy.ghost, false, true, sizes);
    }

    /** A ladder assembled from explicitly declared rungs. */
    public static DegradeFacet of(List<InworldVariant> rungs) {
        return new DegradeFacet(VariantLadder.of(rungs));
    }
}
