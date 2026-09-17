package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A read-only grid of container slots: vanilla-looking bevelled cells
 * (pixel-matched to {@code generic_54.png}) with the item icon and its count /
 * durability decorations on top; empty cells draw the slot bed only. Rows
 * adapt to the item count — {@link #setItems} re-measures.
 */
public final class SlotGridWidget extends Widget {

    /** One slot cell — the vanilla 18×18 container cell. */
    public static final int cell = 18;

    private final int columns;
    private List<ItemStack> items = List.of();

    public SlotGridWidget(int columns) {
        this.columns = Math.max(1, columns);
        onMount((scene, context, handle) -> scene.layoutTree()
                .setMeasureFunc(
                        nodeId(), (style, space) -> new FloatSize(gridWidth(this.columns), gridHeight(rows()))));
    }

    // region configuration

    public int columns() {
        return columns;
    }

    /** Rows the current item count wraps into (at least one — the grid never collapses while loading). */
    public int rows() {
        return rowsOf(items.size(), columns);
    }

    public List<ItemStack> items() {
        return items;
    }

    /** Replaces the displayed stacks; rows re-derive from the count. */
    public void setItems(List<ItemStack> items) {
        this.items = List.copyOf(items);
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    // endregion

    // region geometry (pure — the headless tests drive these)

    /** Rows a count wraps into at the given column count (at least one). */
    public static int rowsOf(int count, int columns) {
        int cols = Math.max(1, columns);
        return Math.max(1, (Math.max(0, count) + cols - 1) / cols);
    }

    public static int gridWidth(int columns) {
        return Math.max(1, columns) * cell;
    }

    public static int gridHeight(int rows) {
        return Math.max(1, rows) * cell;
    }

    public static int slotX(int column) {
        return column * cell;
    }

    public static int slotY(int row) {
        return row * cell;
    }

    /** Which cell an index falls into — row-major. */
    public static int slotColumn(int index, int columns) {
        return index % Math.max(1, columns);
    }

    public static int slotRow(int index, int columns) {
        return index / Math.max(1, columns);
    }

    /** Whether the decorations layer draws a count badge for this stack. */
    public static boolean showsCount(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() != 1;
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int cells = columns * rows();
        for (int i = 0; i < cells; i++) {
            int x = slotX(slotColumn(i, columns));
            int y = slotY(slotRow(i, columns));
            slot(canvas, x, y);
            if (i >= items.size()) continue;
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            canvas.renderItemIcon(stack, x + 1, y + 1);
            canvas.renderItemDecorations(stack, x + 1, y + 1);
        }
    }

    /**
     * The classic container slot: 8B8B8B face, 373737 top/left inset,
     * FFFFFF bottom/right bevel — five fills, pixel-matched to vanilla.
     */
    private static void slot(SceneCanvas canvas, int x, int y) {
        canvas.fill(x, y, 18, 18, 0xFF8B8B8B);
        canvas.fill(x, y, 17, 1, 0xFF373737);
        canvas.fill(x, y, 1, 17, 0xFF373737);
        canvas.fill(x + 17, y + 1, 1, 17, 0xFFFFFFFF);
        canvas.fill(x + 1, y + 17, 17, 1, 0xFFFFFFFF);
    }

    // endregion
}
