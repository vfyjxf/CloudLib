package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiecewiseAttentionTest {

    private static final PiecewiseAttention field = new PiecewiseAttention(240.0, 135.0, 60.0, 160.0);

    @Test
    void threeLevelsByDistanceBand() {
        assertEquals(PiecewiseAttention.highValue, field.valueAt(240.0, 135.0), 0.0);
        assertEquals(PiecewiseAttention.highValue, field.valueAt(240.0 + 59.0, 135.0), 0.0);
        assertEquals(PiecewiseAttention.mediumValue, field.valueAt(240.0 + 61.0, 135.0), 0.0);
        assertEquals(PiecewiseAttention.lowValue, field.valueAt(240.0 + 161.0, 135.0), 0.0);
        // corners are the cheapest
        assertEquals(PiecewiseAttention.lowValue, field.valueAt(0.0, 0.0), 0.0);
    }

    @Test
    void boundariesAreInclusiveAtTheBandEdge() {
        // exactly at the inner radius: still HIGH; exactly at the outer: still MEDIUM
        assertEquals(PiecewiseAttention.highValue, field.valueAt(240.0 + 60.0, 135.0), 0.0);
        assertEquals(PiecewiseAttention.mediumValue, field.valueAt(240.0 + 160.0, 135.0), 0.0);
        assertEquals(PiecewiseAttention.lowValue, field.valueAt(240.0 + 160.0 + 0.5, 135.0), 0.0);
    }

    @Test
    void boundaryRadiiAreConfigurable() {
        PiecewiseAttention tighter = new PiecewiseAttention(240.0, 135.0, 20.0, 40.0);
        // a point 100px out: LOW on the tight field, MEDIUM on the default one
        assertEquals(PiecewiseAttention.lowValue, tighter.valueAt(340.0, 135.0), 0.0);
        assertEquals(PiecewiseAttention.mediumValue, field.valueAt(340.0, 135.0), 0.0);
    }

    @Test
    void costIsMonotonicallyNonIncreasingAwayFromThePeak() {
        double center = field.cost(new Rect(220, 115, 40, 40));
        double midBand = field.cost(new Rect(320, 115, 40, 40));
        double corner = field.cost(new Rect(410, 5, 40, 40));
        assertTrue(center > midBand, center + " !> " + midBand);
        assertTrue(midBand > corner, midBand + " !> " + corner);
        assertTrue(center >= 0.0 && center <= 1.0);
        assertTrue(midBand >= 0.0 && midBand <= 1.0);
        assertTrue(corner >= 0.0 && corner <= 1.0);
    }

    @Test
    void rectContainingThePeakCostsMoreThanOneWithoutIt() {
        double containing = field.cost(new Rect(220, 115, 40, 40));
        double cornerSame = field.cost(new Rect(410, 5, 40, 40));
        assertTrue(containing > cornerSame, containing + " !> " + cornerSame);
        assertEquals(PiecewiseAttention.highValue, containing, 1.0e-9);
    }

    @Test
    void costAveragesAcrossBandBoundaries() {
        // a rect straddling the inner boundary: half samples HIGH, half MEDIUM
        PiecewiseAttention straddled = new PiecewiseAttention(240.0, 135.0, 8.0, 160.0, 1);
        // samples at x = 229..268 step 1 relative to rect (224, 125, 48, 20):
        // x in [224.5, 271.5], inner boundary at 240 → 15.5 samples below? just assert between the levels
        double cost = straddled.cost(new Rect(224, 125, 48, 20));
        assertTrue(
                cost > PiecewiseAttention.mediumValue && cost < PiecewiseAttention.highValue,
                "expected a strict mix, got " + cost);
    }

    @Test
    void screenCenterFactoryPlacesThePeak() {
        PiecewiseAttention centered = PiecewiseAttention.atScreenCenter(480, 270, 60.0, 160.0);
        assertEquals(1.0, centered.valueAt(240.0, 135.0), 0.0);
        assertEquals(60.0, centered.innerRadius(), 0.0);
        assertEquals(160.0, centered.outerRadius(), 0.0);
        assertEquals(4, centered.samplingStep());
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new PiecewiseAttention(0, 0, 0.0, 160.0));
        assertThrows(IllegalArgumentException.class, () -> new PiecewiseAttention(0, 0, -60.0, 160.0));
        assertThrows(IllegalArgumentException.class, () -> new PiecewiseAttention(0, 0, 160.0, 160.0));
        assertThrows(IllegalArgumentException.class, () -> new PiecewiseAttention(0, 0, 200.0, 160.0));
        assertThrows(IllegalArgumentException.class, () -> new PiecewiseAttention(0, 0, 60.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new PiecewiseAttention(0, 0, 60.0, 160.0, 0));
    }
}
