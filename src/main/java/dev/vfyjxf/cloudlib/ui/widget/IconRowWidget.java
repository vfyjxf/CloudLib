package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A single row of equally spaced item icons — an info panel's "what it wants /
 * what it drops" line.
 * <p>
 * Each cell is a real child node, placed at {@code index * cell}, and the row
 * declares the size those cells imply; the icon, its count and its durability
 * decorations come from {@link SlotCellWidget}, shared with
 * {@link SlotGridWidget}. Two knobs shape the cells:
 * <ul>
 *   <li>{@link #slotBacked(boolean)} — <em>off by default</em>. A bare row draws
 *       no slot bed: cells carry no {@code .slot} class, so the theme's slot
 *       background and hover wash never come into play and the icons sit directly
 *       on whatever the panel paints ({@code --slot} is the stock sheet's bed). Turn
 *       it on for an inventory-looking line — the cells then get the themed bed
 *       with the vanilla bevel as the code fallback;</li>
 *   <li>{@link #iconMode(IconRenderMode)} — <em>{@code model} by default</em>: the
 *       vanilla item renderer keeps a block or a shaped item 3D through the canvas's
 *       layered forward path. {@code flat} swaps in the atlas sprite, the batchable
 *       path, for rows that would rather not pay a depth clear per icon.</li>
 * </ul>
 * Either way a cell keeps its 18×18 stride and its decorations, so switching modes
 * never moves the row's geometry.
 * <p>
 * The list is truncated to {@link #max()} entries — a row is a summary, not an
 * inventory; {@link #hidden()} reports what a panel shell may want to spell out
 * as {@code +N}.
 */
public final class IconRowWidget extends CompositeWidget<Widget> {

    /** One cell — the vanilla 18×18 container cell. */
    public static final int cell = SlotCellWidget.cell;

    private final int max;
    private final List<SlotCellWidget> cells = new ArrayList<>();
    private List<ItemStack> items = List.of();
    private boolean slotBacked = false;
    private IconRenderMode iconMode = IconRenderMode.model;

    // region factories

    /** A row of at most {@code max} icons — {@code max} is clamped to at least one cell. */
    public static IconRowWidget items(List<ItemStack> items, int max) {
        IconRowWidget row = new IconRowWidget(max);
        row.setItems(items);
        return row;
    }

    /** A row holding exactly one icon. */
    public static IconRowWidget single(ItemStack stack) {
        return items(List.of(stack), 1);
    }

    private IconRowWidget(int max) {
        this.max = Math.max(1, max);
        syncCells();
    }

    // endregion

    // region configuration

    /** How many cells the row can show. */
    public int max() {
        return max;
    }

    public List<ItemStack> items() {
        return items;
    }

    /** Replaces the row's stacks; the cells follow the visible count. */
    public IconRowWidget setItems(List<ItemStack> items) {
        this.items = List.copyOf(items);
        syncCells();
        return this;
    }

    /** How many of the stacks the row actually shows — {@link #items()} truncated to {@link #max()}. */
    public int shown() {
        return visibleCount(items.size(), max);
    }

    /** How many stacks the row had to drop. */
    public int hidden() {
        return Math.max(0, items.size() - shown());
    }

    /** Whether the cells paint the slot bed — off by default: a bare icon line. */
    public boolean slotBacked() {
        return slotBacked;
    }

    /**
     * Turns the cells' {@code .slot} bed and hover wash on or off; cells already
     * built follow immediately, and the row's geometry is untouched either way.
     */
    public IconRowWidget slotBacked(boolean backed) {
        this.slotBacked = backed;
        applyCellConfig();
        return this;
    }

    /** How the cells draw their icons — the 3D model path unless switched to {@code flat}. */
    public IconRenderMode iconMode() {
        return iconMode;
    }

    /**
     * Switches the cells' icon path (see {@link IconRenderMode}); cells already built
     * follow immediately.
     */
    public IconRowWidget iconMode(IconRenderMode mode) {
        this.iconMode = mode;
        applyCellConfig();
        return this;
    }

    // endregion

    // region geometry (pure — the headless tests drive these)

    /** How many of {@code count} stacks fit in a row of {@code max} cells. */
    public static int visibleCount(int count, int max) {
        return Math.max(0, Math.min(Math.max(0, count), Math.max(1, max)));
    }

    public static int cellX(int index) {
        return Math.max(0, index) * cell;
    }

    public static int rowWidth(int count) {
        return Math.max(0, count) * cell;
    }

    // endregion

    // region cells

    /**
     * Grows or shrinks the cell list to the visible count and re-seats the
     * stacks.
     * <p>
     * The cells are absolute, so a taffy measure function on this node is never
     * consulted (a measured node is a leaf): the row declares the size its cell
     * geometry implies, below the theme, and the relayout follows from the style
     * write.
     */
    private void syncCells() {
        int needed = shown();
        while (cells.size() > needed) {
            remove(cells.removeLast());
        }
        while (cells.size() < needed) {
            SlotCellWidget added = new SlotCellWidget(this, cellBounds(cells.size()), true);
            cells.add(added);
            add(added);
        }
        applyCellConfig();
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setStack(items.get(i));
        }
        defaultStyle(UIStyle.of(UIStyles.sizeOf(rowWidth(needed), cell)));
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    /**
     * Pushes the row's cell framing onto every cell it has — the mode flip path and
     * the cell-creation path share it, so a cell can never arrive half-configured.
     */
    private void applyCellConfig() {
        for (SlotCellWidget cell : cells) {
            cell.slotBacked(slotBacked).iconMode(iconMode);
        }
    }

    /** The one place that knows a row cell is {@code index * 18} from the left. */
    private static Supplier<Rect> cellBounds(int index) {
        return () -> new Rect(cellX(index), 0, cell, cell);
    }

    /** The cells mirror this row's selector surface — a state flip has to re-resolve them too. */
    @Override
    public void markStyleDirty() {
        super.markStyleDirty();
        WidgetPart.markStyleDirtyAll(this);
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.add("items", items.size(), InspectionProperty.categoryData);
        collector.add("shown", shown(), InspectionProperty.categoryData);
        collector.addWithDefault("hidden", hidden(), 0, InspectionProperty.categoryData);
        collector.addWithDefault("max", max, 1, InspectionProperty.categoryLayout);
        collector.addWithDefault("slotBacked", slotBacked, false, InspectionProperty.categoryLayout);
        collector.addFormatted(
            "iconMode",
            iconMode.name(),
            IconRenderMode.model.name(),
            InspectionProperty.categoryLayout
        );
    }

    // endregion
}
