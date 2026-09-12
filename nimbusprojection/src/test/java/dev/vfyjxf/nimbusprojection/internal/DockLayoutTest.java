package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement.DockCorner;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage of {@link DockLayout}: deterministic slot packing, shared side
 * budgets, fold-then-hide overflow, and AUTO-corner hysteresis.
 */
class DockLayoutTest {

    private static final int W = 400, H = 240;

    private static DockLayout.Item item(DockCorner corner, int w, int h) {
        return new DockLayout.Item(corner, w, h, 17, Double.NaN, Double.NaN, null);
    }

    private static DockLayout.Item auto(double ax, double ay, int w, int h, DockCorner prev) {
        return new DockLayout.Item(DockCorner.AUTO, w, h, 17, ax, ay, prev);
    }

    //---- slot packing ----

    @Test
    void stacksFromCornerInOfferOrder() {
        var items = List.of(
                item(DockCorner.TOP_LEFT, 100, 30),
                item(DockCorner.TOP_LEFT, 100, 20));
        var r = DockLayout.solve(items, W, H);
        assertEquals(8, items.get(0).x);
        assertEquals(8, items.get(0).y);
        //second lands below the first + gap
        assertEquals(8 + 30 + 6, items.get(1).y);
        assertEquals(30 + 6 + 20 + 6, r.cursorEnd[DockCorner.TOP_LEFT.ordinal()]);
    }

    @Test
    void rightSideAnchorsToRightEdge() {
        var items = List.of(item(DockCorner.TOP_RIGHT, 100, 30));
        DockLayout.solve(items, W, H);
        assertEquals(W - 8 - 100, items.get(0).x);
    }

    @Test
    void bottomCornersGrowUpward() {
        var items = List.of(
                item(DockCorner.BOTTOM_LEFT, 80, 30),
                item(DockCorner.BOTTOM_LEFT, 80, 20));
        DockLayout.solve(items, W, H);
        //first hugs the bottom margin, second stacks above it
        assertEquals(H - 8 - 30, items.get(0).y);
        assertEquals(H - 8 - 30 - 6 - 20, items.get(1).y);
    }

    @Test
    void topExtentTracksTopStacks() {
        var items = List.of(
                item(DockCorner.TOP_LEFT, 100, 30),
                item(DockCorner.TOP_LEFT, 100, 20),
                item(DockCorner.BOTTOM_LEFT, 80, 25));
        var r = DockLayout.solve(items, W, H);
        assertEquals(8 + 30 + 6 + 20, r.topExtent[0]); //left top extent only counts top stacks
    }

    //---- shared side budget: cross → fold → hide ----

    @Test
    void fullPanelCrossesToEmptySideBeforeFolding() {
        //left column packed, right empty — the newcomer keeps its full size
        //and lands mirrored on the opposite side instead of folding in place
        var blocker = item(DockCorner.TOP_LEFT, 100, 210); //spends 216 of 224
        var crosser = item(DockCorner.BOTTOM_LEFT, 80, 100);
        var r = DockLayout.solve(List.of(blocker, crosser), W, H);
        assertFalse(crosser.folded);
        assertFalse(crosser.hidden);
        //keeps its bottom slot, flips the side
        assertEquals(DockCorner.BOTTOM_RIGHT, crosser.resolved);
        assertEquals(W - 8 - 80, crosser.x);
        assertEquals(H - 8 - 100, crosser.y);
    }

    @Test
    void foldedStripCrossesWhenOnlyOppositeSideHasRoom() {
        //home side can't even take a chrome strip while the opposite side has
        //exactly one strip's worth left → fold AND cross
        var leftBlocker = item(DockCorner.TOP_LEFT, 60, 218);   //left purse spent (224)
        var rightBlocker = item(DockCorner.TOP_RIGHT, 60, 195); //right: 201 used, one strip left
        var strip = item(DockCorner.BOTTOM_LEFT, 80, 30);
        var r = DockLayout.solve(List.of(leftBlocker, rightBlocker, strip), W, H);
        assertTrue(strip.folded);
        assertFalse(strip.hidden);
        assertEquals(DockCorner.BOTTOM_RIGHT, strip.resolved);
        assertEquals(0, r.overflow[DockCorner.BOTTOM_LEFT.ordinal()]);
        assertEquals(0, r.overflow[DockCorner.BOTTOM_RIGHT.ordinal()]);
    }

