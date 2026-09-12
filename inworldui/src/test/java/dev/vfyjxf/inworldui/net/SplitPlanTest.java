package dev.vfyjxf.inworldui.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Item split math — the exact contract the server re-runs at commit time.
 * Determinism matters: client preview and server transfer must agree.
 */
class SplitPlanTest {

    @Test
    void evenSplitDistributesRemainderLeftToRight() {
        assertArrayEquals(new int[]{4, 3, 3}, SplitPlan.evenly(10, 3));
        assertArrayEquals(new int[]{2, 2, 2}, SplitPlan.evenly(6, 3));
        assertArrayEquals(new int[]{64}, SplitPlan.evenly(64, 1));
    }

    @Test
    void evenSplitWhenCountBelowTargetsZerosTail() {
        assertArrayEquals(new int[]{1, 1, 0, 0}, SplitPlan.evenly(2, 4));
        assertArrayEquals(new int[]{0, 0}, SplitPlan.evenly(0, 2));
    }

    @Test
    void degenerateInputsYieldEmptyShares() {
        assertArrayEquals(new int[0], SplitPlan.evenly(5, 0));
        assertArrayEquals(new int[0], SplitPlan.evenly(5, -2));
        assertArrayEquals(new int[]{0}, SplitPlan.evenly(-1, 1));
    }

    @Test
    void oneEachGivesSinglesUntilOut() {
        assertArrayEquals(new int[]{1, 1, 1}, SplitPlan.oneEach(3, 3));
        assertArrayEquals(new int[]{1, 1, 0, 0}, SplitPlan.oneEach(2, 4));
        //more items than targets still gives exactly one each — no stacking up
        assertArrayEquals(new int[]{1, 1, 1}, SplitPlan.oneEach(64, 3));
        assertArrayEquals(new int[]{0}, SplitPlan.oneEach(0, 1));
    }

    @Test
    void sharesAlwaysSumToAtMostCount() {
        int[] even = SplitPlan.evenly(17, 5);
        int sum = 0;
        for (int s : even) sum += s;
        assertEquals(17, sum); //even split conserves every item

        int[] one = SplitPlan.oneEach(17, 5);
        int osum = 0;
        for (int s : one) osum += s;
        assertEquals(5, osum); //one-each keeps the leftover in the source slot
    }

    @Test
    void usedCountsNonZeroShares() {
        assertEquals(3, SplitPlan.used(SplitPlan.evenly(10, 3)));
        assertEquals(2, SplitPlan.used(SplitPlan.evenly(2, 4)));
        assertEquals(0, SplitPlan.used(new int[0]));
    }
}
