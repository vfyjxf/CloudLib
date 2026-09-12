package dev.vfyjxf.nimbusprojection.api.section;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The entity-targeted counterpart of {@link SectionProvider} — collects
 * snapshots for every section of its type on an entity (entity
 * inventories, saddle/armor handlers, ...). Same rules: deterministic
 * order, index is the addressable suffix.
 */
public interface EntitySectionProvider<D extends SectionData> {

    SectionType<D> type();

    List<D> collect(Level level, Entity entity);
}
