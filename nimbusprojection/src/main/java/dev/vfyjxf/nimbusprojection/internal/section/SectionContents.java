package dev.vfyjxf.nimbusprojection.internal.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.network.SectionQueryPayload;
import dev.vfyjxf.nimbusprojection.network.SectionSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of a target's sections — the multi-kind counterpart
 * of CloudLib's {@code ContainerContents}. Vanilla blocks and entities
 * never replicate capability contents, so widgets read the cached
 * snapshot and {@link #watch} keeps the target subscribed on a slow
 * re-query while a panel is up. Everything stays server-authoritative —
 * this cache is display data only.
 */
public final class SectionContents {

    /** Ticks between re-queries for a watched target (~0.4s). */
    private static final int repollTicks = 8;

    private static final Map<SectionTarget, Map<String, SectionData>> cache = new ConcurrentHashMap<>();
    private static final Map<SectionTarget, Long> lastQuery = new ConcurrentHashMap<>();

    private SectionContents() {}

    /**
     * Keeps {@code target} subscribed: the first call and every
     * {@link #repollTicks} ticks after sends a fresh query. Cheap to call
     * every frame from the owning panel's render.
     */
    public static void watch(SectionTarget target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;
        long now = mc.level.getGameTime();
        Long last = lastQuery.get(target);
        if (last == null || now - last >= repollTicks) {
            lastQuery.put(target, now);
            PacketDistributor.sendToServer(new SectionQueryPayload(target));
        }
    }

    /** Latest snapshot of one section — null until the first reply lands. */
    public static @Nullable SectionData latest(SectionTarget target, String sectionId) {
        Map<String, SectionData> sections = cache.get(target);
        return sections != null ? sections.get(sectionId) : null;
    }

    /**
     * Drop the throttle so the next {@link #watch} re-queries immediately —
     * call after an op/drag commit so the just-mutated source refreshes now.
     */
    public static void invalidate(SectionTarget target) {
        lastQuery.remove(target);
    }

    /** Server snapshot arrived — store it. */
    public static void receive(SectionTarget target, List<SectionSnapshotPayload.Entry> entries) {
        Map<String, SectionData> sections = new HashMap<>();
        for (SectionSnapshotPayload.Entry entry : entries) {
            sections.put(entry.id(), entry.data());
        }
        cache.put(target, sections);
    }

    /** Forget everything — call on disconnect. */
    public static void clear() {
        cache.clear();
        lastQuery.clear();
    }
}
