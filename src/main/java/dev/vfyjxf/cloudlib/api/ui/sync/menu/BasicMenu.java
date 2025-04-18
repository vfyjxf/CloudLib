package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
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
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.api.ui.event.MenuEvent;
import dev.vfyjxf.cloudlib.network.CloudlibNetworkPayloads;
import dev.vfyjxf.cloudlib.network.payload.MenuDataReversedPacket;
import dev.vfyjxf.cloudlib.network.payload.MenuSyncDownstreamPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

/**
 * AN extension version of {@link AbstractContainerMenu} ,
 * which provider more convenient way to manage menu and its screen binding.
 * <p>
 * <b>submodules :</b>
 * <ul>
 *     <li> {@link Expose} - a structural way to define what data should be synchronized and how client send data to server.</li>
 * </ul>
 *
 * @param <P> the data provider type
 * @see MenuInfo
 * @see Expose
 * @see LayerExpose
 * @see ReversedOnly
 */
public abstract class BasicMenu<P>
        extends AbstractContainerMenu
        implements EventHandler<MenuEvent> {

    protected final Level level;
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
        this.level = player.level();
    }

    //region structure

    public void init() {
        exposeManagement.updateSnapshotState();
    }

    //endregion

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
    ) {
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

    protected final void sendPayloadToClient(ClientboundPayload payload) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(payload);
        } else {
            CloudlibNetworkPayloads.log.warn("Tried to send payload to client, but player is not a server player.");
        }
    }

    protected final void sendPayloadToServer(ServerboundPayload payload) {
        if (level.isClientSide) {
            PacketDistributor.sendToServer(payload);
        } else {
            CloudlibNetworkPayloads.log.warn("Tried to send payload to server, but player is not a client player.");
        }
    }

    @ApiStatus.Internal
    public void receiveFromServer(RegistryFriendlyByteBuf byteBuf) {
        exposeManagement.receiveFromServer(byteBuf);
    }

    @ApiStatus.Internal
    public void receiveFromClient(RegistryFriendlyByteBuf byteBuf) {
        exposeManagement.receiveFromClient(byteBuf);
    }

    @ApiStatus.Internal
    public void sendReveredDataToServer() {
        if (exposeManagement.anyToServer()) {
            sendPayloadToServer(new MenuDataReversedPacket(containerId, exposeManagement::writeToServer, registryAccess()));
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (exposeManagement.anyToClient()) {
            sendPayloadToClient(new MenuSyncDownstreamPacket(containerId, exposeManagement::writeDifferenceToClient, registryAccess()));
        }
    }

    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();

        if (exposeManagement.hasExpose()) {
            sendPayloadToClient(new MenuSyncDownstreamPacket(containerId, exposeManagement::writeAllToClient, registryAccess()));
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
