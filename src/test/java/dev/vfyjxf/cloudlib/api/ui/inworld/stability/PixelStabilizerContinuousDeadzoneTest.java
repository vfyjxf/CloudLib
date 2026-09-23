package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The continuous-deadzone acceptance of {@link PixelStabilizer} — the
 * residual-micro-jitter finishing contract. The two-state quantizer (rest =
 * hard integer snap, move = float passthrough) left two visible artifacts at
 * the seams, both quantified here on frame sequences:
 * <ul>
 *   <li><strong>the band sweep</strong> (velocity wandering back and forth
 *       across the {@code restVelocity..moveVelocity} band, the camera
 *       slow-turn regime): the rest-phase hysteresis lattice stepped whole
 *       pixels in single frames and the rest→move handoff popped by up to the
 *       full dead-zone trail — a 1 px step on a sub-pixel input;</li>
 *   <li><strong>the sub-threshold drift</strong> (an anchor creeping slower
 *       than 1 px/s): each hysteresis-band crossing jumped the output a whole
 *       pixel in one frame — a low-frequency, very visible pop.</li>
 * </ul>
 * The continuous deadzone replaces both: the output is
 * {@code in + s · lowpass(L − in)} — the latch {@code L} still absorbs
 * chatter, the snap weight {@code s} fades with the recent latch-step density
 * so sustained motion blends to exact passthrough, and every latch step
 * eases through a short glide instead of teleporting. The contracts below are
 * what "no residual micro-jitter" means measurably: no single-frame step
 * beyond half a pixel of added motion, deviation from the float target
 * bounded and monotone in speed, exact passthrough above the move gate, and
 * a perfectly still integer lock at rest.
 */
class PixelStabilizerContinuousDeadzoneTest {

    private static final double dt = 1.0 / 60.0;
    private static final double frameDeltas[] = {1 / 60.0, 1 / 58.0, 1 / 62.0, 1 / 59.0, 1 / 61.0};

    private static final class Sample {
        final double t, in, out;
        final double speed;
        final PixelStabilizer.State state;

        Sample(double t, double in, double out, double speed, PixelStabilizer.State state) {
            this.t = t;
            this.in = in;
            this.out = out;
            this.speed = speed;
            this.state = state;
        }
    }

    /** Drives one axis through the stabilizer on the given input stream, keeping the other fixed. */
    private static List<Sample> run(DoubleUnaryOperator input, int frames) {
        PixelStabilizer stabilizer = new PixelStabilizer();
        List<Sample> samples = new ArrayList<>(frames);
        double t = 0;
        for (int i = 0; i < frames; i++) {
            double delta = frameDeltas[i % frameDeltas.length];
            double x = input.applyAsDouble(t);
            double prevX = i == 0 ? x : input.applyAsDouble(t - delta);
            double speed = Math.abs(x - prevX) / delta;
            PixelStabilizer.Output out = stabilizer.accept(t, x, 50.0);
            samples.add(new Sample(t, x, out.x(), speed, stabilizer.state()));
            t += delta;
        }
        return samples;
    }

    /** A slow deliberate pan whose speed sweeps 9→45→9 px/s — the residual-jitter regime. */
    private static double bandSweep(double t) {
        double v = 27.0 + 18.0 * Math.sin(2.0 * Math.PI * 0.25 * t - Math.PI / 2.0);
        return 200.0 + integral(t, v);
    }

    /** Trapezoid-free exact integral of the sweep velocity (its position is an analytic function). */
    private static double integral(double t, double vUnused) {
        // ∫0..t (27 + 18·sin(ωτ − π/2)) dτ with ω = 2π·0.25; sin(ωτ − π/2) = −cos(ωτ)
        double omega = 2.0 * Math.PI * 0.25;
        return 27.0 * t - 18.0 / omega * Math.sin(omega * t);
    }

    private static double sweepSpeed(double t) {
        return 27.0 + 18.0 * Math.sin(2.0 * Math.PI * 0.25 * t - Math.PI / 2.0);
    }

    // region 1. the band sweep: continuous handoff, bounded monotone deviation

    @Test
    void bandSweepNeverStepsMoreThanHalfAPixelBeyondTheInput() {
        List<Sample> samples = run(PixelStabilizerContinuousDeadzoneTest::bandSweep, 720);
        for (int i = 31; i < samples.size(); i++) {
            Sample a = samples.get(i - 1);
            Sample b = samples.get(i);
            double added = Math.abs((b.out - a.out) - (b.in - a.in));
            assertTrue(
                added <= 0.5,
                "frame " + i + ": the output moved " + added + "px beyond its own input — a lattice step or a"
                        + " state-switch pop (the pre-fix artifacts reach 0.85px)"
            );
        }
    }

