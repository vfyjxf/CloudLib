package dev.vfyjxf.cloudlib.test;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.test.sync.SyncedTestBlock;
import dev.vfyjxf.cloudlib.test.sync.SyncedTestBlockEntity;
import dev.vfyjxf.cloudlib.test.sync.TestBlock;
import dev.vfyjxf.cloudlib.test.sync.TestBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class TestRegistry {

    private static final DeferredRegister.Items items = DeferredRegister.createItems(Constants.modId);
    private static final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks(Constants.modId);

    public static final DeferredBlock<TestBlock> testBlock = block("test_block", TestBlock::new, BlockItem::new);

    public static final DeferredBlock<SyncedTestBlock> testSyncedBlock = block(
        "test_synced_block",
        SyncedTestBlock::new,
        BlockItem::new
    );

    private static final DeferredRegister<BlockEntityType<?>> blockEntities = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, Constants.modId);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlockEntity>> testBlockEntity = blockEntities
            .register(
                "test_block_entity",
                () -> BlockEntityType.Builder.of(TestBlockEntity::new, testBlock.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SyncedTestBlockEntity>> testSyncedBlockEntity = blockEntities
            .register(
                "test_synced_block_entity",
                () -> BlockEntityType.Builder.of(SyncedTestBlockEntity::new, testSyncedBlock.get()).build(null)
            );

    public static void register(IEventBus modBus) {
        blocks.register(modBus);
        blockEntities.register(modBus);
        items.register(modBus);
        CreativeTabValues.creativeModeTabs.register(modBus);
    }

    private static <T extends Block> DeferredBlock<T> block(
        String name,
        Supplier<T> block,
        @Nullable BiFunction<T, Item.Properties, BlockItem> blockItemFactory
    ) {
        DeferredBlock<T> deferredBlock = blocks.register(name, block);
        DeferredItem<BlockItem> deferredItem = items.register(name, () -> {
            if (blockItemFactory == null) {
                return new BlockItem(deferredBlock.get(), new Item.Properties());
            } else {
                return blockItemFactory.apply(deferredBlock.get(), new Item.Properties());
            }
        });
        CreativeTabValues.creativeTagItems.add(deferredItem);
        return deferredBlock;
    }

    private static class CreativeTabValues {
        public static final DeferredRegister<CreativeModeTab> creativeModeTabs = DeferredRegister
                .create(Registries.CREATIVE_MODE_TAB, Constants.modId);
        public static final MutableList<DeferredItem<?>> creativeTagItems = MutableLists.empty();
        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> creativeTab = creativeModeTabs.register(
            "conduit_tab",
            () -> CreativeModeTab.builder().title(Component.literal("Debug Entries"))
                    .icon(() -> Blocks.DARK_OAK_DOOR.asItem().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredItem<?> creativeTagItem : creativeTagItems) {
                            output.accept(creativeTagItem.get());
                        }
                    }).build()
        );
    }

    private TestRegistry() {}
}
