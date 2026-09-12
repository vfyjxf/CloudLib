package dev.vfyjxf.nimbusprojection.api.section;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Server-authoritative data half of a container section — collects the
 * snapshots for every section of its type at a block position.
 * <p>
 * {@link #collect} runs on both sides: on the server it produces the data
 * that {@code SectionSnapshotPayload} ships to watchers; on the client it
 * determines panel structure (which sections exist) before the first
 * snapshot lands. Order in the returned list is the section's
 * server-addressable index — it must be deterministic.
 *
 * @param <D> the snapshot data this provider emits
 */
public interface SectionProvider<D extends SectionData> {

    /** The kind token — one provider per type. */
    SectionType<D> type();

    /**
     * One {@link SectionData} per section of this type at {@code pos}, in
     * deterministic order; empty when the capability is absent. Section id
     * is {@code type.id() + "/" + index}.
     */
    List<D> collect(Level level, BlockPos pos);
}
