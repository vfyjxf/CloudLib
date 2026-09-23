package dev.vfyjxf.cloudlib.ui.sync;

import dev.vfyjxf.cloudlib.network.payload.ContainerQueryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of container contents. Vanilla block inventories are
 * server-only — the client-side block entity capability always reads empty —
 * so widgets that need to <em>see</em> a container ask the server for a
 * snapshot via {@link ContainerQueryPayload} and render the cached reply.
 * <p>
 * {@link #watch(BlockPos)} is the read path: it returns the latest snapshot
 * (null while none has arrived) and keeps the position on a slow re-query so
 * contents stay fresh while the panel is up. Reads and drags stay
 * server-authoritative — the cache is display data only.
 */
public final class ContainerContents {

    /** Ticks between re-queries for a watched position (~0.4s). */
    private static final int repollTicks = 8;

    private static final Map<BlockPos, List<ItemStack>> cache = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> lastQuery = new ConcurrentHashMap<>();

    private ContainerContents() {}

    /**
     * Latest snapshot for the position (null = nothing received yet), and
     * keeps it subscribed: the first call and every {@link #repollTicks}
     * ticks after sends a fresh query. Cheap to call every frame.
     */
    public static @Nullable List<ItemStack> watch(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return null;
        long now = mc.level.getGameTime();
        Long last = lastQuery.get(pos.immutable());
        if (last == null || now - last >= repollTicks) {
            lastQuery.put(pos.immutable(), now);
            PacketDistributor.sendToServer(new ContainerQueryPayload(pos.immutable()));
        }
        return cache.get(pos);
    }

    /** Slot count for layout before the first snapshot lands. */
    public static int slotsOf(BlockPos pos, int fallback) {
        List<ItemStack> stacks = cache.get(pos);
        return stacks != null ? stacks.size() : fallback;
    }

    /** A single slot from the latest snapshot, or empty. */
    public static ItemStack stackAt(BlockPos pos, int slot) {
        List<ItemStack> stacks = cache.get(pos);
        if (stacks == null || slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        return stacks.get(slot);
    }

    /**
     * Drop the throttle so the next {@link #watch} re-queries immediately —
     * call after a drag commit so the just-mutated source refreshes now
     * rather than on the next repoll.
     */
    public static void invalidate(BlockPos pos) {
        lastQuery.remove(pos.immutable());
    }

    /** Server snapshot arrived — store it. */
    public static void receive(BlockPos pos, List<ItemStack> stacks) {
        cache.put(pos.immutable(), List.copyOf(stacks));
    }

    /** Forget everything — call on disconnect. */
    public static void clear() {
        cache.clear();
        lastQuery.clear();
    }
}
