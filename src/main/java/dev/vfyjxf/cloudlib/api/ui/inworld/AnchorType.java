package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.resources.ResourceLocation;

/**
 * Phantom-typed identity token of an {@link InworldAnchor} kind.
 * <p>
 * The generic parameter exists only at compile time — the runtime value is
 * just the id, so awkward anchor shapes (generic impls, package-private
 * classes, proxies) need no {@code Class} gymnastics. Equality is by id,
 * which is what the {@link AnchorCodecs} registry keys on.
 * <p>
 * Every anchor reports its token via {@link InworldAnchor#type()}; a codec
 * declares the token it handles via {@link AnchorCodec#type()}.
 */
public record AnchorType<A extends InworldAnchor>(ResourceLocation id) {

    public static final AnchorType<InworldAnchor.Block> block =
            new AnchorType<>(ResourceLocation.fromNamespaceAndPath("cloudlib", "block"));
    public static final AnchorType<InworldAnchor.Position> position =
            new AnchorType<>(ResourceLocation.fromNamespaceAndPath("cloudlib", "position"));
    public static final AnchorType<InworldAnchor.EntityTarget> entity =
            new AnchorType<>(ResourceLocation.fromNamespaceAndPath("cloudlib", "entity"));
    public static final AnchorType<InworldAnchor.Tracked> tracked =
            new AnchorType<>(ResourceLocation.fromNamespaceAndPath("cloudlib", "tracked"));

    /** A token for lookup only — use when the concrete anchor type is unknown. */
    public static AnchorType<?> of(ResourceLocation id) {
        return new AnchorType<>(id);
    }
}
