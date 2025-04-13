package dev.vfyjxf.cloudlib.api.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ServerPayloadInfo<T extends ServerboundPayload>(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
) {


    public static <T extends ServerboundPayload> ServerPayloadInfo<T> create(
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
            ResourceLocation id
    ) {
        return new ServerPayloadInfo<>(new CustomPacketPayload.Type<>(id), streamCodec);
    }

    public IPayloadHandler<T> handler() {
        return (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                payload.handle(context, player);
            } else throw new IllegalStateException("Serverbound payload can only be handled by a server player");
        };
    }

    public void registerPlay(PayloadRegistrar registrar) {
        registrar.playToServer(type, streamCodec, handler());
    }
}
