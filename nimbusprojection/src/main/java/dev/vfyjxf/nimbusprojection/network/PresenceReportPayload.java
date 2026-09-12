package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.nimbusprojection.api.sync.PresenceKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Client → server presence report: the sender's interaction state over
 * shared-domain panel keys (watching / engaged / dragging / tracing).
 * <p>
 * Sent when the set changes; the server relays a {@link PresenceBroadcastPayload}
 * to the other players in the same dimension and keeps the latest state for
 * join-sync. Presence is also the demand signal for sync tiers — a watching
 * player gets summaries, an engaged one live snapshots.
 */
public record PresenceReportPayload(List<Entry> entries) implements ServerboundPayload {

    public record Entry(PanelKey key, PresenceKind kind) {

        private static final StreamCodec<ByteBuf, PresenceKind> KIND_CODEC =
                ByteBufCodecs.VAR_INT.map(i -> PresenceKind.values()[i], PresenceKind::ordinal).cast();

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                PanelKey.STREAM_CODEC.cast(), Entry::key,
                KIND_CODEC.cast(), Entry::kind,
                Entry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PresenceReportPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(PresenceReportPayload::new, PresenceReportPayload::entries);

    public static final Type<PresenceReportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "presence_report"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        PresenceTracker.report(player, entries);
        //relay to the other players in the same dimension — they see this
        //player's operation state on the shared keys
        PacketDistributor.sendToPlayersInDimension(
                player.serverLevel(),
                new PresenceBroadcastPayload(player.getUUID(), entries, player.level().getGameTime()));
    }
}
