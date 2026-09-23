package dev.vfyjxf.cloudlib.blockentity;

import dev.vfyjxf.cloudlib.api.data.handle.DiffHandle;
import dev.vfyjxf.cloudlib.api.data.handle.Handle;
import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import dev.vfyjxf.cloudlib.api.network.expose.DiffLayerExpose;
import dev.vfyjxf.cloudlib.api.network.expose.Expose;
import dev.vfyjxf.cloudlib.api.network.expose.ExposeManagement;
import dev.vfyjxf.cloudlib.api.network.expose.LayerExpose;
import dev.vfyjxf.cloudlib.api.network.expose.ReversedOnly;
import dev.vfyjxf.cloudlib.api.network.expose.UnaryReversed;
import dev.vfyjxf.cloudlib.network.payload.BlockEntityReversedPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.function.Consumer;

/**
 * Live Expose sync for a {@link BlockEntity}, reusing the host-agnostic {@link ExposeManagement}.
 * Independent of {@link BlockEntitySerializer}; both may observe the same handles without
 * interfering (sync clears the dirty flag after a flush, serialization calls {@code handle.load}).
 * <p>
 * Flushing is automatic: any change to an exposed handle marks this delegate dirty in the
 * per-dimension {@link BlockEntitySyncBatcher}, which sends one merged packet per tick. The vanilla
 * {@code getUpdateTag}/{@code getUpdatePacket} path uses {@link #writeUpdateData}/{@link #readUpdateData}.
 */
public final class BlockEntitySync {

    private static final String syncKey = "CloudLibExpose";

    private final BlockEntity owner;
    private final ExposeManagement management = new ExposeManagement();

    public BlockEntitySync(BlockEntity owner) {
        this.owner = owner;
    }

    public ExposeManagement management() {
        return management;
    }

    // region factories

    public <T> Expose<T> expose(String name, Handle<T> handle, UnaryFlowHandler<T> codec) {
        Expose<T> expose = management.registerExpose(Expose.create(name, management.nextId(), handle, codec));
        expose.whenReceive(handle::apply); // client: received values flow into the handle (no echo)
        handle.onChange(v -> markDirty()); // server: queue for the next batched flush
        return expose;
    }

    public <T> Expose<T> expose(String name, Handle<T> handle, FlowEncoder<T> encoder, FlowDecoder<T> decoder) {
        Expose<T> expose = management
                .registerExpose(Expose.create(name, management.nextId(), handle, encoder, decoder));
        expose.whenReceive(handle::apply);
        handle.onChange(v -> markDirty());
        return expose;
    }

    public <T, E> LayerExpose<E> layerExpose(String name, Handle<T> handle, FlowHandler<T, E> codec) {
        LayerExpose<E> layer = management.registerExpose(LayerExpose.create(name, management.nextId(), handle, codec));
        handle.onChange(v -> markDirty());
        return layer;
    }

    public <T extends DiffObservable<D>, E, D> DiffLayerExpose<E, D> diffLayerExpose(
        String name,
        DiffHandle<T, D> handle,
        FlowHandler<T, E> codec,
        UnaryFlowHandler<D> diffCodec
    ) {
        DiffLayerExpose<E, D> layer = management
                .registerExpose(DiffLayerExpose.create(name, management.nextId(), handle, codec, diffCodec));
        handle.onChange(v -> markDirty());
        return layer;
    }

    // endregion

    // region reversed (client → server)

    /**
     * A client → server channel for user actions (in-world buttons, drags, ...).
     * The client queues a value via {@link ReversedOnly#sendToServer}; the server
     * receives it through {@code whenReceiveFromClient} listeners.
     */
    public <S, R> ReversedOnly<S, R> reversedOnly(String name, FlowEncoder<S> encoder, FlowDecoder<R> decoder) {
        return management.registerReversed(ReversedOnly.create(name, management.nextId(), encoder, decoder));
    }

    public <S, R> ReversedOnly<S, R> reversedOnly(String name, FlowHandler<S, R> codec) {
        return management.registerReversed(ReversedOnly.create(name, management.nextId(), codec));
    }

    public <T> UnaryReversed<T> unaryReversed(String name, UnaryFlowHandler<T> codec) {
        return management.registerReversed(UnaryReversed.create(name, management.nextId(), codec));
    }

    /**
     * Flushes every queued reversed value to the server as one packet.
     * Client-side only; call after {@code sendToServer} writes, typically from a
     * UI event handler or at the end of a tick.
     */
    public void flushToServer() {
        Level level = owner.getLevel();
        if (level == null || !level.isClientSide) return;
        if (!management.anyToServer()) return;
        byte[] data = writeBytes(management::writeToServer, level.registryAccess());
        PacketDistributor.sendToServer(new BlockEntityReversedPacket(owner.getBlockPos(), data));
    }

    /** Server-side entry used by {@link BlockEntityReversedPacket}. */
    public void receiveFromClient(RegistryFriendlyByteBuf buf) {
        management.receiveFromClient(buf);
    }

    // endregion

    // region auto flush

    private void markDirty() {
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide) {
            BlockEntitySyncBatcher.get(level).markDirty(this);
        }
    }

    BlockPos pos() {
        return owner.getBlockPos();
    }

    /**
     * Serialize the difference stream if anything changed (clearing the dirty flag), else null.
     * Called by {@link BlockEntitySyncBatcher#flush}.
     */
    byte[] collectDifference(HolderLookup.Provider registries) {
        if (!management.anyToClient()) return null;
        return writeBytes(management::writeDifferenceToClient, registries);
    }

    // endregion

    // region client receive

    public void receiveFromServer(RegistryFriendlyByteBuf buf) {
        management.receiveFromServer(buf);
    }

    // endregion

    // region vanilla bootstrap (chunk load)

    public void writeUpdateData(CompoundTag data, HolderLookup.Provider registries) {
        // non-clearing: bootstrap may run mid-tick and must not consume an in-flight change
        data.putByteArray(syncKey, writeBytes(buf -> management.writeAllToClient(buf, false), registries));
    }

    public boolean readUpdateData(CompoundTag data, HolderLookup.Provider registries) {
        // tolerate both shapes: loadAdditional unwraps updateTag, onDataPacket passes the outer tag
        CompoundTag inner = data.contains(BasicBlockEntity.updateTagKey, Tag.TAG_COMPOUND)
                ? data.getCompound(BasicBlockEntity.updateTagKey)
                : data;
        if (!inner.contains(syncKey)) return false;
        byte[] bytes = inner.getByteArray(syncKey);
        if (bytes.length == 0) return false;
        management.receiveFromServer(
            new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), registryAccess(registries), ConnectionType.OTHER)
        );
        return true;
    }

    private static byte[] writeBytes(Consumer<RegistryFriendlyByteBuf> writer, HolderLookup.Provider registries) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(registries), ConnectionType.OTHER);
        writer.accept(buffer);
        byte[] result = new byte[buffer.readableBytes()];
        buffer.readBytes(result);
        return result;
    }

    private static RegistryAccess registryAccess(HolderLookup.Provider registries) {
        if (registries instanceof RegistryAccess access) return access;
        throw new IllegalStateException("BlockEntitySync requires a RegistryAccess provider, got: " + registries);
    }
}
