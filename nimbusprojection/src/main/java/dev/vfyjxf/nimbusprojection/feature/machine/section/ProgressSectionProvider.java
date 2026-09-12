package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import java.util.List;

/**
 * The processing face — furnaces, smokers, blast furnaces and brewing
 * stands. Data comes from the menu-synced {@code ContainerData} accessor
 * (widened by AT): the same source vanilla GUIs read, so the numbers match
 * what the classic container screen would show.
 */
public final class ProgressSectionProvider implements SectionProvider<ProgressSectionData> {

    /** Vanilla brewing takes 400 ticks; one blaze powder brews 20 batches. */
    private static final int brewingTotal = 400;

    @Override
    public SectionType<ProgressSectionData> type() {
        return SectionTypes.progress;
    }

    @Override
    public List<ProgressSectionData> collect(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AbstractFurnaceBlockEntity furnace) {
            var data = furnace.dataAccess;
            return List.of(new ProgressSectionData(
                    data.get(AbstractFurnaceBlockEntity.DATA_COOKING_PROGRESS),
                    data.get(AbstractFurnaceBlockEntity.DATA_COOKING_TOTAL_TIME),
                    data.get(AbstractFurnaceBlockEntity.DATA_LIT_TIME),
                    data.get(AbstractFurnaceBlockEntity.DATA_LIT_DURATION)));
        }
        if (be instanceof BrewingStandBlockEntity stand) {
            var data = stand.dataAccess;
            int brewTime = data.get(BrewingStandBlockEntity.DATA_BREW_TIME);
            return List.of(new ProgressSectionData(
                    brewTime > 0 ? brewingTotal - brewTime : 0,
                    brewingTotal,
                    data.get(BrewingStandBlockEntity.DATA_FUEL_USES),
                    BrewingStandBlockEntity.FUEL_USES));
        }
        return List.of();
    }
}
