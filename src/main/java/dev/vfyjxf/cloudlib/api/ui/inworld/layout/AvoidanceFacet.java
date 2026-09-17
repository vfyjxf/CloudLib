package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;

import java.util.Objects;
import java.util.Set;

/**
 * The avoidance facet: who this element dodges, who it lets dodge it, and
 * whether it respects registered exclusion areas (G10). The pipeline's
 * AVOID stage drops candidate placements that intersect the avoided layers'
 * occupancy (or exclusion rectangles) before ranking; the {@code avoidedBy}
 * half is the declared counterpart — it constrains validation (a ghost is
 * dodged by no one) and feeds the adapters that build per-layer occupancy.
 *
 * @param avoids the layers whose occupants this element's candidates dodge
 * @param avoidedBy the layers whose occupants may treat this element as an
 *        obstacle; empty for ghosts (which occupy nothing)
 * @param respectsExclusions whether registered exclusion areas (the vanilla
 *        HUD provider, plugin providers) block this element's candidates
 */
public record AvoidanceFacet(Set<SpaceMask> avoids, Set<SpaceMask> avoidedBy, boolean respectsExclusions) {

    public AvoidanceFacet {
        Objects.requireNonNull(avoids, "avoids");
        Objects.requireNonNull(avoidedBy, "avoidedBy");
        avoids = Set.copyOf(avoids);
        avoidedBy = Set.copyOf(avoidedBy);
    }

    /** Dodges nothing, is dodged by nothing, ignores exclusions — the ghost posture. */
    public static AvoidanceFacet none() {
        return new AvoidanceFacet(Set.of(), Set.of(), false);
    }

    /** Dodges the given layers and respects exclusion areas; dodged by the same layers. */
    public static AvoidanceFacet of(Set<SpaceMask> layers) {
        return new AvoidanceFacet(layers, layers, true);
    }
}
