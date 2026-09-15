package dev.vfyjxf.cloudlib.api.ui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Function;

/**
 * Type-safe wrapper around an arbitrary "stack" object that a tooltip is associated with.
 * <p>
 * Mirrors the role of {@code GuiGraphics.tooltipStack} (an {@link ItemStack} used by NeoForge
 * to fire {@code RenderTooltipEvent} and resolve custom fonts), but generalized so that
 * non-item sources (e.g. {@link FluidStack}) can participate as well.
 *
 * @param <T> the held value type
 * @see Tooltip#setStack(TooltipStack)
 */
public sealed interface TooltipStack<T> {

    /**
     * The raw value this stack wraps.
     */
    T value();

    /**
     * Converts the held value to an {@link ItemStack} for vanilla / NeoForge rendering hooks.
     * Returns {@link ItemStack#EMPTY} when no meaningful conversion exists.
     */
    ItemStack asStack();

    /**
     * Resolves the {@link Font} to use when rendering the tooltip.
     * Delegates to {@link ClientHooks#getTooltipFont(ItemStack, Font)} when the converted
     * stack is non-empty; otherwise returns the given default.
     */
    default Font resolveFont(Font defaultFont) {
        ItemStack stack = asStack();
        return stack.isEmpty() ? defaultFont : ClientHooks.getTooltipFont(stack, defaultFont);
    }

    //region built-in implementations

    /**
     * A tooltip stack backed by an {@link ItemStack}.
     */
    record Item(ItemStack value) implements TooltipStack<ItemStack> {
        @Override
        public ItemStack asStack() {
            return value;
        }
    }

    /**
     * A tooltip stack backed by a {@link FluidStack}.
     * Converts to the fluid's bucket item for vanilla rendering hooks.
     */
    record Fluid(FluidStack value) implements TooltipStack<FluidStack> {
        @Override
        public ItemStack asStack() {
            return value.isEmpty() ? ItemStack.EMPTY : new ItemStack(value.getFluid().getBucket());
        }
    }

    /**
     * A tooltip stack backed by an arbitrary type with an explicit {@link ItemStack} converter.
     */
    record Custom<T>(T value, Function<T, ItemStack> converter) implements TooltipStack<T> {
        @Override
        public ItemStack asStack() {
            return converter.apply(value);
        }
    }

    //endregion

    //region factory

    /**
     * Wraps an {@link ItemStack}.
     */
    static TooltipStack<ItemStack> of(ItemStack stack) {
        return new Item(stack);
    }

    /**
     * Wraps a {@link FluidStack}. Converts to the fluid's bucket item for rendering.
     */
    static TooltipStack<FluidStack> of(FluidStack stack) {
        return new Fluid(stack);
    }

    /**
     * Wraps a custom value with a user-supplied {@link ItemStack} converter.
     */
    static <T> TooltipStack<T> of(T value, Function<T, ItemStack> toItemStack) {
        return new Custom<>(value, toItemStack);
    }

    //endregion

}
