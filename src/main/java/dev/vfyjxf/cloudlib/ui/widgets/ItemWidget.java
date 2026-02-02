package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * ItemStack display with count and tooltip support.
 */
public class ItemWidget extends Widget {

    //region state

    private Supplier<ItemStack> itemSupplier;
    private boolean showCount = true;
    private boolean showTooltip = true;

    //endregion

    //region factory

    public static ItemWidget of(ItemStack item) {
        return new ItemWidget(() -> item);
    }

    public static ItemWidget of(Supplier<ItemStack> supplier) {
        return new ItemWidget(supplier);
    }

    public static ItemWidget empty() {
        return new ItemWidget(() -> ItemStack.EMPTY);
    }

    private ItemWidget(Supplier<ItemStack> supplier) {
        this.itemSupplier = supplier;
        setSize(16, 16);
    }

    //endregion

    //region configuration

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

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
        ItemStack stack = item();
        if (stack.isEmpty()) return;

        canvas.render(g -> {
            g.renderItem(stack, 0, 0);
            if (showCount && stack.getCount() > 1) {
                g.renderItemDecorations(context().font(), stack, 0, 0);
            }
        });
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

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        ItemStack stack = item();
        collector.add("item", stack.isEmpty() ? "empty" : stack.getItem().toString(), InspectionProperty.CATEGORY_DATA);
        if (!stack.isEmpty()) {
            collector.addWithDefault("count", stack.getCount(), 1, InspectionProperty.CATEGORY_DATA);
        }
        collector.addWithDefault("showCount", showCount, true, InspectionProperty.CATEGORY_VISUAL);
        collector.addWithDefault("showTooltip", showTooltip, true, InspectionProperty.CATEGORY_VISUAL);
    }

    //endregion
}
