package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Serialization contract for one {@link InworldAnchor} implementation —
 * what makes an anchor kind network-transmissible.
 * <p>
 * Anchor kinds are registered on {@link AnchorCodecs}; a shared panel's
 * anchor must have a registered codec or sharing fails fast. The codec
 * writes only the anchor's own data — the surrounding transport carries
 * the dimension the anchor applies to.
 */
public interface AnchorCodec<A extends InworldAnchor> {

    /** The anchor kind's registry id, e.g. {@code "cloudlib:block"}. */
    ResourceLocation id();

    /** The anchor implementation this codec handles. */
    Class<A> anchorType();

    /** Anchor data ↔ buffer. */
    StreamCodec<? super RegistryFriendlyByteBuf, A> codec();

}
