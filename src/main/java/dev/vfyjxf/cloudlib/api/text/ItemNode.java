package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.RichNode;
import net.minecraft.world.item.ItemStack;

/**
 * An inline item icon, rendered like an inventory slot (optionally with the
 * damage/stack-size decorations bar).
 *
 * @param stack           the stack to render
 * @param size            edge length of the reserved square box in pixels (16 = vanilla slot)
 * @param showDecorations whether to draw stack size / durability decorations
 */
public record ItemNode(ItemStack stack, int size, boolean showDecorations) implements RichNode {

    public ItemNode {
        if (stack == null) throw new NullPointerException("stack");
    }

    public ItemNode(ItemStack stack) {
        this(stack, 16, false);
    }
}
