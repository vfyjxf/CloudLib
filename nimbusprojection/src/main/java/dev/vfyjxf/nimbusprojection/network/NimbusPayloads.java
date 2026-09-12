package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.nimbusprojection.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NimbusPayloads {

    public static final Logger log = LoggerFactory.getLogger("NimbusNetwork");

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.modId);
        WorldDragPayload.info.registerPlay(registrar);
    }

    public static <T extends ServerboundPayload> ServerPayloadInfo<T> createServerInfo(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            String path
    ) {
        return ServerPayloadInfo.create(codec, ResourceLocation.fromNamespaceAndPath(Constants.namespace, path));
    }

    private NimbusPayloads() {
    }
}
