package dev.vfyjxf.nimbusprojection.feature.board;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A client → server op on a shared board — a <em>request</em>, never a
 * fact: the channel handler re-validates and the result flows back through
 * {@link BoardPayload} updates.
 */
public record BoardOpPayload(int delta) implements CustomPacketPayload {

    public static final Type<BoardOpPayload> type =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "board_op"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardOpPayload> streamCodec =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, BoardOpPayload::delta, BoardOpPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }
}
