package dev.vfyjxf.cloudlib.utils;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public final class ErrorUtils {

    private ErrorUtils() {

    }

    public static String getItemStackInfo(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return "null";
        }
        Item item = itemStack.getItem();
        Registry<Item> itemRegistry = BuiltInRegistries.ITEM;

        final String itemName = itemRegistry.getResourceKey(item)
                .map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .orElseGet(() -> {
                    if (item instanceof BlockItem) {
                        final String blockName;
                        Block block = ((BlockItem) item).getBlock();
                        Registry<Block> blockRegistry = BuiltInRegistries.BLOCK;
                        blockName = blockRegistry.getResourceKey(block)
                                .map(ResourceKey::location)
                                .map(ResourceLocation::toString)
                                .orElseGet(() -> block.getClass().getName());
                        return "BlockItem(" + blockName + ")";
                    } else {
                        return item.getClass().getName();
                    }
                });

        DataComponentMap nbt = itemStack.getComponents();
        return itemStack + " " + itemName + " components:" + nbt;
    }
}