    @Test
    void sideBudgetIsSharedBetweenCorners() {
        //two columns on the left grow toward each other from one purse of
        //H-16; the right side is already spent so overflow has nowhere to go
        var items = List.of(
                item(DockCorner.TOP_RIGHT, 60, 218), //right side spent
                item(DockCorner.TOP_LEFT, 100, 100),
                item(DockCorner.BOTTOM_LEFT, 80, 120));
        var r = DockLayout.solve(items, W, H);
        //100 + 6 + 120 > 224, right is full → second folds to its foldH
        assertFalse(items.get(1).folded);
        assertTrue(items.get(2).folded);
        assertFalse(items.get(2).hidden);
        assertEquals(DockCorner.BOTTOM_LEFT, items.get(2).resolved);
    }

    @Test
    void overBudgetFoldsToChromeStrip() {
        //150+6+70 = 226 > 224 budget and no room across → second folds
        var items = List.of(
                item(DockCorner.TOP_RIGHT, 60, 218),
                item(DockCorner.TOP_LEFT, 100, 150),
                item(DockCorner.TOP_LEFT, 100, 70));
        DockLayout.solve(items, W, H);
        assertTrue(items.get(2).folded);
        assertFalse(items.get(2).hidden);
        //folded panel occupies only its foldH in the stack
        assertEquals(8 + 150 + 6, items.get(2).y);
    }

    @Test
    void evenFoldedOverflowHidesAndCounts() {
        //8×70px on the left with the right side spent: 2 fit unfolded
        //(152px used), 3 fold (175→221), the rest hide — 221 + 17 + 6 > 224
        List<DockLayout.Item> items = new ArrayList<>();
        items.add(item(DockCorner.TOP_RIGHT, 60, 218));
        for (int i = 0; i < 8; i++) {
            items.add(new DockLayout.Item(DockCorner.TOP_LEFT, 60, 70, 17,
                    Double.NaN, Double.NaN, null));
        }
        var r = DockLayout.solve(items, W, H);
        assertEquals(3, items.stream().filter(i -> i.folded && !i.hidden).count());
        assertEquals(3, items.stream().filter(i -> i.hidden).count());
        assertEquals(3, r.overflow[DockCorner.TOP_LEFT.ordinal()]);
    }

    @Test
    void overflowIsPerCorner() {
        //left side spent; 10×100px on the right: 2 fit unfolded, the rest hide
        List<DockLayout.Item> items = new ArrayList<>();
        items.add(item(DockCorner.TOP_LEFT, 60, 218));
        for (int i = 0; i < 10; i++) items.add(item(DockCorner.TOP_RIGHT, 60, 100));
        var r = DockLayout.solve(items, W, H);
        assertEquals(0, r.overflow[DockCorner.TOP_LEFT.ordinal()]);
        assertEquals(8, r.overflow[DockCorner.TOP_RIGHT.ordinal()]);
    }

    //---- AUTO corner hysteresis ----

    @Test
    void autoPicksAnchorQuadrant() {
        var a = auto(100, 60, 50, 20, null);
        var b = auto(300, 200, 50, 20, null);
        DockLayout.solve(List.of(a, b), W, H);
        assertEquals(DockCorner.TOP_LEFT, a.resolved);
        assertEquals(DockCorner.BOTTOM_RIGHT, b.resolved);
    }

    @Test
    void autoKeepsCornerInsideDeadband() {
        //previous TOP_LEFT; anchor slides 40px right of centre — inside the
        //72px deadband, the panel must NOT flap to the right
        var a = auto(W * 0.5 + 40, 60, 50, 20, DockCorner.TOP_LEFT);
        DockLayout.solve(List.of(a), W, H);
        assertEquals(DockCorner.TOP_LEFT, a.resolved);
    }

    @Test
    void autoFlipsPastDeadband() {
        var a = auto(W * 0.5 + 80, 60, 50, 20, DockCorner.TOP_LEFT);
        DockLayout.solve(List.of(a), W, H);
        assertEquals(DockCorner.TOP_RIGHT, a.resolved);
    }

    @Test
    void autoWithoutAnchorDefaultsTopLeft() {
        var a = auto(Double.NaN, Double.NaN, 50, 20, null);
        DockLayout.solve(List.of(a), W, H);
        assertEquals(DockCorner.TOP_LEFT, a.resolved);
    }

    @Test
    void solveIsDeterministic() {
        var items = List.of(
                item(DockCorner.TOP_LEFT, 100, 30),
                auto(300, 40, 60, 20, DockCorner.TOP_RIGHT),
                item(DockCorner.BOTTOM_LEFT, 80, 25));
        var r1 = DockLayout.solve(items, W, H);
        int x0 = items.get(0).x, y1 = items.get(1).y;
        var r2 = DockLayout.solve(items, W, H);
        assertEquals(x0, items.get(0).x);
        assertEquals(y1, items.get(1).y);
        assertEquals(r1.topExtent[0], r2.topExtent[0]);
    }

}
