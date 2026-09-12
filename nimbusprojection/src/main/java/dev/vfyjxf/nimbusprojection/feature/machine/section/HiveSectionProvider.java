package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** The hive face — occupant count + honey level (the block state field). */
public final class HiveSectionProvider implements SectionProvider<HiveSectionData> {

    @Override
    public SectionType<HiveSectionData> type() {
        return SectionTypes.hive;
    }

    @Override
    public List<HiveSectionData> collect(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BeehiveBlockEntity hive)) return List.of();
        BlockState state = level.getBlockState(pos);
        int honey = state.hasProperty(BeehiveBlock.HONEY_LEVEL) ? state.getValue(BeehiveBlock.HONEY_LEVEL) : 0;
        return List.of(new HiveSectionData(hive.getOccupantCount(), BeehiveBlockEntity.MAX_OCCUPANTS, honey));
    }
}
