package dev.vfyjxf.cloudlib.api.ui.inworld.group;

import java.util.Objects;

/**
 * A group identity (§3.7): the tuple that says which elements belong together.
 * Two elements share a group when their {@link InworldGroup}s are equal —
 * same {@code namespace} (typically the mod id), same {@code kind} (the
 * semantic family: {@code "statusRow"}, {@code "riders"}, {@code "markers"}
 * …), same {@code discriminator} (the instance key: the entity id, the block
 * pos, the marker channel). The group itself carries no behavior: which
 * strategy arranges a group is declared separately (the layout side's
 * {@code GroupFacet}), so identity and arrangement stay orthogonal.
 *
 * @param namespace the owning namespace, non-empty (typically the mod id)
 * @param kind the semantic family name, non-empty
 * @param discriminator the instance discriminator, non-empty
 */
public record InworldGroup(String namespace, String kind, String discriminator) {

    public InworldGroup {
        requireNonEmpty(namespace, "namespace");
        requireNonEmpty(kind, "kind");
        requireNonEmpty(discriminator, "discriminator");
    }

    private static void requireNonEmpty(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }
}
