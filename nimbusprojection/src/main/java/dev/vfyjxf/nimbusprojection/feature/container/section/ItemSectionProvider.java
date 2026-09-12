package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The unsided {@code IItemHandler} — one section per block, mirroring the
 * player's intuitive view (sided pipe semantics stay out of scope).
 */
public final class ItemSectionProvider implements SectionProvider<ItemSectionData> {

    private static final int maxSlots = 512;

    @Override
    public SectionType<ItemSectionData> type() {
        return SectionTypes.item;
    }

    @Override
    public List<ItemSectionData> collect(Level level, BlockPos pos) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null) return List.of();
        int slots = Math.min(handler.getSlots(), maxSlots);
        List<ItemStack> stacks = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            stacks.add(handler.getStackInSlot(i).copy());
        }
        return List.of(new ItemSectionData(stacks));
    }
}
