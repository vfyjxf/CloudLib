package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The input-level dead zone of {@link DisplacementGate}: the held point is
 * the reference until the live point escapes the release radius, so
 * sub-threshold drift is invisible downstream while every release is a real
 * position the gate actually saw.
 */
class DisplacementGateTest {

    private static final double release = 4.0;

    private final DisplacementGate gate = new DisplacementGate(DisplacementGate.Config.of(release));

    @Test
    void firstObservationSeedsTheReference() {
        assertEquals(new FloatPos(10, 10), gate.observe(new FloatPos(10, 10)));
        assertEquals(new FloatPos(10, 10), gate.held());
    }

    @Test
    void subThresholdDriftIsInvisible() {
        gate.observe(new FloatPos(100, 100));
        // wander staying inside the release radius measured from the held
        // reference — including diagonals (euclidean, not per-axis)
        assertEquals(new FloatPos(100, 100), gate.observe(new FloatPos(103, 100)));
        assertEquals(new FloatPos(100, 100), gate.observe(new FloatPos(102, 102)));
        assertEquals(new FloatPos(100, 100), gate.observe(new FloatPos(98, 98)));
        assertEquals(new FloatPos(100, 100), gate.observe(new FloatPos(100, 100)));
        assertEquals(new FloatPos(100, 100), gate.held());
    }

    @Test
    void releaseReanchorsAtExactlyTheThreshold() {
        gate.observe(new FloatPos(0, 0));
        // exactly release px — inclusive boundary
        assertEquals(new FloatPos(4, 0), gate.observe(new FloatPos(4, 0)));
        assertEquals(new FloatPos(4, 0), gate.held());
        // and the reference is measured from the new anchor, not the old one
        assertEquals(new FloatPos(4, 0), gate.observe(new FloatPos(7, 0)));
        assertEquals(new FloatPos(8, 0), gate.observe(new FloatPos(8, 0)));
    }

    @Test
    void euclideanDistanceGovernsNotPerAxis() {
        gate.observe(new FloatPos(0, 0));
        // (3, 3) is 4.24 px away — above the release radius although each axis
        // moved less than the threshold
        assertEquals(new FloatPos(3, 3), gate.observe(new FloatPos(3, 3)));
    }

    @Test
    void theHeldPointIsAlwaysAPreviouslySeenPosition() {
        FloatPos live = new FloatPos(50, 50);
        FloatPos held = gate.observe(live);
        for (int i = 0; i < 200; i++) {
            double angle = i * 0.7;
            double radius = (i % 7) * 1.3;
            live = new FloatPos(live.x() + Math.cos(angle) * radius, live.y() + Math.sin(angle) * radius);
            FloatPos next = gate.observe(live);
            // every release lands exactly on the live point; every hold keeps
            // the previous reference — never an invented in-between position
            assertTrue(
                next.equals(live) || next.equals(held),
                "held " + next + " must be the live " + live + " or the previous reference " + held
            );
            held = next;
        }
    }

    @Test
    void nullObservationResetsAndReseeds() {
        gate.observe(new FloatPos(10, 10));
        assertNull(gate.observe(null));
        assertNull(gate.held());
        assertEquals(new FloatPos(500, 500), gate.observe(new FloatPos(500, 500)));
    }

    @Test
    void resetDropsTheReference() {
        gate.observe(new FloatPos(10, 10));
        gate.reset();
        assertNull(gate.held());
        assertEquals(new FloatPos(20, 20), gate.observe(new FloatPos(20, 20)));
    }

    @Test
    void configRejectsNonPositiveRelease() {
        assertThrows(IllegalArgumentException.class, () -> DisplacementGate.Config.of(0.0));
        assertThrows(IllegalArgumentException.class, () -> DisplacementGate.Config.of(-1.0));
        assertThrows(IllegalArgumentException.class, () -> DisplacementGate.Config.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> DisplacementGate.Config.of(Double.POSITIVE_INFINITY));
    }

    @Test
    void configRejectsNull() {
        assertThrows(NullPointerException.class, () -> new DisplacementGate(null));
    }
}
