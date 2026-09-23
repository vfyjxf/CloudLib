package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.testutil.FrameReplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Spring2Test {

    @Test
    void settlesMonotonicallyOnTargetFromRestWithoutOvershoot() {
        Spring2 spring = new Spring2(8.0, 0.0, 0.0);

        double previousX = Double.NEGATIVE_INFINITY;
        double previousY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 240; i++) {
            spring.step(100.0, 50.0, 1.0 / 60.0);
            assertTrue(spring.x() <= 100.0 + 1.0e-9, "x overshoot at frame " + i + ": " + spring.x());
            assertTrue(spring.y() <= 50.0 + 1.0e-9, "y overshoot at frame " + i + ": " + spring.y());
            assertTrue(spring.x() >= previousX, "x must not reverse while approaching: " + spring.x());
            assertTrue(spring.y() >= previousY, "y must not reverse while approaching: " + spring.y());
            previousX = spring.x();
            previousY = spring.y();
        }

        assertEquals(100.0, spring.x(), 1.0e-6);
        assertEquals(50.0, spring.y(), 1.0e-6);
        assertEquals(0.0, spring.velocityX(), 1.0e-6);
        assertEquals(0.0, spring.velocityY(), 1.0e-6);
    }

    @Test
    void impulseCarriesVelocityAndDecaysBack() {
        Spring2 spring = new Spring2(8.0, 0.0, 0.0);
        spring.impulse(50.0, 0.0);

        double maxX = 0.0;
        for (int i = 0; i < 480; i++) {
            spring.step(0.0, 0.0, 1.0 / 60.0);
            maxX = Math.max(maxX, spring.x());
        }

        assertEquals(50.0 / 8.0 * Math.exp(-1.0), maxX, 0.05);
        assertEquals(0.0, spring.x(), 1.0e-3);
        assertEquals(0.0, spring.velocityX(), 1.0e-3);
    }

    @Test
    void axesAreIndependent() {
        Spring2 spring = new Spring2(6.0, 0.0, 0.0);
        spring.impulse(30.0, 0.0);

        for (int i = 0; i < 480; i++) {
            spring.step(0.0, 40.0, 1.0 / 60.0);
            assertTrue(spring.velocityY() >= -1.0e-9, "the x impulse must not disturb y: " + spring.velocityY());
        }

        assertEquals(0.0, spring.x(), 1.0e-6);
        assertEquals(40.0, spring.y(), 1.0e-6);
    }

    @Test
    void snapResetsPositionAndVelocity() {
        Spring2 spring = new Spring2(4.0, 10.0, 20.0);
        spring.impulse(5.0, -5.0);

        spring.snap(7.0, 8.0);

        assertEquals(7.0, spring.x(), 0.0);
        assertEquals(8.0, spring.y(), 0.0);
        assertEquals(0.0, spring.velocityX(), 0.0);
        assertEquals(0.0, spring.velocityY(), 0.0);

        spring.step(7.0, 8.0, 1.0);
        assertEquals(7.0, spring.x(), 1.0e-12);
        assertEquals(8.0, spring.y(), 1.0e-12);
    }

    @Test
    void zeroDtLeavesStateUntouched() {
        Spring2 spring = new Spring2(5.0, 1.0, 2.0);
        spring.impulse(3.0, 4.0);

        spring.step(100.0, 100.0, 0.0);

        assertEquals(1.0, spring.x(), 0.0);
        assertEquals(2.0, spring.y(), 0.0);
        assertEquals(3.0, spring.velocityX(), 0.0);
        assertEquals(4.0, spring.velocityY(), 0.0);
    }

    @Test
    void negativeFiniteDtLeavesStateUntouched() {
        Spring2 spring = new Spring2(5.0, 1.0, 2.0);
        spring.impulse(3.0, 4.0);

        spring.step(100.0, 100.0, -0.05);

        assertEquals(1.0, spring.x(), 0.0);
        assertEquals(2.0, spring.y(), 0.0);
        assertEquals(3.0, spring.velocityX(), 0.0);
        assertEquals(4.0, spring.velocityY(), 0.0);
    }

    @Test
    void frameRateIndependentForAFixedTarget() {
        double[] at60fps = replaySpring(1.0 / 60.0, 120);
        double[] at240fps = replaySpring(1.0 / 240.0, 480);

        assertArrayEquals(at240fps, at60fps, 1.0e-9);
    }

    @Test
    void tracksConstantVelocityTargetWithCriticalDampingLag() {
        double omega = 10.0;
        double velocity = 100.0;
        double dt = 1.0 / 480.0;
        Spring2 spring = new Spring2(omega, 0.0, 0.0);

        double t = 0.0;
        for (int i = 0; i < 4800; i++) {
            t += dt;
            spring.step(velocity * t, 0.0, dt);
        }

        double lag = velocity * t - spring.x();
        assertEquals(2.0 * velocity / omega, lag, 0.15 * 2.0 * velocity / omega);
    }

    @Test
    void omegaIsReadable() {
        assertEquals(7.5, new Spring2(7.5).omega(), 0.0);
    }

    @Test
    void translateMovesThePositionAndKeepsTheVelocity() {
        Spring2 spring = new Spring2(4.0, 10.0, 20.0);
        spring.impulse(5.0, -5.0);

        spring.translate(3.0, -2.0);

        assertEquals(13.0, spring.x(), 0.0);
        assertEquals(18.0, spring.y(), 0.0);
        assertEquals(5.0, spring.velocityX(), 0.0);
        assertEquals(-5.0, spring.velocityY(), 0.0);

        // the target is untouched: the displaced spring still eases to it
        for (int frame = 0; frame < 300; frame++) {
            spring.step(0.0, 0.0, 1.0 / 60.0);
        }
        assertEquals(0.0, spring.x(), 1.0e-6);
        assertEquals(0.0, spring.y(), 1.0e-6);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Spring2(0.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(-1.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(Double.NaN, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0, Double.POSITIVE_INFINITY, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0, 0.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).step(0.0, 0.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).step(0.0, 0.0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).step(Double.NaN, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).step(0.0, Double.NEGATIVE_INFINITY, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).impulse(Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).impulse(0.0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).snap(0.0, Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).snap(Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).translate(Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Spring2(1.0).translate(0.0, Double.POSITIVE_INFINITY));
    }

    private static double[] replaySpring(double dtSeconds, int frames) {
        Spring2 spring = new Spring2(6.0, 0.0, 0.0);
        FrameReplay<Double, double[]> replay = FrameReplay
                .runUniform(spring, dtSeconds, frames, 40.0, (subject, frameDt, target) -> {
                    subject.step(target, -20.0, frameDt);
                    return new double[]{subject.x(), subject.y(), subject.velocityX(), subject.velocityY()};
                });
        return replay.lastOutput();
    }
}
