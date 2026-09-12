package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Pure cursor → panel-surface mapping for a trace session. The defining rule
 * of trace mode is that <b>every input is frozen at {@code traceBegin}</b>:
 * the camera position, the panel's world-space basis (or flat-screen rect) —
 * so a still mouse maps to a still cursor no matter what the live camera or
 * panel does afterwards.
 * <p>
 * Freezing kills the two historical jitter sources: the camera look-assist
 * rotating mid-trace (the same screen pixel then maps to a different world ray
 * every frame — the cursor slid under a still mouse) and the panel's own
 * per-frame basis updates (billboard yaw, expand glide).
 */
final class TraceMap {

    /**
     * {@code |dir · normal|} below this means the view ray is near-parallel to
     * the panel plane — the intersection runs off to infinity and the cursor
     * would teleport. Keep the last cursor instead.
     */
    static final double GRAZE_MIN = 0.12;
    /** Cursor may leave the content rect by this many px before clamping. */
    static final float EDGE_SLACK = 4f;

    private TraceMap() {
    }

    /**
     * Ray → panel-pixel coordinates on a world-space quad.
     *
     * @param eye    frozen camera position
     * @param dir    frozen-frame ray direction through the cursor pixel
     * @param origin panel top-left corner in world space (frozen)
     * @param u      panel +x basis, px → world units (frozen)
     * @param v      panel +y basis, px → world units (frozen)
     * @param normal panel plane normal (frozen)
     * @return panel-local pixels (may be outside the rect), or null when the
     * ray grazes the plane or points away — callers keep the last
     * cursor rather than letting it teleport
     */
    static @Nullable FloatPos worldUv(Vec3 eye, Vec3 dir,
                                      Vec3 origin, Vec3 u, Vec3 v, Vec3 normal) {
        double dn = dir.dot(normal);
        if (Math.abs(dn) < GRAZE_MIN) return null;
        double t = origin.subtract(eye).dot(normal) / dn;
        if (t <= 0) return null; //panel behind the eye
        Vec3 hit = eye.add(dir.scale(t)).subtract(origin);
        return new FloatPos(
                hit.dot(u) / u.lengthSqr(),
                hit.dot(v) / v.lengthSqr());
    }

    /** Cursor → panel pixels for a flat (screen-space) panel, against its frozen rect. */
    static FloatPos flatUv(double sx, double sy, double panelX, double panelY) {
        return new FloatPos(sx - panelX, sy - panelY);
    }

    /** Clamps a panel-space point into the content rect with {@link #EDGE_SLACK} of slack. */
    static FloatPos clampContent(FloatPos px, double offX, double offY, int w, int h) {
        float cx = (float) Math.max(-EDGE_SLACK,
                Math.min(w + EDGE_SLACK, px.x - offX));
        float cy = (float) Math.max(-EDGE_SLACK,
                Math.min(h + EDGE_SLACK, px.y - offY));
        return new FloatPos(cx, cy);
    }

}
