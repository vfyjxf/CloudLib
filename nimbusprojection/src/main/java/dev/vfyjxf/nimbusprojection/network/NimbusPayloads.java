package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ClientPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.nimbusprojection.Constants;
import dev.vfyjxf.nimbusprojection.feature.board.BoardOpPayload;
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
        ServerPayloadInfo.create(
                        PanelChannelPayload.streamCodec,
                        ResourceLocation.fromNamespaceAndPath(Constants.namespace, "panel_channel"))
                .registerPlay(registrar);
        ClientPayloadInfo.create(
                        ClientboundPanelPayload.streamCodec,
                        ResourceLocation.fromNamespaceAndPath(Constants.namespace, "panel_channel_s2c"))
                .registerPlay(registrar);
        ServerPayloadInfo.create(
                        PresenceReportPayload.streamCodec,
                        ResourceLocation.fromNamespaceAndPath(Constants.namespace, "presence_report"))
                .registerPlay(registrar);
        ClientPayloadInfo.create(
                        PresenceBroadcastPayload.streamCodec,
                        ResourceLocation.fromNamespaceAndPath(Constants.namespace, "presence_broadcast"))
                .registerPlay(registrar);
        ServerPayloadInfo.create(
                        ContainerOpsPayload.streamCodec,
                        ResourceLocation.fromNamespaceAndPath(Constants.namespace, "container_ops"))
                .registerPlay(registrar);
        ClientPayloadInfo.create(
                        SharedPanelSpawnPayload.streamCodec,
                        ResourceLocation.fromNamespaceAndPath(Constants.namespace, "shared_spawn"))
                .registerPlay(registrar);
        ClientPayloadInfo.create(
                        SharedPanelRemovePayload.streamCodec,
                        ResourceLocation.fromNamespaceAndPath(Constants.namespace, "shared_remove"))
                .registerPlay(registrar);
        ServerPayloadInfo.create(
                        TransferPayload.streamCodec,
                        ResourceLocation.fromNamespaceAndPath(Constants.namespace, "transfer"))
                .registerPlay(registrar);
        SectionQueryPayload.info.registerPlay(registrar);
        SectionSnapshotPayload.info.registerPlay(registrar);
        // payloads that travel nested inside a PanelChannel transport
        PanelChannelPayload.registerChannelType(WorldDragPayload.info.type(), WorldDragPayload.streamCodec);
        PanelChannelPayload.registerChannelType(ContainerOpsPayload.type, ContainerOpsPayload.streamCodec);
        PanelChannelPayload.registerChannelType(TransferPayload.type, TransferPayload.streamCodec);
        PanelChannelPayload.registerChannelType(BoardOpPayload.type, BoardOpPayload.streamCodec);
    }

    public static <T extends ServerboundPayload> ServerPayloadInfo<T> createServerInfo(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, String path) {
        return ServerPayloadInfo.create(codec, ResourceLocation.fromNamespaceAndPath(Constants.namespace, path));
    }

    public static <T extends ClientboundPayload> ClientPayloadInfo<T> createClientInfo(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, String path) {
        return ClientPayloadInfo.create(codec, ResourceLocation.fromNamespaceAndPath(Constants.namespace, path));
    }

    private NimbusPayloads() {}
}
