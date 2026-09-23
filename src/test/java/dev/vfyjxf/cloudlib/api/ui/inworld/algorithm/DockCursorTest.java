package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.ui.inworld.ScreenEdge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockCursorTest {

    private static final DockCursor bottomEdge() {
        return new DockCursor(ScreenEdge.bottom, 1000, 10, 4);
    }

    private static List<String> ids(List<DockCursor.Slot> slots) {
        return slots.stream().map(DockCursor.Slot::id).toList();
    }

    @Test
    void allocatesSequentiallyFromTheMargin() {
        DockCursor cursor = bottomEdge();

        DockCursor.Slot a = cursor.allocate("a", 100);
        DockCursor.Slot b = cursor.allocate("b", 200);
        DockCursor.Slot c = cursor.allocate("c", 50);

        assertEquals(10, a.start());
        assertEquals(114, b.start());
        assertEquals(318, c.start());
        assertEquals(ScreenEdge.bottom, a.edge());
        assertEquals(314, b.end());
        assertEquals(343, c.center());
    }

    @Test
    void allocationIsFirstFitIntoTheEarliestHole() {
        DockCursor cursor = bottomEdge();
        cursor.allocate("a", 100);
        cursor.allocate("b", 200);
        cursor.allocate("c", 50);

        assertTrue(cursor.release("b"));

        DockCursor.Slot d = cursor.allocate("d", 150);
        assertEquals(114, d.start());
        assertEquals(List.of("a", "d", "c"), ids(cursor.slots()));
    }

    @Test
    void returnsNullWhenNoGapFits() {
        DockCursor cursor = new DockCursor(ScreenEdge.top, 120, 10, 4);

        assertNotNull(cursor.allocate("a", 100));
        assertNull(cursor.allocate("b", 10));
        assertNull(cursor.allocate("c", 5));
    }

    @Test
    void compactionSlidesTowardTheMarginAndPreservesOrder() {
        DockCursor cursor = bottomEdge();
        cursor.allocate("a", 100);
        cursor.allocate("b", 200);
        cursor.allocate("c", 50);
        cursor.allocate("d", 80);
        cursor.release("b");
        cursor.release("d");

        assertTrue(cursor.compact());

        List<DockCursor.Slot> slots = cursor.slots();
        assertEquals(List.of("a", "c"), ids(slots));
        assertEquals(10, slots.get(0).start());
        assertEquals(114, slots.get(1).start());
        assertFalse(cursor.compact());
        assertEquals(slots, cursor.slots());
    }

    @Test
    void compactionNeverReorders() {
        DockCursor cursor = bottomEdge();
        for (int i = 0; i < 8; i++) {
            cursor.allocate("slot" + i, 60);
        }
        for (int i = 1; i < 8; i += 2) {
            cursor.release("slot" + i);
        }

        cursor.compact();

        List<String> remaining = ids(cursor.slots());
        assertEquals(List.of("slot0", "slot2", "slot4", "slot6"), remaining);
        for (int i = 1; i < remaining.size(); i++) {
            assertTrue(cursor.slot(remaining.get(i - 1)).end() <= cursor.slot(remaining.get(i)).start());
        }
    }

    @Test
    void resizeAdaptsWidthToTheContentTierWithoutMoving() {
        DockCursor cursor = bottomEdge();
        cursor.allocate("a", 100);
        DockCursor.Slot b = cursor.allocate("b", 200);
        cursor.allocate("c", 50);

        DockCursor.Slot shrunk = cursor.resize("b", 120);
        assertEquals(114, shrunk.start());
        assertEquals(120, shrunk.width());
        assertEquals(234, shrunk.end());

        assertNull(cursor.resize("b", 210));
        assertEquals(120, cursor.slot("b").width());
        assertEquals(114, cursor.slot("b").start());
    }

    @Test
    void resizeOfTheLastSlotFillsToTheTrailingMargin() {
        DockCursor cursor = bottomEdge();
        cursor.allocate("a", 100);

        DockCursor.Slot grown = cursor.resize("a", 980);
        assertEquals(10, grown.start());
        assertEquals(980, grown.width());
        assertEquals(0, cursor.freeLength());
    }

    @Test
    void releaseOfUnknownIdIsFalseAndLengthsTrackState() {
        DockCursor cursor = bottomEdge();
        assertFalse(cursor.release("nope"));

        cursor.allocate("a", 100);
        cursor.allocate("b", 200);

        assertEquals(300, cursor.usedLength());
        assertEquals(1000 - 10 - 314 - 4, cursor.freeLength());
    }

    @Test
    void rejectsInvalidUse() {
        DockCursor cursor = bottomEdge();
        cursor.allocate("a", 10);

        assertThrows(IllegalArgumentException.class, () -> cursor.allocate("a", 10));
        assertThrows(IllegalArgumentException.class, () -> cursor.allocate("b", 0));
        assertThrows(IllegalArgumentException.class, () -> cursor.allocate("b", Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> cursor.resize("nope", 10));
        assertThrows(IllegalArgumentException.class, () -> cursor.resize("a", -1));
        assertThrows(IllegalArgumentException.class, () -> new DockCursor(ScreenEdge.top, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DockCursor(ScreenEdge.top, 100, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DockCursor(ScreenEdge.top, 100, 0, -1));
    }
}
