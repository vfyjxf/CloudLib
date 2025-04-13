package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import dev.vfyjxf.cloudlib.api.network.expose.Expose;
import dev.vfyjxf.cloudlib.api.network.expose.ExposeManagement;
import dev.vfyjxf.cloudlib.api.network.expose.LayerExpose;
import dev.vfyjxf.cloudlib.api.network.expose.ReversedOnly;
import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.ui.event.MenuEvent;
import dev.vfyjxf.cloudlib.network.payload.MenuSyncDownstreamPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public abstract class BasicMenu<P> extends AbstractContainerMenu implements EventHandler<MenuEvent> {
    protected final Player player;
    protected final Inventory playerInventory;
    protected final P provider;
    protected final EventChannel<MenuEvent> eventChannel = EventChannel.create(this);
    protected final ExposeManagement exposeManagement = new ExposeManagement();

    protected BasicMenu(MenuType<?> menuType, int containerId, P holder, Inventory inventory) {
        super(menuType, containerId);
        this.provider = holder;
        this.playerInventory = inventory;
        this.player = inventory.player;
    }

    //region utils

    protected RegistryAccess registryAccess() {
        return player.level().registryAccess();
    }

    //endregion

    //region expose factories

    protected <T> Expose<T> expose(
            String name,
            Snapshot<T> snapshot, Function<P, T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<T> decoder
    ) {
        Snapshot.init(snapshot, valueSupplier.apply(provider));
        return exposeManagement.registerExpose(
                Expose.create(
                        name, exposeManagement.nextId(),
                        snapshot, () -> valueSupplier.apply(provider),
                        encoder, decoder
                )
        );
    }

    protected <T> Expose<T> expose(
            String name,
            Snapshot<T> snapshot, Function<P, T> valueSupplier,
            UnaryFlowHandler<T> exposeCodec
    ) {
        Snapshot.init(snapshot, valueSupplier.apply(provider));
        return exposeManagement.registerExpose(
                Expose.create(
                        name, exposeManagement.nextId(),
                        snapshot, () -> valueSupplier.apply(provider),
                        exposeCodec
                )
        );
    }


    protected <T, E> LayerExpose<E> layerExpose(
            String name,
            Snapshot<T> snapshot, Function<P, T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder
    )
    {
        Snapshot.init(snapshot, valueSupplier.apply(provider));
        return exposeManagement.registerExpose(
                LayerExpose.create(
                        name, exposeManagement.nextId(),
                        snapshot, () -> valueSupplier.apply(provider),
                        encoder, decoder
                )
        );
    }

    protected <S, R> ReversedOnly<S, R> reversedOnly(
            String name,
            FlowEncoder<S> reversedEncoder,
            FlowDecoder<R> reversedDecoder
    ) {
        return exposeManagement.registerReversed(
                ReversedOnly.create(
                        name, exposeManagement.nextId(),
                        reversedEncoder, reversedDecoder
                )
        );
    }

    //endregion

    //region network & sync

    protected final void sendPacketToClient(ClientboundPayload packet) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(packet);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (exposeManagement.anyToClient()) {
            sendPacketToClient(new MenuSyncDownstreamPacket(containerId, exposeManagement::sendDifferenceToClient, registryAccess()));
        }
    }

    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();

        if (exposeManagement.hasExpose()) {
            sendPacketToClient(new MenuSyncDownstreamPacket(containerId, exposeManagement::sendAllToClient, registryAccess()));
        }
    }

    //endregion


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

}
