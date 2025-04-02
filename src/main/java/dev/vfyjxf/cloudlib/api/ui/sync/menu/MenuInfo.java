package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.ui.sync.accessor.ValueObserver;
import dev.vfyjxf.cloudlib.ui.sync.holder.BlockEntityProviderType;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.factory.BiMaps;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record MenuInfo<M extends AbstractContainerMenu & ValueObserver, A>(
        MenuType<M> menuType,
        MenuFactory<M, A> menuFactory,
        Supplier<ScreenFactory<M, ?>> screenFactory,
        Class<A> accessorType
) {

    static final Object2ObjectMap<ResourceLocation, MenuType<?>> typeToRegister = new Object2ObjectLinkedOpenHashMap<>();
    static final MutableBiMap<ResourceLocation, MenuProviderType<?>> PROVIDER_TYPES = BiMaps.mutable.empty();

    public <P> ServerMenuFactory<M, A, P> createProvider(
            Supplier<Component> displayName,
            MenuProviderType<P> providerType,
            P provider,
            boolean resetOnClose
    ) {
        return new ServerMenuFactory<>(
                displayName,
                ((id, playerInv, accessor) -> menuFactory.create(menuType, id, playerInv, accessor)),
                providerType,
                provider,
                resetOnClose,
                accessorType
        );
    }

    public ServerMenuFactory<M, A, BlockEntity> fromBlock(
            BlockEntity blockEntity,
            boolean resetOnClose
    ) {
        return createProvider(
                ServerMenuFactory.EMPTY_NAME,
                BlockEntityProviderType.INSTANCE,
                blockEntity,
                resetOnClose
        );
    }

    public ServerMenuFactory<M, A, BlockEntity> fromBlock(
            BlockEntity blockEntity
    ) {
        return fromBlock(blockEntity, true);
    }

    public <P> void openMenu(Player player, ServerMenuFactory<M, A, P> infoProvider) {
        player.openMenu(
                infoProvider,
                byteBuf -> {
                    MenuProviderType<P> type = infoProvider.providerType();
                    byteBuf.writeResourceLocation(type.id());
                    type.writeProvider(infoProvider.provider(), byteBuf);
                }
        );
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <M extends AbstractContainerMenu & ValueObserver, A, P, S extends Screen & MenuAccess<M>> MenuInfo<M, A> create(
            ResourceLocation menuTypeId,
            MenuFactory<M, A> menuFactory,
            Supplier<ScreenFactory<M, S>> screenFactory,
            A... typeCatch
    ) {
        AtomicReference<MenuType<M>> reference = new AtomicReference<>();
        //region client menu
        MenuType<M> menuType = IMenuTypeExtension.create((containerId, inv, data) -> {
            ResourceLocation typeId = data.readResourceLocation();
            MenuProviderType<P> providerType = (MenuProviderType<P>) PROVIDER_TYPES.get(typeId);
            if (providerType == null) {
                throw new IllegalStateException("No provider type found for " + typeId);
            }
            P provider = providerType.readProvider(inv.player, data);
            if (provider == null) {
                throw new IllegalStateException("Cannot find provider for " + typeId);
            }
            A accessor = providerType.findAccessor(provider, getAccessorType(typeCatch));
            if (accessor == null) {
                throw new IllegalStateException("Cannot find accessor for " + typeId);
            }
            return menuFactory.create(reference.get(), containerId, inv, accessor);
        });
        //endregion
        reference.set(menuType);
        typeToRegister.put(menuTypeId, menuType);
        return createInstance(menuType, menuFactory, screenFactory, getAccessorType(typeCatch));
    }

    @SafeVarargs
    public static <M extends AbstractContainerMenu & ValueObserver, A, S extends Screen & MenuAccess<M>> MenuInfo<M, A> create(
            MenuType<M> menuType,
            MenuFactory<M, A> menuFactory,
            Supplier<ScreenFactory<M, S>> screenFactory,
            A... typeCatch
    ) {
        return createInstance(menuType, menuFactory, screenFactory, getAccessorType(typeCatch));
    }

    public static <M extends AbstractContainerMenu & ValueObserver, A, S extends Screen & MenuAccess<M>> MenuInfo<M, A> create(
            MenuType<M> menuType,
            MenuFactory<M, A> menuFactory,
            Supplier<ScreenFactory<M, S>> screenFactory,
            Class<A> accessorType
    ) {
        return createInstance(menuType, menuFactory, screenFactory, accessorType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <M extends AbstractContainerMenu & ValueObserver, A, S extends Screen & MenuAccess<M>> MenuInfo<M, A> createInstance(
            MenuType<M> menuType,
            MenuFactory<M, A> menuFactory,
            Supplier<ScreenFactory<M, S>> screenFactory,
            Class<A> accessorType
    ) {

        return new MenuInfo<>(menuType, menuFactory, (Supplier) screenFactory, accessorType);
    }

    @SuppressWarnings("all")
    @Deprecated
    @ApiStatus.Internal
    public MenuInfo {

    }

    @SubscribeEvent
    private static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.MENU) {
            Registry<MenuType<?>> registry = event.getRegistry(Registries.MENU);
            if (registry == null) {
                throw new IllegalStateException("Menu registry is null");
            }
            typeToRegister.forEach((id, type) -> Registry.register(registry, id, type));
        }
        typeToRegister.clear();
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private static <T> Class<T> getAccessorType(T... typeToken) {
        if (typeToken.length != 0) {
            throw new IllegalArgumentException("Type token must be empty");
        }
        return (Class<T>) typeToken.getClass().getComponentType();
    }
}
