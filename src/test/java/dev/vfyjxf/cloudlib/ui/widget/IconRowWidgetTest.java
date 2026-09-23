package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IconRowWidget: the visible-count truncation, the bare/slot-backed cells, the icon
 * render mode and the geometry both modes share. The draws themselves need a client,
 * so the tests pin the state the render paths read.
 */
class IconRowWidgetTest {

    @BeforeAll
    static void bootItems() {
        Bootstrap.bootStrap();
    }

    // region truncation

    @Test
    void theRowShowsAtMostMaxIcons() {
        assertEquals(3, IconRowWidget.visibleCount(3, 9));
        assertEquals(9, IconRowWidget.visibleCount(20, 9), "the overflow is dropped, not wrapped");
        assertEquals(0, IconRowWidget.visibleCount(0, 9));
        assertEquals(1, IconRowWidget.visibleCount(1, 0), "a row is at least one cell wide");
        assertEquals(0, IconRowWidget.visibleCount(-4, 9));
    }

    @Test
    void theRowReportsWhatItDropped() {
        IconRowWidget row = IconRowWidget.items(Collections.nCopies(12, new ItemStack(Items.BREAD)), 5);
        assertEquals(12, row.items().size());
        assertEquals(5, row.shown());
        assertEquals(7, row.hidden());

        row.setItems(List.of(new ItemStack(Items.BREAD)));
        assertEquals(1, row.shown());
        assertEquals(0, row.hidden());
    }

    // endregion

    // region geometry

    @Test
    void cellsAreOneCellApartAndTheRowIsAsWideAsThem() {
        assertEquals(0, IconRowWidget.cellX(0));
        assertEquals(18, IconRowWidget.cellX(1));
        assertEquals(54, IconRowWidget.cellX(3));
        assertEquals(0, IconRowWidget.cellX(-2), "a negative index clamps instead of going off the row");
        assertEquals(0, IconRowWidget.rowWidth(0));
        assertEquals(90, IconRowWidget.rowWidth(5));
    }

    @Test
    void oneIconIsARowOfOne() {
        IconRowWidget row = IconRowWidget.single(new ItemStack(Items.DIAMOND));
        assertEquals(1, row.max());
        assertEquals(1, row.shown());
        assertEquals(1, row.children().size());
    }

    // endregion

    // region cells

    @Test
    void theStockRowIsNakedAndThreeDimensional() {
        IconRowWidget row = IconRowWidget.items(List.of(new ItemStack(Items.APPLE)), 9);
        assertFalse(row.slotBacked(), "an info-panel line carries no slot bed");
        assertEquals(IconRenderMode.model, row.iconMode(), "blocks and shaped items keep their model");
        assertFalse(row.children().getFirst().styleClasses().contains("slot"), "a bare cell is no slot");
    }

    @Test
    void aSlotBackedRowDressesEveryCellInSlot() {
        IconRowWidget row = IconRowWidget
                .items(List.of(new ItemStack(Items.APPLE, 12), new ItemStack(Items.DIAMOND)), 9).slotBacked(true);
        assertEquals(2, row.children().size(), "one cell per shown stack");
        for (Widget cell : row.children()) {
            assertTrue(cell.styleClasses().contains("slot"), ".slot is what the base sheet paints");
            assertEquals("icon-row", cell.styleTag(), "a cell answers to the row's tag");
            assertTrue(cell.interactive(), "cells stay hoverable — .slot:hover");
            assertTrue(((SlotCellWidget) cell).slotBacked(), "the switch reaches the cells");
        }
    }

    @Test
    void theCellsFollowTheVisibleCount() {
        IconRowWidget row = IconRowWidget.items(Collections.nCopies(12, new ItemStack(Items.BREAD)), 5);
        assertEquals(5, row.children().size());

        row.setItems(List.of(new ItemStack(Items.BREAD)));
        assertEquals(1, row.children().size(), "the row shrinks to what it shows");
    }

    @Test
    void theSlotClassPaintsASlotBackedRowsCells() {
        IconRowWidget row = IconRowWidget.items(List.of(new ItemStack(Items.APPLE)), 9).slotBacked(true);
        Theme theme = theme(".slot { background: color(#101010) }");
        var key = Styles.byId("background");
        assertNotNull(key);
        assertNotNull(theme.resolve(row.children().getFirst()).get(key), ".slot matches a cell");
        assertTrue(theme.resolve(row).isEmpty(), "the row itself is not a slot");
    }

