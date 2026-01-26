package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A widget for displaying Minecraft ItemStacks.
 * <p>
 * Supports:
 * <ul>
 *   <li>Static and dynamic item stacks</li>
 *   <li>Count overlay rendering</li>
 *   <li>Tooltip display</li>
 * </ul>
 */
public class ItemWidget extends Widget {

    private Supplier<ItemStack> itemSupplier;
    private boolean showCount = true;
    private boolean showTooltip = true;

    public static ItemWidget of(ItemStack item) {
        return new ItemWidget(() -> item);
    }

    public static ItemWidget of(Supplier<ItemStack> itemSupplier) {
        return new ItemWidget(itemSupplier);
    }

    public static ItemWidget empty() {
        return new ItemWidget(() -> ItemStack.EMPTY);
    }

    private ItemWidget(Supplier<ItemStack> itemSupplier) {
        this.itemSupplier = itemSupplier;
        setSize(16, 16); // Default item size
    }

    public ItemStack item() {
        return itemSupplier.get();
    }

    public ItemWidget setItem(ItemStack item) {
        this.itemSupplier = () -> item;
        return this;
    }

    public ItemWidget setItemSupplier(Supplier<ItemStack> supplier) {
        this.itemSupplier = supplier;
        return this;
    }

    public boolean showCount() {
        return showCount;
    }

    public ItemWidget setShowCount(boolean showCount) {
        this.showCount = showCount;
        return this;
    }

    public boolean showTooltip() {
        return showTooltip;
    }

    public ItemWidget setShowTooltip(boolean showTooltip) {
        this.showTooltip = showTooltip;
        return this;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);

        ItemStack stack = item();
        if (stack.isEmpty()) return;

        // Render the item
        graphics.renderItem(stack, 0, 0);

        // Render count overlay
        if (showCount && stack.getCount() > 1) {
            graphics.renderItemDecorations(context().font(), stack, 0, 0);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (showTooltip && isMouseOver(mouseX, mouseY)) {
            ItemStack stack = item();
            if (!stack.isEmpty()) {
                graphics.renderTooltip(context().font(), stack, mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }
}
