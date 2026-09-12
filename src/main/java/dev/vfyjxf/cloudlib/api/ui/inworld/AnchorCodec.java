package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Serialization contract for one {@link InworldAnchor} kind — what makes it
 * network-transmissible.
 * <p>
 * Anchor kinds are registered on {@link AnchorCodecs} by their
 * {@link AnchorType} token; a shared panel's anchor must have a registered
 * codec or sharing fails fast. The codec writes only the anchor's own
 * data — the surrounding transport carries the dimension it applies to.
 */
public interface AnchorCodec<A extends InworldAnchor> {

    /** The kind token this codec handles. */
    AnchorType<A> type();

    /** Anchor data ↔ buffer. */
    StreamCodec<? super RegistryFriendlyByteBuf, A> codec();
}