    @Test
    void aNakedRowResolvesNoBedAtAll() {
        IconRowWidget row = IconRowWidget.items(List.of(new ItemStack(Items.APPLE)), 9);
        Theme theme = theme(".slot { background: color(#101010) }");
        var key = Styles.byId("background");
        assertNotNull(key);
        assertNull(theme.resolve(row.children().getFirst()).get(key), "a bare cell matches no .slot rule");
    }

    @Test
    void flippingTheBedAndTheIconModeReachesTheLiveCells() {
        IconRowWidget row = IconRowWidget.items(List.of(new ItemStack(Items.APPLE)), 9);
        SlotCellWidget cell = (SlotCellWidget) row.children().getFirst();
        assertEquals(IconRenderMode.model, cell.iconMode(), "cells start on the 3D path");

        row.slotBacked(true).iconMode(IconRenderMode.flat);
        assertTrue(cell.styleClasses().contains("slot"));
        assertEquals(IconRenderMode.flat, cell.iconMode(), "the mode flip reaches a cell built earlier");

        row.slotBacked(false);
        assertFalse(cell.styleClasses().contains("slot"), "the class leaves with the bed");
    }

    // endregion

    // region layout

    @Test
    void theCellsLandOnTheRowGeometry() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            IconRowWidget row = fixture
                    .add(IconRowWidget.items(List.of(new ItemStack(Items.APPLE), new ItemStack(Items.DIAMOND)), 9));
            fixture.stabilize();

            assertEquals(36, row.width(), "two cells wide");
            assertEquals(18, row.height());
            assertEquals(new Rect(0, 0, 18, 18), row.children().get(0).bounds());
            assertEquals(new Rect(18, 0, 18, 18), row.children().get(1).bounds());
        }
    }

    @Test
    void reReadingTheItemsResizesTheRow() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            IconRowWidget row = fixture.add(IconRowWidget.items(Collections.nCopies(5, ItemStack.EMPTY), 9));
            fixture.stabilize();
            assertEquals(90, row.width());

            row.setItems(Collections.nCopies(2, ItemStack.EMPTY));
            fixture.stabilize();
            assertEquals(36, row.width(), "the row re-declares the size its cells imply");
            assertEquals(2, row.children().size());
        }
    }

    @Test
    void aSheetThatDeclaresTheRowSizeWins() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            IconRowWidget row = fixture.add(IconRowWidget.items(List.of(new ItemStack(Items.APPLE)), 9));
            fixture.stabilize();
            assertEquals(18, row.width());

            row.applyThemeStyle(theme("icon-row { width: 100px }").resolve(row));
            fixture.stabilize();
            assertEquals(100, row.width(), "the declaration sits below the theme");
        }
    }

    @Test
    void switchingTheModesMovesNoCell() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            IconRowWidget row = fixture
                    .add(IconRowWidget.items(List.of(new ItemStack(Items.APPLE), new ItemStack(Items.DIAMOND)), 9));
            fixture.stabilize();
            assertEquals(new Rect(18, 0, 18, 18), row.children().get(1).bounds());

            row.slotBacked(true).iconMode(IconRenderMode.flat);
            fixture.stabilize();
            assertEquals(36, row.width(), "the stride survives both switches");
            assertEquals(18, row.height());
            assertEquals(new Rect(18, 0, 18, 18), row.children().get(1).bounds());

            row.slotBacked(false).iconMode(IconRenderMode.model);
            fixture.stabilize();
            assertEquals(new Rect(18, 0, 18, 18), row.children().get(1).bounds());
        }
    }

    @Test
    void cellsFallBackToTheVanillaBedWhenTheSheetPaintsNone() {
        IconRowWidget row = IconRowWidget.items(List.of(new ItemStack(Items.APPLE)), 9).slotBacked(true);
        Widget cell = row.children().getFirst();
        assertTrue(((WidgetPart) cell).texture() == null, "the shared cell has no code texture: it draws the bed");

        Theme theme = theme(".slot { background: color(#202020) }");
        cell.applyThemeStyle(theme.resolve(cell));
        assertEquals(0xFF202020, ((ColorTexture) Objects.requireNonNull(((WidgetPart) cell).texture())).color());
    }

    // endregion

    // region fixture

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "icon-row"), CssParser.parse(css));
    }

    // endregion
}
