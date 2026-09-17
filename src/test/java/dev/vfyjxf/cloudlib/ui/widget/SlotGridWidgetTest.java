package dev.vfyjxf.cloudlib.ui.widget;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SlotGridWidget's row/column math, slot geometry and count-badge rules. */
class SlotGridWidgetTest {

    @BeforeAll
    static void bootItems() {
        Bootstrap.bootStrap();
    }

    @Test
    void rowsAdaptToTheItemCount() {
        assertEquals(1, SlotGridWidget.rowsOf(0, 9), "empty still shows one row of empty slots");
        assertEquals(1, SlotGridWidget.rowsOf(9, 9));
        assertEquals(2, SlotGridWidget.rowsOf(10, 9));
        assertEquals(2, SlotGridWidget.rowsOf(17, 9), "17 items over 9 columns: 9 + 8");
        assertEquals(1, SlotGridWidget.rowsOf(3, 5));
        assertEquals(4, SlotGridWidget.rowsOf(17, 5), "a chested donkey's 17 slots over 5 columns");
        assertEquals(9, SlotGridWidget.rowsOf(9, 1), "9 items in a single column stack vertically");
        // degenerate columns clamp instead of dividing by zero
        assertEquals(5, SlotGridWidget.rowsOf(5, 0));
        assertEquals(1, SlotGridWidget.rowsOf(-3, 9));
    }

    @Test
    void gridSizeIsCellsTimesEighteen() {
        assertEquals(162, SlotGridWidget.gridWidth(9));
        assertEquals(90, SlotGridWidget.gridWidth(5));
        assertEquals(18, SlotGridWidget.gridHeight(1));
        assertEquals(72, SlotGridWidget.gridHeight(4));
        assertEquals(18, SlotGridWidget.gridWidth(0), "at least one column");
    }

    @Test
    void slotGeometryIsRowMajor() {
        assertEquals(0, SlotGridWidget.slotX(0));
        assertEquals(36, SlotGridWidget.slotX(2));
        assertEquals(54, SlotGridWidget.slotY(3));
        // index → cell mapping
        assertEquals(2, SlotGridWidget.slotColumn(7, 5));
        assertEquals(1, SlotGridWidget.slotRow(7, 5));
        assertEquals(0, SlotGridWidget.slotColumn(10, 5));
        assertEquals(2, SlotGridWidget.slotRow(10, 5));
    }

    @Test
    void widgetExposesColumnsRowsAndItems() {
        SlotGridWidget grid = new SlotGridWidget(5);
        assertEquals(5, grid.columns());
        assertEquals(1, grid.rows(), "no items yet — one placeholder row");

        grid.setItems(List.of(new ItemStack(Items.APPLE, 12), ItemStack.EMPTY, new ItemStack(Items.DIAMOND)));
        assertEquals(1, grid.rows());
        assertEquals(3, grid.items().size());

        List<ItemStack> seventeen = Collections.nCopies(17, new ItemStack(Items.BREAD, 2));
        grid.setItems(seventeen);
        assertEquals(4, grid.rows(), "17 items wrap to 4 rows at 5 columns");
    }

    @Test
    void countBadgeOnlyForStacksBeyondOne() {
        assertTrue(SlotGridWidget.showsCount(new ItemStack(Items.APPLE, 12)));
        assertFalse(SlotGridWidget.showsCount(new ItemStack(Items.APPLE, 1)));
        assertFalse(SlotGridWidget.showsCount(ItemStack.EMPTY));
    }
}
