package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceMaskTest {

    @Test
    void everyMaskOwnsADistinctBit() {
        SpaceMask[] masks = SpaceMask.values();
        for (int i = 0; i < masks.length; i++) {
            assertEquals(1 << i, masks[i].bit());
            for (int j = i + 1; j < masks.length; j++) {
                assertFalse(masks[i].intersects(masks[j]), masks[i] + " shares a bit with " + masks[j]);
            }
            assertTrue(masks[i].intersects(masks[i]));
        }
    }

    @Test
    void allBitsIsTheUnionOfEveryMask() {
        int expected = 0;
        for (SpaceMask mask : SpaceMask.values()) {
            expected |= mask.bit();
        }

        assertEquals(expected, SpaceMask.allBits());
        assertEquals((1 << SpaceMask.values().length) - 1, SpaceMask.allBits());
    }

    @Test
    void withinChecksMembershipInABitSet() {
        int bits = SpaceMask.hudBase.bit() | SpaceMask.screenPanel.bit();

        assertTrue(SpaceMask.hudBase.within(bits));
        assertTrue(SpaceMask.screenPanel.within(bits));
        assertFalse(SpaceMask.worldAnchored.within(bits));
        assertFalse(SpaceMask.hudOverlay.within(bits));
    }

    @Test
    void hudMasksAndScreenPanelsStaySeparateLayers() {
        assertFalse(SpaceMask.hudBase.intersects(SpaceMask.hudOverlay));
        assertFalse(SpaceMask.hudBase.intersects(SpaceMask.screenPanel));
        assertFalse(SpaceMask.worldAnchored.intersects(SpaceMask.indicator));
        assertFalse(SpaceMask.debug.intersects(SpaceMask.screenPanel));
    }
}
