package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A read-only grid of container slots: vanilla-looking bevelled cells
 * (pixel-matched to {@code generic_54.png}) with the item icon and its count /
 * durability decorations on top; empty cells draw the slot bed only. Rows
 * adapt to the item count — {@link #setItems} re-declares the grid's size.
 * <p>
 * Every cell is a real child node carrying the {@code .slot} class (the
 * selector {@code standard/base.css} paints with {@code --slot} /
 * {@code --slot-dark}), so a theme can restyle the bed; a cell draws its
 * themed background when the theme paints one and the vanilla bevel otherwise.
 * Cells are absolutely placed at their row-major offset, so the grid's own
 * measured size is exactly what it always was.
 * <p>
 * The cell itself — bed, hover wash, item icon and decorations — lives in
 * {@link SlotCellWidget}, shared with {@link IconRowWidget}.
 */
public final class SlotGridWidget extends CompositeWidget<Widget> {

    /** One slot cell — the vanilla 18×18 container cell. */
    public static final int cell = SlotCellWidget.cell;

    private final int columns;
    private final List<SlotCell> cells = new ArrayList<>();
    private List<ItemStack> items = List.of();

    public SlotGridWidget(int columns) {
        this.columns = Math.max(1, columns);
        syncCells();
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
        syncCells();
    }

    /** Grows or shrinks the cell list to one cell per grid position and re-seats the stacks. */
    private void syncCells() {
        int needed = columns * rows();
        while (cells.size() > needed) {
            removeWidget(cells.remove(cells.size() - 1));
        }
        while (cells.size() < needed) {
            SlotCell cell = new SlotCell(this, cells.size());
            cells.add(cell);
            addWidget(cell);
        }
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setStack(i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
        // the cells are absolute, so a taffy measure function on this node is
        // never consulted (a measured node is a leaf): the grid declares the
        // size its own cell geometry implies, below the theme, and the relayout
        // follows from the style write
        defaultStyle(UIStyle.of(UIStyles.sizeOf(gridWidth(columns), gridHeight(rows()))));
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

    // region cells

    /**
     * The cells mirror this grid's selector surface — a state flip (the grid
     * going inactive, say) has to re-resolve them too.
     */
    @Override
    public void markStyleDirty() {
        super.markStyleDirty();
        WidgetPart.markStyleDirtyAll(this);
    }

    /**
     * One cell of the grid: the shared {@link SlotCellWidget} placed at this
     * cell's row-major offset.
     */
    static final class SlotCell extends SlotCellWidget {

        SlotCell(SlotGridWidget owner, int index) {
            super(owner, cellBounds(owner, index), true);
        }

        private static Supplier<Rect> cellBounds(SlotGridWidget owner, int index) {
            return () -> new Rect(
                slotX(slotColumn(index, owner.columns())),
                slotY(slotRow(index, owner.columns())),
                cell,
                cell
            );
        }
    }

    // endregion
}
