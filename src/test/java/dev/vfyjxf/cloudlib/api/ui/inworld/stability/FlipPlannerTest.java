package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.testutil.GeometryAsserts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlipPlannerTest {

    private static final FloatRect rectA = new FloatRect(0, 0, 10, 10);
    private static final FloatRect rectB = new FloatRect(100, 0, 10, 10);
    private static final FloatRect rectC = new FloatRect(100, 100, 10, 10);

    @Test
    void idleVisualIsThePlacedRect() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);

        GeometryAsserts.assertRectEquals(rectA, planner.visual(0.0), 0.0);
        GeometryAsserts.assertRectEquals(rectA, planner.visual(10.0), 0.0);
        GeometryAsserts.assertRectEquals(rectA, planner.target(), 0.0);
        assertFalse(planner.isAnimating(10.0));
    }

    @Test
    void flipInterpolatesLinearlyAtConstantSpeed() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        assertTrue(planner.isAnimating(0.1));
        GeometryAsserts.assertRectEquals(new FloatRect(25, 0, 10, 10), planner.visual(0.05), 1.0e-9);
        GeometryAsserts.assertRectEquals(new FloatRect(50, 0, 10, 10), planner.visual(0.1), 1.0e-9);
        GeometryAsserts.assertRectEquals(new FloatRect(75, 0, 10, 10), planner.visual(0.15), 1.0e-9);
    }

    @Test
    void durationIsDistanceOverSpeed() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        assertTrue(planner.isAnimating(0.2 - 1.0e-9));
        assertFalse(planner.isAnimating(0.2));
    }

    @Test
    void completesAtTargetAndStops() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        GeometryAsserts.assertRectEquals(rectB, planner.visual(0.2), 1.0e-9);
        assertFalse(planner.isAnimating(0.2));
        GeometryAsserts.assertRectEquals(rectB, planner.visual(1.0), 0.0);
        assertFalse(planner.isAnimating(1.0));
    }

    @Test
    void smallMovesTakeAtLeastTheMinimumDuration() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(new FloatRect(1, 0, 10, 10), 0.0);

        assertTrue(planner.isAnimating(0.119));
        assertFalse(planner.isAnimating(0.121));
        GeometryAsserts.assertRectEquals(new FloatRect(0.5, 0, 10, 10), planner.visual(0.06), 1.0e-9);
    }

    @Test
    void largeMovesAreCappedAtTheMaximumDuration() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(new FloatRect(2000, 0, 10, 10), 0.0);

        GeometryAsserts.assertRectEquals(new FloatRect(1000, 0, 10, 10), planner.visual(0.125), 1.0e-9);
        assertFalse(planner.isAnimating(0.251));
    }

    @Test
    void midFlightRetargetContinuesFromTheCurrentVisualPosition() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        GeometryAsserts.assertRectEquals(new FloatRect(50, 0, 10, 10), planner.visual(0.1), 1.0e-9);
        assertEquals(49.95, planner.visual(0.0999).x(), 1.0e-9);

        planner.flipTo(rectC, 0.1);

        GeometryAsserts.assertRectEquals(new FloatRect(50, 0, 10, 10), planner.visual(0.1), 1.0e-9);
        FloatRect after = planner.visual(0.1001);
        assertTrue(Math.abs(after.x() - 50.0) < 0.06, "seam must be continuous: " + after);
        assertTrue(Math.abs(after.y() - 0.0) < 0.06, "seam must be continuous: " + after);

        double secondLegDistance = Math.hypot(50, 100);
        double halfWay = 0.1 + secondLegDistance / 500.0 / 2.0;
        GeometryAsserts.assertRectEquals(new FloatRect(75, 50, 10, 10), planner.visual(halfWay), 1.0e-9);

        double arrival = 0.1 + secondLegDistance / 500.0;
        GeometryAsserts.assertRectEquals(rectC, planner.visual(arrival + 1.0e-6), 1.0e-9);
        assertFalse(planner.isAnimating(arrival + 1.0e-6));
    }

    @Test
    void retargetToTheCurrentTargetWhileAnimatingKeepsTheSchedule() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        planner.flipTo(rectB, 0.05);

        assertTrue(planner.isAnimating(0.199));
        GeometryAsserts.assertRectEquals(rectB, planner.visual(0.2), 1.0e-9);
    }

    @Test
    void flipToTheCurrentVisualMidFlightSettles() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        planner.flipTo(new FloatRect(50, 0, 10, 10), 0.1);

        assertFalse(planner.isAnimating(0.1));
        GeometryAsserts.assertRectEquals(new FloatRect(50, 0, 10, 10), planner.visual(5.0), 0.0);
    }

    @Test
    void nonMonotonicTimeIsTolerated() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        GeometryAsserts.assertRectEquals(new FloatRect(75, 0, 10, 10), planner.visual(0.15), 1.0e-9);
        GeometryAsserts.assertRectEquals(new FloatRect(50, 0, 10, 10), planner.visual(0.1), 1.0e-9);

        planner.flipTo(rectC, 0.05);

        assertTrue(planner.isAnimating(0.05));
        GeometryAsserts.assertRectEquals(new FloatRect(25, 0, 10, 10), planner.visual(0.05), 1.0e-9);
    }

    @Test
    void snapCancelsAnimationAndJumps() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        planner.snap(new FloatRect(7, 7, 20, 20));

        GeometryAsserts.assertRectEquals(new FloatRect(7, 7, 20, 20), planner.visual(0.05), 0.0);
        assertFalse(planner.isAnimating(0.05));

        planner.flipTo(rectB, 0.05);
        assertTrue(planner.isAnimating(0.1));
    }

    @Test
    void pureResizeMorphsOverTheMinimumDuration() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(new FloatRect(0, 0, 30, 30), 0.0);

        assertTrue(planner.isAnimating(0.06));
        GeometryAsserts.assertRectEquals(new FloatRect(0, 0, 20, 20), planner.visual(0.06), 1.0e-9);
    }

    @Test
    void samplingOrderDoesNotMatter() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        GeometryAsserts.assertRectEquals(rectB, planner.visual(5.0), 0.0);
        GeometryAsserts.assertRectEquals(new FloatRect(50, 0, 10, 10), planner.visual(0.1), 1.0e-9);
        GeometryAsserts.assertRectEquals(rectB, planner.visual(5.0), 0.0);
        GeometryAsserts.assertRectEquals(rectA, planner.visual(0.0), 0.0);
    }

    @Test
    void translateShiftsTheMorphWithoutChangingItsPhase() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        planner.flipTo(rectB, 0.0);

        planner.translate(20.0, -5.0);

        // every sample moves with the push: origin, target and the mid-flight
        // visual alike, so progress, remaining travel and duration are intact
        GeometryAsserts.assertRectEquals(new FloatRect(45, -5, 10, 10), planner.visual(0.05), 1.0e-9);
        GeometryAsserts.assertRectEquals(new FloatRect(120, -5, 10, 10), planner.target(), 1.0e-9);
        assertTrue(planner.isAnimating(0.2 - 1.0e-9));
        assertFalse(planner.isAnimating(0.2));
        GeometryAsserts.assertRectEquals(new FloatRect(120, -5, 10, 10), planner.visual(0.2), 1.0e-9);
    }

    @Test
    void translateWhileIdleShiftsTheRestingRect() {
        FlipPlanner planner = new FlipPlanner(500.0, rectA);

        planner.translate(5.0, 5.0);

        GeometryAsserts.assertRectEquals(new FloatRect(5, 5, 10, 10), planner.visual(1.0), 0.0);
        assertFalse(planner.isAnimating(1.0));
    }

    @Test
    void rejectsInvalidConstructionInputAndTime() {
        assertThrows(IllegalArgumentException.class, () -> new FlipPlanner(0.0, rectA));
        assertThrows(IllegalArgumentException.class, () -> new FlipPlanner(-1.0, rectA));
        assertThrows(IllegalArgumentException.class, () -> new FlipPlanner(Double.NaN, rectA));
        assertThrows(IllegalArgumentException.class, () -> new FlipPlanner(500.0, rectA, 0.2, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new FlipPlanner(500.0, rectA, 0.0, 0.25));
        assertThrows(IllegalArgumentException.class, () -> new FlipPlanner(500.0, rectA, Double.NaN, 0.25));
        assertThrows(NullPointerException.class, () -> new FlipPlanner(500.0, null));

        FlipPlanner planner = new FlipPlanner(500.0, rectA);
        assertThrows(NullPointerException.class, () -> planner.flipTo(null, 0.0));
        assertThrows(IllegalArgumentException.class, () -> planner.flipTo(rectB, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> planner.flipTo(rectB, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> planner.visual(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> planner.isAnimating(Double.NaN));
        assertThrows(NullPointerException.class, () -> planner.snap(null));
        assertThrows(IllegalArgumentException.class, () -> planner.translate(Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class, () -> planner.translate(0.0, Double.POSITIVE_INFINITY));
    }
}
