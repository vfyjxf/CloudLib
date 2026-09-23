package dev.vfyjxf.cloudlib.test.sync;

import com.mojang.serialization.Codec;
import dev.vfyjxf.cloudlib.api.data.handle.Handle;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import dev.vfyjxf.cloudlib.api.network.expose.UnaryReversed;
import dev.vfyjxf.cloudlib.blockentity.BasicSyncedBlockEntity;
import dev.vfyjxf.cloudlib.blockentity.BlockEntitySyncBatcher;
import dev.vfyjxf.cloudlib.blockentity.Schema;
import dev.vfyjxf.cloudlib.test.TestRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration block entity using {@link BasicSyncedBlockEntity} with the schema + hook pattern.
 * <p>
 * Field definitions live once as static {@link Schema}s in a static inner container ({@link Network}),
 * instance-independent and reusable. The block entity creates instance-bound {@link Handle}s from
 * them via the React-hook-style {@code useSynced(schema)} factory, which implicitly wires both
 * serialization and sync. Sync changes are batched into one packet per dimension per tick by
 * {@link BlockEntitySyncBatcher}.
 * <p>
 * {@link #action} is a client → server reversed channel used by the in-world UI
 * panel: buttons queue an action id on the client, the server validates and
 * applies it here.
 */
public class SyncedTestBlockEntity extends BasicSyncedBlockEntity {

    public static final int actionIncrement = 0;
    public static final int actionDecrement = 1;
    public static final int actionReset = 2;
    public static final int actionPickItem = 3;

    private static final Item[] itemPool = {Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
            Items.REDSTONE, Items.ENDER_PEARL, Items.NETHERITE_SCRAP, Items.COPPER_INGOT};

    private static final class Network {
        static final Schema<Integer> count = Schema
                .of("count", 0, Codec.INT, UnaryFlowHandler.codecOf(ByteBufCodecs.INT));
        static final Schema<String> label = Schema
                .of("label", "", Codec.STRING, UnaryFlowHandler.codecOf(ByteBufCodecs.STRING_UTF8));
        static final Schema<Boolean> active = Schema
                .of("active", true, Codec.BOOL, UnaryFlowHandler.codecOf(ByteBufCodecs.BOOL));
        static final Schema<List<ItemStack>> items = Schema.of(
            "items",
            List.of(),
            ItemStack.CODEC.listOf(),
            UnaryFlowHandler.codecOf(ItemStack.OPTIONAL_LIST_STREAM_CODEC)
        );
    }

    private final Handle<Integer> count = useSynced(Network.count);
    private final Handle<String> label = useSynced(Network.label);
    private final Handle<Boolean> active = useSynced(Network.active);
    private final Handle<List<ItemStack>> items = useSynced(Network.items);
    private final UnaryReversed<Integer> action = unaryReversed(
        "action",
        UnaryFlowHandler.codecOf(ByteBufCodecs.VAR_INT)
    );

    private long tick;

    public SyncedTestBlockEntity(BlockPos pos, BlockState state) {
        super(TestRegistry.testSyncedBlockEntity.get(), pos, state);
        action.whenReceiveFromClient(this::onAction);
    }

    public Handle<Integer> count() {
        return count;
    }

    public Handle<String> label() {
        return label;
    }

    public Handle<Boolean> active() {
        return active;
    }

    public Handle<List<ItemStack>> items() {
        return items;
    }

    /** Client-side: queues an action for the server and flushes the channel. */
    public void sendAction(int actionId) {
        try {
            action.sendToServer(actionId);
        } catch (IllegalStateException ignored) {
            return; // a value is already queued this tick — drop the extra click
        }
        pushReversed();
    }

    /** Server-side validation of actions sent from the in-world UI. */
    private void onAction(int actionId) {
        switch (actionId) {
            case actionIncrement -> count.set(count.get() + 1);
            case actionDecrement -> count.set(count.get() - 1);
            case actionReset -> {
                count.set(0);
                items.set(List.of());
            }
            case actionPickItem -> {
                var next = new ArrayList<>(items.get());
                next.add(new ItemStack(itemPool[count.get() % itemPool.length], 1 + count.get() % 64));
                items.set(List.copyOf(next));
            }
            default -> {
                // unknown action — drop silently
            }
        }
        label.set("action " + actionId);
    }

    public static BlockEntityTicker<SyncedTestBlockEntity> ticker() {
        return (level, pos, state, be) -> {
            if (level.isClientSide) return;
            be.tick++;
            if (be.tick % 20 == 0 && be.active.get()) {
                be.count.set(be.count.get() + 1);
                be.label.set("tick " + be.tick);
            }
        };
    }
}
