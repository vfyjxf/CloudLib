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

public class TestBlock extends BasicEntityBlock<TestBlockEntity> {

    public TestBlock() {
        super(TestRegistry.testBlockEntity, TestBlockEntity.Menu.INFO, BlockBehaviour.Properties.of());
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (BlockEntityTicker<T>) TestBlockEntity.ticker();
    }
}
