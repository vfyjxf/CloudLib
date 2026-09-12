package dev.vfyjxf.nimbusprojection.demo;

import dev.vfyjxf.nimbusprojection.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DemoRegistry {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.modId);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.modId);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.modId);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.modId);

    private static final List<DeferredItem<?>> creativeTabItems = new ArrayList<>();

    public static final DeferredBlock<TrackerBlock> trackerBlock = block(
            "tracker_block",
            TrackerBlock::new
    );

    public static final DeferredBlock<WaypointBlock> waypointBlock = block(
            "waypoint_block",
            WaypointBlock::new
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrackerBlockEntity>> trackerBlockEntity =
            BLOCK_ENTITIES.register(
                    "tracker_block_entity",
                    () -> BlockEntityType.Builder
                            .of(TrackerBlockEntity::new, trackerBlock.get())
                            .build(null)
            );

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> creativeTab = CREATIVE_TABS.register(
            "tab",
            () -> CreativeModeTab.builder()
                                 .title(Component.translatable("itemGroup.nimbusprojection.tab"))
                                 .icon(() -> trackerBlock.get().asItem().getDefaultInstance())
                                 .displayItems((parameters, output) -> {
                                     for (DeferredItem<?> item : creativeTabItems) {
                                         output.accept(item.get());
                                     }
                                 })
                                 .build()
    );

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
    }

    private static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> block) {
        DeferredBlock<T> deferredBlock = BLOCKS.register(name, block);
        DeferredItem<BlockItem> deferredItem = ITEMS.register(name, () -> new BlockItem(deferredBlock.get(), new Item.Properties()));
        creativeTabItems.add(deferredItem);
        return deferredBlock;
    }

    private DemoRegistry() {
    }
}
