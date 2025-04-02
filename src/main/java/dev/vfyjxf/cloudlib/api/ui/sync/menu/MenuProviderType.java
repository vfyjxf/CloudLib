package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface MenuProviderType<P> {

    static ResourceLocation createId(String id) {
        return ResourceLocation.fromNamespaceAndPath("provider_type", id);
    }

    ResourceLocation id();

    Class<P> typeToken();

    @Nullable
    <T> T findAccessor(P provider, Class<T> accessorType);

    void writeProvider(P provider, RegistryFriendlyByteBuf byteBuf);

    @Nullable
    P readProvider(Player player, RegistryFriendlyByteBuf byteBuf);

}
