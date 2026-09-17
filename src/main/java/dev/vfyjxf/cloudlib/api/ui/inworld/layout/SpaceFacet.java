package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;

import java.util.Objects;
import java.util.Set;

/**
 * The space facet: which space layers the element occupies, how it
 * participates in space resolution, and its arbitration precedence.
 *
 * @param layers the layers this element belongs to; collision with other
 *        elements is decided by layer intersection — non-empty
 * @param policy the participation policy ({@code fixed} defaults to
 *        occlusion-exempt per G9, {@code ghost} participates in nothing)
 * @param priority arbitration precedence within the same space kind:
 *        higher wins the contested spot
 */
public record SpaceFacet(Set<SpaceMask> layers, SpacePolicy policy, int priority) {

    public SpaceFacet {
        Objects.requireNonNull(layers, "layers");
        Objects.requireNonNull(policy, "policy");
        layers = Set.copyOf(layers);
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("an element occupies at least one space layer");
        }
    }

    /** Whether this element claims the world-anchored layer. */
    public boolean claimsWorldLayer() {
        return layers.contains(SpaceMask.worldAnchored);
    }

    /** Layers + policy + priority 0. */
    public static SpaceFacet of(Set<SpaceMask> layers, SpacePolicy policy) {
        return new SpaceFacet(layers, policy, 0);
    }
}
