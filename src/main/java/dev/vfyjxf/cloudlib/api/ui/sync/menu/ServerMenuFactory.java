package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.function.Supplier;

public record ServerMenuFactory<M extends AbstractContainerMenu, A, P>(
        Supplier<Component> displayName,
        ServerMenuFactoryDelegate<M, A> menuFactory,
        MenuProviderType<P> providerType,
        P provider,
        boolean resetOnClose,
        Class<A> accessorType
) implements MenuProvider {

    public static final Supplier<Component> EMPTY_NAME = Component::empty;

    @Override
    @SuppressWarnings("ConstantConditions")
    public Component getDisplayName() {
        if (displayName == EMPTY_NAME && provider instanceof Nameable nameable) {
            return nameable.hasCustomName() ? nameable.getCustomName() : nameable.getDisplayName();
        }
        return displayName.get();
    }

    @Override
    public M createMenu(int containerId, Inventory inventory, Player player) {
        A accessor = providerType.findAccessor(provider, accessorType);
        if (accessor == null) {
            throw new IllegalStateException("Cannot find accessor for " + MenuInfo.PROVIDER_TYPES.inverse().get(providerType));
        }
        return menuFactory.create(containerId, inventory, accessor);
    }

    @Override
    public boolean shouldTriggerClientSideContainerClosingOnOpen() {
        return resetOnClose;
    }
}
