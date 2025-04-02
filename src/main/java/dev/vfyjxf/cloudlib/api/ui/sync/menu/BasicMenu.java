package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import dev.vfyjxf.cloudlib.api.data.EqualsChecker;
import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.ui.event.MenuEvent;
import dev.vfyjxf.cloudlib.api.ui.sync.accessor.ReferenceAccessor;
import dev.vfyjxf.cloudlib.api.ui.sync.accessor.TransformAccessor;
import dev.vfyjxf.cloudlib.api.ui.sync.accessor.ValueAccessor;
import dev.vfyjxf.cloudlib.api.ui.sync.accessor.ValueAccessorContainer;
import dev.vfyjxf.cloudlib.api.ui.sync.accessor.ValueObserver;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public abstract class BasicMenu<P> extends AbstractContainerMenu implements ValueObserver<P>, EventHandler<MenuEvent> {
    protected final P provider;
    protected final EventChannel<MenuEvent> eventChannel = EventChannel.create(this);
    protected final ValueAccessorContainer<P> accessorContainer = new ValueAccessorContainer<>(this);

    protected BasicMenu(MenuType<?> menuType, int containerId, P holder) {
        super(menuType, containerId);
        this.provider = holder;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
    }


    /**
     * Not implemented.
     * TODO: 支持更多类型的quick move
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public EventChannel<MenuEvent> events() {
        return eventChannel;
    }

    @Override
    public ValueAccessorContainer<P> accessors() {
        return accessorContainer;
    }

    protected <T, B extends ByteBuf> ValueAccessor<T> defineAccessor(
            StreamCodec<B, T> streamCodec,
            EqualsChecker<T> equalsChecker,
            String name,
            Function<P, T> getter
    ) {
        short usableId = accessorContainer.nextUsableId();
        return accessorContainer.add(new ReferenceAccessor<>(streamCodec, equalsChecker, usableId, name, () -> getter.apply(provider)));
    }

    protected <R, T, B extends ByteBuf> TransformAccessor<T> defineTransformAccessor(
            StreamCodec<B, T> streamCodec,
            EqualsChecker<R> rawEqualsChecker,
            EqualsChecker<T> targetEqualsChecker,
            String name,
            Function<P, R> getter,
            Function<R, T> transformer

    ) {
        return null;
    }

}
