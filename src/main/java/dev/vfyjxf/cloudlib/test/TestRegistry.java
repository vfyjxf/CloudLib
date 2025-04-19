package dev.vfyjxf.cloudlib.test;

import dev.vfyjxf.cloudlib.Constants;
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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class TestRegistry {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);

    public static final DeferredBlock<TestBlock> testBlock = block(
            "test_block",
            TestBlock::new,
            BlockItem::new
    );

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlockEntity>> testBlockEntity =
            BLOCK_ENTITIES.register(
                    "test_block_entity",
                    () -> BlockEntityType.Builder
                            .of(TestBlockEntity::new, testBlock.get())
                            .build(null)
            );

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        CreativeTabValues.CREATIVE_TAB.register(modBus);
    }


    private static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> block, @Nullable BiFunction<T, Item.Properties, @NotNull BlockItem> blockItemFactory) {
        DeferredBlock<T> deferredBlock = BLOCKS.register(name, block);
        DeferredItem<BlockItem> deferredItem = ITEMS.register(name, () ->
        {
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
        public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);
        public static final MutableList<DeferredItem<?>> creativeTagItems = Lists.mutable.empty();
        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> creativeTab = CREATIVE_TAB.register(
                "conduit_tab",
                () -> CreativeModeTab.builder()
                        .title(Component.literal("Debug Entries"))
                        .icon(() -> Blocks.DARK_OAK_DOOR.asItem().getDefaultInstance())
                        .displayItems((parameters, output) -> {
                            for (DeferredItem<?> creativeTagItem : creativeTagItems) {
                                output.accept(creativeTagItem.get());
                            }
                        })
                        .build()
        );
    }

    private TestRegistry() {
    }
}
