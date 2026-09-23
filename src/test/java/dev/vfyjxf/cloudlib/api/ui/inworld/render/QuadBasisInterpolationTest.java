package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import dev.vfyjxf.cloudlib.testutil.GeometryAsserts;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuadBasisInterpolationTest {

    private static final double eps = 1.0e-6;

    // two yaw-billboard-like standing panels: same origin region, right axis
    // rotated 90° about world up (east → north)
    private final QuadBasis east = QuadBasis
            .axes(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(0, -1, 0), 100, 80, 40);
    private final QuadBasis north = QuadBasis
            .axes(new Vec3(4, 6, 2), new Vec3(0, 0, -1), new Vec3(0, -1, 0), 100, 80, 40);

    @Test
    void endpointsReturnTheInputsExactly() {
        assertSame(east, QuadBasis.slerp(east, north, 0));
        assertSame(north, QuadBasis.slerp(east, north, 1));
        assertSame(east, QuadBasis.nlerp(east, north, 0));
        assertSame(north, QuadBasis.nlerp(east, north, 1));
        assertSame(east, QuadBasis.slerp(east, north, -0.5));
        assertSame(north, QuadBasis.slerp(east, north, 1.5));
        assertSame(east, QuadBasis.damp(east, north, 4, 0));
    }

    @Test
    void slerpMidpointBisectsTheYawRotation() {
        QuadBasis mid = QuadBasis.slerp(east, north, 0.5);

        double halfAngle = angleBetween(east.u(), north.u()) * 0.5;
        assertEquals(halfAngle, angleBetween(east.u(), mid.u()), 1.0e-4);
        assertEquals(halfAngle, angleBetween(mid.u(), north.u()), 1.0e-4);
        assertEquals(0, mid.u().dot(mid.v()), 1.0e-6);
        assertEquals(1, mid.u().length() / (1.0 / 100), 1.0e-4);
        assertEquals(lerp(east.origin().x, north.origin().x, 0.5), mid.origin().x, 1.0e-9);
        assertEquals(lerp(east.origin().y, north.origin().y, 0.5), mid.origin().y, 1.0e-9);
        assertEquals(lerp(east.origin().z, north.origin().z, 0.5), mid.origin().z, 1.0e-9);
    }

    @Test
    void slerpAngleAdvancesLinearly() {
        double total = angleBetween(east.u(), north.u());
        for (double t = 0.1; t < 0.999; t += 0.1) {
            QuadBasis basis = QuadBasis.slerp(east, north, t);
            assertEquals(total * t, angleBetween(east.u(), basis.u()), 1.0e-4, "at t=" + t);
            assertEquals(total * (1 - t), angleBetween(basis.u(), north.u()), 1.0e-4, "at t=" + t);
        }
    }

    @Test
    void slerpVAxisRotatesWithTheFrame() {
        // v stays screen-down for pure yaw blends
        QuadBasis mid = QuadBasis.slerp(east, north, 0.5);
        GeometryAsserts.assertVecEquals(new Vec3(0, -1, 0), mid.v().normalize(), eps);
    }

    @Test
    void slerpBlendsAxisScales() {
        QuadBasis big = QuadBasis.axes(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(0, -1, 0), 50, 80, 40);
        QuadBasis small = QuadBasis.axes(new Vec3(0, 0, 0), new Vec3(0, 0, -1), new Vec3(0, -1, 0), 200, 80, 40);

        QuadBasis mid = QuadBasis.slerp(big, small, 0.5);

        assertEquals(lerp(1.0 / 50, 1.0 / 200, 0.5), mid.u().length(), 1.0e-6);
        assertEquals(lerp(1.0 / 50, 1.0 / 200, 0.5), mid.v().length(), 1.0e-6);
    }

    @Test
    void antipodalAxesStayFinite() {
        QuadBasis flipped = QuadBasis.of(new Vec3(4, 6, 2), east.u().scale(-1), east.v().scale(-1));

        QuadBasis slerped = QuadBasis.slerp(east, flipped, 0.5);
        QuadBasis nlerped = QuadBasis.nlerp(east, flipped, 0.5);

        for (QuadBasis basis : new QuadBasis[]{slerped, nlerped}) {
            assertTrue(Double.isFinite(basis.u().x + basis.u().y + basis.u().z));
            assertTrue(Double.isFinite(basis.v().x + basis.v().y + basis.v().z));
            assertEquals(1, basis.u().normalize().length(), eps);
            assertEquals(0, Math.abs(basis.u().dot(basis.v())), 1.0e-9);
        }
    }

    @Test
    void nlerpMatchesSlerpForSmallAngles() {
        QuadBasis from = QuadBasis.axes(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(0, -1, 0), 100, 80, 40);
        double radians = Math.toRadians(5);
        Vec3 turned = new Vec3(Math.cos(radians), 0, -Math.sin(radians));
        QuadBasis to = QuadBasis.axes(new Vec3(1, 0, 0), turned, new Vec3(0, -1, 0), 100, 80, 40);

        QuadBasis slerped = QuadBasis.slerp(from, to, 0.5);
        QuadBasis nlerped = QuadBasis.nlerp(from, to, 0.5);

        assertEquals(slerped.u().x, nlerped.u().x, 1.0e-3);
        assertEquals(slerped.u().y, nlerped.u().y, 1.0e-3);
        assertEquals(slerped.u().z, nlerped.u().z, 1.0e-3);
    }

    @Test
    void interpolationIsContinuousInT() {
        QuadBasis previous = QuadBasis.slerp(east, north, 0);
        for (int i = 1; i <= 200; i++) {
            double t = i / 200.0;
            QuadBasis basis = QuadBasis.slerp(east, north, t);
            assertTrue(basis.u().subtract(previous.u()).length() < 0.02, "jump at t=" + t);
            assertTrue(basis.origin().subtract(previous.origin()).length() < 0.05, "origin jump at t=" + t);
            previous = basis;
        }
    }

    @Test
    void dampIsFrameRateIndependent() {
        double lambda = 4;
        QuadBasis fine = east;
        QuadBasis coarse = east;
        for (int i = 0; i < 120; i++) fine = QuadBasis.damp(fine, north, lambda, 1.0 / 120);
        for (int i = 0; i < 24; i++) coarse = QuadBasis.damp(coarse, north, lambda, 5.0 / 120);

        assertEquals(fine.origin().x, coarse.origin().x, 1.0e-9);
        assertEquals(fine.origin().y, coarse.origin().y, 1.0e-9);
        assertEquals(fine.origin().z, coarse.origin().z, 1.0e-9);
        assertEquals(fine.u().x, coarse.u().x, 1.0e-4);
        assertEquals(fine.u().z, coarse.u().z, 1.0e-4);
    }

    @Test
    void dampConvergesTowardTheTarget() {
        QuadBasis basis = east;
        for (int i = 0; i < 600; i++) basis = QuadBasis.damp(basis, north, 6, 1.0 / 20);

        assertEquals(north.u().x, basis.u().x, 1.0e-3);
        assertEquals(north.u().z, basis.u().z, 1.0e-3);
        assertEquals(north.v().y, basis.v().y, 1.0e-3);
        assertEquals(north.origin().x, basis.origin().x, 1.0e-3);
    }

    private static double angleBetween(Vec3 a, Vec3 b) {
        return Math.acos(clamp(a.normalize().dot(b.normalize()), -1, 1));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
