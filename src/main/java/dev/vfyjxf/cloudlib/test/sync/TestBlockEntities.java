package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.registry.block.BlockEntityRegister;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TestBlockEntities {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);

    public static final DeferredBlock<TestBlock> testBlock = BLOCKS.register(
            "test_block",
            () -> new TestBlock(BlockBehaviour.Properties.of())
    );

    private static final BlockEntityRegister myRegister = new BlockEntityRegister(Constants.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlockEntity>> testBlockEntity =
            myRegister.create(
                    "test_block_entity",
                    TestBlockEntity::new,
                    testBlock.get()
            );

    private TestBlockEntities() {
    }
}
