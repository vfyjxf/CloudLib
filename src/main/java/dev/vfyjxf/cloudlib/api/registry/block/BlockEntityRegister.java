package dev.vfyjxf.cloudlib.api.registry.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.concurrent.atomic.AtomicReference;

public class BlockEntityRegister {

    private final DeferredRegister<BlockEntityType<?>> register;

    public BlockEntityRegister(String namespace) {
        this.register = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, namespace);
    }

    public <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> create(
            String id,
            BlockEntityFactory<T> factory,
            Block... blocks
    ) {
        return register.register(
                id,
                () -> {
                    AtomicReference<BlockEntityType<T>> typeReference = new AtomicReference<>();
                    @SuppressWarnings("ConstantConditions")
                    BlockEntityType<T> blockEntityType = BlockEntityType.Builder.of(
                            (pos, state) -> factory.create(typeReference.get(), pos, state),
                            blocks
                    ).build(null);
                    typeReference.setPlain(blockEntityType);
                    return blockEntityType;
                }
        );
    }

    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockEntityType<T> type, BlockPos pos, BlockState state);
    }
}
