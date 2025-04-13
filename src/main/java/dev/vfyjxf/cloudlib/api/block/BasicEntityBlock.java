package dev.vfyjxf.cloudlib.api.block;

import dev.vfyjxf.cloudlib.api.ui.sync.menu.MenuInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

public abstract class BasicEntityBlock<T extends BlockEntity> extends Block implements EntityBlock {

    @Nullable
    @SuppressWarnings("rawtypes")
    private final MenuInfo menuInfo;
    private final DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> entityType;

    protected BasicEntityBlock(
            DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> entityType,
            @Nullable MenuInfo<?, ?> menuInfo,
            Properties properties
    ) {
        super(properties);
        this.entityType = entityType;
        this.menuInfo = menuInfo;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        //spectator mode use this method to open menu,but it doesn't provide a byteBuf to write custom data,so we don't support it.
        return null;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return entityType.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (menuInfo != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null && blockEntity.getType() == entityType.get()) {
                if (!level.isClientSide && shouldOpenMenu(blockEntity, level, pos, player, hitResult)) {
                    menuInfo.openMenu(player, menuInfo.fromBlock(blockEntity));
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    protected boolean shouldOpenMenu(BlockEntity blockEntity, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return true;
    }
}
