package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeRectIndexTest {

    @Test
    void startsAsOneFreeRectCoveringEverything() {
        FreeRectIndex index = new FreeRectIndex(100, 80);

        assertEquals(List.of(new Rect(0, 0, 100, 80)), index.freeRects());
        assertTrue(index.canFit(100, 80));
        assertFalse(index.canFit(101, 80));
    }

    @Test
    void insertChoosesTopLeftOfWholeArea() {
        FreeRectIndex index = new FreeRectIndex(100, 100);

        Rect placement = index.insert(50, 50);

        assertEquals(new Rect(0, 0, 50, 50), placement);
        assertEquals(List.of(new Rect(50, 0, 50, 100), new Rect(0, 50, 100, 50)), index.freeRects());
    }

    @Test
    void insertPicksBestShortSideFit() {
        FreeRectIndex index = new FreeRectIndex(100, 100);
        index.occupy(new Rect(0, 0, 100, 30));

        // Free area is (0,30,100,70): a 60x40 block leaves 40x30 there, but a
        // vertical 30-wide strip (0,0,30,100)... does not exist — the only free
        // rect is the bottom, so placement goes to its top-left.
        Rect placement = index.insert(60, 40);

        assertEquals(new Rect(0, 30, 60, 40), placement);
    }

    @Test
    void bestShortSideFitPrefersTighterFreeRect() {
        FreeRectIndex index = new FreeRectIndex(100, 100);
        // Carve out a 40x40 hole at (0,0) and leave the rest as one L-free area.
        index.occupy(new Rect(40, 0, 60, 40));

        // Free rects after the carve: (0,0,40,100), (0,40,100,60) and pruned
        // variants. A 35x35 block fits (0,0,40,100) with leftovers 5/65 and
        // (0,40,100,60) with leftovers 65/25 — the first is tighter.
        Rect placement = index.insert(35, 35);

        assertEquals(new Rect(0, 0, 35, 35), placement);
    }

    @Test
    void occupyMiddleSplitsIntoMaximalStrips() {
        FreeRectIndex index = new FreeRectIndex(100, 100);

        index.occupy(new Rect(40, 40, 20, 20));

        assertEquals(
                List.of(
                        new Rect(0, 0, 40, 100),
                        new Rect(60, 0, 40, 100),
                        new Rect(0, 0, 100, 40),
                        new Rect(0, 60, 100, 40)),
                index.freeRects());
    }

    @Test
    void occupyPrunesContainedFreeRects() {
        FreeRectIndex index = new FreeRectIndex(100, 100);
        index.occupy(new Rect(0, 0, 40, 100));

        // Now occupying the rest of the top row subsumes the bottom strip's
        // left portion; whatever remains must not contain duplicates.
        index.occupy(new Rect(40, 0, 60, 100));

        assertEquals(List.of(), index.freeRects());
        assertFalse(index.canFit(1, 1));
    }

    @Test
    void occupyClipsOutOfBoundsRects() {
        FreeRectIndex index = new FreeRectIndex(100, 100);

        index.occupy(new Rect(50, 50, 200, 200));

        assertEquals(List.of(new Rect(0, 0, 50, 100), new Rect(0, 0, 100, 50)), index.freeRects());
    }

    @Test
    void insertFillsUntilFullThenReturnsNull() {
        FreeRectIndex index = new FreeRectIndex(100, 100);

        assertEquals(new Rect(0, 0, 100, 50), index.insert(100, 50));
        assertEquals(new Rect(0, 50, 100, 50), index.insert(100, 50));
        assertNull(index.insert(1, 1));
        assertFalse(index.canFit(1, 1));
    }

    @Test
    void rejectsInvalidSizes() {
        FreeRectIndex index = new FreeRectIndex(10, 10);

        assertThrows(IllegalArgumentException.class, () -> new FreeRectIndex(0, 10));
        assertThrows(IllegalArgumentException.class, () -> index.insert(0, 5));
        assertThrows(IllegalArgumentException.class, () -> index.insert(5, -1));
    }

    @Test
    void resetRestoresSingleFreeRect() {
        FreeRectIndex index = new FreeRectIndex(100, 100);
        index.occupy(new Rect(10, 10, 50, 50));

        index.reset();

        assertEquals(List.of(new Rect(0, 0, 100, 100)), index.freeRects());
    }

    @Test
    void identicalOperationSequencesProduceIdenticalSnapshots() {
        List<Rect> first = allocateSequence();
        List<Rect> second = allocateSequence();

        assertEquals(first, second);
        // Deterministic end state worth pinning: seven heuristic placements in
        // a 200x100 area always leave exactly these free rectangles.
        assertEquals(List.of(new Rect(100, 0, 100, 100), new Rect(80, 60, 120, 40), new Rect(60, 80, 140, 20)), first);
    }

    private static List<Rect> allocateSequence() {
        FreeRectIndex index = new FreeRectIndex(200, 100);
        List<Rect> placements = new ArrayList<>();
        for (int[] size : new int[][] {{40, 40}, {40, 40}, {40, 40}, {40, 40}, {20, 60}, {40, 20}, {20, 20}}) {
            Rect placement = index.insert(size[0], size[1]);
            if (placement != null) {
                placements.add(placement);
            }
        }
        return index.freeRects();
    }

    @Test
    void placementsDoNotOverlapEachOther() {
        FreeRectIndex index = new FreeRectIndex(200, 100);
        List<Rect> placements = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Rect placement = index.insert(20 + (i % 3) * 10, 20 + (i % 2) * 10);
            if (placement != null) {
                placements.add(placement);
            }
        }
        for (int i = 0; i < placements.size(); i++) {
            for (int j = i + 1; j < placements.size(); j++) {
                assertFalse(
                        placements.get(i).intersects(placements.get(j)), "placements " + i + " and " + j + " overlap");
            }
        }
    }
}
