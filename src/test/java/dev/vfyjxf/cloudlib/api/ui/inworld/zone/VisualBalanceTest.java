package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualBalanceTest {

    private static final double W = 1291;
    private static final double H = 611;
    private static final double epsilon = 1.0e-9;

    /** A small panel centered on the given point — cell weight without area spread. */
    private static VisualBalance.Panel dot(double cx, double cy) {
        return VisualBalance.Panel.opaque(FloatRect.around(new FloatPos(cx, cy), 2, 2));
    }

    private static double gridAt(double fx, double fy) {
        return VisualBalance.nineGridWeight(W * fx, H * fy, W, H);
    }

    @Test
    void theOpticalCenterSitsAboveTheGeometricMiddle() {
        FloatPos optical = VisualBalance.opticalCenter(W, H);
        assertEquals(W * 0.5, optical.x(), epsilon);
        assertEquals(H * (0.5 - 0.035), optical.y(), epsilon);
        assertTrue(optical.y() < H * 0.5, "the optical center lifts up (negative y)");
    }

    @Test
    void nineGridWeightsMatchTheVmeCalibration() {
        // the VME fixation counts 23/14/13/12/11/8/7/6/6 normalized to the
        // center's 1.00 (Zhang 2025, Symmetry)
        assertEquals(23.0 / 23.0, gridAt(0.5, 0.5), epsilon);
        assertEquals(14.0 / 23.0, gridAt(0.5, 1.0 / 6.0), epsilon, "top-mid");
        assertEquals(13.0 / 23.0, gridAt(1.0 / 6.0, 0.5), epsilon, "mid-left");
        assertEquals(12.0 / 23.0, gridAt(5.0 / 6.0, 0.5), epsilon, "mid-right");
        assertEquals(11.0 / 23.0, gridAt(0.5, 5.0 / 6.0), epsilon, "bottom-mid");
        assertEquals(8.0 / 23.0, gridAt(1.0 / 6.0, 1.0 / 6.0), epsilon, "top-left");
        assertEquals(7.0 / 23.0, gridAt(5.0 / 6.0, 1.0 / 6.0), epsilon, "top-right");
        assertEquals(6.0 / 23.0, gridAt(1.0 / 6.0, 5.0 / 6.0), epsilon, "bottom-left");
        assertEquals(6.0 / 23.0, gridAt(5.0 / 6.0, 5.0 / 6.0), epsilon, "bottom-right");
        // boundary points resolve through the half-open thirds: the middle
        // cell spans [W/3, 2W/3), the last cell closes the interval
        assertEquals(1.0, VisualBalance.nineGridWeight(W / 3 + 1, H / 2, W, H), epsilon);
        assertEquals(12.0 / 23.0, VisualBalance.nineGridWeight(2 * W / 3 + 1, H / 2, W, H), epsilon);
        assertEquals(12.0 / 23.0, VisualBalance.nineGridWeight(W - 1, H / 2, W, H), epsilon);
    }

    @Test
    void panelWeightIsAreaTimesAlphaTimesGrid() {
        FloatRect rect = new FloatRect(100, 100, 40, 20);
        VisualBalance.Panel panel = new VisualBalance.Panel(rect, 0.5);
        double expected = rect.area() * 0.5 * VisualBalance.nineGridWeight(rect.centerX(), rect.centerY(), W, H);
        assertEquals(expected, VisualBalance.panelWeight(panel, W, H), epsilon);
        // a zero-area panel weighs nothing
        assertEquals(0.0, VisualBalance.panelWeight(VisualBalance.Panel.opaque(FloatRect.empty), W, H), epsilon);
    }

    @Test
    void aCenteredPanelScoresAboveACorneredOne() {
        FloatPos optical = VisualBalance.opticalCenter(W, H);
        double centered = VisualBalance
                .score(List.of(VisualBalance.Panel.opaque(FloatRect.around(optical, 120, 40))), W, H);
        double cornered = VisualBalance
                .score(List.of(VisualBalance.Panel.opaque(new FloatRect(1080, 520, 120, 40))), W, H);
        assertEquals(1.0, centered, epsilon, "a centroid on the optical center is a perfect score");
        assertTrue(cornered < centered, "the cornered panel scores below the centered one: " + cornered);
        assertTrue(cornered < 1.0);
    }

    @Test
    void lateralImbalanceIsWeightedWorseThanVertical() {
        FloatPos target = new FloatPos(W / 2, H / 2);
        double lateral = VisualBalance.offset(new FloatPos(W / 2 + 100, H / 2), target);
        double vertical = VisualBalance.offset(new FloatPos(W / 2, H / 2 + 100), target);
        assertEquals(1.5 * 100, lateral, epsilon);
        assertEquals(100, vertical, epsilon);
        assertTrue(lateral > vertical, "the ×1.5 lateral axis dominates the same displacement");
    }

    @Test
    void aSymmetricLayoutOutscoresTheSamePileShiftedAside() {
        // two equal panels mirrored about the screen's vertical midline vs
        // the same pair both sitting in the right third
        List<VisualBalance.Panel> symmetric = List.of(
            VisualBalance.Panel.opaque(new FloatRect(60, 260, 200, 60)),
            VisualBalance.Panel.opaque(new FloatRect(W - 260, 260, 200, 60))
        );
        List<VisualBalance.Panel> shifted = List.of(
            VisualBalance.Panel.opaque(new FloatRect(760, 260, 200, 60)),
            VisualBalance.Panel.opaque(new FloatRect(W - 260, 260, 200, 60))
        );
        double symmetricScore = VisualBalance.score(symmetric, W, H);
        double shiftedScore = VisualBalance.score(shifted, W, H);
        assertTrue(symmetricScore > shiftedScore, "symmetric " + symmetricScore + " vs shifted " + shiftedScore);
        assertTrue(symmetricScore > 0.9, "a mirrored pair sits near the target: " + symmetricScore);
    }

    @Test
    void theCentroidFollowsTheWeights() {
        // equal weights: the midpoint; a heavier opaque panel drags it. Both
        // centers sit inside the same nine-grid cell so the grid weights cancel
        FloatRect left = new FloatRect(440, 280, 60, 50);
        FloatRect right = new FloatRect(790, 280, 60, 50);
        FloatPos mid = VisualBalance
                .centroid(List.of(VisualBalance.Panel.opaque(left), VisualBalance.Panel.opaque(right)), W, H);
        assertEquals((left.centerX() + right.centerX()) / 2, mid.x(), epsilon);
        FloatPos dragged = VisualBalance
                .centroid(List.of(new VisualBalance.Panel(left, 1.0), new VisualBalance.Panel(right, 0.25)), W, H);
        assertTrue(dragged.x() < mid.x(), "the opaque panel's side carries the centroid");
    }

    @Test
    void aWeightlessLayoutIsVacuouslyBalanced() {
        assertNull(VisualBalance.centroid(List.of(), W, H));
        assertEquals(1.0, VisualBalance.score(List.of(), W, H), epsilon);
        // zero-area panels weigh nothing either
        assertEquals(1.0, VisualBalance.score(List.of(VisualBalance.Panel.opaque(FloatRect.empty)), W, H), epsilon);
    }

    @Test
    void theScoreNormalizesByTheHalfSum() {
        // a centroid one maxOffset of pure vertical offset from the target
        // reads exactly 0.0
        FloatPos target = VisualBalance.opticalCenter(W, H);
        double dMax = VisualBalance.maxOffset(W, H);
        assertEquals((W + H) / 2, dMax, epsilon);
        double score = 1.0 - VisualBalance.offset(new FloatPos(target.x(), target.y() + dMax), target) / dMax;
        assertEquals(0.0, score, epsilon);
    }

    @Test
    void invalidInputsThrow() {
        assertThrows(IllegalArgumentException.class, () -> VisualBalance.opticalCenter(0, H));
        assertThrows(IllegalArgumentException.class, () -> VisualBalance.maxOffset(W, -1));
        assertThrows(IllegalArgumentException.class, () -> new VisualBalance.Panel(new FloatRect(0, 0, 1, 1), 1.5));
        assertThrows(IllegalArgumentException.class, () -> new VisualBalance.Panel(new FloatRect(0, 0, 1, 1), -0.1));
        assertThrows(NullPointerException.class, () -> new VisualBalance.Panel(null, 1.0));
    }
}
