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
    void theOrthogonalMorphAnswersTheEndpointsVerbatim() {
        List<FloatPos> from = List.of(new FloatPos(0, 0), new FloatPos(92, 0), new FloatPos(92, 100));
        List<FloatPos> to =
                List.of(new FloatPos(0, 0), new FloatPos(48, 0), new FloatPos(48, 40), new FloatPos(140, 40));

        assertEquals(from, GuideLineSdf.morphOrthogonal(from, to, 0.0), "t = 0 is the old shape, unresampled");
        assertEquals(to, GuideLineSdf.morphOrthogonal(from, to, 1.0), "t = 1 is the new shape, unresampled");
        assertEquals(from, GuideLineSdf.morphOrthogonal(from, from, 0.5), "an unchanged route is a no-op");
        // a degenerate side hands the drawing to the other
        assertEquals(to, GuideLineSdf.morphOrthogonal(List.of(new FloatPos(5, 5)), to, 0.5));
    }

    @Test
    void theOrthogonalMorphSlidesALaneWithoutEverGoingDiagonal() {
        // the classic settle: the middle lane slides x 92 → 48, both ends
        // pinned — every intermediate is a Z whose middle run stays vertical
        List<FloatPos> from = List.of(new FloatPos(0, 0), new FloatPos(92, 0), new FloatPos(92, 100));
        List<FloatPos> to = List.of(new FloatPos(0, 0), new FloatPos(48, 0), new FloatPos(48, 100));

        for (double t : new double[] {0.1, 0.25, 0.5, 0.75, 0.9}) {
            List<FloatPos> mid = GuideLineSdf.morphOrthogonal(from, to, t);
            assertEquals(3, mid.size(), "t = " + t + ": a pure lane slide keeps the vertex count");
            assertEquals(new FloatPos(0, 0), mid.get(0), "t = " + t + ": the pinned end");
            assertEquals(92 + (48 - 92) * t, mid.get(1).x(), eps, "t = " + t + ": the lane slides");
            assertEquals(mid.get(1).x(), mid.get(2).x(), eps, "t = " + t + ": the middle run stays vertical");
            assertEquals(0, mid.get(1).y(), eps);
            assertEquals(100, mid.get(2).y(), eps);
        }
    }

    @Test
    void theOrthogonalMorphKeepsACornerACornerWhileItSlides() {
        // where the arc-length morph cuts corners (its resample lands
        // mid-run — the 150 px first leg puts the corner off every even
        // sample — so the sample's last segment leaves the axis), the
        // orthogonal morph keeps every bend a bend: an L sliding sideways
        // settles through right-angled Ls
        List<FloatPos> from = List.of(new FloatPos(0, 0), new FloatPos(150, 0), new FloatPos(150, 100));
        List<FloatPos> to = List.of(new FloatPos(0, 50), new FloatPos(150, 50), new FloatPos(150, 150));

        for (double t : new double[] {0.2, 0.5, 0.8}) {
            List<FloatPos> mid = GuideLineSdf.morphOrthogonal(from, to, t);
            assertEquals(3, mid.size(), "t = " + t);
            assertEquals(0 + 50 * t, mid.get(0).y(), eps, "t = " + t + ": the first run stays horizontal");
            assertEquals(mid.get(1).y(), mid.get(0).y(), eps, "t = " + t + ": …both ends of it");
            assertEquals(mid.get(1).x(), mid.get(2).x(), eps, "t = " + t + ": the second run stays vertical");
            assertEquals(150, mid.get(1).x(), eps, "t = " + t + ": the corner keeps its x");

            List<FloatPos> resampled = GuideLineSdf.morph(from, to, t, 3);
            assertTrue(
                    Math.abs(resampled.get(0).y() - resampled.get(1).y()) > eps
                            || Math.abs(resampled.get(1).x() - resampled.get(2).x()) > eps,
                    "t = " + t + ": the resample the old settle used cuts the corner — the regression");
        }
    }

    @Test
    void theOrthogonalMorphLocalizesTheDiagonalsOfATopologyChange() {
        // a route gaining a bend: the alignment pads the shorter side with
        // duplicates, so the settle grows the new bend in place instead of
        // lerping the whole line through diagonals
        List<FloatPos> from = List.of(new FloatPos(0, 0), new FloatPos(92, 0), new FloatPos(92, 100));
        List<FloatPos> to = List.of(
                new FloatPos(0, 0),
                new FloatPos(48, 0),
                new FloatPos(48, 40),
                new FloatPos(140, 40),
                new FloatPos(140, 100));

        for (double t : new double[] {0.25, 0.5, 0.75}) {
            List<FloatPos> mid = GuideLineSdf.morphOrthogonal(from, to, t);
            int diagonals = 0;
            for (int i = 0; i + 1 < mid.size(); i++) {
                FloatPos a = mid.get(i);
                FloatPos b = mid.get(i + 1);
                if (Math.abs(a.x() - b.x()) > eps && Math.abs(a.y() - b.y()) > eps) {
                    diagonals++;
                }
            }
            assertTrue(
                    diagonals <= 2,
                    "t = " + t + ": a topology change settles through at most a couple of diagonals — " + mid);
        }
    }

    @Test
    void anOrthogonalCornerRendersRoundSquareWithNoProtrusionAndContinuousArc() {
        // mirrors guide_line_hud.fsh's per-segment loop — min distance to the
        // segments plus the arc position at the closest point. An orthogonal
        // corner must render as a rounded-square corner: the union of
        // round-capped segments never pokes past the corner vertex's offset
        // disc (the miter spike a bevel renderer would show), and the arc
        // attribution must walk continuously through the corner — a dash or
        // fade that tears at the bend reads as a broken joint
        List<FloatPos> elbow = List.of(new FloatPos(0, 0), new FloatPos(150, 0), new FloatPos(150, 100));

        // no protrusion: along the corner's outer bisector the field is the
        // plain radial distance to the vertex — the level set is a quarter
        // disc of radius w, strictly inside the miter square
        for (double d = 0.05; d <= 6.0; d += 0.25) {
            double t = d / Math.sqrt(2);
            double dist = distanceField(elbow, 150 + t, -t);
            assertEquals(d, dist, 1.0e-9, "bisector distance at d = " + d);
        }
        // and along the offset walls the field is the perpendicular one
        for (double y = -5.0; y <= -0.5; y += 0.5) {
            assertEquals(-y, distanceField(elbow, 149.0, y), 1.0e-9, "below the horizontal run");
        }
        for (double x = 151.0; x <= 155.0; x += 0.5) {
            assertEquals(x - 150, distanceField(elbow, x, 50), 1.0e-9, "right of the vertical run");
        }

        // arc continuity: walking the centerline across the bend, the arc
        // position tracks the walked distance with no jump at the corner
        double previous = Double.NaN;
        for (double walked = 130; walked <= 170; walked += 0.5) {
            double x = walked <= 150 ? walked : 150;
            double y = walked <= 150 ? 0 : walked - 150;
            double arc = arcAt(elbow, x, y);
            if (!Double.isNaN(previous)) {
                double step = Math.abs(arc - previous);
                assertTrue(step <= 0.5 + 1.0e-9, "arc jumped " + step + " px at walked = " + walked);
            }
            previous = arc;
        }
    }

    /** The shader's distance field: the minimum distance to the polyline's segments. */
    private static double distanceField(List<FloatPos> points, double x, double y) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i + 1 < points.size(); i++) {
            best = Math.min(best, pointSegmentDistance(new FloatPos(x, y), points.get(i), points.get(i + 1)));
        }
        return best;
    }

    /** The distance from p to the segment a→b, round caps included. */
    private static double pointSegmentDistance(FloatPos p, FloatPos a, FloatPos b) {
        double h = pointAt(a, b, p);
        return distance(p, new FloatPos(a.x() + (b.x() - a.x()) * h, a.y() + (b.y() - a.y()) * h));
    }

    /** The shader's arc attribution: the arc position at the closest point. */
    private static double arcAt(List<FloatPos> points, double x, double y) {
        double best = Double.POSITIVE_INFINITY;
        double bestArc = 0;
        double walked = 0;
        for (int i = 0; i + 1 < points.size(); i++) {
            FloatPos a = points.get(i);
            FloatPos b = points.get(i + 1);
            double d = pointSegmentDistance(new FloatPos(x, y), a, b);
            if (d < best) {
                best = d;
                double len = distance(a, b);
                double h = pointAt(a, b, new FloatPos(x, y));
                bestArc = walked + len * h;
            }
            walked += distance(a, b);
        }
        return bestArc;
    }

    /** The projection parameter of p onto a→b, clamped to [0, 1]. */
    private static double pointAt(FloatPos a, FloatPos b, FloatPos p) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double len2 = dx * dx + dy * dy;
        if (len2 < eps) return 0;
        return Math.max(0.0, Math.min(1.0, ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / len2));
    }

    @Test
    void trimEndCutsTheArrivalGapOffTheWorldEnd() {
        List<FloatPos> elbow = List.of(new FloatPos(0, 0), new FloatPos(100, 0), new FloatPos(100, 100));

        List<FloatPos> trimmed = GuideLineSdf.trimEnd(elbow, 10);
        assertEquals(new FloatPos(100, 90), trimmed.get(trimmed.size() - 1), "10 px short of the far end");
        assertEquals(
                List.of(new FloatPos(0, 0), new FloatPos(100, 0), new FloatPos(100, 90)),
                trimmed,
                "the surviving corner keeps its vertex");
        assertEquals(elbow, GuideLineSdf.trimEnd(elbow, 0), "no gap, no change");
        assertEquals(
                List.of(new FloatPos(0, 0)),
                GuideLineSdf.trimEnd(elbow, 500),
                "a gap past the end leaves the start point");
    }

    @Test
    void trimEndKeepsTheStartWhenTheCutLandsInTheFirstSegment() {
        // the straight two-point stroke: the arrival-gap cut always lands in
        // the first (only) segment — dropping the segment's start collapses
        // the stroke to a lone point, and a one-point polyline renders nothing
        List<FloatPos> straight = List.of(new FloatPos(994, 250), new FloatPos(300, 250));
        assertEquals(List.of(new FloatPos(994, 250), new FloatPos(306, 250)), GuideLineSdf.trimEnd(straight, 6));
        // a cut at the very start still answers the lone start point
        assertEquals(List.of(new FloatPos(994, 250)), GuideLineSdf.trimEnd(straight, 694 - 1.0e-12));
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
