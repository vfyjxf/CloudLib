package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared {@link AttentionField} contract: what {@link #cost(Rect)} means
 * for any implementation, and that the two built-in fields agree on
 * magnitude.
 */
class AttentionFieldTest {

    /** A deterministic test field: constant value everywhere. */
    private static final class ConstantField implements AttentionField {

        private final double value;

        ConstantField(double value) {
            this.value = value;
        }

        @Override
        public double valueAt(double x, double y) {
            return value;
        }

        @Override
        public int samplingStep() {
            return 4;
        }
    }

    @Test
    void costIsTheMeanOfTheFieldOverTheSampleGrid() {
        // a 4×4 rect on a 4px grid samples exactly once, at its center
        GaussianAttention gaussian = new GaussianAttention(0.0, 0.0, 100.0, 4);
        assertEquals(gaussian.valueAt(22.0, 31.0), gaussian.cost(new Rect(20, 29, 4, 4)), 1.0e-12);

        // a constant field's cost is the constant, whatever the rect
        ConstantField half = new ConstantField(0.5);
        assertEquals(0.5, half.cost(new Rect(0, 0, 13, 7)), 1.0e-12);
        assertEquals(0.5, half.cost(new Rect(100, 100, 40, 40)), 1.0e-12);
    }

    @Test
    void coarserSamplingStepsStillEstimateTheMean() {
        GaussianAttention fine = new GaussianAttention(240.0, 135.0, 100.0, 2);
        GaussianAttention coarse = new GaussianAttention(240.0, 135.0, 100.0, 16);
        Rect rect = new Rect(200, 100, 80, 60);
        // both are estimates of the same integral; they must agree within a
        // generous tolerance and stay inside [0, 1]
        assertEquals(fine.cost(rect), coarse.cost(rect), 0.15);
        assertTrue(fine.cost(rect) >= 0.0 && fine.cost(rect) <= 1.0);
        assertTrue(coarse.cost(rect) >= 0.0 && coarse.cost(rect) <= 1.0);
    }

    @Test
    void bothFieldsPeakWhereTheCrosshairIs() {
        GaussianAttention gaussian = GaussianAttention.atScreenCenter(480, 270, 100.0);
        PiecewiseAttention piecewise = PiecewiseAttention.atScreenCenter(480, 270, 60.0, 160.0);

        assertEquals(1.0, gaussian.valueAt(240.0, 135.0), 1.0e-12);
        assertEquals(1.0, piecewise.valueAt(240.0, 135.0), 0.0);
    }

    @Test
    void bothFieldsAgreeOnMagnitudeAndOrdering() {
        GaussianAttention gaussian = GaussianAttention.atScreenCenter(480, 270, 100.0);
        PiecewiseAttention piecewise = PiecewiseAttention.atScreenCenter(480, 270, 60.0, 160.0);

        double gaussianCenter = gaussian.cost(new Rect(220, 115, 40, 40));
        double piecewiseCenter = piecewise.cost(new Rect(220, 115, 40, 40));
        double gaussianCorner = gaussian.cost(new Rect(410, 5, 40, 40));
        double piecewiseCorner = piecewise.cost(new Rect(410, 5, 40, 40));

        // both put the crosshair rect near the top of the scale...
        assertTrue(gaussianCenter > 0.9, "gaussian center too low: " + gaussianCenter);
        assertTrue(piecewiseCenter > 0.9, "piecewise center too low: " + piecewiseCenter);
        // ...and the corner rect near the bottom
        assertTrue(gaussianCorner < 0.3, "gaussian corner too high: " + gaussianCorner);
        assertTrue(piecewiseCorner < 0.3, "piecewise corner too high: " + piecewiseCorner);
        // and both within the same order of magnitude at the corner
        assertTrue(gaussianCorner > 0.01);
        assertEquals(PiecewiseAttention.lowValue, piecewiseCorner, 1.0e-9);

        // same ordering of three positions for both fields
        double gaussianEdgeMid = gaussian.cost(new Rect(220, 0, 40, 40));
        double piecewiseEdgeMid = piecewise.cost(new Rect(220, 0, 40, 40));
        List.of(gaussianCenter, gaussianEdgeMid, gaussianCorner).forEach(v -> assertTrue(v >= 0.0 && v <= 1.0));
        assertTrue(gaussianCenter > gaussianEdgeMid && gaussianEdgeMid > gaussianCorner);
        assertTrue(piecewiseCenter > piecewiseEdgeMid && piecewiseEdgeMid > piecewiseCorner);
    }

    @Test
    void zeroAreaRectsCostNothingOnBothFields() {
        GaussianAttention gaussian = GaussianAttention.atScreenCenter(480, 270, 100.0);
        PiecewiseAttention piecewise = PiecewiseAttention.atScreenCenter(480, 270, 60.0, 160.0);
        assertEquals(0.0, gaussian.cost(new Rect(240, 135, 0, 10)));
        assertEquals(0.0, piecewise.cost(new Rect(240, 135, 10, 0)));
    }
}
