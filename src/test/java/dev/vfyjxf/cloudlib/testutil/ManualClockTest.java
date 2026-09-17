package dev.vfyjxf.cloudlib.testutil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManualClockTest {

    @Test
    void startsAtZeroTimeAndZeroFrames() {
        ManualClock clock = new ManualClock();

        assertEquals(0.0, clock.now());
        assertEquals(0L, clock.frameCount());
    }

    @Test
    void advanceAccumulatesTimeAndCountsFrames() {
        ManualClock clock = new ManualClock();

        clock.advance(0.5).advance(1.0 / 60.0);

        assertEquals(0.5 + 1.0 / 60.0, clock.now(), 1.0e-12);
        assertEquals(2L, clock.frameCount());
    }

    @Test
    void zeroDtIsAValidFrame() {
        ManualClock clock = new ManualClock();

        clock.advance(0);

        assertEquals(0.0, clock.now());
        assertEquals(1L, clock.frameCount());
    }

    @Test
    void advanceFramesStepsTheGivenNumberOfUniformFrames() {
        ManualClock clock = new ManualClock();

        clock.advance(0.1).advanceFrames(120, 1.0 / 60.0);

        assertEquals(0.1 + 120.0 / 60.0, clock.now(), 1.0e-9);
        assertEquals(121L, clock.frameCount());
    }

    @Test
    void rejectsInvalidDt() {
        ManualClock clock = new ManualClock();

        assertThrows(IllegalArgumentException.class, () -> clock.advance(-1.0e-4));
        assertThrows(IllegalArgumentException.class, () -> clock.advance(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> clock.advance(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> clock.advanceFrames(4, -1));
        assertThrows(IllegalArgumentException.class, () -> clock.advanceFrames(-1, 1.0 / 60.0));

        assertEquals(0.0, clock.now());
        assertEquals(0L, clock.frameCount());
    }
}
