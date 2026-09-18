package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.testutil.ProjectionSimulator;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffscreenProjectorTest {

    private static final double eps = 1.0e-6;

    private final Projection projection =
            ProjectionSimulator.at(0, 0, 0).lookAt(0, 0, -1).screen(480, 270).build();

    private final OffscreenProjector projector = new OffscreenProjector(8);

    @Test
    void targetAtScreenCenterIsOnScreenWithoutAnEdge() {
        OffscreenProjector.Result result = projector.project(projection, new Vec3(0, 0, -10));

        assertTrue(result.onScreen());
        assertFalse(result.behind());
        assertNull(result.edgePoint());
        assertNull(result.edge());
        assertEquals(240, result.screenPos().x(), 0.01);
        assertEquals(135, result.screenPos().y(), 0.01);
    }

    @Test
    void farLeftTargetLandsOnTheLeftEdge() {
        OffscreenProjector.Result result = projector.project(projection, new Vec3(-50, 0, -10));

        assertFalse(result.onScreen());
        assertFalse(result.behind());
        assertEquals(ScreenEdge.left, result.edge());
        assertEquals(8, result.edgePoint().x(), eps);
        assertTrue(result.edgePoint().y() > 8 && result.edgePoint().y() < 262);
        assertTrue(result.dirX() < 0);
    }

    @Test
    void farUpTargetLandsOnTheTopEdge() {
        OffscreenProjector.Result result = projector.project(projection, new Vec3(0, 40, -10));

        assertEquals(ScreenEdge.top, result.edge());
        assertEquals(8, result.edgePoint().y(), eps);
        assertTrue(result.edgePoint().x() > 8 && result.edgePoint().x() < 472);
    }

    @Test
    void farDownAndFarRightTargetsLandOnTheirEdges() {
        assertEquals(
                ScreenEdge.bottom,
                projector.project(projection, new Vec3(0, -40, -10)).edge());
        assertEquals(
                ScreenEdge.right,
                projector.project(projection, new Vec3(50, 0, -10)).edge());
    }

    @Test
    void directionMatchesThePerspectiveProjectionForInFrontTargets() {
        // the camera-basis orthogonal direction equals the direction from the
        // projected position to the screen center for aspect-consistent
        // perspective projections
        double centerX = projection.screenWidth() * 0.5;
        double centerY = projection.screenHeight() * 0.5;
        for (double x = -60; x <= 60; x += 17.3) {
            for (double y = -40; y <= 40; y += 11.7) {
                Vec3 target = new Vec3(x, y, -10);
                if (Math.abs(x) < 12 && Math.abs(y) < 8) continue; // on-screen
                OffscreenProjector.Result result = projector.project(projection, target);
                FloatPos screen = result.screenPos();
                assertNotNull(screen);
                double dx = screen.x() - centerX;
                double dy = screen.y() - centerY;
                double len = Math.sqrt(dx * dx + dy * dy);
                assertEquals(dx / len, result.dirX(), 1.0e-3);
                assertEquals(dy / len, result.dirY(), 1.0e-3);
            }
        }
    }

    @Test
    void behindTargetHasNoScreenPositionButAValidEdge() {
        OffscreenProjector.Result result = projector.project(projection, new Vec3(-30, 0, 10));

        assertTrue(result.behind());
        assertFalse(result.onScreen());
        assertNull(result.screenPos());
        assertNotNull(result.edgePoint());
        // behind-left target points left, same side it would enter from
        assertTrue(result.dirX() < -0.5);
        assertEquals(ScreenEdge.left, result.edge());
    }

    @Test
    void targetCirclingThroughTheCameraPlaneWindsOnce() {
        // circle in a tilted plane through the camera eye, eye at its center:
        // the screen direction must wind exactly one full, jump-free
        // revolution — including through the camera's back half
        Projection circling = ProjectionSimulator.at(0, 0, 0)
                .lookAt(0.7, 0.5, -0.5)
                .screen(480, 270)
                .build();
        Vec3 forward = circling.cameraForward();
        Vec3 circleU = new Vec3(1, 0, 0);
        Vec3 circleV = new Vec3(0, -1, -1).normalize();
        int steps = 720;
        double firstAngle = 0;
        double previousAngle = 0;
        double unwound = 0;
        for (int i = 0; i < steps; i++) {
            double theta = (i + 0.5) * 2.0 * Math.PI / steps;
            Vec3 target = circleU.scale(10 * Math.cos(theta)).add(circleV.scale(10 * Math.sin(theta)));

            OffscreenProjector.Result result = projector.project(circling, target);

            assertTrue(
                    Double.isFinite(result.dirX()) && Double.isFinite(result.dirY()),
                    "direction must stay finite at theta=" + theta);
            assertEquals(1, Math.hypot(result.dirX(), result.dirY()), 1.0e-6);
            assertEquals(target.dot(forward) <= 0, result.behind(), "behind flag at theta=" + theta);
            if (!result.onScreen()) {
                FloatPos edge = result.edgePoint();
                assertNotNull(edge);
                assertTrue(edge.x() >= 8 - eps && edge.x() <= 472 + eps, "edge x in bounds at theta=" + theta);
                assertTrue(edge.y() >= 8 - eps && edge.y() <= 262 + eps, "edge y in bounds at theta=" + theta);
                double reach = Math.max(Math.abs(edge.x() - 240) / 232, Math.abs(edge.y() - 135) / 127);
                assertEquals(1, reach, 1.0e-5, "edge point must touch the inset rectangle");
            }

            double angle = result.angle();
            if (i == 0) {
                firstAngle = angle;
                previousAngle = angle;
                continue;
            }
            double delta = wrapAngle(angle - previousAngle);
            assertTrue(Math.abs(delta) < 0.05, "direction jumped by " + delta + " at step " + i);
            unwound += delta;
            previousAngle = angle;
        }
        // close the loop back to the first sample: one full, monotone
        // revolution covers exactly ±2π
        unwound += wrapAngle(firstAngle - previousAngle);
        assertEquals(2.0 * Math.PI, Math.abs(unwound), 1.0e-6);
    }

    @Test
    void targetCirclingBelowTheCameraStaysContinuousThroughBehind() {
        // camera hovering above the circle's plane: the target never hits the
        // view axis, and sweeping through the camera's back half must not
        // produce jumps, flips or out-of-bounds edge points
        Projection circling = ProjectionSimulator.at(0, 5, 0)
                .lookAt(0, 5, -1)
                .screen(480, 270)
                .build();
        Vec3 eye = circling.cameraPos();
        Vec3 forward = circling.cameraForward();
        int steps = 720;
        double previousAngle = 0;
        for (int i = 0; i < steps; i++) {
            double theta = (i + 0.5) * 2.0 * Math.PI / steps;
            Vec3 target = new Vec3(10 * Math.cos(theta), 0, 10 * Math.sin(theta));

            OffscreenProjector.Result result = projector.project(circling, target);

            assertEquals(1, Math.hypot(result.dirX(), result.dirY()), 1.0e-6);
            assertEquals(target.subtract(eye).dot(forward) <= 0, result.behind());
            double angle = result.angle();
            if (i > 0) {
                double delta = wrapAngle(angle - previousAngle);
                assertTrue(Math.abs(delta) < 0.05, "direction jumped by " + delta + " at step " + i);
            }
            previousAngle = angle;
        }
    }

    @Test
    void exactlyBehindTargetEchoesThePreviousDirection() {
        Vec3 straightBack = new Vec3(0, 0, 10);

        OffscreenProjector.Result withLast = projector.project(projection, straightBack, new FloatPos(0.6, 0.8));
        assertEquals(0.6, withLast.dirX(), eps);
        assertEquals(0.8, withLast.dirY(), eps);

        OffscreenProjector.Result withoutLast = projector.project(projection, straightBack);
        assertEquals(0, withoutLast.dirX(), eps);
        assertEquals(1, withoutLast.dirY(), eps);
        assertNotEquals(0, withoutLast.angle());
    }

    @Test
    void degenerateAheadTargetFallsBackLikeTheBehindOne() {
        // exactly on the view axis in front: projection is the screen center,
        // direction is degenerate and falls back the same way
        OffscreenProjector.Result result = projector.project(projection, new Vec3(0, 0, -10), new FloatPos(0, 1));

        assertEquals(0, result.dirX(), eps);
        assertEquals(1, result.dirY(), eps);
    }

    @Test
    void marginShrinksTheLandingRectangle() {
        OffscreenProjector wideMargin = new OffscreenProjector(60);
        OffscreenProjector.Result result = wideMargin.project(projection, new Vec3(50, 0, -10));

        assertEquals(420, result.edgePoint().x(), eps);
    }

    @Test
    void negativeMarginIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OffscreenProjector(-1));
    }

    private static double wrapAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
}
