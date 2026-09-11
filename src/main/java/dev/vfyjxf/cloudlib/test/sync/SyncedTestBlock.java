package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.api.block.BasicEntityBlock;
import dev.vfyjxf.cloudlib.test.TestRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SyncedTestBlock extends BasicEntityBlock<SyncedTestBlockEntity> {

    public SyncedTestBlock() {
        //no menu info — this block has no menu; BasicEntityBlock.useWithoutItem null-checks it.
        super(TestRegistry.testSyncedBlockEntity, null, BlockBehaviour.Properties.of());
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (BlockEntityTicker<T>) SyncedTestBlockEntity.ticker();
    }
}
