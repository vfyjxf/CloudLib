package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.nimbusprojection.api.Nimbus;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanel;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanelSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transport for {@code PanelChannel.sendToServer}: wraps the sender's panel
 * key around the operation payload.
 * <p>
 * The nested payload's codec comes from the channel type registry —
 * {@link #registerChannelType} — keyed by payload {@linkplain CustomPacketPayload.Type type}.
 * An unregistered type fails to encode loudly rather than dropping bytes.
 * <p>
 * Server-side routing: a key matching a live {@link SharedPanel} dispatches to
 * that panel's {@link SharedPanelSpec#channel} handler; otherwise a
 * {@link ServerboundPayload} handles itself (self-contained ops like world
 * drags carry their own validation); anything else is dropped.
 */
public record PanelChannelPayload(PanelKey key, CustomPacketPayload payload) implements ServerboundPayload {

    private static final Map<
                    CustomPacketPayload.Type<?>, StreamCodec<RegistryFriendlyByteBuf, ? extends CustomPacketPayload>>
            channelCodecs = new ConcurrentHashMap<>();

    /** Registers a payload type as channel-transmissible (its codec resolves the nested payload). */
    public static <T extends CustomPacketPayload> void registerChannelType(
            CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        channelCodecs.put(type, codec);
    }

    /**
     * Nested payload codec: {@code typeId + bytes}, both directions resolved
     * through the channel type registry. {@code Type} equality is by id, so
     * a freshly-read id wraps into a key that matches registrations.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, CustomPacketPayload> payloadCodec = StreamCodec.of(
            (buf, payload) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, payload.type().id());
                StreamCodec codec = channelCodecs.get(payload.type());
                if (codec == null) {
                    throw new IllegalArgumentException("No channel codec for payload type: "
                            + payload.type().id());
                }
                codec.encode(buf, payload);
            },
            buf -> {
                ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                CustomPacketPayload.Type<?> type = new CustomPacketPayload.Type<>(id);
                StreamCodec<RegistryFriendlyByteBuf, ? extends CustomPacketPayload> codec = channelCodecs.get(type);
                if (codec == null) {
                    throw new IllegalArgumentException("No channel codec for payload type: " + id);
                }
                return codec.decode(buf);
            });

    /** Nested-payload codec shared with the clientbound transport. */
    public static StreamCodec<RegistryFriendlyByteBuf, CustomPacketPayload> payloadCodec() {
        return payloadCodec;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PanelChannelPayload> streamCodec = StreamCodec.composite(
            PanelKey.streamCodec.cast(),
            PanelChannelPayload::key,
            payloadCodec,
            PanelChannelPayload::payload,
            PanelChannelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public static final CustomPacketPayload.Type<PanelChannelPayload> type =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "panel_channel"));

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        var server = Nimbus.server();
        if (server != null) {
            for (SharedPanel panel : server.shared()) {
                if (panel.key().equals(key)) {
                    SharedPanelSpec spec = panel.spec();
                    if (spec.channel() != null && spec.canInteract().test(player)) {
                        spec.channel().receive(panel, player, payload);
                    }
                    return;
                }
            }
        }
        if (payload instanceof ServerboundPayload self) {
            self.handle(context, player);
        }
    }
}
