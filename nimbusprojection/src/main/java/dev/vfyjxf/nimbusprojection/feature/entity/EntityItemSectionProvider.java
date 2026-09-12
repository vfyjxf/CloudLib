package dev.vfyjxf.nimbusprojection.feature.entity;

import dev.vfyjxf.nimbusprojection.api.section.EntitySectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.feature.container.section.ItemSectionData;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The entity {@code IItemHandler} — horses, llamas, chest boats, armored
 * mobs. Same {@link SectionTypes#item} data shape as block sections, so
 * the grid widget, ops payloads and drags all reuse the container stack.
 */
public final class EntityItemSectionProvider implements EntitySectionProvider<ItemSectionData> {

    private static final int maxSlots = 512;

    @Override
    public SectionType<ItemSectionData> type() {
        return SectionTypes.item;
    }

    @Override
    public List<ItemSectionData> collect(Level level, Entity entity) {
        IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler == null) return List.of();
        int slots = Math.min(handler.getSlots(), maxSlots);
        List<ItemStack> stacks = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            stacks.add(handler.getStackInSlot(i).copy());
        }
        return List.of(new ItemSectionData(stacks));
    }
}
