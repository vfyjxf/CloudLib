package dev.vfyjxf.inworldui.internal;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage of {@link TraceMap}: the frozen-basis ray→plane mapping that trace
 * mode depends on, the grazing-angle guard, and content clamping.
 * <p>
 * The key invariant: <b>identical input produces identical output</b> — the
 * map is a pure function of the frozen state, so a still mouse can never move
 * the cursor.
 */
class TraceMapTest {

    /** A 96×96 panel at 96 ppb: 1 block² in the XY plane, origin at world (0,0,0), facing +Z. */
    private static final Vec3 EYE = new Vec3(0.5, 0.5, 5);
    private static final Vec3 O = Vec3.ZERO;
    private static final Vec3 U = new Vec3(1.0 / 96, 0, 0);
    private static final Vec3 V = new Vec3(0, 1.0 / 96, 0);
    private static final Vec3 N = new Vec3(0, 0, 1);

    @Test
    void straightAheadHitsPanelCenter() {
        //ray straight at the panel centre maps to (48, 48) px
        Vec3 center = O.add(U.scale(48)).add(V.scale(48));
        Vec3 dir = center.subtract(EYE).normalize();
        FloatPos uv = TraceMap.worldUv(EYE, dir, O, U, V, N);
        assertNotNull(uv);
        assertEquals(48, uv.x, 0.5);
        assertEquals(48, uv.y, 0.5);
    }

    @Test
    void offAxisMapsLinearly() {
        //ray at the top-left quarter → (24, 24)
        Vec3 q = O.add(U.scale(24)).add(V.scale(24));
        Vec3 dir = q.subtract(EYE).normalize();
        FloatPos uv = TraceMap.worldUv(EYE, dir, O, U, V, N);
        assertNotNull(uv);
        assertEquals(24, uv.x, 0.5);
        assertEquals(24, uv.y, 0.5);
    }

    @Test
    void deterministicForFrozenInputs() {
        Vec3 dir = new Vec3(0.1, 0.05, -1).normalize();
        FloatPos a = TraceMap.worldUv(EYE, dir, O, U, V, N);
        FloatPos b = TraceMap.worldUv(EYE, dir, O, U, V, N);
        assertNotNull(a);
        assertEquals(a.x, b.x, 1e-9);
        assertEquals(a.y, b.y, 1e-9);
    }

    @Test
    void grazingRayRejected() {
        //near-parallel ray: |dir·n| below GRAZE_MIN → keep last cursor
        Vec3 dir = new Vec3(0.995, 0, -0.05).normalize();
        assertNull(TraceMap.worldUv(EYE, dir, O, U, V, N));
    }

    @Test
    void edgeGrazingNearThreshold() {
        //just above the guard still resolves — the guard only kills true grazes
        Vec3 dir = new Vec3(0.5, 0, -TraceMap.GRAZE_MIN * 1.3).normalize();
        assertNotNull(TraceMap.worldUv(EYE, dir, O, U, V, N));
        Vec3 tooFlat = new Vec3(0.999, 0, -TraceMap.GRAZE_MIN * 0.5).normalize();
        assertNull(TraceMap.worldUv(EYE, tooFlat, O, U, V, N));
    }

    @Test
    void behindEyeRejected() {
        //ray pointing away from the panel (panel behind the eye) → t ≤ 0
        Vec3 dir = new Vec3(0, 0, 1);
        assertNull(TraceMap.worldUv(EYE, dir, O, U, V, N));
    }

    @Test
    void offPanelUvIsNotClamped() {
        //ray past the right edge → u > width; the widget decides how to treat it
        Vec3 far = O.add(U.scale(140)).add(V.scale(48));
        Vec3 dir = far.subtract(EYE).normalize();
        FloatPos uv = TraceMap.worldUv(EYE, dir, O, U, V, N);
        assertNotNull(uv);
        assertTrue(uv.x > 96);
    }

    @Test
    void nonUnitBasisScalesToPixels() {
        //48 ppb → same physical hit lands at 2× the pixel coords
        Vec3 u2 = new Vec3(1.0 / 48, 0, 0);
        Vec3 v2 = new Vec3(0, 1.0 / 48, 0);
        Vec3 q = O.add(u2.scale(24)).add(v2.scale(24)); //world (0.5, 0.5, 0)
        Vec3 dir = q.subtract(EYE).normalize();
        FloatPos uv = TraceMap.worldUv(EYE, dir, O, u2, v2, N);
        assertNotNull(uv);
        assertEquals(24, uv.x, 0.5);
    }

    //---- flat panels ----

    @Test
    void flatPanelIsScreenRelative() {
        FloatPos uv = TraceMap.flatUv(120.5, 80.25, 100, 60);
        assertEquals(20.5, uv.x, 1e-6);
        assertEquals(20.25, uv.y, 1e-6);
    }

    //---- content clamp ----

    @Test
    void clampAllowsEdgeSlack() {
        var c = TraceMap.clampContent(new FloatPos(-2, 100), 8, 4, 80, 60);
        //content-local = px - offset; slack lets the cursor sit 4px past edges
        assertEquals(-4, c.x, 1e-6); //-2 - 8 = -10 → clamped to -4
        assertEquals(60 + 4, c.y, 1e-6); //100 - 4 = 96 → clamped to 64
    }

    @Test
    void clampInsideIsIdentity() {
        var c = TraceMap.clampContent(new FloatPos(48, 48), 8, 4, 80, 60);
        assertEquals(40, c.x, 1e-6);
        assertEquals(44, c.y, 1e-6);
    }

}
