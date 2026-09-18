package dev.vfyjxf.cloudlib.testutil;

import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionSimulatorTest {

    @Test
    void defaultCameraLooksDownMinusZOntoTheScreenCenter() {
        Projection projection = ProjectionSimulator.create().build();

        ProjectionSimulator.assertScreenOf(projection, new Vec3(0, 0, -5), 240, 135, 1.0e-2);
        assertTrue(projection.inFront(new Vec3(0, 0, -5)));
    }

    @Test
    void ninetyDegreeFovMapsLateralOffsetsByPerspectiveRatio() {
        // fov 90, screen 800x400 (aspect 2): tan(45) = 1, so at depth d the
        // half-width is 2d and the half-height is d.
        Projection projection = ProjectionSimulator.create()
                .fov(90)
                .screen(800, 400)
                .lookAt(0, 0, -1)
                .build();

        ProjectionSimulator.assertScreenOf(projection, new Vec3(5, 0, -10), 500, 200, 1.0e-2);
        ProjectionSimulator.assertScreenOf(projection, new Vec3(10, 0, -20), 500, 200, 1.0e-2);
        ProjectionSimulator.assertScreenOf(projection, new Vec3(0, 5, -10), 400, 100, 1.0e-2);
        ProjectionSimulator.assertScreenOf(projection, new Vec3(0, 10, -10), 400, 0, 1.0e-2);
    }

    @Test
    void pointsBehindTheCameraDoNotProject() {
        Projection projection = ProjectionSimulator.create().lookAt(0, 0, -1).build();
        Vec3 behind = new Vec3(0, 0, 5);

        assertNull(projection.worldToScreen(behind));
        assertNull(projection.worldToScreenDepth(behind));
        assertFalse(projection.inFront(behind));
    }

    @Test
    void depthProjectionCarriesScreenPositionAndViewDepth() {
        Projection projection = ProjectionSimulator.create().build();

        Projection.ScreenPoint point = projection.worldToScreenDepth(new Vec3(0, 0, -5));
        assertNotNull(point);
        assertEquals(240, point.x(), 0.01);
        assertEquals(135, point.y(), 0.01);
        assertEquals(5, point.depth(), 1.0e-3);
    }

    @Test
    void crosshairRayPointsAlongTheLookDirection() {
        Projection projection =
                ProjectionSimulator.at(0, 0, 0).lookAt(1, 0, 0).screen(800, 400).build();

        GeometryAsserts.assertVecEquals(new Vec3(1, 0, 0), projection.crosshairDirection(), 1.0e-4);
    }

    @Test
    void distanceIsMeasuredFromTheCameraEye() {
        Projection projection = ProjectionSimulator.at(3, 4, 5).build();

        assertEquals(5.0, projection.distance(new Vec3(3, 4, 0)), 1.0e-9);
    }

    @Test
    void worldScreenRayRoundTripReconstructsOriginalPoints() {
        Projection projection = ProjectionSimulator.at(3, 4, 5)
                .lookAt(10, 20, -7)
                .screen(960, 540)
                .build();

        for (Vec3 world :
                List.of(new Vec3(10, 20, -7), new Vec3(8, 18, -5), new Vec3(15, 25, -12), new Vec3(5, 10, 3))) {
            ProjectionSimulator.assertRoundTrip(projection, world, 1.0e-3);
        }
    }

    @Test
    void roundTripAssertionFailsForPointsBehindTheCamera() {
        Projection projection = ProjectionSimulator.create().lookAt(0, 0, -1).build();

        AssertionError error = assertThrows(
                AssertionError.class, () -> ProjectionSimulator.assertRoundTrip(projection, new Vec3(0, 0, 5), 1.0e-3));
        assertTrue(error.getMessage().contains("behind the camera"));
    }

    @Test
    void screenAssertionFailsOnWrongExpectation() {
        Projection projection = ProjectionSimulator.create()
                .fov(90)
                .screen(800, 400)
                .lookAt(0, 0, -1)
                .build();

        ProjectionSimulator.assertScreenOf(projection, new Vec3(5, 0, -10), 500, 200, 1.0e-2);
        assertThrows(
                AssertionError.class,
                () -> ProjectionSimulator.assertScreenOf(projection, new Vec3(5, 0, -10), 460, 200, 1.0e-2));
        assertThrows(
                AssertionError.class,
                () -> ProjectionSimulator.assertScreenOf(projection, new Vec3(0, 0, 5), 400, 200, 1.0e-2));
    }

    @Test
    void yawPitchFollowTheMinecraftConvention() {
        GeometryAsserts.assertVecEquals(
                new Vec3(0, 0, 1), ProjectionSimulator.create().yawPitch(0, 0).forward(), 1.0e-9);
        GeometryAsserts.assertVecEquals(
                new Vec3(-1, 0, 0), ProjectionSimulator.create().yawPitch(90, 0).forward(), 1.0e-9);
        GeometryAsserts.assertVecEquals(
                new Vec3(0, -1, 0), ProjectionSimulator.create().yawPitch(0, 90).forward(), 1.0e-9);
        GeometryAsserts.assertVecEquals(
                new Vec3(0, Math.sin(Math.toRadians(45)), Math.cos(Math.toRadians(45))),
                ProjectionSimulator.create().yawPitch(0, -45).forward(),
                1.0e-9);
    }

    @Test
    void yawPitchCameraPutsStraightAheadPointsAtTheScreenCenter() {
        Vec3 eye = new Vec3(10, 64, 10);
        Vec3 forward = ProjectionSimulator.create().yawPitch(135, -15).forward();
        Projection projection = ProjectionSimulator.at(10, 64, 10)
                .yawPitch(135, -15)
                .screen(640, 360)
                .build();

        ProjectionSimulator.assertScreenOf(projection, eye.add(forward.scale(8)), 320, 180, 1.0e-2);
        assertNotNull(projection.worldToScreen(eye.add(forward.scale(8))));
    }

    @Test
    void straightDownPitchStaysNonDegenerateWithYawDerivedUp() {
        // pitch 90 at yaw 0: screen-up is +Z, so at depth 10 with the default
        // fov 70 a +2 offset lands above the center by (2 / 10*tan(35)) of the
        // half height.
        Projection projection = ProjectionSimulator.at(0, 10, 0)
                .yawPitch(0, 90)
                .screen(400, 300)
                .build();

        ProjectionSimulator.assertScreenOf(projection, new Vec3(0, 0, 0), 200, 150, 1.0e-2);
        double expectedY = 150 - (2.0 / (10.0 * Math.tan(Math.toRadians(35)))) * 150;
        ProjectionSimulator.assertScreenOf(projection, new Vec3(0, 0, 2), 200, expectedY, 0.05);
    }

    @Test
    void builderRejectsInvalidConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectionSimulator.create().fov(0).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectionSimulator.create().fov(180).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectionSimulator.create().screen(0, 100).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectionSimulator.create().screen(100, -1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectionSimulator.create().depthRange(1, 0.5).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectionSimulator.create().depthRange(0, 10).build());
    }
}
