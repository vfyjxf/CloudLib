package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.testutil.FrameReplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothingTest {

    private static final double twoPi = 2.0 * Math.PI;

    @Test
    void dampMovesTowardTargetWithoutOvershoot() {
        double current = 0.0;
        for (int i = 0; i < 500; i++) {
            current = Smoothing.damp(current, 10.0, 3.0, 1.0 / 60.0);
            assertTrue(current >= 0.0 && current <= 10.0, "left the cur..target interval: " + current);
        }
        assertEquals(10.0, current, 1.0e-6);
    }

    @Test
    void dampWithZeroDtReturnsCurrent() {
        assertEquals(4.5, Smoothing.damp(4.5, 100.0, 3.0, 0.0), 0.0);
    }

    @Test
    void negativeFiniteDtIsClampedToAZeroLengthStep() {
        assertEquals(4.5, Smoothing.damp(4.5, 100.0, 3.0, -0.05), 0.0);
        assertEquals(4.5, Smoothing.dampHalfLife(4.5, 100.0, 0.5, -0.05), 0.0);
        assertEquals(1.25, Smoothing.dampAngle(1.25, -1.25, 3.0, -0.05), 0.0);
    }

    @Test
    void dampReachesTargetWhenRateTimesDtIsHuge() {
        assertEquals(10.0, Smoothing.damp(0.0, 10.0, 1000.0, 10.0), 1.0e-12);
    }

    @Test
    void dampHalfLifeHalvesDistancePerHalfLife() {
        assertEquals(5.0, Smoothing.dampHalfLife(0.0, 10.0, 0.5, 0.5), 1.0e-12);
        assertEquals(7.5, Smoothing.dampHalfLife(0.0, 10.0, 0.5, 1.0), 1.0e-12);
    }

    @Test
    void dampHalfLifeAgreesWithEquivalentLambda() {
        double viaHalfLife = Smoothing.dampHalfLife(2.0, 9.0, 0.35, 1.0 / 60.0);
        double viaLambda = Smoothing.damp(2.0, 9.0, Math.log(2.0) / 0.35, 1.0 / 60.0);
        assertEquals(viaLambda, viaHalfLife, 1.0e-15);
    }

    @Test
    void dampIsFrameRateIndependent() {
        double at60fps = replayDamp(1.0 / 60.0, 72);
        double at240fps = replayDamp(1.0 / 240.0, 288);

        assertEquals(at240fps, at60fps, 1.0e-9);
    }

    @Test
    void dampAngleTakesShortestArcUpwardThroughWrap() {
        double current = Math.toRadians(350.0);
        double target = Math.toRadians(10.0);

        double next = Smoothing.dampAngle(current, target, 1.0, 0.5);

        double expected = current + Math.toRadians(20.0) * (1.0 - Math.exp(-0.5));
        assertEquals(expected, next, 1.0e-12);
        assertTrue(next > current, "must move up through the wrap, not down the long way");
    }

    @Test
    void dampAngleTakesShortestArcDownwardThroughWrap() {
        double current = Math.toRadians(10.0);
        double target = Math.toRadians(350.0);

        double next = Smoothing.dampAngle(current, target, 1.0, 0.5);

        double expected = current - Math.toRadians(20.0) * (1.0 - Math.exp(-0.5));
        assertEquals(expected, next, 1.0e-12);
    }

    @Test
    void dampAngleConvergesAcrossTheWrap() {
        double current = Math.toRadians(350.0);
        for (int i = 0; i < 200; i++) {
            current = Smoothing.dampAngle(current, Math.toRadians(10.0), 6.0, 1.0 / 60.0);
        }
        double wrapped = ((current % twoPi) + twoPi) % twoPi;

        assertEquals(Math.toRadians(10.0), wrapped, 1.0e-6);
    }

    @Test
    void dampAngleIsFrameRateIndependent() {
        double at60fps = replayDampAngle(1.0 / 60.0, 120);
        double at240fps = replayDampAngle(1.0 / 240.0, 480);

        assertEquals(at240fps, at60fps, 1.0e-9);
    }

    @Test
    void dampAngleWithZeroDtReturnsCurrent() {
        assertEquals(1.25, Smoothing.dampAngle(1.25, -1.25, 3.0, 0.0), 0.0);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> Smoothing.damp(0.0, 1.0, 0.0, 0.016));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.damp(0.0, 1.0, -1.0, 0.016));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.damp(0.0, 1.0, Double.NaN, 0.016));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.damp(0.0, 1.0, 3.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.damp(0.0, 1.0, 3.0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.dampHalfLife(0.0, 1.0, 0.0, 0.016));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.dampHalfLife(0.0, 1.0, -0.5, 0.016));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.dampHalfLife(0.0, 1.0, Double.NaN, 0.016));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.dampHalfLife(0.0, 1.0, 0.5, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.dampAngle(0.0, 1.0, 0.0, 0.016));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.dampAngle(0.0, 1.0, 2.0, Double.NaN));
        assertThrows(
                IllegalArgumentException.class, () -> Smoothing.dampAngle(0.0, 1.0, 2.0, Double.NEGATIVE_INFINITY));
    }

    private static double replayDamp(double dtSeconds, int frames) {
        double[] value = {0.0};
        FrameReplay<Double, Double> replay = FrameReplay.runUniform(
                value,
                dtSeconds,
                frames,
                0.0,
                (subject, frameDt, input) -> subject[0] = Smoothing.damp(subject[0], 10.0, 3.0, frameDt));
        return replay.lastOutput();
    }

    private static double replayDampAngle(double dtSeconds, int frames) {
        double[] angle = {Math.toRadians(350.0)};
        FrameReplay<Double, Double> replay = FrameReplay.runUniform(
                angle,
                dtSeconds,
                frames,
                0.0,
                (subject, frameDt, input) ->
                        subject[0] = Smoothing.dampAngle(subject[0], Math.toRadians(10.0), 4.0, frameDt));
        return replay.lastOutput();
    }
}
