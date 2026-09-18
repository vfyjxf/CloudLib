package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FocusBubbleTest {

    @Test
    void startsNeverActivated() {
        FocusBubble bubble = FocusBubble.withDefaults();

        assertNull(bubble.active());
        assertFalse(bubble.inside());
        assertFalse(bubble.everActivated());
        assertEquals(FocusBubble.Config.defaults(), bubble.config());
    }

    @Test
    void entersAtTheExpandRadiusAndLeavesOnlyBeyondCollapse() {
        FocusBubble bubble = new FocusBubble(FocusBubble.Config.of(130.0, 180.0));

        // far away: no activation, no flip
        assertFalse(bubble.update(200.0));
        assertNull(bubble.active());

        // at the expand boundary (inclusive): enters, first activation is a flip
        assertTrue(bubble.update(130.0));
        assertTrue(bubble.inside());
        assertTrue(bubble.everActivated());
        assertEquals(Boolean.TRUE, bubble.active());

        // at the collapse boundary exactly: stays inside (exit is strict)
        assertFalse(bubble.update(180.0));
        assertTrue(bubble.inside());

        // beyond collapse: leaves
        assertTrue(bubble.update(180.5));
        assertFalse(bubble.inside());
        assertEquals(Boolean.FALSE, bubble.active());
        assertTrue(bubble.everActivated());

        // well inside the hysteresis band: no re-entry
        assertFalse(bubble.update(179.0));
        assertFalse(bubble.inside());

        // back within expand: re-enters
        assertTrue(bubble.update(130.0));
        assertTrue(bubble.inside());
    }

    @Test
    void jitterAroundTheExpandThresholdNeverFlipsBack() {
        FocusBubble bubble = new FocusBubble(FocusBubble.Config.of(130.0, 180.0));

        // approach jittering 131/129: the first 129 enters, the 131s can never leave
        assertFalse(bubble.update(131.0));
        assertNull(bubble.active());
        assertTrue(bubble.update(129.0));
        for (int i = 0; i < 100; i++) {
            double distance = (i % 2 == 0) ? 131.0 : 129.0;
            assertFalse(bubble.update(distance), "unexpected flip at " + distance);
            assertTrue(bubble.inside(), "unexpected leave at " + distance);
        }
    }

    @Test
    void jitterAroundTheCollapseThresholdNeverFlipsBack() {
        FocusBubble bubble = new FocusBubble(FocusBubble.Config.of(130.0, 180.0));

        // enter, then jitter 179/181: the first 181 leaves, the 179s can never re-enter
        assertTrue(bubble.update(100.0));
        assertTrue(bubble.update(181.0));
        assertFalse(bubble.inside());
        for (int i = 0; i < 100; i++) {
            double distance = (i % 2 == 0) ? 179.0 : 181.0;
            assertFalse(bubble.update(distance), "unexpected flip at " + distance);
            assertFalse(bubble.inside(), "unexpected re-entry at " + distance);
        }
    }

    @Test
    void mixedBoundaryJitterStaysPut() {
        FocusBubble bubble = new FocusBubble(FocusBubble.Config.of(130.0, 180.0));

        // a long wander inside the hysteresis band: 129..180 never leaves once in
        assertTrue(bubble.update(129.0));
        double[] wander = {180, 131, 179, 130, 175, 129, 132, 180};
        for (double distance : wander) {
            assertFalse(bubble.update(distance));
            assertTrue(bubble.inside());
        }
        // only a clear exit (> 180) flips out
        assertTrue(bubble.update(181.0));
        assertFalse(bubble.inside());
    }

    @Test
    void zeroDistanceEntersImmediately() {
        FocusBubble bubble = FocusBubble.withDefaults();

        assertTrue(bubble.update(0.0));
        assertTrue(bubble.inside());
    }

    @Test
    void defaultsMatchTheSpecifiedRadii() {
        assertEquals(new FocusBubble.Config(130.0, 180.0), FocusBubble.Config.defaults());
    }

    @Test
    void rejectsInvalidUse() {
        assertThrows(IllegalArgumentException.class, () -> FocusBubble.Config.of(0.0, 180.0));
        assertThrows(IllegalArgumentException.class, () -> FocusBubble.Config.of(180.0, 180.0));
        assertThrows(IllegalArgumentException.class, () -> FocusBubble.Config.of(200.0, 180.0));
        assertThrows(IllegalArgumentException.class, () -> FocusBubble.Config.of(130.0, Double.NaN));
        FocusBubble bubble = FocusBubble.withDefaults();
        assertThrows(IllegalArgumentException.class, () -> bubble.update(-1.0));
        assertThrows(IllegalArgumentException.class, () -> bubble.update(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> bubble.update(Double.POSITIVE_INFINITY));
    }
}
