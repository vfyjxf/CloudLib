package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchGateTest {

    private static SwitchGate<String> gate(double band, int dwellFrames, double deadZone, int minFramesHeld) {
        return new SwitchGate<>(SwitchGate.Config.of(band, dwellFrames, deadZone, minFramesHeld), "left", 0.0);
    }

    @Test
    void commitsOnNthConsecutiveEligibleTick() {
        SwitchGate<String> gate = gate(10.0, 3, 0.5, 0);

        assertFalse(gate.propose("right", 12.0));
        assertFalse(gate.propose("right", 12.0));
        assertTrue(gate.isDwelling());
        assertTrue(gate.propose("right", 12.0));

        assertEquals("right", gate.current());
        assertFalse(gate.isDwelling());
    }

    @Test
    void pingPongAroundTheThresholdIsSuppressed() {
        SwitchGate<String> gate = gate(10.0, 2, 0.5, 0);

        for (int i = 0; i < 200; i++) {
            double metric = (i % 2 == 0) ? 7.0 : 9.0;
            gate.propose("right", metric);
        }

        assertEquals("left", gate.current());
        assertFalse(gate.isDwelling());
    }

    @Test
    void withoutHysteresisTheSameInputPingPongs() {
        SwitchGate<String> gate = gate(0.1, 1, 0.05, 0);

        gate.propose("right", 9.0);
        assertEquals("right", gate.current());
        gate.propose("left", 7.0);
        assertEquals("left", gate.current());
        gate.propose("right", 9.0);
        assertEquals("right", gate.current());
    }

    @Test
    void bounceBackWithinBandIsSuppressed() {
        SwitchGate<String> gate = gate(10.0, 1, 0.5, 0);

        assertTrue(gate.propose("right", 15.0));
        assertFalse(gate.propose("left", 15.4));
        assertFalse(gate.propose("left", 20.0));
        assertTrue(gate.propose("left", 26.0));

        assertEquals("left", gate.current());
    }

    @Test
    void deadZoneFreezesSubNoiseJitter() {
        SwitchGate<String> frozen = gate(6.0, 1, 2.0, 0);
        for (int i = 0; i < 100; i++) {
            frozen.propose("right", (i % 2 == 0) ? 5.0 : 6.9);
        }
        assertEquals("left", frozen.current());

        SwitchGate<String> open = gate(6.0, 1, 0.0, 0);
        assertFalse(open.propose("right", 5.0));
        assertTrue(open.propose("right", 6.9));
    }

    @Test
    void steadySignalBeyondBandCommitsDespiteDeadZone() {
        SwitchGate<String> gate = gate(6.0, 2, 2.0, 0);

        assertFalse(gate.propose("right", 20.0));
        assertTrue(gate.propose("right", 20.1));

        assertEquals("right", gate.current());
    }

    @Test
    void dwellResetsWhenMetricDipsBackInsideBand() {
        SwitchGate<String> gate = gate(10.0, 3, 0.5, 0);

        assertFalse(gate.propose("right", 12.0));
        assertFalse(gate.propose("right", 12.0));
        assertFalse(gate.propose("right", 9.0));
        assertFalse(gate.propose("right", 12.0));
        assertFalse(gate.propose("right", 12.0));
        assertTrue(gate.propose("right", 12.0));
    }

    @Test
    void dwellResetsWhenCandidateChanges() {
        SwitchGate<String> gate = gate(10.0, 2, 0.5, 0);

        assertFalse(gate.propose("b", 12.0));
        assertFalse(gate.propose("c", 12.0));
        assertTrue(gate.propose("c", 12.0));

        assertEquals("c", gate.current());
    }

    @Test
    void proposingTheIncumbentClearsPendingDwell() {
        SwitchGate<String> gate = gate(10.0, 3, 0.5, 0);

        assertFalse(gate.propose("right", 12.0));
        assertFalse(gate.propose("right", 12.0));
        assertFalse(gate.propose("left", 0.0));
        assertFalse(gate.isDwelling());
        assertFalse(gate.propose("right", 12.0));
        assertFalse(gate.propose("right", 12.0));
        assertTrue(gate.propose("right", 12.0));
    }

    @Test
    void minFramesHeldDefersCommitsUntilHeldLongEnough() {
        SwitchGate<String> gate = gate(10.0, 1, 0.5, 5);

        assertFalse(gate.propose("right", 15.0));
        assertFalse(gate.propose("right", 15.0));
        assertFalse(gate.propose("right", 15.0));
        assertFalse(gate.propose("right", 15.0));
        assertTrue(gate.propose("right", 15.0));

        assertFalse(gate.propose("left", 30.0));
        assertFalse(gate.propose("left", 30.0));
        assertFalse(gate.propose("left", 30.0));
        assertFalse(gate.propose("left", 30.0));
        assertTrue(gate.propose("left", 30.0));
    }

    @Test
    void boundaryMetricsAreInclusive() {
        SwitchGate<String> exactBand = gate(10.0, 1, 2.0, 0);
        assertTrue(exactBand.propose("right", 10.0));

        SwitchGate<String> justBelowBand = gate(10.0, 1, 2.0, 0);
        assertFalse(justBelowBand.propose("right", 10.0 - 1.0e-9));

        SwitchGate<String> exactDeadZone = gate(3.0, 1, 2.0, 0);
        assertFalse(exactDeadZone.propose("left", 2.0));
        assertTrue(exactDeadZone.propose("right", 4.0));
    }

    @Test
    void rejectsInvalidConstructionAndMetrics() {
        assertThrows(IllegalArgumentException.class, () -> SwitchGate.Config.of(0.0, 1, 0.0, 0));
        assertThrows(IllegalArgumentException.class, () -> SwitchGate.Config.of(-1.0, 1, 0.0, 0));
        assertThrows(IllegalArgumentException.class, () -> SwitchGate.Config.of(Double.NaN, 1, 0.0, 0));
        assertThrows(IllegalArgumentException.class, () -> SwitchGate.Config.of(10.0, 0, 0.0, 0));
        assertThrows(IllegalArgumentException.class, () -> SwitchGate.Config.of(10.0, -1, 0.0, 0));
        assertThrows(IllegalArgumentException.class, () -> SwitchGate.Config.of(10.0, 1, -0.1, 0));
        assertThrows(IllegalArgumentException.class, () -> SwitchGate.Config.of(10.0, 1, Double.POSITIVE_INFINITY, 0));
        assertThrows(IllegalArgumentException.class, () -> SwitchGate.Config.of(10.0, 1, 0.0, -1));

        SwitchGate<String> gate = gate(10.0, 1, 0.5, 0);
        assertThrows(IllegalArgumentException.class, () -> gate.propose("right", Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> gate.propose("right", Double.NEGATIVE_INFINITY));

        assertThrows(NullPointerException.class, () -> new SwitchGate<>(SwitchGate.Config.of(10, 1, 0, 0), null, 0.0));
        assertThrows(NullPointerException.class, () -> gate.propose(null, 1.0));
        assertThrows(
            IllegalArgumentException.class,
            () -> new SwitchGate<>(SwitchGate.Config.of(10, 1, 0, 0), "s", Double.NaN)
        );
    }
}
