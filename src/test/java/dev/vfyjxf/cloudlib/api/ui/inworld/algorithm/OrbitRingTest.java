package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.RayFan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbitRingTest {

    private static OrbitRing rings() {
        return new OrbitRing(60, 40, 30, 4);
    }

    @Test
    void ringGeometryFollowsConfiguration() {
        OrbitRing ring = rings();

        assertEquals(60, ring.ringRadius(0));
        assertEquals(100, ring.ringRadius(1));
        assertEquals(4, ring.maxRings());

        // circumference / desired arc length, at least two slots per ring
        assertEquals(12, ring.ringSlotCount(0));
        assertEquals(20, ring.ringSlotCount(1));
        assertEquals(Math.PI / 12, ring.slotHalfArc(0));
    }

    @Test
    void evenRingsStartAtZeroAndOddRingsAreStaggered() {
        OrbitRing ring = rings();

        assertEquals(0, ring.slot(0, 0).angle(), 1.0e-12);
        assertEquals(2 * Math.PI * 5 / 12, ring.slot(0, 5).angle(), 1.0e-12);
        assertEquals(Math.PI / 20, ring.slot(1, 0).angle(), 1.0e-12);

        List<OrbitRing.Slot> ringOne = ring.ringSlots(1);
        assertEquals(20, ringOne.size());
        for (int i = 0; i < ringOne.size(); i++) {
            assertEquals(i, ringOne.get(i).index());
        }
    }

    @Test
    void slotOffsetsArePolarToCartesian() {
        OrbitRing.Slot slot = rings().slot(0, 3);

        assertEquals(60 * Math.cos(slot.angle()), slot.offsetX(), 1.0e-12);
        assertEquals(60 * Math.sin(slot.angle()), slot.offsetY(), 1.0e-12);
    }

    @Test
    void blockedArcsRemoveTheSlotsTheyShadow() {
        OrbitRing ring = rings();
        RayFan fan = new RayFan(500, 400, 200);
        fan.block(new Rect(540, 380, 20, 40));

        // the rect subtends roughly ±0.46 rad around angle 0: slots 11, 0 and 1
        // straddle it, everything else stays free
        assertFalse(ring.isFree(ring.slot(0, 11), fan));
        assertFalse(ring.isFree(ring.slot(0, 0), fan));
        assertFalse(ring.isFree(ring.slot(0, 1), fan));
        assertTrue(ring.isFree(ring.slot(0, 2), fan));
        assertTrue(ring.isFree(ring.slot(0, 6), fan));

        List<OrbitRing.Slot> free = ring.freeSlots(fan);
        assertEquals(9, free.stream().filter(slot -> slot.ring() == 0).count());
        for (OrbitRing.Slot slot : free) {
            if (slot.ring() != 0) {
                continue;
            }
            assertTrue(slot.index() >= 2 && slot.index() <= 10, "unexpected free slot " + slot);
        }
    }

    @Test
    void anOccluderContainingTheAnchorBlocksEverything() {
        OrbitRing ring = rings();
        RayFan fan = new RayFan(500, 400, 200);
        fan.block(new Rect(400, 300, 200, 200));

        assertTrue(ring.freeSlots(fan).isEmpty());
        assertFalse(ring.isFree(ring.slot(2, 0), fan));
    }

    @Test
    void capacityExhaustionSpillsIntoTheNextRing() {
        OrbitRing ring = rings();
        RayFan fan = new RayFan(500, 400, 200);

        List<OrbitRing.Slot> free = ring.freeSlots(fan, 20);

        assertEquals(20, free.size());
        assertEquals(12, free.stream().filter(slot -> slot.ring() == 0).count());
        assertEquals(8, free.stream().filter(slot -> slot.ring() == 1).count());
        for (int i = 1; i < free.size(); i++) {
            int previous = free.get(i - 1).ring();
            int current = free.get(i).ring();
            assertTrue(previous < current
                    || (previous == current
                            && free.get(i - 1).index() < free.get(i).index()));
        }
    }

    @Test
    void ringBoundsAndCountsAreEnforced() {
        OrbitRing ring = rings();

        assertEquals(rings().ringSlots(1), ring.ringSlots(1));
        assertThrows(IllegalArgumentException.class, () -> ring.ringRadius(4));
        assertThrows(IllegalArgumentException.class, () -> ring.ringRadius(-1));
        assertThrows(IllegalArgumentException.class, () -> ring.slot(4, 0));
        assertThrows(IllegalArgumentException.class, () -> new OrbitRing(0, 40, 30, 4));
        assertThrows(IllegalArgumentException.class, () -> new OrbitRing(60, 0, 30, 4));
        assertThrows(IllegalArgumentException.class, () -> new OrbitRing(60, 40, 0, 4));
        assertThrows(IllegalArgumentException.class, () -> new OrbitRing(60, 40, 30, 0));
        assertEquals(List.of(), ring.freeSlots(new RayFan(500, 400, 200), 0));
    }

    @Test
    void sameConfigurationYieldsTheSameSlots() {
        RayFan fan = new RayFan(500, 400, 200);
        fan.blockAll(List.of(new Rect(560, 460, 40, 20), new Rect(380, 300, 30, 30)));

        assertEquals(rings().freeSlots(fan, 30), rings().freeSlots(fan, 30));
        assertEquals(rings().slot(2, 7), rings().slot(2, 7));
    }
}
