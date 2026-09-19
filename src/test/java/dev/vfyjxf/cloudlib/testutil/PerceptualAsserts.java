package dev.vfyjxf.cloudlib.testutil;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance assertions distilled from the perception literature on
 * screen-space stability, as reusable checks over per-frame output traces
 * (one sample per frame, per axis). All of them are plain deterministic
 * arithmetic on the trace — no FFTs, no randomness.
 * <ul>
 *   <li><strong>Static segment:</strong> a panel that is not really moving
 *       must sit still: per-axis peak-to-peak excursion ≤ 1 px and zero
 *       direction reversals.</li>
 *   <li><strong>Monotone tracking:</strong> output may never move more than
 *       0.5 px beyond what the input moved in the same frame pair — a
 *       stabilizer must not add motion, only remove it.</li>
 *   <li><strong>Tremor suppression:</strong> for an input carrying a tremor
 *       band stacked on slow drift, the high-frequency energy of the output
 *   (variance of the first differences — a simple high-pass) must be at most
 *       a fifth of the input's.</li>
 * </ul>
 */
public final class PerceptualAsserts {

    private PerceptualAsserts() {}

    /**
     * Asserts the static-segment contract on both axes: peak-to-peak ≤ 1 px
     * and no direction reversals.
     */
    public static void assertStableWhenStatic(double[] xs, double[] ys, String context) {
        assertPeakToPeakAtMost(xs, 1.0, context + " (x)");
        assertPeakToPeakAtMost(ys, 1.0, context + " (y)");
        assertEquals(0, reversals(xs), context + " (x): static output must never reverse direction");
        assertEquals(0, reversals(ys), context + " (y): static output must never reverse direction");
    }

    /**
     * Asserts the tracking contract: for every frame pair,
     * {@code |out[i+1] - out[i]| <= |in[i+1] - in[i]| + 0.5}.
     */
    public static void assertTracksWithoutJerk(double[] input, double[] output, String context) {
        assertTrue(
                input.length == output.length && input.length >= 2,
                context + ": traces must align and have 2+ samples");
        for (int i = 0; i + 1 < input.length; i++) {
            double outStep = Math.abs(output[i + 1] - output[i]);
            double inStep = Math.abs(input[i + 1] - input[i]);
            assertTrue(
                    outStep <= inStep + 0.5,
                    context + ": output moved " + outStep + " px at frame " + i + " while input moved " + inStep
                            + " px");
        }
    }

    /**
     * The quantization-aware form of {@link #assertTracksWithoutJerk}: the
     * same literature contract (output must not outrun input by more than
     * half a pixel) evaluated over {@code windowFrames}-frame windows. A
     * lattice quantizer must step a whole pixel at once, so on the single
     * step frame the per-frame reading is momentarily 1 px against a fraction
     * of input motion; over a window the stepping cadence carries exactly the
     * input's motion and the contract holds on average, which is what the
     * eye judges.
     */
    public static void assertTracksWithoutJerkWindowed(
            double[] input, double[] output, int windowFrames, double tolerancePx, String context) {
        assertTrue(
                input.length == output.length && input.length > windowFrames,
                context + ": traces must align and be longer than the window");
        for (int i = 0; i + windowFrames < input.length; i++) {
            double outStep = Math.abs(output[i + windowFrames] - output[i]);
            double inStep = Math.abs(input[i + windowFrames] - input[i]);
            assertTrue(
                    outStep <= inStep + tolerancePx,
                    context + ": output moved " + outStep + " px over the window at frame " + i + " while input moved "
                            + inStep + " px");
        }
    }

    /**
     * Asserts the tremor contract: the high-frequency energy of the output
     * (variance of first differences) is at most {@code ratio} (default
     * 1/5) of the input's.
     */
    public static void assertTremorSuppressed(double[] input, double[] output, String context) {
        assertTremorSuppressed(input, output, 1.0 / 5.0, context);
    }

    /** {@link #assertTremorSuppressed(double[], double[], String)} with an explicit ratio. */
    public static void assertTremorSuppressed(double[] input, double[] output, double ratio, String context) {
        double inEnergy = highFrequencyEnergy(input);
        double outEnergy = highFrequencyEnergy(output);
        assertTrue(
                outEnergy <= ratio * inEnergy,
                context + ": output high-frequency energy " + outEnergy + " exceeds " + ratio + " * input " + inEnergy);
    }

    /** Peak-to-peak excursion: max minus min. */
    public static double peakToPeak(double[] samples) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double sample : samples) {
            min = Math.min(min, sample);
            max = Math.max(max, sample);
        }
        return max - min;
    }

    /**
     * The number of direction reversals: sign changes between consecutive
     * non-zero first differences.
     */
    public static int reversals(double[] samples) {
        int reversals = 0;
        int previousSign = 0;
        for (int i = 0; i + 1 < samples.length; i++) {
            double delta = samples[i + 1] - samples[i];
            if (delta == 0.0) {
                continue;
            }
            int sign = delta > 0.0 ? 1 : -1;
            if (previousSign != 0 && sign != previousSign) {
                reversals++;
            }
            previousSign = sign;
        }
        return reversals;
    }

    /**
     * High-frequency energy: the variance of the first differences. Frame
     * differencing is a crude high-pass — slow components survive as small
     * differences, tremor components as large ones.
     */
    public static double highFrequencyEnergy(double[] samples) {
        if (samples.length < 3) {
            return 0.0;
        }
        double[] diffs = new double[samples.length - 1];
        double mean = 0.0;
        for (int i = 0; i < diffs.length; i++) {
            diffs[i] = samples[i + 1] - samples[i];
            mean += diffs[i];
        }
        mean /= diffs.length;
        double variance = 0.0;
        for (double diff : diffs) {
            double centered = diff - mean;
            variance += centered * centered;
        }
        return variance / diffs.length;
    }

    private static void assertPeakToPeakAtMost(double[] samples, double limit, String context) {
        double excursion = peakToPeak(samples);
        assertTrue(
                excursion <= limit,
                context + ": static peak-to-peak " + excursion + " exceeds " + limit + " px over "
                        + Arrays.toString(truncate(samples)));
    }

    private static double[] truncate(double[] samples) {
        return samples.length > 8 ? Arrays.copyOf(samples, 8) : samples;
    }
}
