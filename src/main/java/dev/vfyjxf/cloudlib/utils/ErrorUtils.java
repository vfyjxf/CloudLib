package dev.vfyjxf.cloudlib.utils;

import mezz.jei.common.platform.IPlatformRegistry;
import mezz.jei.common.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
        IPlatformRegistry<Item> itemRegistry = Services.PLATFORM.getRegistry(Registries.ITEM);

        final String itemName = itemRegistry.getRegistryName(item)
                .map(ResourceLocation::toString)
                .orElseGet(() -> {
                    if (item instanceof BlockItem) {
                        final String blockName;
                        Block block = ((BlockItem) item).getBlock();
                        if (block == null) {
                            blockName = "null";
                        } else {
                            IPlatformRegistry<Block> blockRegistry = Services.PLATFORM.getRegistry(Registries.BLOCK);
                            blockName = blockRegistry.getRegistryName(block)
                                    .map(ResourceLocation::toString)
                                    .orElseGet(() -> block.getClass().getName());
                        }
                        return "BlockItem(" + blockName + ")";
                    } else {
                        return item.getClass().getName();
                    }
                });

        CompoundTag nbt = itemStack.getTag();
        if (nbt != null) {
            return itemStack + " " + itemName + " nbt:" + nbt;
        }
        return itemStack + " " + itemName;
    }
}
