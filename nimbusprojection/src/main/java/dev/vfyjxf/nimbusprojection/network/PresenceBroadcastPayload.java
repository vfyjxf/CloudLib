package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.nimbusprojection.api.sync.PresenceInfo;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

/**
 * Server → client presence relay: one remote player's interaction state over
 * shared-domain keys. The client merges it into the presence view used for
 * ghost affordances (seeing where others are watching / engaged / dragging).
 */
public record PresenceBroadcastPayload(UUID playerId, List<PresenceReportPayload.Entry> entries, long sinceTick)
        implements ClientboundPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, PresenceBroadcastPayload> streamCodec =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC.cast(),
                    PresenceBroadcastPayload::playerId,
                    PresenceReportPayload.Entry.streamCodec.apply(ByteBufCodecs.list()),
                    PresenceBroadcastPayload::entries,
                    ByteBufCodecs.VAR_LONG.cast(),
                    PresenceBroadcastPayload::sinceTick,
                    PresenceBroadcastPayload::new);

    public static final Type<PresenceBroadcastPayload> type =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "presence_broadcast"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    @Override
    public void handle(IPayloadContext context, Player player) {
        InworldManager manager = InworldManager.instance();
        if (manager != null) {
            manager.onPresence(
                    playerId,
                    entries.stream()
                            .map(e -> new PresenceInfo(playerId, e.key(), e.kind(), sinceTick))
                            .toList());
        }
    }
}
