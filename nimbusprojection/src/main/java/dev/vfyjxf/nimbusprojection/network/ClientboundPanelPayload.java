package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client panel message: routed to the target panel's declared
 * {@code channelHandler}. The nested payload resolves through the same
 * channel type registry as {@link PanelChannelPayload}.
 * <p>
 * Delivery is guaranteed; ordering is not — sequence-sensitive consumers
 * carry their own sequence numbers.
 */
public record ClientboundPanelPayload(PanelKey key, CustomPacketPayload payload) implements ClientboundPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPanelPayload> STREAM_CODEC = StreamCodec.composite(
            PanelKey.STREAM_CODEC.cast(),
            ClientboundPanelPayload::key,
            PanelChannelPayload.payloadCodec(),
            ClientboundPanelPayload::payload,
            ClientboundPanelPayload::new);

    public static final Type<ClientboundPanelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "panel_channel_s2c"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context, Player player) {
        InworldManager manager = InworldManager.instance();
        if (manager != null) manager.onChannelMessage(key, payload);
    }
}
