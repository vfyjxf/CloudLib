package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import dev.vfyjxf.cloudlib.testutil.GeometryAsserts;
import dev.vfyjxf.cloudlib.testutil.ProjectionSimulator;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuadBasisOrientationTest {

    private static final double eps = 1.0e-6;
    private static final int wPx = 80;
    private static final int hPx = 40;
    private static final double ppb = 100;

    private final Vec3 center = new Vec3(10, 64, -7);

    // region groundParallel

    @Test
    void groundParallelFacingNorthIsReadableFromTheSouth() {
        // reader walking north: text right = east, text bottom toward them
        QuadBasis basis = QuadBasis.groundParallel(center, new Vec3(0, 0, -1), ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(new Vec3(1, 0, 0), basis.u().normalize(), eps);
        GeometryAsserts.assertVecEquals(new Vec3(0, 0, 1), basis.v().normalize(), eps);
        GeometryAsserts.assertVecEquals(new Vec3(0, -1, 0), basis.normal(), eps);
        assertBasisGeometry(basis);
    }

    @Test
    void groundParallelFacingSouthMirrorsRight() {
        QuadBasis basis = QuadBasis.groundParallel(center, new Vec3(0, 0, 1), ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(new Vec3(-1, 0, 0), basis.u().normalize(), eps);
        GeometryAsserts.assertVecEquals(new Vec3(0, 0, -1), basis.v().normalize(), eps);
    }

    @Test
    void groundParallelDegeneratePitchUsesFallbackRight() {
        Vec3 straightDown = new Vec3(0, -1, 0);
        Vec3 straightUp = new Vec3(0, 1, 0);

        QuadBasis withFallback = QuadBasis.groundParallel(center, straightDown, new Vec3(0, 0, 1), ppb, wPx, hPx);
        GeometryAsserts.assertVecEquals(new Vec3(0, 0, 1), withFallback.u().normalize(), eps);

        QuadBasis withoutFallback = QuadBasis.groundParallel(center, straightUp, ppb, wPx, hPx);
        GeometryAsserts.assertVecEquals(new Vec3(1, 0, 0), withoutFallback.u().normalize(), eps);
        assertBasisGeometry(withoutFallback);
    }

    @Test
    void groundParallelSlopeHugsThePlane() {
        Vec3 slopeNormal = new Vec3(0, 1, 1).normalize();

        QuadBasis basis = QuadBasis.groundParallelOnPlane(center, new Vec3(0, 0, -1), slopeNormal, ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(new Vec3(1, 0, 0), basis.u().normalize(), eps);
        GeometryAsserts.assertVecEquals(new Vec3(0, -Math.sqrt(0.5), Math.sqrt(0.5)), basis.v().normalize(), eps);
        GeometryAsserts.assertVecEquals(slopeNormal.scale(-1), basis.normal(), eps);
        assertBasisGeometry(basis);
    }

    @Test
    void orientationMemoryHoldsRightThroughVerticalExcursions() {
        // the camera pitch bobs across the vertical: the degenerate frames
        // must hold the last computed right instead of flipping
        QuadOrientation orientation = new QuadOrientation();
        Vec3 lookingDownSteeply = new Vec3(0, Math.sin(Math.toRadians(-80)), -Math.cos(Math.toRadians(-80)));
        Vec3 straightDown = new Vec3(0, -1, 0);
        Vec3 lookingUpSteeply = new Vec3(0, Math.sin(Math.toRadians(80)), -Math.cos(Math.toRadians(80)));

        QuadBasis first = orientation.groundParallel(center, lookingDownSteeply, ppb, wPx, hPx);
        QuadBasis degenerate = orientation.groundParallel(center, straightDown, ppb, wPx, hPx);
        QuadBasis back = orientation.groundParallel(center, lookingUpSteeply, ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(new Vec3(1, 0, 0), first.u().normalize(), 1.0e-3);
        GeometryAsserts.assertVecEquals(new Vec3(1, 0, 0), degenerate.u().normalize(), eps);
        GeometryAsserts.assertVecEquals(new Vec3(1, 0, 0), back.u().normalize(), 1.0e-3);
        GeometryAsserts.assertVecEquals(new Vec3(1, 0, 0), orientation.lastRight(), eps);
    }

    // endregion

    // region yawBillboard

    @Test
    void yawBillboardFacesTheObserverWithWorldUp() {
        QuadBasis basis = QuadBasis.yawBillboard(center, new Vec3(20, 64, -7), ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(new Vec3(0, 0, -1), basis.u().normalize(), eps);
        GeometryAsserts.assertVecEquals(new Vec3(0, -1, 0), basis.v().normalize(), eps);
        // normal points away from the observer (readable side faces them)
        GeometryAsserts.assertVecEquals(new Vec3(-1, 0, 0), basis.normal(), eps);
        assertBasisGeometry(basis);
    }

    @Test
    void yawBillboardIgnoresObserverHeight() {
        QuadBasis level = QuadBasis.yawBillboard(center, new Vec3(20, 64, -7), ppb, wPx, hPx);
        QuadBasis highObserver = QuadBasis.yawBillboard(center, new Vec3(20, 90, -7), ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(level.u(), highObserver.u(), eps);
        GeometryAsserts.assertVecEquals(level.v(), highObserver.v(), eps);
    }

    @Test
    void yawBillboardOverheadDegenerateUsesFallbackRight() {
        QuadBasis withFallback = QuadBasis
                .yawBillboard(center, new Vec3(10, 140, -7), new Vec3(0, 0, 1), ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(new Vec3(0, 0, 1), withFallback.u().normalize(), eps);
        GeometryAsserts.assertVecEquals(new Vec3(0, -1, 0), withFallback.v().normalize(), eps);
        assertBasisGeometry(withFallback);
    }

    // endregion

    // region cameraBillboard

    @Test
    void cameraBillboardFromProjectionMatchesItsBasis() {
        Projection projection = ProjectionSimulator.at(0, 0, 0).lookAt(0, 0, -1).build();

        QuadBasis basis = QuadBasis.cameraBillboard(center, projection, ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(projection.cameraRight().scale(1 / ppb), basis.u(), eps);
        GeometryAsserts.assertVecEquals(projection.cameraUp().scale(-1 / ppb), basis.v(), eps);
        assertBasisGeometry(basis);
    }

    @Test
    void billboardFromACameraMatchesTheProjectionDerivedBasis() {
        // vanilla Camera orientation vs ProjectionSimulator's matrices must
        // produce the same billboard
        float yaw = 117f;
        float pitch = -23f;
        TestCamera camera = new TestCamera();
        camera.orient(yaw, pitch);
        Projection projection = ProjectionSimulator.at(0, 0, 0).yawPitch(yaw, pitch).build();

        QuadBasis fromCamera = QuadBasis.billboard(center, camera, ppb, wPx, hPx);
        QuadBasis fromProjection = QuadBasis.cameraBillboard(center, projection, ppb, wPx, hPx);

        GeometryAsserts.assertVecEquals(fromProjection.origin(), fromCamera.origin(), 1.0e-4);
        GeometryAsserts.assertVecEquals(fromProjection.u(), fromCamera.u(), 1.0e-4);
        GeometryAsserts.assertVecEquals(fromProjection.v(), fromCamera.v(), 1.0e-4);
    }

    /** Exposes the protected rotation setup for headless construction. */
    private static final class TestCamera extends Camera {
        void orient(float yaw, float pitch) {
            setRotation(yaw, pitch, 0f);
        }
    }

    // endregion

    // region depth-based scale

    @Test
    void quadWorldPerPixelEvaluatesAtTheQuadCenterDepth() {
        Projection projection = ProjectionSimulator.at(0, 0, 0).lookAt(0, 0, -1).screen(640, 360).build();
        QuadBasis basis = QuadBasis.screen(new Vec3(0, 0, -12), Direction.SOUTH, ppb, wPx, hPx);

        assertEquals(
            projection.worldPerPixel(basis.center(wPx, hPx)),
            basis.worldPerPixel(projection, wPx, hPx),
            1.0e-12
        );
    }

    @Test
    void pixelsPerBlockScalesLinearlyWithInverseDepth() {
        assertEquals(100 * 4 / 8, QuadBasis.pixelsPerBlockAtDepth(100, 4, 8), 1.0e-9);
        assertEquals(100 * 4 / 2, QuadBasis.pixelsPerBlockAtDepth(100, 4, 2), 1.0e-9);
        assertEquals(100, QuadBasis.pixelsPerBlockAtDepth(100, 4, 4), 1.0e-9);
        // depth at or behind the camera clamps instead of dividing by zero
        assertTrue(Double.isFinite(QuadBasis.pixelsPerBlockAtDepth(100, 4, 0)));
        assertThrows(IllegalArgumentException.class, () -> QuadBasis.pixelsPerBlockAtDepth(0, 4, 4));
        assertThrows(IllegalArgumentException.class, () -> QuadBasis.pixelsPerBlockAtDepth(100, 0, 4));
    }

    // endregion

    /** Shared invariants: unit scale, perpendicular axes, quad centered on the anchor. */
    private void assertBasisGeometry(QuadBasis basis) {
        assertEquals(1 / ppb, basis.u().length(), 1.0e-9);
        assertEquals(1 / ppb, basis.v().length(), 1.0e-9);
        assertEquals(0, basis.u().dot(basis.v()), 1.0e-9);
        GeometryAsserts.assertVecEquals(center, basis.center(wPx, hPx), 1.0e-9);
    }
}
