package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.testutil.GeometryAsserts;
import dev.vfyjxf.cloudlib.testutil.ProjectionSimulator;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionTest {

    private static final float matrixEpsilon = 1.0e-4f;

    @Test
    void worldToClipCombinesViewAndProjectionMatrices() {
        Projection projection = ProjectionSimulator.at(3, 4, 5).lookAt(10, 20, -7).screen(960, 540).build();

        Matrix4f expected = projection.viewToClip().mul(projection.worldToView());
        assertTrue(projection.worldToClip().equals(expected, matrixEpsilon));
    }

    @Test
    void clipToWorldInvertsWorldToClip() {
        Projection projection = ProjectionSimulator.at(1, 2, 3).lookAt(-5, 8, 4).screen(800, 400).build();

        Matrix4f product = projection.worldToClip().mul(projection.clipToWorld());
        assertTrue(product.equals(new Matrix4f(), matrixEpsilon));
    }

    @Test
    void matrixAccessorsReturnDefensiveCopies() {
        Projection projection = ProjectionSimulator.at(0, 0, 0).lookAt(0, 0, -1).build();
        Vec3 world = new Vec3(1, 2, -8);
        FloatPos before = projection.worldToScreen(world);

        projection.worldToClip().m00(999f);
        projection.clipToWorld().m00(999f);
        projection.worldToView().m00(999f);
        projection.viewToClip().m00(999f);

        GeometryAsserts.assertPosEquals(before, projection.worldToScreen(world), 1.0e-6);
    }

    @Test
    void cameraBasisMatchesTheLookDirection() {
        Vec3 eye = new Vec3(1, 2, 3);
        Vec3 target = new Vec3(8, 6, -4);
        Projection projection = ProjectionSimulator.at(1, 2, 3).lookAt(8, 6, -4).build();

        Vec3 forward = projection.cameraForward();
        Vec3 right = projection.cameraRight();
        Vec3 up = projection.cameraUp();

        GeometryAsserts.assertVecEquals(target.subtract(eye).normalize(), forward, 1.0e-6);
        assertEquals(0, right.y, 1.0e-6);
        assertEquals(1, right.length(), 1.0e-6);
        assertEquals(1, up.length(), 1.0e-6);
        assertEquals(0, right.dot(up), 1.0e-6);
        assertEquals(0, right.dot(forward), 1.0e-6);
        assertEquals(0, up.dot(forward), 1.0e-6);
        // view space is right-handed in (right, up, backward)
        GeometryAsserts.assertVecEquals(forward.scale(-1), right.cross(up), 1.0e-6);
    }

    @Test
    void cameraBasisStaysValidWhenLookingStraightDown() {
        Projection projection = ProjectionSimulator.at(0, 10, 0).yawPitch(0, 90).build();

        Vec3 right = projection.cameraRight();
        Vec3 up = projection.cameraUp();
        Vec3 forward = projection.cameraForward();

        GeometryAsserts.assertVecEquals(new Vec3(0, -1, 0), forward, 1.0e-6);
        GeometryAsserts.assertVecEquals(new Vec3(-1, 0, 0), right, 1.0e-6);
        GeometryAsserts.assertVecEquals(new Vec3(0, 0, 1), up, 1.0e-6);
    }

    @Test
    void viewDepthIsSignedDistanceAlongTheForwardAxis() {
        Projection projection = ProjectionSimulator.at(0, 0, 0).lookAt(0, 0, -1).build();

        assertEquals(10, projection.viewDepth(new Vec3(3, -4, -Math.sqrt(125 - 9 - 16))), 1.0e-6);
        assertEquals(-7, projection.viewDepth(new Vec3(0, 0, 7)), 1.0e-6);
    }

    @Test
    void worldPerPixelAtDepthFollowsTheFovFormula() {
        Projection fov90 = ProjectionSimulator.create().fov(90).screen(800, 400).build();
        // 2·d·tan(45)/400 = 2d/400
        assertEquals(2.0 * 10 / 400, fov90.worldPerPixelAtDepth(10), 1.0e-6);

        Projection fov70 = ProjectionSimulator.create().fov(70).screen(480, 270).build();
        double expected = 2.0 * 25 * Math.tan(Math.toRadians(35)) / 270;
        assertEquals(expected, fov70.worldPerPixelAtDepth(25), 1.0e-8);
        assertEquals(expected, fov70.worldPerPixelAtDepth(-25), 1.0e-8);
    }

    @Test
    void worldPerPixelAtAPointMatchesItsViewDepth() {
        Projection projection = ProjectionSimulator.at(0, 0, 0).lookAt(0, 0, -1).screen(640, 360).build();
        Vec3 world = new Vec3(4, -3, -12);

        assertEquals(
            projection.worldPerPixelAtDepth(projection.viewDepth(world)),
            projection.worldPerPixel(world),
            1.0e-9
        );
    }

    @Test
    void worldScreenRoundTripHoldsAcrossCameras() {
        List<Projection> cameras = List.of(
            ProjectionSimulator.at(3, 4, 5).lookAt(10, 20, -7).screen(960, 540).build(),
            ProjectionSimulator.at(-8, 64, 12).yawPitch(135, -15).screen(640, 360).build(),
            ProjectionSimulator.at(0, 10, 0).yawPitch(0, 90).screen(400, 300).build(),
            ProjectionSimulator.at(0, 30, 0).yawPitch(0, -90).screen(400, 300).build()
        );
        List<Vec3> points = List.of(
            new Vec3(10, 20, -7),
            new Vec3(8, 18, -5),
            new Vec3(15, 25, -12),
            new Vec3(-4, 2, 9),
            new Vec3(0, 0, 0)
        );

        for (Projection projection : cameras) {
            for (Vec3 world : points) {
                if (!projection.inFront(world)) continue;
                ProjectionSimulator.assertRoundTrip(projection, world, 1.0e-3);
                assertNotNull(projection.worldToScreen(world));
            }
        }
    }

    @Test
    void viewAxisPointSitsAtScreenCenterAtItsViewDepth() {
        // a point on the view axis at distance d sits at the screen center with
        // view depth d — forward extracted from worldToView must match the
        // matrix-defined view space
        Projection projection = ProjectionSimulator.at(2, 3, 4).lookAt(2, 3, -6).screen(480, 270).build();
        Vec3 world = new Vec3(2, 3, -6);

        assertEquals(10, projection.viewDepth(world), 1.0e-6);
        ProjectionSimulator.assertScreenOf(projection, world, 240, 135, 1.0e-3);
        assertNull(projection.worldToScreen(new Vec3(2, 3, 16)));
    }
}
