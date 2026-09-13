package dev.vfyjxf.nimbusprojection.feature.board;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The shared board's server-authoritative state — broadcast as the view's
 * spawn payload and re-pushed on every accepted op through
 * {@code SharedPanel.update}, so every watcher renders the same count.
 */
public record BoardPayload(int count, String lastBy) implements CustomPacketPayload {

    public static final Type<BoardPayload> type =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "board_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardPayload> streamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            BoardPayload::count,
            ByteBufCodecs.STRING_UTF8,
            BoardPayload::lastBy,
            BoardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }
}
