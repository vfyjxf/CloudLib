package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A horizontal strip of item icons — shows the synced item list of the demo
 * block entity live inside an in-world panel.
 */
public class ItemStripWidget extends Widget {

    private static final int cell = 17;
    private static final int max = 9;

    private List<ItemStack> items = List.of();

    public ItemStripWidget() {
        onMount((scene, context, handle) -> scene.layoutTree()
                .setMeasureFunc(
                        nodeId(),
                        (style, space) -> new FloatSize(Math.max(cell, Math.min(items.size(), max) * cell), 18)));
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int count = Math.min(items.size(), max);
        for (int i = 0; i < count; i++) {
            canvas.renderItemIcon(items.get(i), i * cell, 1);
            canvas.renderItemDecorations(items.get(i), i * cell, 1);
        }
    }
}
