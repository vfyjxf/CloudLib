package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.ui.sync.holder.BlockEntityProviderType;
import dev.vfyjxf.cloudlib.utils.ClassUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.factory.BiMaps;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record MenuInfo<M extends BasicMenu<?>, A>(
        MenuType<M> menuType,
        MenuFactory<M, A> menuFactory,
        Supplier<ScreenFactory<M, ?>> screenFactory,
        Class<A> accessorType
) {
    private static final MutableList<MenuInfo<?, ?>> allInfos = Lists.mutable.empty();
    private static final ConcurrentHashMap<ResourceLocation, MenuType<?>> typeToRegister = new ConcurrentHashMap<>();
    static final MutableBiMap<ResourceLocation, MenuProviderType<?>> PROVIDER_TYPES = BiMaps.mutable.empty();

    static {
        PROVIDER_TYPES.put(
                BlockEntityProviderType.INSTANCE.id(),
                BlockEntityProviderType.INSTANCE
        );
    }

    public <P> ServerMenuFactory<M, A, P> createProvider(
            Supplier<Component> displayName,
            MenuProviderType<P> providerType,
            P provider,
            boolean resetOnClose
    ) {
        return new ServerMenuFactory<>(
                displayName,
                ((id, playerInv, accessor) -> menuFactory.createWithInit(menuType, id, playerInv, accessor)),
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
    //TODO:提供一套注解驱动的注册方法或者类似Block那类的注册方法，将声明区分开
    public static <M extends BasicMenu<?>, A, P, S extends Screen & MenuAccess<M>> MenuInfo<M, A> create(
            ResourceLocation menuTypeId,
            MenuFactory<M, A> menuFactory,
            Supplier<ScreenFactory<M, S>> screenFactory,
            A... typeCatch
    ) {
        AtomicReference<MenuType<M>> reference = new AtomicReference<>();
        //region client menu
        MenuType<M> menuType = IMenuTypeExtension.create((containerId, inv, data) -> {

            //region checks
            ResourceLocation typeId = data.readResourceLocation();
            MenuProviderType<P> providerType = (MenuProviderType<P>) PROVIDER_TYPES.get(typeId);
            if (providerType == null) throw new IllegalStateException("No provider type found for " + typeId);
            P provider = providerType.readProvider(inv.player, data);
            if (provider == null) throw new IllegalStateException("Cannot find provider for " + typeId);
            A accessor = providerType.findAccessor(provider, ClassUtils.getGenericType(typeCatch));
            if (accessor == null) throw new IllegalStateException("Cannot find accessor for " + typeId);
            //endregion

            return menuFactory.createWithInit(reference.get(), containerId, inv, accessor);
        });
        //endregion
        reference.set(menuType);
        typeToRegister.put(menuTypeId, menuType);
        return createInstance(menuType, menuFactory, screenFactory, ClassUtils.getGenericType(typeCatch));
    }

    @SafeVarargs
    public static <M extends BasicMenu<?>, A, S extends Screen & MenuAccess<M>> MenuInfo<M, A> create(
            MenuType<M> menuType,
            MenuFactory<M, A> menuFactory,
            Supplier<ScreenFactory<M, S>> screenFactory,
            A... typeCatch
    ) {
        return createInstance(menuType, menuFactory, screenFactory, ClassUtils.getGenericType(typeCatch));
    }

    public static <M extends BasicMenu<?>, A, S extends Screen & MenuAccess<M>> MenuInfo<M, A> create(
            MenuType<M> menuType,
            MenuFactory<M, A> menuFactory,
            Supplier<ScreenFactory<M, S>> screenFactory,
            Class<A> accessorType
    ) {
        return createInstance(menuType, menuFactory, screenFactory, accessorType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <M extends BasicMenu<?>, A, S extends Screen & MenuAccess<M>> MenuInfo<M, A> createInstance(
            MenuType<M> menuType,
            MenuFactory<M, A> menuFactory,
            Supplier<ScreenFactory<M, S>> screenFactory,
            Class<A> accessorType
    ) {

        MenuInfo menuInfo = new MenuInfo<>(menuType, menuFactory, (Supplier) screenFactory, accessorType);
        allInfos.add(menuInfo);
        return menuInfo;
    }

    @SuppressWarnings("all")
    @Deprecated
    @ApiStatus.Internal
    public MenuInfo {

    }

    @SubscribeEvent
    private static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() != Registries.MENU) return;
        Registry<MenuType<?>> registry = event.getRegistry(Registries.MENU);
        if (registry == null) {
            throw new IllegalStateException("Menu registry is null");
        }
        typeToRegister.forEach((id, type) -> Registry.register(registry, id, type));
        typeToRegister.clear();
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    private static class ClientListener {
        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void registerMenuScreen(RegisterMenuScreensEvent event) {
            for (MenuInfo info : allInfos) {
                if (info.screenFactory != null) {
                    registerMenuScreenHelper(event, info, info.screenFactory);
                }
            }
        }

        private static <M extends BasicMenu<?>, S extends Screen & MenuAccess<M>> void registerMenuScreenHelper(
                RegisterMenuScreensEvent event,
                MenuInfo<M, ?> menuInfo,
                Supplier<ScreenFactory<M, S>> screenFactorySupplier
        ) {
            MenuType<M> menuType = menuInfo.menuType;
            //compiler can't infer the type if we put the lambda in the position of constructor
            MenuScreens.ScreenConstructor<M, S> constructor = (menu, inventory, title) ->
                    screenFactorySupplier.get().createScreen(menu, inventory);
            event.register(
                    menuType,
                    constructor
            );
        }
    }
}
