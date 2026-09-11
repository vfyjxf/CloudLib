package dev.vfyjxf.cloudlib.test.sync;

import com.mojang.serialization.Codec;
import dev.vfyjxf.cloudlib.api.data.handle.Handle;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import dev.vfyjxf.cloudlib.blockentity.BasicSyncedBlockEntity;
import dev.vfyjxf.cloudlib.blockentity.Schema;
import dev.vfyjxf.cloudlib.test.TestRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Demonstration block entity using {@link BasicSyncedBlockEntity} with the schema + hook pattern.
 * <p>
 * Field definitions live once as static {@link Schema}s in a static inner container ({@link Network}),
 * instance-independent and reusable. The block entity creates instance-bound {@link Handle}s from
 * them via the React-hook-style {@code useSynced(schema)} factory, which implicitly wires both
 * serialization and sync. Sync changes are batched into one packet per dimension per tick by
 * {@link dev.vfyjxf.cloudlib.blockentity.BlockEntitySyncBatcher}.
 */
public class SyncedTestBlockEntity extends BasicSyncedBlockEntity {

    private static final class Network {
        static final Schema<Integer> count = Schema.of("count", 0, Codec.INT, UnaryFlowHandler.codecOf(ByteBufCodecs.INT));
        static final Schema<String> label = Schema.of("label", "", Codec.STRING, UnaryFlowHandler.codecOf(ByteBufCodecs.STRING_UTF8));
    }

    private final Handle<Integer> count = useSynced(Network.count);
    private final Handle<String> label = useSynced(Network.label);

    private long tick;

    public SyncedTestBlockEntity(BlockPos pos, BlockState state) {
        super(TestRegistry.testSyncedBlockEntity.get(), pos, state);
    }

    public Handle<Integer> count() {
        return count;
    }

    public Handle<String> label() {
        return label;
    }

    public static BlockEntityTicker<SyncedTestBlockEntity> ticker() {
        return (level, pos, state, be) -> {
            if (level.isClientSide) return;
            be.tick++;
            if (be.tick % 20 == 0) {
                be.count.set(be.count.get() + 1);
                be.label.set("tick " + be.tick);
            }
        };
    }
}
