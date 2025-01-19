package dev.vfyjxf.cloudlib.utils;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class ItemHandlers {

    public static ItemStack insert(@Nullable IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null) return stack;
        if (stack.isEmpty()) return ItemStack.EMPTY;

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            stack = handler.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }


    private ItemHandlers() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
