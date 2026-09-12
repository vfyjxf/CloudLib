package dev.vfyjxf.nimbusprojection.internal;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trail collection for world-drags: dedup, hover-throttling and the capacity
 * bound — the commit list must be exactly the containers the player swept.
 */
class DragTrailTest {

    private static final BlockPos A = new BlockPos(1, 0, 0);
    private static final BlockPos B = new BlockPos(2, 0, 0);
    private static final BlockPos C = new BlockPos(3, 0, 0);

    @Test
    void sweepingDistinctContainersAppendsInOrder() {
        DragTrail t = new DragTrail();
        assertTrue(t.offer(A));
        assertTrue(t.offer(B));
        assertTrue(t.offer(C));
        assertEquals(List.of(A, B, C), t.targets());
    }

    @Test
    void hoveringTheSameBlockDoesNotGrow() {
        DragTrail t = new DragTrail();
        assertTrue(t.offer(A));
        for (int i = 0; i < 20; i++) assertFalse(t.offer(A));
        assertEquals(1, t.size());
    }

    @Test
    void revisitingAnOldContainerDoesNotDuplicate() {
        DragTrail t = new DragTrail();
        t.offer(A);
        t.offer(B);
        assertFalse(t.offer(A)); //swept back over A — already in the trail
        assertEquals(List.of(A, B), t.targets());
    }

    @Test
    void leavingContainersReArmsConsecutiveLatch() {
        DragTrail t = new DragTrail();
        t.offer(A);
        t.leave();          //looked at air
        assertFalse(t.offer(A)); //A is in the trail — still no duplicate
        assertEquals(1, t.size());
    }

    @Test
    void capacityCapsTheCommitList() {
        DragTrail t = new DragTrail();
        for (int i = 0; i < DragTrail.CAPACITY + 4; i++) {
            t.offer(new BlockPos(i, 0, 0));
        }
        assertEquals(DragTrail.CAPACITY, t.size());
        assertEquals(DragTrail.CAPACITY, t.targets().size());
    }

    @Test
    void clearResetsEverything() {
        DragTrail t = new DragTrail();
        t.offer(A);
        t.clear();
        assertTrue(t.isEmpty());
        assertTrue(t.offer(A)); //A can be trailed again after a clear
    }
}