    @Test
    void bandSweepDeviationStaysWithinHalfAPixelAndShrinksWithSpeed() {
        List<Sample> samples = run(PixelStabilizerContinuousDeadzoneTest::bandSweep, 720);
        double[][] bins = {{9, 15}, {15, 25}, {25, 35}, {35, 45}};
        double[] binMean = new double[bins.length];
        int[] binCount = new int[bins.length];
        for (int i = 30; i < samples.size(); i++) {
            Sample s = samples.get(i);
            double v = sweepSpeed(s.t);
            for (int b = 0; b < bins.length; b++) {
                if (v >= bins[b][0] && v < bins[b][1]) {
                    binMean[b] += Math.abs(s.out - s.in);
                    binCount[b]++;
                }
            }
        }
        double previousMean = Double.NaN;
        for (int b = 0; b < bins.length; b++) {
            assertTrue(binCount[b] > 100, "bin [" + bins[b][0] + "," + bins[b][1] + ") must be well sampled");
            double mean = binMean[b] / binCount[b];
            assertTrue(mean <= 0.5, "bin [" + bins[b][0] + "," + bins[b][1] + ") mean deviation " + mean + "px > 0.5");
            if (!Double.isNaN(previousMean)) {
                assertTrue(
                    mean <= previousMean + 0.03,
                    "deviation must shrink (or hold) as speed rises: bin [" + bins[b][0] + "," + bins[b][1] + ") mean "
                            + mean + "px after " + previousMean + "px — switch-induced error"
                );
            }
            previousMean = mean;
        }
    }

    // endregion

    // region 2. sub-threshold drift: the hysteresis crossing slides, never pops

    @Test
    void verySlowDriftCrossesTheHysteresisBandWithoutASinglePixelPop() {
        // 0.5 px/s: the anchor crosses a whole lattice cell every 2 s — the
        // pre-fix rest phase held the old cell until the ±0.75px band edge,
        // then teleported a full pixel in one frame
        List<Sample> samples = run(t -> 200.0 + 0.5 * t, 600);
        double maxFrameStep = 0;
        int worst = -1;
        for (int i = 1; i < samples.size(); i++) {
            double step = Math.abs(samples.get(i).out - samples.get(i - 1).out);
            if (step > maxFrameStep) worst = i;
            maxFrameStep = Math.max(maxFrameStep, step);
        }
        assertTrue(
            maxFrameStep <= 0.5,
            "the drift crossed the band with a " + maxFrameStep + "px single-frame step at frame " + worst
                    + " — sub-pixel continuous sliding is the contract (the pre-fix pop is 1.0px)"
        );
        // and the output still keeps station: bounded trail, same end station as the input
        for (Sample s : samples) {
            assertTrue(
                Math.abs(s.out - s.in) <= 0.75,
                "frame " + s.t + ": trail " + (s.out - s.in) + " beyond the band"
            );
        }
        assertTrue(Math.abs(samples.get(samples.size() - 1).out - samples.get(samples.size() - 1).in) <= 0.75);
    }

    @Test
    void verySlowDriftHoldsDeadStillBetweenCrossings() {
        // between crossings the panel is effectively parked inside one cell:
        // over a 60-frame window away from a crossing the output must sit
        // still to well under a tenth of a pixel (the latch, not the noise
        // floor, owns the output there)
        List<Sample> samples = run(t -> 200.0 + 0.5 * t, 600);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 120; i < 180; i++) { // 2.0s..3.0s: input 201.0→201.5, mid-cell, no crossing
            double out = samples.get(i).out;
            min = Math.min(min, out);
            max = Math.max(max, out);
        }
        assertTrue(max - min <= 0.1, "mid-cell hold wandered " + (max - min) + "px over a crossing-free second");
    }

    // endregion

    // region 3. the regressions the continuous layer may not break

    @Test
    void fastMotionIsExactPassthroughFromTheMovePhaseOn() {
        List<Sample> samples = run(t -> 100.0 + 120.0 * t, 90);
        int moveFrames = 0;
        for (int i = 0; i < samples.size(); i++) {
            Sample s = samples.get(i);
            if (s.state != PixelStabilizer.State.move) continue;
            moveFrames++;
            assertEquals(s.in, s.out, 0.0, "move frame " + i + " must pass the float input through exactly");
        }
        assertTrue(moveFrames > 80, "the stream must be in the move phase (" + moveFrames + " frames)");
    }

    @Test
    void restStateOutputIsExactlyOnTheLattice() {
        // 2 px/s sustained drift with the latch stepping, then a dead stop:
        // after the glide and the snap weight have re-formed (~0.5 s — the
        // arm window plus the density decay), the output must be the latched
        // integer, exactly, every frame (the dead-still contract)
        PixelStabilizer stabilizer = new PixelStabilizer();
        double t = 0;
        for (int i = 0; i < 300; i++) {
            stabilizer.accept(t, 300.0 + 2.0 * t, 50.0);
            t += dt;
        }
        double still = 300.0 + 2.0 * t;
        for (int i = 0; i < 60; i++) {
            PixelStabilizer.Output out = stabilizer.accept(t, still, 50.0);
            t += dt;
            if (i < 30) continue; // the crossing glide and the density decay settle here
            assertTrue(out.x() == Math.rint(out.x()), "still frame " + i + " output " + out.x() + " is off-lattice");
            assertEquals(PixelStabilizer.State.rest, stabilizer.state(), "still frame " + i);
        }
    }

    // endregion
}
