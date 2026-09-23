package dev.vfyjxf.cloudlib.ui.sync;

import dev.vfyjxf.cloudlib.network.payload.EntityContainerQueryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

/**
 * Client-side mirror of entity inventories — the entity twin of
 * {@link ContainerContents}. Server-only inventories (horse chests, modded
 * entity item handlers) are fetched via {@link EntityContainerQueryPayload}
 * and rendered from the cached reply.
 * <p>
 * {@link #watch(int)} is the read path: it returns the latest snapshot (null
 * while none has arrived) and keeps the entity on a slow re-query while the
 * panel is up. A watched entity that left the level or died is dropped from
 * the cache automatically — its snapshot can never outlive the entity.
 */
public final class EntityContainerContents {

    /** Ticks between re-queries for a watched entity (~0.4s). */
    private static final int repollTicks = 8;

    private static final Map<Integer, List<ItemStack>> cache = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> lastQuery = new ConcurrentHashMap<>();
    /** Bumped on every received snapshot — the cheap change signal widgets poll. */
    private static final Map<Integer, Long> revisions = new ConcurrentHashMap<>();

    private EntityContainerContents() {}

    /**
     * Latest snapshot for the entity (null = nothing received yet), and keeps
     * it subscribed: the first call and every {@link #repollTicks} ticks after
     * sends a fresh query. Entities no longer in the level are invalidated.
     * Cheap to call every tick.
     */
    public static @Nullable List<ItemStack> watch(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return null;
        Entity entity = mc.level.getEntity(entityId);
        if (entity == null || !entity.isAlive()) {
            forget(entityId);
            return null;
        }
        long now = mc.level.getGameTime();
        Long last = lastQuery.get(entityId);
        if (last == null || now - last >= repollTicks) {
            lastQuery.put(entityId, now);
            PacketDistributor.sendToServer(new EntityContainerQueryPayload(entityId));
        }
        return cache.get(entityId);
    }

    /** Slot count for layout before the first snapshot lands. */
    public static int slotsOf(int entityId, int fallback) {
        List<ItemStack> stacks = cache.get(entityId);
        return stacks != null ? stacks.size() : fallback;
    }

    /** A single slot from the latest snapshot, or empty. */
    public static ItemStack stackAt(int entityId, int slot) {
        List<ItemStack> stacks = cache.get(entityId);
        if (stacks == null || slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        return stacks.get(slot);
    }

    /**
     * How many snapshots have arrived for the entity (0 = none) — widgets
     * compare it against their last-seen value to refresh on change.
     */
    public static long revision(int entityId) {
        return revisions.getOrDefault(entityId, 0L);
    }

    /** Drops one entity's cache — an expired watch or an explicit refresh request. */
    public static void forget(int entityId) {
        cache.remove(entityId);
        lastQuery.remove(entityId);
        revisions.remove(entityId);
    }

    /** Server snapshot arrived — store it and bump the revision. */
    public static void receive(int entityId, List<ItemStack> stacks) {
        cache.put(entityId, List.copyOf(stacks));
        revisions.merge(entityId, 1L, Long::sum);
    }

    /** Drops every cached entity failing the predicate — the level-scoped liveness sweep. */
    static void retainIf(IntPredicate keep) {
        cache.keySet().removeIf(id -> !keep.test(id));
        lastQuery.keySet().removeIf(id -> !keep.test(id));
        revisions.keySet().removeIf(id -> !keep.test(id));
    }

    /** Forget everything — call on disconnect. */
    public static void clear() {
        cache.clear();
        lastQuery.clear();
        revisions.clear();
    }
}
