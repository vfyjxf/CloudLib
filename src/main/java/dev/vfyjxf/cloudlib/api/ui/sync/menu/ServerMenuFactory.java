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

    public static final Supplier<Component> emptyName = Component::empty;

    @Override
    public Component getDisplayName() {
        if (displayName == emptyName && provider instanceof Nameable nameable) {
            Component customName = nameable.getCustomName();
            return customName != null ? customName : nameable.getDisplayName();
        }
        return displayName.get();
    }

    @Override
    public M createMenu(int containerId, Inventory inventory, Player player) {
        A accessor = providerType.findAccessor(provider, accessorType);
        if (accessor == null) {
            throw new IllegalStateException(
                "Cannot find accessor for " + MenuInfo.providerTypes.inverse().get(providerType)
            );
        }
        return menuFactory.create(containerId, inventory, accessor);
    }

    @Override
    public boolean shouldTriggerClientSideContainerClosingOnOpen() {
        return resetOnClose;
    }
}
