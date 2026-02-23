package dev.vfyjxf.cloudlib.api.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ClientPayloadInfo<T extends ClientboundPayload>(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
) {

    public static <T extends ClientboundPayload> ClientPayloadInfo<T> create(
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
            ResourceLocation id
    ) {

        return new ClientPayloadInfo<>(new CustomPacketPayload.Type<>(id), streamCodec);
    }

    public IPayloadHandler<T> handler() {
        return (payload, context) -> payload.handle(context, context.player());
    }

    public void registerPlay(PayloadRegistrar registrar) {
        registrar.playToClient(type(), streamCodec(), handler());
    }

}
