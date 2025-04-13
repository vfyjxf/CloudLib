package dev.vfyjxf.cloudlib.network;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.network.payload.ClientPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.network.payload.MenuReversedPacket;
import dev.vfyjxf.cloudlib.network.payload.MenuSyncDownstreamPacket;
import dev.vfyjxf.cloudlib.utils.Locations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CloudlibNetworkPayloads {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);
        //region play 2 client
        MenuSyncDownstreamPacket.INFO.registerPlay(registrar);
        MenuReversedPacket.INFO.registerPlay(registrar);
        //endregion

        //region play 2 server

        //endregion
    }


    public static <T extends ClientboundPayload> ClientPayloadInfo<T> createClientInfo(
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
            String path
    ) {
        return ClientPayloadInfo.create(streamCodec, Locations.of(path));
    }

    public static <T extends ServerboundPayload> ServerPayloadInfo<T> createServerInfo(
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
            String path
    ) {
        return ServerPayloadInfo.create(streamCodec, Locations.of(path));
    }
}
