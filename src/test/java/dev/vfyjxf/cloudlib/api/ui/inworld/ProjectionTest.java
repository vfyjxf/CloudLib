package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-math coverage of the world↔screen conversion formulas in {@link Projection}.
 */
class ProjectionTest {

    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;

    /** Camera at origin looking down -Z (identity view), standard perspective. */
    private static Projection identityView() {
        Matrix4f view = new Matrix4f();
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(70), WIDTH / (float) HEIGHT, 0.05f, 1000f);
        return Projection.capture(view, proj, Vec3.ZERO, WIDTH, HEIGHT);
    }

    @Test
    void pointAheadProjectsToScreenCenter() {
        Projection p = identityView();
        FloatPos screen = p.worldToScreen(new Vec3(0, 0, -5));
        assertNotNull(screen);
        assertEquals(WIDTH * 0.5, screen.x, 0.5);
        assertEquals(HEIGHT * 0.5, screen.y, 0.5);
    }

    @Test
    void pointBehindCameraIsRejected() {
        Projection p = identityView();
        assertNull(p.worldToScreen(new Vec3(0, 0, 5)));
    }

    @Test
    void offAxisPointProjectsSymmetrically() {
        Projection p = identityView();
        FloatPos left = p.worldToScreen(new Vec3(-1, 0, -5));
        FloatPos right = p.worldToScreen(new Vec3(1, 0, -5));
        assertNotNull(left);
        assertNotNull(right);
        assertTrue(left.x < WIDTH * 0.5);
        assertTrue(right.x > WIDTH * 0.5);
        assertEquals(WIDTH - left.x, right.x, 0.5);
        assertEquals(left.y, right.y, 0.01);
    }

    @Test
    void screenRayPassesBackThroughProjectedPoint() {
        Projection p = identityView();
        Vec3 world = new Vec3(0.5, -0.25, -4);
        FloatPos screen = p.worldToScreen(world);
        assertNotNull(screen);

        Vec3 dir = p.rayDirection(screen.x, screen.y);
        //the world point must lie on the ray: distance from point to ray ~ 0
        Vec3 toPoint = world.subtract(p.cameraPos());
        Vec3 cross = toPoint.cross(dir);
        assertTrue(cross.length() < 1e-4, "projected point should lie on its own ray");
    }

    @Test
    void centerRayIsTheViewAxis() {
        Projection p = identityView();
        Vec3 dir = p.rayDirection(WIDTH * 0.5, HEIGHT * 0.5);
        assertEquals(0, dir.x, 1e-5);
        assertEquals(0, dir.y, 1e-5);
        assertEquals(-1, dir.z, 1e-5);
    }

    @Test
    void rayPlaneHitInsidePanelRect() {
        //panel of 96×48 px at 48 px/block → 2×1 blocks, plane z = -2, normal +z
        Vec3 origin = new Vec3(0, 0, 0);
        Vec3 dir = new Vec3(0, 0, -1);
        Vec3 planeOrigin = new Vec3(-1, -0.5, -2);          //2 wide, 1 tall, centered on origin
        Vec3 u = new Vec3(2.0 / 96, 0, 0);                  //px → world (96 px over 2 blocks)
        Vec3 v = new Vec3(0, 1.0 / 48, 0);                  //48 px over 1 block
        Vec3 n = new Vec3(0, 0, 1);

        FloatPos uv = Projection.rayPlane(origin, dir, planeOrigin, u, v, n, 96, 48);
        assertNotNull(uv);
        assertEquals(48, uv.x, 0.01);   //dead center
        assertEquals(24, uv.y, 0.01);
    }

    @Test
    void rayPlaneMissesOutsideRect() {
        Vec3 origin = new Vec3(0, 0, 0);
        Vec3 dir = new Vec3(1, 0, -1).normalize();          //hits far right of the panel
        Vec3 planeOrigin = new Vec3(-1, -0.5, -2);
        Vec3 u = new Vec3(2.0 / 96, 0, 0);
        Vec3 v = new Vec3(0, 1.0 / 48, 0);
        Vec3 n = new Vec3(0, 0, 1);

        assertNull(Projection.rayPlane(origin, dir, planeOrigin, u, v, n, 96, 48));
    }

    @Test
    void rayPlaneParallelIsNull() {
        assertNull(Projection.rayPlane(
                new Vec3(0, 0, 0), new Vec3(1, 0, 0),
                new Vec3(0, 0, -2), new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1),
                10, 10));
    }

    @Test
    void rayPlaneBehindRayIsNull() {
        //plane faces the ray origin but sits behind it
        assertNull(Projection.rayPlane(
                new Vec3(0, 0, 0), new Vec3(0, 0, 1),
                new Vec3(-1, -1, -2), new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1),
                10, 10));
    }

    @Test
    void rayPlaneUVReturnsOutOfBoundsCoordinates() {
        //same setup as rayPlaneMissesOutsideRect — the unclamped variant must
        //still return the panel-local hit (trace cursors may sit off-panel)
        Vec3 origin = new Vec3(0, 0, 0);
        Vec3 dir = new Vec3(1, 0, -1).normalize();
        Vec3 planeOrigin = new Vec3(-1, -0.5, -2);
        Vec3 u = new Vec3(2.0 / 96, 0, 0);
        Vec3 v = new Vec3(0, 1.0 / 48, 0);
        Vec3 n = new Vec3(0, 0, 1);

        FloatPos uv = Projection.rayPlaneUV(origin, dir, planeOrigin, u, v, n);
        assertNotNull(uv);
        assertTrue(uv.x > 96, "hit lands right of the panel — x beyond width");
        assertEquals(24, uv.y, 0.01);
    }

    @Test
    void rayPlaneUVAgreesWithClampedVariantInside() {
        Vec3 origin = new Vec3(0, 0, 0);
        Vec3 dir = new Vec3(0.1, -0.2, -1).normalize();
        Vec3 planeOrigin = new Vec3(-1, -0.5, -2);
        Vec3 u = new Vec3(2.0 / 96, 0, 0);
        Vec3 v = new Vec3(0, 1.0 / 48, 0);
        Vec3 n = new Vec3(0, 0, 1);

        FloatPos raw = Projection.rayPlaneUV(origin, dir, planeOrigin, u, v, n);
        FloatPos clamped = Projection.rayPlane(origin, dir, planeOrigin, u, v, n, 96, 48);
        assertNotNull(raw);
        assertNotNull(clamped);
        assertEquals(raw.x, clamped.x, 1e-6);
        assertEquals(raw.y, clamped.y, 1e-6);
    }

    @Test
    void rayPlaneUVRejectsParallelAndBackfacing() {
        assertNull(Projection.rayPlaneUV(
                new Vec3(0, 0, 0), new Vec3(1, 0, 0),
                new Vec3(0, 0, -2), new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)));
        assertNull(Projection.rayPlaneUV(
                new Vec3(0, 0, 0), new Vec3(0, 0, 1),
                new Vec3(-1, -1, -2), new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)));
    }

    @Test
    void distanceIsEuclidean() {
        Projection p = identityView();
        assertEquals(5.0, p.distance(new Vec3(0, 0, -5)), 1e-6);
    }
}
