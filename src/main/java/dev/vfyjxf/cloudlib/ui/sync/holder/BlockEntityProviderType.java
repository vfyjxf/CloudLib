package dev.vfyjxf.cloudlib.ui.sync.holder;

import dev.vfyjxf.cloudlib.api.ui.sync.menu.MenuProviderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public enum BlockEntityProviderType implements MenuProviderType<BlockEntity> {
    INSTANCE;

    private static final ResourceLocation ID = MenuProviderType.createId("block_entity");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Class<BlockEntity> typeToken() {
        return BlockEntity.class;
    }

    @Override
    public <T> @Nullable T findAccessor(BlockEntity provider, Class<T> accessorType) {
        return accessorType.isInstance(provider) ? accessorType.cast(provider) : null;
    }

    @Override
    public void writeProvider(BlockEntity holder, RegistryFriendlyByteBuf byteBuf) {
        byteBuf.writeBlockPos(holder.getBlockPos());
    }

    @Override
    public @Nullable BlockEntity readProvider(Player player, RegistryFriendlyByteBuf byteBuf) {
        BlockPos pos = byteBuf.readBlockPos();
        if (player.level().isLoaded(pos)) {
            return player.level().getBlockEntity(pos);
        } else {
            throw new IllegalStateException("Can't open a menu for a block entity that doesn't loaded");
        }
    }
}
