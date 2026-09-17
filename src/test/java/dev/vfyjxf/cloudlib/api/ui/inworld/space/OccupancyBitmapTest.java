package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OccupancyBitmapTest {

    @Test
    void marksAndQueriesSingleRect() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(0, 0, 8, 8));

        assertTrue(bitmap.isOccupied(new Pos(0, 0)));
        assertTrue(bitmap.isOccupied(new Pos(7, 7)));
        assertFalse(bitmap.isOccupied(new Pos(8, 8)));
        assertTrue(bitmap.isOccupied(new Rect(0, 0, 8, 8)));
        assertTrue(bitmap.isFree(new Rect(8, 0, 8, 8)));
        assertTrue(bitmap.isFree(new Rect(16, 16, 16, 16)));
        assertEquals(1, bitmap.occupiedCellCount());
    }

    @Test
    void defaultCellSizeIsEightPixels() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        assertEquals(OccupancyBitmap.defaultCellSize, bitmap.cellSize());
        assertEquals(8, bitmap.cellsWide());
        assertEquals(8, bitmap.cellsHigh());
        assertEquals(new Rect(0, 0, 64, 64), bitmap.bounds());
    }

    @Test
    void rectTouchingPartOfCellOccupiesIt() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(0, 0, 1, 1));

        assertEquals(1, bitmap.occupiedCellCount());
        assertTrue(bitmap.isOccupied(new Rect(7, 7, 1, 1)));
        assertFalse(bitmap.isFree(new Rect(0, 0, 1, 1)));
    }

    @Test
    void rectEndingOnBoundaryDoesNotOccupyNextCell() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(0, 0, 8, 8));

        assertTrue(bitmap.isFree(new Rect(8, 0, 8, 8)));
        assertTrue(bitmap.isFree(new Rect(0, 8, 8, 8)));
    }

    @Test
    void marksMergeOverlappingRects() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(0, 0, 16, 16));
        bitmap.mark(new Rect(8, 8, 16, 16));

        assertTrue(bitmap.isOccupied(new Rect(0, 0, 24, 24)));
        assertTrue(bitmap.isFree(new Rect(24, 24, 8, 8)));
        assertEquals(7, bitmap.occupiedCellCount());
    }

    @Test
    void clipsRectsToBitmapBounds() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(-8, -8, 24, 24));
        bitmap.mark(new Rect(56, 56, 24, 24));

        assertEquals(5, bitmap.occupiedCellCount());
        assertTrue(bitmap.isOccupied(new Pos(63, 63)));
        assertTrue(bitmap.isFree(new Rect(24, 24, 8, 8)));
    }

    @Test
    void rectFullyOutsideBoundsIsIgnored() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(100, 100, 10, 10));
        bitmap.mark(new Rect(-10, -10, 5, 5));

        assertEquals(0, bitmap.occupiedCellCount());
    }

    @Test
    void emptyRectTouchesNothing() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(10, 10, 0, 0));
        bitmap.mark(new Rect(10, 10, 5, 0));

        assertEquals(0, bitmap.occupiedCellCount());
    }

    @Test
    void clearRemovesCoveredCellsOnly() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.markAll(List.of(new Rect(0, 0, 32, 32), new Rect(32, 0, 32, 32)));

        bitmap.clear(new Rect(0, 0, 16, 16));

        assertFalse(bitmap.isOccupied(new Pos(0, 0)));
        assertTrue(bitmap.isOccupied(new Pos(16, 0)));
        assertTrue(bitmap.isOccupied(new Pos(32, 0)));
        assertEquals(28, bitmap.occupiedCellCount());
    }

    @Test
    void clearAllEmptiesEverything() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.markAll(List.of(new Rect(0, 0, 32, 32), new Rect(32, 32, 32, 32)));

        bitmap.clearAll();

        assertEquals(0, bitmap.occupiedCellCount());
        assertTrue(bitmap.isFree(bitmap.bounds()));
    }

    @Test
    void occupiedFractionReportsDensity() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(0, 0, 16, 16));

        assertEquals(1.0, bitmap.occupiedFraction(new Rect(0, 0, 16, 16)), 1e-9);
        assertEquals(0.25, bitmap.occupiedFraction(new Rect(0, 0, 32, 32)), 1e-9);
        assertEquals(0.0, bitmap.occupiedFraction(new Rect(32, 32, 32, 32)), 1e-9);
    }

    @Test
    void isFreeSpansMarkedBoundary() {
        OccupancyBitmap bitmap = new OccupancyBitmap(64, 64);
        bitmap.mark(new Rect(24, 24, 16, 16));

        assertFalse(bitmap.isFree(new Rect(0, 0, 64, 64)));
        assertTrue(bitmap.isFree(new Rect(0, 0, 24, 64)));
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new OccupancyBitmap(0, 64));
        assertThrows(IllegalArgumentException.class, () -> new OccupancyBitmap(64, -1));
        assertThrows(IllegalArgumentException.class, () -> new OccupancyBitmap(64, 64, 0));
    }

    @Test
    void occupiedCellRectsSnapshotMatchesMarks() {
        OccupancyBitmap bitmap = new OccupancyBitmap(32, 32, 16);
        bitmap.mark(new Rect(0, 16, 32, 16));

        assertEquals(List.of(new Rect(0, 16, 16, 16), new Rect(16, 16, 16, 16)), bitmap.occupiedCellRects());
    }
}
