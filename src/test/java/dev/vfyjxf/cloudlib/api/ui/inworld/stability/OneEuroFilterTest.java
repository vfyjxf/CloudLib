package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.testutil.LcgNoise;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OneEuroFilterTest {

    private static final double dt = 1.0 / 60.0;

    @Test
    void firstSampleIsAdoptedVerbatim() {
        OneEuroFilter filter = new OneEuroFilter();
        assertEquals(42.0, filter.filter(0.0, 42.0), 0.0);
        assertEquals(42.0, filter.value(), 0.0);
        assertTrue(filter.hasSample());
    }

    @Test
    void stationaryNoiseIsStronglyAttenuated() {
        OneEuroFilter filter = new OneEuroFilter();
        LcgNoise noise = new LcgNoise(0xC0FFEE);
        double[] input = new double[240];
        double[] output = new double[240];
        for (int i = 0; i < input.length; i++) {
            input[i] = 50.0 + noise.next(0.3);
            output[i] = filter.filter(i * dt, input[i]);
        }
        double inputExcursion = peakToPeak(input);
        double outputExcursion = peakToPeak(output);
        assertTrue(inputExcursion > 0.3, "the noise must actually be noisy: " + inputExcursion);
        assertTrue(outputExcursion < inputExcursion, "the output excursion must be strictly calmer");

        // the 1€ literature's dispersion metric (std around the mean): with
        // the canonical 1 Hz rest cutoff at 60 fps sampling the output
        // dispersion shrinks several-fold — peak-to-peak on a smoothed trace
        // is dominated by the noise's own low-frequency wander, which no
        // causal filter may remove
        double inputStd = std(input);
        double outputStd = std(output);
        assertTrue(
            outputStd < inputStd / 3.0,
            "output std " + outputStd + " is not significantly below input " + inputStd
        );
    }

    @Test
    void rampSteadyStateLagMatchesTheCutoffTimeConstant() {
        // beta = 0: a plain low-pass at minCutoff, so the steady-state ramp lag
        // is exactly v * tau with tau = 1 / (2*pi*minCutoff)
        double minCutoff = 1.0;
        double velocity = 10.0;
        OneEuroFilter filter = new OneEuroFilter(minCutoff, 0.0, 1.0, 12.0);

        double t = 0.0;
        double x = 0.0;
        for (int i = 0; i < 300; i++) {
            filter.filter(t, x);
            t += dt;
            x += velocity * dt;
        }
        double lagStart = x - filter.value();

        for (int i = 0; i < 120; i++) {
            filter.filter(t, x);
            t += dt;
            x += velocity * dt;
        }
        double lagEnd = x - filter.value();

        double expected = velocity / (2.0 * Math.PI * minCutoff);
        assertEquals(expected, lagStart, 0.2 * expected, "steady-state lag after 5 s");
        assertEquals(lagStart, lagEnd, 1.0e-6, "the lag must be stationary, not drifting");
    }

    @Test
    void adaptationShrinksTheTimeLagAtSpeed() {
        double slowLag = rampTimeLag(5.0);
        double fastLag = rampTimeLag(300.0);
        assertTrue(
            fastLag < slowLag / 3.0,
            "a 300 px/s ramp must lag far less in time than a 5 px/s one: " + fastLag + " vs " + slowLag
        );
        assertTrue(fastLag < 0.05, "the fast lag should be a few milliseconds: " + fastLag);
        assertTrue(slowLag > 0.1, "the slow lag should sit near 1/(2*pi*minCutoff): " + slowLag);
    }

    @Test
    void highSpeedSineLagsLessThanLowSpeedSine() {
        double[] slowIn = new double[360];
        double[] slowOut = new double[360];
        runSine(new OneEuroFilter(), 10.0, 0.25, slowIn, slowOut);

        double[] fastIn = new double[360];
        double[] fastOut = new double[360];
        runSine(new OneEuroFilter(), 100.0, 0.5, fastIn, fastOut);

        double slowLagSeconds = bestAlignmentLag(slowIn, slowOut) * dt;
        double fastLagSeconds = bestAlignmentLag(fastIn, fastOut) * dt;
        assertTrue(
            fastLagSeconds < slowLagSeconds / 2.0,
            "the fast sine must lag far less in time than the slow one: " + fastLagSeconds + " vs " + slowLagSeconds
        );
        assertTrue(fastLagSeconds < 0.1, "the fast sine should track within a few frames: " + fastLagSeconds);
    }

    @Test
    void maxCutoffCeilsTheAdaptiveCutoff() {
        // with a huge beta the adaptive cutoff saturates at maxCutoff, so the
        // filter must behave exactly like a fixed-cutoff low-pass at maxCutoff
        OneEuroFilter saturated = new OneEuroFilter(1.0, 1.0e6, 1.0, 12.0);
        OneEuroFilter fixedAtCeiling = new OneEuroFilter(12.0, 0.0, 1.0, 12.0);

        double t = 0.0;
        double x = 0.0;
        for (int i = 0; i < 600; i++) {
            double a = saturated.filter(t, x);
            double b = fixedAtCeiling.filter(t, x);
            assertEquals(b, a, 0.0, "saturated beta must equal a fixed 12 Hz cutoff at frame " + i);
            t += dt;
            x = 100.0 + 40.0 * Math.sin(2.0 * Math.PI * 0.5 * t);
        }
    }

    @Test
    void resetDropsPositionAndDerivativeHistory() {
        OneEuroFilter filter = new OneEuroFilter();
        OneEuroFilter fresh = new OneEuroFilter();
        double t = 0.0;
        for (int i = 0; i < 240; i++) {
            filter.filter(t, 20.0 + 120.0 * t);
            t += dt;
        }
        assertTrue(filter.value() > 0.0);
        filter.reset();
        assertTrue(!filter.hasSample());
        assertEquals(0.0, filter.value(), 0.0);

        // after reset, feeding a constant must not drift at all: the velocity
        // estimate is zero, so nothing pulls the output anywhere
        for (int i = 0; i < 60; i++) {
            assertEquals(fresh.filter(t, 42.0), filter.filter(t, 42.0), 0.0, "post-reset frame " + i);
            assertEquals(42.0, filter.value(), 1.0e-9);
            t += dt;
        }
    }

    @Test
    void variableFrameRateConvergesIdentically() {
        OneEuroFilter filter = new OneEuroFilter();
        double t = 0.0;
        double previous = Double.NaN;
        for (int i = 0; i < 600; i++) {
            double dt = i % 2 == 0 ? 1.0 / 60.0 : 1.0 / 144.0;
            previous = filter.filter(t, 100.0);
            t += dt;
        }
        assertEquals(100.0, previous, 1.0e-6, "must converge on a constant regardless of frame cadence");
    }

    @Test
    void nonAdvancingTimestampsAreIgnored() {
        OneEuroFilter filter = new OneEuroFilter();
        assertEquals(10.0, filter.filter(1.0, 10.0), 0.0);
        assertEquals(10.0, filter.filter(1.0, 99.0), 0.0, "a duplicate timestamp must not consume the sample");
        // the derivative estimate moves first, then the position: dxHat = aD*0.5,
        // cutoff = 1 + beta*|dxHat|, and only then the position low-pass
        double derivativeEstimate = alphaAt(1.0, 1.0) * 0.5;
        double cutoffAfterStep = 1.0 + 0.007 * derivativeEstimate;
        double afterOneSecondStep = 10.0 + 0.5 * alphaAt(cutoffAfterStep, 1.0);
        assertEquals(afterOneSecondStep, filter.filter(2.0, 10.5), 1.0e-9);
        assertEquals(filter.value(), filter.filter(1.5, -50.0), 0.0, "a rewound timestamp must not move state");
        double advanced = filter.filter(2.5, 10.5);
        assertTrue(afterOneSecondStep < advanced && advanced < 10.5, "a normal step must continue toward the sample");
    }

    @Test
    void rejectsInvalidConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(0.0, 0.007, 1.0, 12.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(-1.0, 0.007, 1.0, 12.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(Double.NaN, 0.007, 1.0, 12.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(1.0, -0.001, 1.0, 12.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(1.0, Double.POSITIVE_INFINITY, 1.0, 12.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(1.0, 0.007, 0.0, 12.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(1.0, 0.007, 1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(1.0, 0.007, 1.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter(2.0, 0.007, 1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter().filter(Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new OneEuroFilter().filter(0.0, Double.POSITIVE_INFINITY));
    }

    private static double rampTimeLag(double velocity) {
        OneEuroFilter filter = new OneEuroFilter();
        double t = 0.0;
        double x = 0.0;
        for (int i = 0; i < 420; i++) {
            filter.filter(t, x);
            t += dt;
            x += velocity * dt;
        }
        double lagPixels = 0.0;
        int averaged = 0;
        for (int i = 0; i < 120; i++) {
            filter.filter(t, x);
            lagPixels += x - filter.value();
            averaged++;
            t += dt;
            x += velocity * dt;
        }
        return lagPixels / averaged / velocity;
    }

    private static void runSine(
        OneEuroFilter filter,
        double amplitude,
        double frequencyHz,
        double[] input,
        double[] output
    ) {
        double t = 0.0;
        for (int i = 0; i < input.length; i++) {
            input[i] = 100.0 + amplitude * Math.sin(2.0 * Math.PI * frequencyHz * t);
            output[i] = filter.filter(t, input[i]);
            t += dt;
        }
    }

    /** The frame shift (0..30) by which the delayed output trails the input. */
    private static int bestAlignmentLag(double[] input, double[] output) {
        int bestShift = -1;
        double bestError = Double.POSITIVE_INFINITY;
        for (int shift = 0; shift <= 30; shift++) {
            double error = 0.0;
            for (int i = 0; i + shift < output.length; i++) {
                double delta = output[i + shift] - input[i];
                error += delta * delta;
            }
            if (error < bestError) {
                bestError = error;
                bestShift = shift;
            }
        }
        return bestShift;
    }

    private static double std(double[] samples) {
        double mean = 0.0;
        for (double sample : samples) {
            mean += sample;
        }
        mean /= samples.length;
        double variance = 0.0;
        for (double sample : samples) {
            double centered = sample - mean;
            variance += centered * centered;
        }
        return Math.sqrt(variance / samples.length);
    }

    private static double alphaAt(double cutoff, double dt) {
        double tau = 2.0 * Math.PI * cutoff * dt;
        return tau / (tau + 1.0);
    }

    private static double peakToPeak(double[] samples) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double sample : samples) {
            min = Math.min(min, sample);
            max = Math.max(max, sample);
        }
        return max - min;
    }
}
