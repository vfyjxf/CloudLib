package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import dev.vfyjxf.cloudlib.testutil.ProjectionSimulator;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link QuadBasis#hit} — the shared pick geometry: plane intersection with
 * an explicit (caller-granted) normal, the uv mapping into panel pixels, and
 * the ray distance {@code t}. The rays come from a {@link ProjectionSimulator}
 * frustum the way the crosshair pointing solves them.
 */
class QuadBasisHitTest {

    /** A 100×50 panel in the z=-5 plane, facing the origin (camera at 0, looking down -z). */
    private static QuadBasis panel() {
        return QuadBasis.of(new Vec3(-0.5, -0.25, -5), new Vec3(0.01, 0, 0), new Vec3(0, 0.01, 0));
    }

    private static final Vec3 viewerFacingNormal = new Vec3(0, 0, 1);
    private static final Vec3 eye = Vec3.ZERO;

    /** The crosshair ray of the default simulator camera — dead ahead down -z. */
    private static Vec3 centerRay(Projection projection) {
        return projection.rayDirection(projection.screenWidth() * 0.5, projection.screenHeight() * 0.5);
    }

    @Test
    void frontHitReturnsUvAndDistance() {
        Projection projection = ProjectionSimulator.create().build();

        QuadBasis.QuadHit hit = panel().hit(eye, centerRay(projection), viewerFacingNormal, 100, 50);
        assertNotNull(hit);
        assertEquals(50, hit.uv().x, 1.0e-4); // dead center
        assertEquals(25, hit.uv().y, 1.0e-4);
        assertEquals(5, hit.t(), 1.0e-6);
    }

    @Test
    void offCenterRayMapsToPanelPixels() {
        // the ray through world (0.25, -0.125, -5) — panel-local (75, 12.5);
        // screen y grows downward
        Projection projection = ProjectionSimulator.create().build();
        Vec3 world = new Vec3(0.25, -0.125, -5);
        FloatPos screen = projection.worldToScreen(world);
        assertNotNull(screen);
        Vec3 dir = projection.rayDirection(screen.x(), screen.y());

        QuadBasis.QuadHit hit = panel().hit(eye, dir, viewerFacingNormal, 100, 50);
        assertNotNull(hit);
        assertEquals(75, hit.uv().x, 1.0e-3);
        assertEquals(12.5, hit.uv().y, 1.0e-3);
        // t is the distance along the (unit) ray — the camera distance of the hit point
        assertEquals(eye.distanceTo(world), hit.t(), 1.0e-3);
    }

    @Test
    void theHitLandsOnTheExplicitNormalPlane() {
        // the layouter may grant a normal that differs from the quad's own
        // geometric one — the explicit normal defines the pick plane
        Vec3 tilted = new Vec3(0.3, 0, 1).normalize();

        QuadBasis.QuadHit hit = panel().hit(eye, new Vec3(0, 0, -1), tilted, 100, 50);
        assertNotNull(hit);
        // on the plane through the corner with that normal
        Vec3 hitPoint = eye.add(new Vec3(0, 0, -1).scale(hit.t()));
        assertEquals(0, hitPoint.subtract(panel().origin()).dot(tilted), 1.0e-9);
        // and a different plane than the geometric normal's — the tilt moved the hit
        QuadBasis.QuadHit geometric = panel().hit(eye, new Vec3(0, 0, -1), viewerFacingNormal, 100, 50);
        assertNotNull(geometric);
        assertNotEquals(geometric.t(), hit.t(), 1.0e-6);
    }

    @Test
    void rayLeavingThePlaneBehindIsRejected() {
        // origin behind the panel plane, ray traveling away — t <= 0 → null
        Vec3 behind = new Vec3(0, 0, -6);
        assertNull(panel().hit(behind, new Vec3(0, 0, -1), viewerFacingNormal, 100, 50));
        // origin on the viewer side, ray moving away from the plane — same rule
        assertNull(panel().hit(new Vec3(0, 0, -4), new Vec3(0, 0, 1), viewerFacingNormal, 100, 50));
    }

    @Test
    void grazingRaysAreRejected() {
        // a ray in the panel's plane (perpendicular to the normal) never resolves
        assertNull(panel().hit(eye, new Vec3(1, 0, 0), viewerFacingNormal, 100, 50));
    }

    @Test
    void missOutsideTheQuadIsRejected() {
        // through world (0.9, 0, -5): panel-local u = 140 > 100 → outside
        assertNull(panel().hit(eye, new Vec3(0.9, 0, -5).normalize(), viewerFacingNormal, 100, 50));
        // above the top edge: v < 0 → outside
        assertNull(panel().hit(eye, new Vec3(0, 0.5, -5).normalize(), viewerFacingNormal, 100, 50));
    }

    @Test
    void theQuadBoundaryIsInclusive() {
        // through the far corner (0.5, 0.25, -5): u = 100, v = 50 — on the edge
        QuadBasis.QuadHit hit = panel().hit(eye, new Vec3(0.5, 0.25, -5).normalize(), viewerFacingNormal, 100, 50);
        assertNotNull(hit);
        assertEquals(100, hit.uv().x, 1.0e-3);
        assertEquals(50, hit.uv().y, 1.0e-3);
    }
}
