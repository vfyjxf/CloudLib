package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.testutil.LcgNoise;
import dev.vfyjxf.cloudlib.testutil.PerceptualAsserts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowStabilizerTest {

    private static final double dt = 1.0 / 60.0;

    @Test
    void noisySlowDriftRendersSmoothAndTracks() {
        // 3 px/s clean drift under ±0.2 px noise: the continuous deadzone
        // renders this as sticky lattice motion — sub-threshold wiggle at
        // most, never a whole-pixel teleport — and the drift axis advances
        // monotonically at the visible scale (reversals below 0.35 px are
        // sub-visible blend ripple; the old integer-step contract could
        // assert zero reversals at any scale only by quantizing the output,
        // which is exactly what produced the 1 px crossing pops)
        FollowStabilizer stabilizer = new FollowStabilizer();
        LcgNoise driftNoise = new LcgNoise(0xD21AF);
        LcgNoise crossNoise = new LcgNoise(0xC1055);
        double[] inX = new double[600];
        double[] cleanX = new double[600];
        double[] outX = new double[600];
        double[] outY = new double[600];
        for (int i = 0; i < inX.length; i++) {
            cleanX[i] = 200.0 + 0.05 * i;
            inX[i] = cleanX[i] + driftNoise.next(0.2);
            double inY = 300.0 + crossNoise.next(0.2);
            PixelStabilizer.Output out = stabilizer.accept(i * dt, inX[i], inY);
            outX[i] = out.x();
            outY[i] = out.y();
            if (i > 0) {
                double step = outX[i] - outX[i - 1];
                assertTrue(step >= -0.35, "no visible backward motion, frame " + i + ": " + step);
            }
        }

        assertEquals(0, reversals(outX, 0.35), "the drifting axis must never visibly flip direction");
        assertEquals(0, reversals(outY, 0.35), "the static axis must never visibly flip direction");
        assertTrue(PerceptualAsserts.peakToPeak(outY) <= 1.0, "the static axis must sit still");
        assertTrue(
                Math.abs(outX[outX.length - 1] - inX[inX.length - 1]) <= 2.0,
                "the lattice must track the drift within filter lag plus one quantum");
        // the drifting axis may trail its input by the snap band and recover
        // at crossings, and the blend passes a bounded fraction of the anchor
        // noise — both inside the ±(snapHalf + hysteresis) band, so the honest
        // windowed slack is the band, not the old quantizer's half-quantum
        PerceptualAsserts.assertTracksWithoutJerkWindowed(cleanX, outX, 12, 0.75, "noisy drift");
        PerceptualAsserts.assertTremorSuppressed(inX, outX, "noisy drift's noise floor");
    }

    /** Direction reversals above a visibility threshold: sign flips between consecutive differences of at least {@code minStep} px. */
    private static int reversals(double[] samples, double minStep) {
        int reversals = 0;
        int previousSign = 0;
        for (int i = 0; i + 1 < samples.length; i++) {
            double delta = samples[i + 1] - samples[i];
            if (Math.abs(delta) < minStep) continue;
            int sign = delta > 0 ? 1 : -1;
            if (previousSign != 0 && sign != previousSign) reversals++;
            previousSign = sign;
        }
        return reversals;
    }

    @Test
    void staticNoisyAnchorRendersPerfectlyStill() {
        FollowStabilizer stabilizer = new FollowStabilizer();
        LcgNoise noise = new LcgNoise(0x57A11);
        double[] xs = new double[240];
        double[] ys = new double[240];
        for (int i = 0; i < xs.length; i++) {
            PixelStabilizer.Output out = stabilizer.accept(i * dt, 100.0 + noise.next(0.3), 200.0);
            xs[i] = out.x();
            ys[i] = out.y();
        }
        PerceptualAsserts.assertStableWhenStatic(xs, ys, "static anchor");
        assertEquals(0.0, PerceptualAsserts.peakToPeak(xs), 0.0, "a static anchor must be perfectly latched");
        assertEquals(0.0, PerceptualAsserts.peakToPeak(ys), 0.0);
    }

    @Test
    void tremorBandIsSuppressedBelowAFifth() {
        FollowStabilizer stabilizer = new FollowStabilizer();
        double[] in = new double[720];
        double[] out = new double[720];
        for (int i = 0; i < in.length; i++) {
            double t = i * dt;
            in[i] = 300.0
                    + 40.0 * Math.sin(2.0 * Math.PI * 0.25 * t)
                    + 2.0 * Math.sin(2.0 * Math.PI * 8.0 * t)
                    + 2.0 * Math.sin(2.0 * Math.PI * 10.0 * t)
                    + 2.0 * Math.sin(2.0 * Math.PI * 12.0 * t);
            out[i] = stabilizer.accept(t, in[i], 400.0).x();
        }
        PerceptualAsserts.assertTremorSuppressed(in, out, "synthetic tremor on slow drift");
    }

    @Test
    void fastMonotoneMotionTracksWithinHalfAPixelPerFrame() {
        FollowStabilizer stabilizer = new FollowStabilizer();
        double[] in = new double[120];
        double[] out = new double[120];
        for (int i = 0; i < in.length; i++) {
            in[i] = 100.0 + 3.0 * i;
            out[i] = stabilizer.accept(i * dt, in[i], 0.0).x();
        }
        PerceptualAsserts.assertTracksWithoutJerk(in, out, "fast monotone ramp");
    }

    @Test
    void bypassSticksInstantlyWhilePlainAcceptGlides() {
        FollowStabilizer plain = new FollowStabilizer();
        FollowStabilizer escaped = new FollowStabilizer();
        for (int i = 0; i < 120; i++) {
            double x = 100.0 + 2.0 * i;
            plain.accept(i * dt, x, 50.0);
            escaped.accept(i * dt, x, 50.0);
        }
        double t = 120 * dt;

        // without the escape hatch the teleport triggers a long filter glide
        PixelStabilizer.Output gliding = plain.accept(t, 800.0, 600.0);
        assertTrue(gliding.x() < 790.0, "a plain accept after a teleport must still be gliding: " + gliding.x());

        // with it, the panel re-sticks on the lattice in one call
        PixelStabilizer.Output stuck = escaped.bypass(t, 800.4, 600.6);
        assertEquals(800.0, stuck.x(), 0.0);
        assertEquals(601.0, stuck.y(), 0.0);
        assertEquals(PixelStabilizer.State.rest, escaped.state());

        for (int i = 0; i < 60; i++) {
            PixelStabilizer.Output out = escaped.accept(t + (i + 1) * dt, 800.4, 600.6);
            assertEquals(800.0, out.x(), 0.0, "no glide tail after the bypass, frame " + i);
            assertEquals(601.0, out.y(), 0.0);
        }
        assertNotEquals(stuck.x(), gliding.x());
    }

    @Test
    void resetReseedsBothStagesWithoutVelocityPollution() {
        FollowStabilizer stabilizer = new FollowStabilizer();
        for (int i = 0; i < 120; i++) {
            stabilizer.accept(i * dt, 50.0 + 2.0 * i, 50.0 - 1.0 * i);
        }
        stabilizer.reset();
        assertEquals(PixelStabilizer.State.rest, stabilizer.state());

        PixelStabilizer.Output out = stabilizer.accept(3.0, 42.0, 42.0);
        assertEquals(42.0, out.x(), 0.0, "the first sample after reset must seed, not glide");
        assertEquals(42.0, out.y(), 0.0);
        for (int i = 0; i < 30; i++) {
            out = stabilizer.accept(3.0 + (i + 1) * dt, 42.0, 42.0);
            assertEquals(42.0, out.x(), 0.0, "no residual velocity may move the panel, frame " + i);
            assertEquals(42.0, out.y(), 0.0);
        }
    }

    @Test
    void sharedParametersFlowToBothAxes() {
        FollowStabilizer stabilizer = new FollowStabilizer(2.0, 0.004, 1.0, 10.0, PixelStabilizer.Config.ofDefaults());
        PixelStabilizer.Output out = stabilizer.accept(0.0, 10.5, 20.5);
        assertEquals(11.0, out.x(), 0.0);
        assertEquals(21.0, out.y(), 0.0);
        out = stabilizer.accept(dt, 10.5, 20.5);
        assertEquals(11.0, out.x(), 0.0);
        assertEquals(21.0, out.y(), 0.0);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FollowStabilizer(0.0, 0.007, 1.0, 12.0, PixelStabilizer.Config.ofDefaults()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FollowStabilizer(1.0, -1.0, 1.0, 12.0, PixelStabilizer.Config.ofDefaults()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FollowStabilizer(1.0, 0.007, 1.0, 0.5, PixelStabilizer.Config.ofDefaults()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FollowStabilizer(
                        1.0, 0.007, 1.0, 12.0, new PixelStabilizer.Config(0.0, 0.25, 45.0, 9.0, 5, 0.12)));
        assertThrows(NullPointerException.class, () -> new FollowStabilizer(1.0, 0.007, 1.0, 12.0, null));
        assertThrows(IllegalArgumentException.class, () -> new FollowStabilizer().accept(0.0, Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new FollowStabilizer().bypass(0.0, 0.0, Double.NaN));
    }
}
