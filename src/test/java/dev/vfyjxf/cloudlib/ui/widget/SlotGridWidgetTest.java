package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SlotGridWidget's row/column math, slot geometry, cell parts and count-badge rules. */
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

    // ------------------------------------------------------------------ cells

    @Test
    void everyCellCarriesTheSlotClass() {
        SlotGridWidget grid = new SlotGridWidget(5);
        assertEquals(5, grid.children().size(), "one cell per column in the single placeholder row");
        for (var cell : grid.children()) {
            assertTrue(cell.styleClasses().contains("slot"), ".slot is what the base sheet paints");
            assertEquals("slot-grid", cell.styleTag(), "a cell answers to the grid's tag");
            assertTrue(cell.interactive(), "cells stay hoverable — .slot:hover");
        }
    }

    @Test
    void cellsFollowTheRowsTheItemsWrapInto() {
        SlotGridWidget grid = new SlotGridWidget(5);
        grid.setItems(Collections.nCopies(7, new ItemStack(Items.BREAD)));
        assertEquals(2, grid.rows());
        assertEquals(10, grid.children().size(), "7 items over 5 columns means two rows of cells");
    }

    @Test
    void cellsLandOnTheSlotGeometry() {
        try (WidgetTestScene fixture = new WidgetTestScene(200, 100)) {
            SlotGridWidget grid = fixture.add(new SlotGridWidget(5));
            grid.setItems(Collections.nCopies(7, new ItemStack(Items.BREAD)));
            fixture.stabilize();

            assertEquals(SlotGridWidget.gridWidth(5), grid.width(), "the cells never grow the grid");
            assertEquals(SlotGridWidget.gridHeight(2), grid.height());
            var cells = grid.children();
            assertEquals(10, cells.size());
            for (int i = 0; i < cells.size(); i++) {
                Rect expected = new Rect(
                    SlotGridWidget.slotX(SlotGridWidget.slotColumn(i, 5)),
                    SlotGridWidget.slotY(SlotGridWidget.slotRow(i, 5)),
                    SlotGridWidget.cell,
                    SlotGridWidget.cell
                );
                assertEquals(expected, cells.get(i).bounds(), "cell " + i);
            }
            assertEquals(new Rect(36, 18, 18, 18), cells.get(7).bounds(), "row-major: index 7 is column 2, row 1");
        }
    }

    @Test
    void theSlotClassPaintsTheCells() {
        SlotGridWidget grid = new SlotGridWidget(5);
        Theme theme = theme(".slot { background: color(#101010) }");
        var key = Styles.byId("background");
        assertNotNull(key);
        assertNotNull(theme.resolve(grid.children().get(0)).get(key), ".slot matches a cell");
        assertNotNull(theme.resolve(grid.children().get(4)).get(key), "every cell, not just the first");
        assertTrue(
            theme.resolve(grid).isEmpty(),
            "the grid itself is not a slot — the cells carry the look, not the container"
        );
    }

    @Test
    void theHoverOverlayWashesTheSlotWhileThePointerIsOnIt() {
        try (WidgetTestScene fixture = new WidgetTestScene(200, 100)) {
            SlotGridWidget grid = fixture.add(new SlotGridWidget(5));
            fixture.stabilize();
            SlotGridWidget.SlotCell cell = (SlotGridWidget.SlotCell) grid.children().get(0);
            cell.applyThemeStyle(theme(".slot { hover-overlay: color(#FFFFFF26) }").resolve(cell));

            assertNull(cell.hoverOverlay(), "nothing washes the bed while the pointer is elsewhere");

            fixture.scene.mouseMoved(SlotGridWidget.slotX(0) + 4, SlotGridWidget.slotY(0) + 4);
            assertTrue(cell.hovered(), "the pointer sits on the first cell");
            assertNotNull(cell.hoverOverlay(), "the sheet's hover-overlay paints the hovered bed");

            cell.applyThemeStyle(UIStyle.empty);
            assertNull(cell.hoverOverlay(), "a sheet without one leaves the bed alone");
        }
    }

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "slots"), CssParser.parse(css));
    }
}
