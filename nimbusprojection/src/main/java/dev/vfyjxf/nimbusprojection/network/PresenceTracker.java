package dev.vfyjxf.nimbusprojection.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side store of the latest {@link PresenceReportPayload} each player
 * sent — kept so newly joining players can catch up on who is interacting
 * with which shared-domain panel, and so a disconnect can clear stale state.
 */
public final class PresenceTracker {

    private static final Map<UUID, List<PresenceReportPayload.Entry>> reports = new ConcurrentHashMap<>();

    private PresenceTracker() {}

    /** Records a player's latest report (empty list = fully disengaged). */
    public static void report(ServerPlayer player, List<PresenceReportPayload.Entry> entries) {
        if (entries.isEmpty()) {
            reports.remove(player.getUUID());
        } else {
            reports.put(player.getUUID(), entries);
        }
    }

    /** Drops a player's presence on disconnect and notifies the dimension. */
    public static void remove(MinecraftServer server, ServerPlayer player) {
        if (reports.remove(player.getUUID()) != null) {
            PacketDistributor.sendToPlayersInDimension(
                    player.serverLevel(),
                    new PresenceBroadcastPayload(
                            player.getUUID(), List.of(), player.level().getGameTime()));
        }
    }

    /** Join sync: sends the joining player every live report in its dimension. */
    public static void syncTo(MinecraftServer server, ServerPlayer joining) {
        long now = joining.level().getGameTime();
        for (var entry : reports.entrySet()) {
            if (entry.getKey().equals(joining.getUUID()) || entry.getValue().isEmpty()) continue;
            ServerPlayer source = server.getPlayerList().getPlayer(entry.getKey());
            if (source == null || source.level() != joining.level()) continue;
            PacketDistributor.sendToPlayer(
                    joining, new PresenceBroadcastPayload(entry.getKey(), entry.getValue(), now));
        }
    }
}
