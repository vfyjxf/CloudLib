package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaussianAttentionTest {

    private static final GaussianAttention field = new GaussianAttention(240.0, 135.0, 80.0);

    @Test
    void peaksAtOneOnTheOrigin() {
        assertEquals(1.0, field.valueAt(240.0, 135.0), 1.0e-12);
    }

    @Test
    void decaysWithTheConfiguredSigma() {
        // one σ out: e^(−1/2)
        assertEquals(Math.exp(-0.5), field.valueAt(320.0, 135.0), 1.0e-12);
        assertEquals(Math.exp(-0.5), field.valueAt(240.0, 55.0), 1.0e-12);
        // two σ out: e^(−2), negligible beyond four and a half σ
        assertEquals(Math.exp(-2.0), field.valueAt(400.0, 135.0), 1.0e-12);
        assertTrue(field.valueAt(240.0 + 360.0, 135.0) < 1.0e-4);
    }

    @Test
    void costIsMonotonicallyNonIncreasingAwayFromThePeak() {
        double previous = Double.MAX_VALUE;
        for (int offset = 0; offset <= 200; offset += 20) {
            Rect rect = new Rect(240 + offset, 135, 40, 40);
            double cost = field.cost(rect);
            assertTrue(cost <= previous + 1.0e-12, "cost rose at offset " + offset + ": " + cost + " > " + previous);
            assertTrue(cost >= 0.0 && cost <= 1.0);
            previous = cost;
        }
        // and strictly decreasing across the sampled offsets
        assertTrue(field.cost(new Rect(240, 135, 40, 40)) > field.cost(new Rect(320, 135, 40, 40)));
        assertTrue(field.cost(new Rect(320, 135, 40, 40)) > field.cost(new Rect(440, 135, 40, 40)));
    }

    @Test
    void rectContainingThePeakCostsMoreThanOneWithoutIt() {
        double containing = field.cost(new Rect(220, 115, 40, 40));
        double cornerSame = field.cost(new Rect(410, 5, 40, 40));
        assertTrue(containing > cornerSame, containing + " !> " + cornerSame);
        assertTrue(containing > 0.9, "peak rect should be near 1: " + containing);
        assertTrue(cornerSame < 0.3, "corner rect should be near 0: " + cornerSame);
    }

    @Test
    void costIsSymmetricAroundTheOrigin() {
        assertEquals(
            field.cost(new Rect(240 + 60, 135, 40, 40)),
            field.cost(new Rect(240 - 100, 135, 40, 40)),
            1.0e-12
        );
        assertEquals(
            field.cost(new Rect(240, 135 + 60, 40, 40)),
            field.cost(new Rect(240, 135 - 100, 40, 40)),
            1.0e-12
        );
    }

    @Test
    void emptyRectCostsNothing() {
        assertEquals(0.0, field.cost(new Rect(240, 135, 0, 40)));
        assertEquals(0.0, field.cost(new Rect(240, 135, 40, 0)));
    }

    @Test
    void screenCenterFactoryPlacesThePeak() {
        GaussianAttention centered = GaussianAttention.atScreenCenter(480, 270, 80.0);
        assertEquals(1.0, centered.valueAt(240.0, 135.0), 1.0e-12);
        assertEquals(240.0, centered.centerX(), 0.0);
        assertEquals(135.0, centered.centerY(), 0.0);
        assertEquals(80.0, centered.sigma(), 0.0);
        assertEquals(4, centered.samplingStep());
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new GaussianAttention(0, 0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new GaussianAttention(0, 0, -80.0));
        assertThrows(IllegalArgumentException.class, () -> new GaussianAttention(0, 0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new GaussianAttention(0, 0, 80.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new GaussianAttention(0, 0, 80.0, -4));
    }
}
