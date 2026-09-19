package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideLineSdfTest {

    private static final double eps = 1.0e-9;

    @Test
    void theDashPhaseTravelsOnePeriodPerCycle() {
        assertEquals(0.0, GuideLineSdf.dashPhasePx(0.0, 8, 0.4, 1), eps);
        assertEquals(4.0, GuideLineSdf.dashPhasePx(0.2, 8, 0.4, 1), eps, "half a cycle, half a period");
        assertEquals(0.0, GuideLineSdf.dashPhasePx(0.4, 8, 0.4, 1), eps, "a full cycle wraps to zero");
        assertEquals(2.0, GuideLineSdf.dashPhasePx(0.5, 8, 0.4, 1), eps, "and keeps wrapping");
    }

    @Test
    void aHoveredLineFlowsItsAntsFaster() {
        double base = GuideLineSdf.dashPhasePx(0.1, 8, 0.4, 1);
        double hovered = GuideLineSdf.dashPhasePx(0.1, 8, 0.4, 1.75);

        assertEquals(2.0, base, eps);
        assertEquals(3.5, hovered, eps, "1.75× the flow over the same elapsed time");
    }

    @Test
    void theDutyCycleSplitsSolidFromGap() {
        assertTrue(GuideLineSdf.dashOn(0.0, 100, 0.0, 8, 0.5));
        assertTrue(GuideLineSdf.dashOn(3.9, 100, 0.0, 8, 0.5));
        assertFalse(GuideLineSdf.dashOn(4.0, 100, 0.0, 8, 0.5), "half the period is gap");
        assertTrue(GuideLineSdf.dashOn(8.0, 100, 0.0, 8, 0.5), "the next period resumes");
        // the phase slides the pattern, it does not resize it
        assertFalse(GuideLineSdf.dashOn(0.0, 100, 4.0, 8, 0.5));
        assertTrue(GuideLineSdf.dashOn(4.0, 100, 4.0, 8, 0.5));
    }

    @Test
    void theWorldEndFadeCoversOnlyTheLastFraction() {
        assertEquals(1.0, GuideLineSdf.worldEndAlpha(0.0, 0.25, 0.15), eps);
        assertEquals(1.0, GuideLineSdf.worldEndAlpha(0.5, 0.25, 0.15), eps);
        assertEquals(1.0, GuideLineSdf.worldEndAlpha(0.75, 0.25, 0.15), eps, "the fade starts here");
        assertEquals(0.575, GuideLineSdf.worldEndAlpha(0.875, 0.25, 0.15), eps, "halfway down the fade");
        assertEquals(0.15, GuideLineSdf.worldEndAlpha(1.0, 0.25, 0.15), eps, "the tail's floor");
        assertEquals(1.0, GuideLineSdf.worldEndAlpha(1.0, 0.0, 0.15), eps, "a zero fade never dims");
    }

    @Test
    void theArcParameterizationWalksThePolylineByLength() {
        List<FloatPos> elbow = List.of(new FloatPos(0, 0), new FloatPos(100, 0), new FloatPos(100, 100));

        assertEquals(200.0, GuideLineSdf.length(elbow), eps);
        assertEquals(new FloatPos(0, 0), GuideLineSdf.pointAt(elbow, 0.0));
        assertEquals(new FloatPos(100, 0), GuideLineSdf.pointAt(elbow, 0.5), "the corner sits at the arc's middle");
        assertEquals(new FloatPos(100, 50), GuideLineSdf.pointAt(elbow, 0.75));
        assertEquals(new FloatPos(100, 100), GuideLineSdf.pointAt(elbow, 1.0));
    }

    @Test
    void resampleSpacesPointsEvenlyAlongTheArc() {
        List<FloatPos> elbow = List.of(new FloatPos(0, 0), new FloatPos(100, 0), new FloatPos(100, 100));
        List<FloatPos> even = GuideLineSdf.resample(elbow, 5);

        assertEquals(5, even.size());
        assertEquals(new FloatPos(0, 0), even.get(0));
        assertEquals(new FloatPos(100, 0), even.get(2), "the 3rd of 5 lands on the corner at 50% arc");
        assertEquals(new FloatPos(100, 100), even.get(4));
        // equal arc spacing: every step is the same length
        for (int i = 2; i < even.size(); i++) {
            double previous = distance(even.get(i - 1), even.get(i));
            assertEquals(distance(even.get(0), even.get(1)), previous, 1.0e-6);
        }
    }

    @Test
    void theMorphInterpolatesBetweenTwoShapes() {
        List<FloatPos> from = List.of(new FloatPos(0, 0), new FloatPos(100, 0));
        List<FloatPos> to = List.of(new FloatPos(0, 50), new FloatPos(100, 50));

        assertEquals(GuideLineSdf.resample(from, 3), GuideLineSdf.morph(from, to, 0.0, 3));
        assertEquals(GuideLineSdf.resample(to, 3), GuideLineSdf.morph(from, to, 1.0, 3));
        assertEquals(new FloatPos(50, 25), GuideLineSdf.morph(from, to, 0.5, 3).get(1), "the midpoint of the slide");
        // a vertex-count mismatch is what the resample is for
        assertEquals(
                4,
                GuideLineSdf.morph(from, List.of(new FloatPos(0, 0), new FloatPos(50, 40), to.get(1)), 0.5, 4)
                        .size());
    }

    @Test
    void trimEndCutsTheArrivalGapOffTheWorldEnd() {
        List<FloatPos> elbow = List.of(new FloatPos(0, 0), new FloatPos(100, 0), new FloatPos(100, 100));

        List<FloatPos> trimmed = GuideLineSdf.trimEnd(elbow, 10);
        assertEquals(new FloatPos(100, 90), trimmed.get(trimmed.size() - 1), "10 px short of the far end");
        assertEquals(2, trimmed.size(), "the surviving corner keeps its vertex");
        assertEquals(elbow, GuideLineSdf.trimEnd(elbow, 0), "no gap, no change");
        assertEquals(
                List.of(new FloatPos(0, 0)),
                GuideLineSdf.trimEnd(elbow, 500),
                "a gap past the end leaves the start point");
    }

    @Test
    void rejectsInvalidUse() {
        assertThrows(IllegalArgumentException.class, () -> GuideLineSdf.dashPhasePx(1, 0, 0.4, 1));
        assertThrows(IllegalArgumentException.class, () -> GuideLineSdf.dashPhasePx(1, 8, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> GuideLineSdf.dashPhasePx(1, 8, 0.4, 0));
        assertThrows(IllegalArgumentException.class, () -> GuideLineSdf.resample(List.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> GuideLineSdf.pointAt(List.of(), 0.5));
        // degenerate geometry answers the endpoints rather than dividing by zero
        assertEquals(0.0, GuideLineSdf.length(List.of(new FloatPos(5, 5))), 0.0);
        assertEquals(new FloatPos(5, 5), GuideLineSdf.pointAt(List.of(new FloatPos(5, 5)), 0.5));
    }

    private static double distance(FloatPos a, FloatPos b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }
}
