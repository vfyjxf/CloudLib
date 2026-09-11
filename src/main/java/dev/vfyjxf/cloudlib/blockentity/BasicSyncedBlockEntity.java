package dev.vfyjxf.cloudlib.blockentity;

import com.mojang.serialization.Codec;
import dev.vfyjxf.cloudlib.api.data.handle.DiffHandle;
import dev.vfyjxf.cloudlib.api.data.handle.Handle;
import dev.vfyjxf.cloudlib.api.data.serialize.Serialize;
import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import dev.vfyjxf.cloudlib.api.network.expose.DiffLayerExpose;
import dev.vfyjxf.cloudlib.api.network.expose.Expose;
import dev.vfyjxf.cloudlib.api.network.expose.LayerExpose;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity base holding state in {@link Handle}s, with serialization and sync wired on as
 * observers. Declare fields once as static {@link Schema}s, then materialize instance-bound handles
 * via the hook-style {@link #useSynced} (implicit serialize+sync binding):
 * <pre>{@code
 * class MyBE extends BasicSyncedBlockEntity {
 *     private static final class Network {
 *         static final Schema<Integer> COUNT = Schema.of("count", 0, Codec.INT, UnaryFlowHandler.codecOf(ByteBufCodecs.INT));
 *     }
 *     private final Handle<Integer> count = useSynced(Network.COUNT);
 * }
 * }</pre>
 * Sync flushing is automatic (one batched packet per dimension per tick). Save/load/update hooks
 * are pre-wired. For BEs that cannot extend this, compose {@link BlockEntitySerializer} +
 * {@link BlockEntitySync} and implement the two capability interfaces.
 */
public abstract class BasicSyncedBlockEntity extends BasicBlockEntity
        implements SerializableBlockEntity, SyncedBlockEntity {

    private final BlockEntitySerializer serializer = new BlockEntitySerializer();
    private final BlockEntitySync sync;

    protected BasicSyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.sync = new BlockEntitySync(this);
    }

    @Override
    public final BlockEntitySerializer serializer() {
        return serializer;
    }

    @Override
    public final BlockEntitySync sync() {
        return sync;
    }

    /** Materialize a handle from a {@link Schema}, implicitly wiring serialize + sync. */
    protected <T> Handle<T> useSynced(Schema<T> schema) {
        return useSynced(schema, schema.initial());
    }

    /** As above, overriding the schema's default initial value. */
    protected <T> Handle<T> useSynced(Schema<T> schema, T initial) {
        Handle<T> handle = Handle.of(initial);
        serializer.management().register(Serialize.create(schema.name(), handle, schema.nbtCodec()));
        sync.expose(schema.name(), handle, schema.netCodec());
        return handle;
    }

    /** Wire an existing handle into serialization only. */
    protected <T> Serialize<T> serialize(String name, Handle<T> handle, Codec<T> codec) {
        return serializer.management().register(Serialize.create(name, handle, codec));
    }

    /** Wire an existing handle into sync only (received values auto-apply to the handle). */
    protected <T> Expose<T> expose(String name, Handle<T> handle, UnaryFlowHandler<T> codec) {
        return sync.expose(name, handle, codec);
    }

    protected <T> Expose<T> expose(String name, Handle<T> handle, FlowEncoder<T> encoder, FlowDecoder<T> decoder) {
        return sync.expose(name, handle, encoder, decoder);
    }

    protected <T, E> LayerExpose<E> layerExpose(String name, Handle<T> handle, FlowHandler<T, E> codec) {
        return sync.layerExpose(name, handle, codec);
    }

    protected <T extends DiffObservable<D>, E, D> DiffLayerExpose<E, D> diffLayerExpose(
            String name, DiffHandle<T, D> handle, FlowHandler<T, E> codec, UnaryFlowHandler<D> diffCodec
    ) {
        return sync.diffLayerExpose(name, handle, codec, diffCodec);
    }

    @Override
    public void saveData(CompoundTag tag, HolderLookup.Provider registries) {
        serializer.saveData(tag, registries);
    }

    @Override
    public void loadData(CompoundTag tag, HolderLookup.Provider registries) {
        serializer.loadData(tag, registries);
    }

    @Override
    protected void writeUpdateData(CompoundTag data, HolderLookup.Provider registries) {
        sync.writeUpdateData(data, registries);
    }

    @Override
    protected boolean readUpdateData(CompoundTag data, HolderLookup.Provider registries) {
        return sync.readUpdateData(data, registries);
    }

    @Override
    public AbstractContainerMenu createMenu() {
        return null;
    }
}
