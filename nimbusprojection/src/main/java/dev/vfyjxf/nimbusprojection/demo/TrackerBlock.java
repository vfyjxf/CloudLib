package dev.vfyjxf.nimbusprojection.demo;

import dev.vfyjxf.cloudlib.api.block.BasicEntityBlock;
import dev.vfyjxf.nimbusprojection.demo.DemoRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TrackerBlock extends BasicEntityBlock<TrackerBlockEntity> {

    public TrackerBlock() {
        //no menu info — this block is controlled purely through its in-world panels
        super(DemoRegistry.trackerBlockEntity, null, BlockBehaviour.Properties.of());
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (BlockEntityTicker<T>) TrackerBlockEntity.ticker();
    }
}
