package dev.vfyjxf.cloudlib.blockentity;

import dev.vfyjxf.cloudlib.network.payload.BlockEntitySyncPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-dimension accumulator that merges every changed block entity into a single sync packet per
 * tick. Block-entity handles mark themselves dirty here on change (via {@link BlockEntitySync});
 * the level-tick-end listener ({@code LevelTickEvent.Post}) calls {@link #flush}.
 */
public final class BlockEntitySyncBatcher {

    private static final ConcurrentHashMap<ResourceKey<Level>, BlockEntitySyncBatcher> BATCHERS = new ConcurrentHashMap<>();

    private final Set<BlockEntitySync> dirty = ConcurrentHashMap.newKeySet();

    public static BlockEntitySyncBatcher get(Level level) {
        return BATCHERS.computeIfAbsent(level.dimension(), k -> new BlockEntitySyncBatcher());
    }

    void markDirty(BlockEntitySync sync) {
        dirty.add(sync);
    }

    public void flush(ServerLevel level) {
        if (dirty.isEmpty()) return;
        var registries = level.registryAccess();
        List<BlockEntitySyncPacket.Entry> entries = new ArrayList<>(dirty.size());
        for (BlockEntitySync sync : dirty) {
            byte[] bytes = sync.collectDifference(registries);
            if (bytes != null) entries.add(new BlockEntitySyncPacket.Entry(sync.pos(), bytes));
        }
        dirty.clear();
        if (!entries.isEmpty()) {
            PacketDistributor.sendToPlayersInDimension(level, new BlockEntitySyncPacket(entries));
        }
    }
}
