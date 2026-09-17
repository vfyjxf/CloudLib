package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntervalSetTest {

    @Test
    void startsEmpty() {
        IntervalSet set = new IntervalSet();

        assertTrue(set.isEmpty());
        assertEquals(List.of(), set.intervals());
        assertEquals(0.0, set.totalLength(), 1e-12);
    }

    @Test
    void addKeepsSingleInterval() {
        IntervalSet set = IntervalSet.of(1, 2);

        assertEquals(List.of(new IntervalSet.Interval(1, 2)), set.intervals());
        assertTrue(set.contains(1));
        assertTrue(set.contains(1.5));
        assertFalse(set.contains(2));
    }

    @Test
    void addMergesOverlappingIntervals() {
        IntervalSet set = new IntervalSet();
        set.add(1, 3);
        set.add(2, 4);

        assertEquals(List.of(new IntervalSet.Interval(1, 4)), set.intervals());
    }

    @Test
    void addMergesAdjacentIntervals() {
        IntervalSet set = new IntervalSet();
        set.add(1, 2);
        set.add(2, 3);

        assertEquals(List.of(new IntervalSet.Interval(1, 3)), set.intervals());
        assertEquals(2.0, set.totalLength(), 1e-12);
    }

    @Test
    void addKeepsDisjointIntervalsSorted() {
        IntervalSet set = new IntervalSet();
        set.add(5, 6);
        set.add(1, 2);
        set.add(3, 3.5);

        assertEquals(
                List.of(
                        new IntervalSet.Interval(1, 2),
                        new IntervalSet.Interval(3, 3.5),
                        new IntervalSet.Interval(5, 6)),
                set.intervals());
    }

    @Test
    void addBridgesGapBetweenIntervals() {
        IntervalSet set = new IntervalSet();
        set.add(1, 2);
        set.add(4, 5);
        set.add(2, 4);

        assertEquals(List.of(new IntervalSet.Interval(1, 5)), set.intervals());
    }

    @Test
    void emptyAndInvertedIntervalsAreNoOps() {
        IntervalSet set = IntervalSet.of(1, 2);
        set.add(3, 3);
        set.add(5, 4);
        set.subtract(2, 2);
        set.subtract(4, 3);

        assertEquals(List.of(new IntervalSet.Interval(1, 2)), set.intervals());
    }

    @Test
    void subtractSplitsMiddleOut() {
        IntervalSet set = IntervalSet.of(0, 10);
        set.subtract(4, 6);

        assertEquals(List.of(new IntervalSet.Interval(0, 4), new IntervalSet.Interval(6, 10)), set.intervals());
    }

    @Test
    void subtractTrimsOverlappingEnds() {
        IntervalSet set = IntervalSet.of(2, 8);

        set.subtract(0, 4);
        assertEquals(List.of(new IntervalSet.Interval(4, 8)), set.intervals());

        set.subtract(6, 10);
        assertEquals(List.of(new IntervalSet.Interval(4, 6)), set.intervals());
    }

    @Test
    void subtractSwallowingWholeIntervalEmptiesIt() {
        IntervalSet set = IntervalSet.of(2, 8);

        set.subtract(0, 10);

        assertTrue(set.isEmpty());
    }

    @Test
    void subtractOutsideRangeIsNoOp() {
        IntervalSet set = IntervalSet.of(2, 8);

        set.subtract(10, 12);
        set.subtract(0, 1);

        assertEquals(List.of(new IntervalSet.Interval(2, 8)), set.intervals());
    }

    @Test
    void subtractOnlyHitsOverlappingIntervals() {
        IntervalSet set = new IntervalSet();
        set.add(1, 2);
        set.add(4, 5);
        set.add(7, 8);

        set.subtract(4.5, 7.5);

        assertEquals(
                List.of(
                        new IntervalSet.Interval(1, 2),
                        new IntervalSet.Interval(4, 4.5),
                        new IntervalSet.Interval(7.5, 8)),
                set.intervals());
    }

    @Test
    void intervalRejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () -> new IntervalSet.Interval(2, 1));
    }

    @Test
    void complementWithinBounds() {
        IntervalSet set = new IntervalSet();
        set.add(1, 2);
        set.add(4, 5);

        assertEquals(
                List.of(new IntervalSet.Interval(0, 1), new IntervalSet.Interval(2, 4), new IntervalSet.Interval(5, 8)),
                set.complement(0, 8));
    }

    @Test
    void complementOfEmptyIsWholeRange() {
        IntervalSet set = new IntervalSet();

        assertEquals(List.of(new IntervalSet.Interval(0, 1)), set.complement(0, 1));
    }

    @Test
    void complementIgnoresIntervalsOutsideBounds() {
        IntervalSet set = new IntervalSet();
        set.add(0, 2);
        set.add(6, 8);

        assertEquals(List.of(new IntervalSet.Interval(3, 5)), set.complement(3, 5));
    }

    @Test
    void clearRemovesEverything() {
        IntervalSet set = IntervalSet.of(1, 2);
        set.add(4, 6);

        set.clear();

        assertTrue(set.isEmpty());
    }
}
