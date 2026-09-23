package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RayFanTest {

    private static final double twoPi = 2 * Math.PI;

    @Test
    void defaultRayCountIsOneHundredTwentyEight() {
        assertEquals(128, RayFan.defaultRayCount);
        RayFan fan = new RayFan(0, 0, 60);

        assertEquals(128, fan.rayCount());
        assertEquals(0.0, fan.rayAngle(0), 1e-12);
        assertEquals(Math.PI, fan.rayAngle(64), 1e-12);
        assertEquals(twoPi / 128, fan.rayAngle(1), 1e-12);
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new RayFan(0, 0, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new RayFan(0, 0, 128, 0));
    }

    @Test
    void allFreeYieldsSingleFullTurnArc() {
        RayFan fan = new RayFan(100, 100, 60);

        assertEquals(List.of(new IntervalSet.Interval(0, twoPi)), fan.freeArcs());
        assertEquals(List.of(Math.PI), fan.candidateDirections());
        assertEquals(128, fan.freeRays().size());
        assertFalse(fan.isBlocked(0));
        assertFalse(fan.isBlocked(Math.PI));
    }

    @Test
    void fullyOccupiedYieldsNothing() {
        RayFan fan = new RayFan(100, 100, 60);
        fan.block(new Rect(80, 80, 40, 40));

        assertEquals(List.of(), fan.freeArcs());
        assertEquals(List.of(), fan.candidateDirections());
        assertEquals(List.of(), fan.freeRays());
        assertNull(fan.bestDirection());
        assertTrue(fan.isBlocked(0));
        assertTrue(fan.isBlocked(2.7));
    }

    @Test
    void occluderBeyondRadiusDoesNotBlock() {
        RayFan fan = new RayFan(100, 100, 60);
        fan.block(new Rect(300, 0, 20, 20));

        assertEquals(List.of(new IntervalSet.Interval(0, twoPi)), fan.freeArcs());
        assertEquals(List.of(Math.PI), fan.candidateDirections());
    }

    @Test
    void occluderOnTheRightBlocksAroundZero() {
        RayFan fan = new RayFan(100, 100, 60);
        fan.block(new Rect(130, 90, 20, 20));

        double theta = Math.atan2(10, 50);
        assertEquals(List.of(new IntervalSet.Interval(theta, twoPi - theta)), fan.freeArcs());
        assertEquals(List.of(Math.PI), fan.candidateDirections());
        assertTrue(fan.isBlocked(0));
        assertTrue(fan.isBlocked(theta - 1e-9));
        assertFalse(fan.isBlocked(theta + 1e-9));
        assertTrue(fan.isBlocked(-theta + 1e-9));
        assertFalse(fan.isBlocked(Math.PI));

        // Rays blocked near angle 0: i = 0..4 below theta, i = 124..127 above 2π - theta.
        assertEquals(119, fan.freeRays().size());
        assertEquals(fan.rayAngle(5), fan.freeRays().getFirst(), 0.0);
        assertEquals(fan.rayAngle(123), fan.freeRays().getLast(), 0.0);
    }

    @Test
    void occluderOnTheLeftWrapsFreeArcAroundZero() {
        RayFan fan = new RayFan(100, 100, 60);
        fan.block(new Rect(50, 90, 20, 20));

        double theta = Math.atan2(10, 30);
        // Blocked [π - θ, π + θ]; the free space is one arc crossing angle 0.
        List<IntervalSet.Interval> arcs = fan.freeArcs();
        assertEquals(1, arcs.size());
        assertEquals(Math.PI + theta, arcs.getFirst().start(), 1e-12);
        assertEquals(Math.PI - theta + twoPi, arcs.getFirst().end(), 1e-12);
        assertEquals(twoPi - 2 * theta, arcs.getFirst().length(), 1e-12);

        List<Double> candidates = fan.candidateDirections();
        assertEquals(1, candidates.size());
        assertEquals(0.0, candidates.getFirst(), 1e-12);
        assertEquals(0.0, Objects.requireNonNull(fan.bestDirection()), 1e-12);
        assertTrue(fan.isBlocked(Math.PI));
        assertFalse(fan.isBlocked(0));
    }

    @Test
    void twoOccludersLeaveTwoArcsOrderedWidestFirst() {
        RayFan fan = new RayFan(100, 100, 60);
        fan.blockAll(List.of(new Rect(130, 90, 20, 20), new Rect(95, 60, 10, 10)));

        double rightTheta = Math.atan2(10, 50);
        double topMin = Math.atan2(-30, -5) + twoPi;
        double topMax = Math.atan2(-30, 5) + twoPi;
        assertEquals(
            List.of(new IntervalSet.Interval(rightTheta, topMin), new IntervalSet.Interval(topMax, twoPi - rightTheta)),
            fan.freeArcs()
        );

        List<Double> candidates = fan.candidateDirections();
        assertEquals(2, candidates.size());
        assertEquals((rightTheta + topMin) / 2, candidates.get(0), 1e-12);
        assertEquals((topMax + twoPi - rightTheta) / 2, candidates.get(1), 1e-12);
        assertEquals(candidates.getFirst(), Objects.requireNonNull(fan.bestDirection()), 1e-12);
    }

    @Test
    void blockIgnoresEmptyRectangles() {
        RayFan fan = new RayFan(100, 100, 60);
        fan.block(new Rect(130, 90, 0, 20));
        fan.block(new Rect(130, 90, 20, 0));

        assertEquals(List.of(new IntervalSet.Interval(0, twoPi)), fan.freeArcs());
    }

    @Test
    void resetClearsBlocks() {
        RayFan fan = new RayFan(100, 100, 60);
        fan.block(new Rect(130, 90, 20, 20));

        fan.reset();

        assertEquals(List.of(new IntervalSet.Interval(0, twoPi)), fan.freeArcs());
    }
}
