package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A UI surface's placement in world space: the top-left corner {@code origin}
 * plus the {@code u}/{@code v} basis vectors that map one gui pixel to a
 * world-space offset — so the quad's world extent is
 * {@code |u|·widthPx × |v|·heightPx}.
 * <p>
 * The {@code u×v} cross product points <em>away</em> from the viewer-facing
 * side (GUI-style quads are front-facing to the camera/observer); use
 * {@link #normal()} for the geometric normal.
 */
public record QuadBasis(Vec3 origin, Vec3 u, Vec3 v) {

    private static final double degenerateEpsilon = 1.0e-6;
    private static final double interpolationEpsilon = 1.0e-12;
    private static final Vec3 worldUp = new Vec3(0, 1, 0);

    /**
     * A panel hugging a block face. {@code u}/{@code v} (0..1) position the
     * panel center on the face; {@code pixelsPerBlock} sets the world scale.
     */
    public static QuadBasis face(
        BlockPos pos,
        Direction face,
        double u,
        double v,
        double pixelsPerBlock,
        int wPx,
        int hPx
    ) {
        double s = 1.0 / pixelsPerBlock;
        Vec3 n = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 uAxis = faceUAxis(face);
        Vec3 vAxis = faceVAxis(face);

        Vec3 facePoint = Vec3.atCenterOf(pos).add(n.scale(0.5)).add(uAxis.scale(u - 0.5)).add(vAxis.scale(v - 0.5))
                .add(n.scale(0.001 + s));

        Vec3 uu = uAxis.scale(s);
        Vec3 vv = vAxis.scale(s);
        Vec3 origin = facePoint.subtract(uu.scale(wPx * 0.5)).subtract(vv.scale(hPx * 0.5));
        return new QuadBasis(origin, uu, vv);
    }

    /**
     * A free-standing screen centred at {@code center} facing {@code facing}
     * — the wall-mounted display / signage placement: fixed position, fixed
     * orientation, occludes and parallaxes like any world object.
     * {@code facing} is the direction the readable side points at (e.g.
     * {@link Direction#SOUTH} is readable by a viewer south of the screen).
     */
    public static QuadBasis screen(Vec3 center, Direction facing, double pixelsPerBlock, int wPx, int hPx) {
        double s = 1.0 / pixelsPerBlock;
        Vec3 uu = faceUAxis(facing).scale(s);
        Vec3 vv = faceVAxis(facing).scale(s);
        Vec3 origin = center.subtract(uu.scale(wPx * 0.5)).subtract(vv.scale(hPx * 0.5));
        return new QuadBasis(origin, uu, vv);
    }

    /**
     * A camera-facing quad centered on a world position — the hologram
     * placement. Recompute per frame from the live camera.
     */
    public static QuadBasis billboard(Vec3 center, Camera camera, double pixelsPerBlock, int wPx, int hPx) {
        // camera basis: left/up/look vectors — panel u is screen-right, v is screen-down
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 down = new Vec3(camera.getUpVector()).scale(-1);
        return cameraBillboard(center, right, down, pixelsPerBlock, wPx, hPx);
    }

    /**
     * The generalization of {@link #billboard}: a fully camera-facing quad
     * from explicit orientation axes — {@code cameraRight}/{@code cameraDown}
     * are the reader's right/down directions (e.g.
     * {@link Projection#cameraRight()} and the negated
     * {@link Projection#cameraUp()}). Panel u is screen-right, v is
     * screen-down.
     */
    public static QuadBasis cameraBillboard(
        Vec3 center,
        Vec3 cameraRight,
        Vec3 cameraDown,
        double pixelsPerBlock,
        int wPx,
        int hPx
    ) {
        return axes(center, cameraRight, cameraDown, pixelsPerBlock, wPx, hPx);
    }

    /**
     * A fully camera-facing quad oriented by a {@link Projection}'s camera
     * basis — the headless-testable {@link #billboard} equivalent that needs
     * no {@code Camera} instance.
     */
    public static QuadBasis cameraBillboard(
        Vec3 center,
        Projection projection,
        double pixelsPerBlock,
        int wPx,
        int hPx
    ) {
        return cameraBillboard(
            center,
            projection.cameraRight(),
            projection.cameraUp().scale(-1),
            pixelsPerBlock,
            wPx,
            hPx
        );
    }

    /**
     * A quad that only rotates around the world Y axis to face an observer —
     * the health-bar / hologram standee placement. Panel up stays
     * {@code (0, 1, 0)}; the quad's readable side faces {@code observerPos}.
     * <p>
     * Degenerates when the observer sits (almost) exactly above or below
     * {@code center}; pass the previous frame's right axis as
     * {@code fallbackRight} (see {@code QuadOrientation}) to hold orientation
     * instead of flipping, or {@code null} for a deterministic default.
     */
    public static QuadBasis yawBillboard(
        Vec3 center,
        Vec3 observerPos,
        @Nullable Vec3 fallbackRight,
        double pixelsPerBlock,
        int wPx,
        int hPx
    ) {
        Vec3 toPanel = horizontal(center.subtract(observerPos));
        Vec3 right = toPanel.lengthSqr() < degenerateEpsilon
                ? horizontalFallback(fallbackRight)
                : toPanel.cross(worldUp).normalize();
        Vec3 down = new Vec3(0, -1, 0);
        return axes(center, right, down, pixelsPerBlock, wPx, hPx);
    }

    /**
     * {@link #yawBillboard} without a fallback axis; a deterministic default
     * right axis is used in the degenerate overhead case.
     */
    public static QuadBasis yawBillboard(Vec3 center, Vec3 observerPos, double pixelsPerBlock, int wPx, int hPx) {
        return yawBillboard(center, observerPos, null, pixelsPerBlock, wPx, hPx);
    }

    /**
     * A quad lying parallel to the ground plane ({@code normal = world up})
     * and readable by an observer looking along {@code observerFacing}: the
     * text's top edge points away from the observer, its bottom edge toward
     * them — the ground-decal / projected-marking placement (read naturally
     * while walking toward it).
     * <p>
     * Degenerates when the observer looks (almost) straight down or up — the
     * facing's horizontal projection vanishes. Pass the previous frame's right
     * axis as {@code fallbackRight} (see {@code QuadOrientation}) to hold
     * orientation, or {@code null} for a deterministic default.
     */
    public static QuadBasis groundParallel(
        Vec3 center,
        Vec3 observerFacing,
        @Nullable Vec3 fallbackRight,
        double pixelsPerBlock,
        int wPx,
        int hPx
    ) {
        return groundParallelOnPlane(center, observerFacing, worldUp, fallbackRight, pixelsPerBlock, wPx, hPx);
    }

    /**
     * {@link #groundParallel} without a fallback axis; a deterministic
     * default right axis is used in the vertical-pitch degenerate case.
     */
    public static QuadBasis groundParallel(Vec3 center, Vec3 observerFacing, double pixelsPerBlock, int wPx, int hPx) {
        return groundParallel(center, observerFacing, null, pixelsPerBlock, wPx, hPx);
    }

    /**
     * The sloped-ground generalization of {@link #groundParallel}: the quad
     * hugs the plane through {@code center} with unit {@code planeNormal}
     * (world up for flat ground). Right is the in-plane axis perpendicular to
     * the in-plane heading; the readable side faces the plane's
     * {@code +normal} half-space. Degenerates when {@code observerFacing} is
     * (almost) parallel to {@code planeNormal}.
     */
    public static QuadBasis groundParallelOnPlane(
        Vec3 center,
        Vec3 observerFacing,
        Vec3 planeNormal,
        @Nullable Vec3 fallbackRight,
        double pixelsPerBlock,
        int wPx,
        int hPx
    ) {
        Vec3 normal = planeNormal.normalize();
        Vec3 right = observerFacing.cross(normal);
        if (right.lengthSqr() < degenerateEpsilon) {
            right = inPlaneFallback(fallbackRight, normal);
        } else {
            right = right.normalize();
        }
        Vec3 down = right.cross(normal);
        return axes(center, right, down, pixelsPerBlock, wPx, hPx);
    }

    /**
     * {@link #groundParallelOnPlane} without a fallback axis; a deterministic
     * in-plane default right axis is used in the degenerate case.
     */
    public static QuadBasis groundParallelOnPlane(
        Vec3 center,
        Vec3 observerFacing,
        Vec3 planeNormal,
        double pixelsPerBlock,
        int wPx,
        int hPx
    ) {
        return groundParallelOnPlane(center, observerFacing, planeNormal, null, pixelsPerBlock, wPx, hPx);
    }

    /** Projects a direction onto the horizontal plane. */
    private static Vec3 horizontal(Vec3 dir) {
        return new Vec3(dir.x, 0, dir.z);
    }

    /** The fallback right axis for a yaw billboard: the last right, or east. */
    private static Vec3 horizontalFallback(@Nullable Vec3 fallbackRight) {
        if (fallbackRight != null) {
            Vec3 h = horizontal(fallbackRight);
            if (h.lengthSqr() >= degenerateEpsilon) return h.normalize();
        }
        return new Vec3(1, 0, 0);
    }

    /** The fallback right axis in a plane: the last right projected in, or a deterministic in-plane axis. */
    private static Vec3 inPlaneFallback(@Nullable Vec3 fallbackRight, Vec3 normal) {
        if (fallbackRight != null) {
            Vec3 inPlane = fallbackRight.subtract(normal.scale(fallbackRight.dot(normal)));
            if (inPlane.lengthSqr() >= degenerateEpsilon) return inPlane.normalize();
        }
        Vec3 seed = Math.abs(normal.x) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
        return seed.subtract(normal.scale(seed.dot(normal))).normalize();
    }

    /**
     * A free quad from explicit basis vectors — {@code u}/{@code v} are panel
     * +x/+y directions with {@code 1/pixelsPerBlock} length baked in by the
     * caller.
     */
    public static QuadBasis of(Vec3 origin, Vec3 u, Vec3 v) {
        return new QuadBasis(origin, u, v);
    }

    /**
     * A quad centered at {@code center} from orthonormal direction axes —
     * {@code uDir} is panel right, {@code vDir} is panel down (as the reader
     * sees it), each scaled to {@code 1/pixelsPerBlock} world units. This is
     * the general constructor the layout engine emits for arbitrary
     * orientations (tilted, face-mounted, billboard).
     */
    public static QuadBasis axes(Vec3 center, Vec3 uDir, Vec3 vDir, double pixelsPerBlock, int wPx, int hPx) {
        double s = 1.0 / pixelsPerBlock;
        Vec3 uu = uDir.normalize().scale(s);
        Vec3 vv = vDir.normalize().scale(s);
        Vec3 origin = center.subtract(uu.scale(wPx * 0.5)).subtract(vv.scale(hPx * 0.5));
        return new QuadBasis(origin, uu, vv);
    }

    /** The world-space point the quad's center sits at. */
    public Vec3 center(int wPx, int hPx) {
        return origin.add(u.scale(wPx * 0.5)).add(v.scale(hPx * 0.5));
    }

    /** Geometric normal of the quad plane (points away from the viewer side). */
    public Vec3 normal() {
        return u.cross(v).normalize();
    }

    // region interpolation

    /**
     * Interpolates between two bases with constant angular velocity — the
     * orientation frame (u, v, u×v) is blended by quaternion slerp, the axis
     * lengths and the origin linearly. {@code t ≤ 0} returns {@code from},
     * {@code t ≥ 1} returns {@code to} (exact endpoints); interior frames are
     * re-orthonormalized, so near-parallel input axes come out perpendicular.
     * <p>
     * Pair with an externally computed blend factor (e.g. the exponential
     * {@link #damp} below) to smooth orientation-mode transitions.
     */
    public static QuadBasis slerp(QuadBasis from, QuadBasis to, double t) {
        if (t <= 0) return from;
        if (t >= 1) return to;
        Quaternionf rotation = frameRotation(from).slerp(frameRotation(to), (float) t);
        Vec3 u = rotateAxis(rotation, 1, 0, 0).scale(lerp(from.u.length(), to.u.length(), t));
        Vec3 v = rotateAxis(rotation, 0, 1, 0).scale(lerp(from.v.length(), to.v.length(), t));
        return new QuadBasis(lerpVec(from.origin, to.origin, t), u, v);
    }

    /**
     * Interpolates between two bases by normalized linear blending — cheaper
     * than {@link #slerp} and equivalent for small per-frame steps: axes are
     * lerped, normalized and re-orthogonalized; the origin and axis lengths
     * are lerped. Falls back to slerp when an axis blend passes through zero
     * (antipodal axes), which linear interpolation cannot traverse.
     */
    public static QuadBasis nlerp(QuadBasis from, QuadBasis to, double t) {
        if (t <= 0) return from;
        if (t >= 1) return to;
        Vec3 u = lerpVec(from.u, to.u, t);
        Vec3 v = lerpVec(from.v, to.v, t);
        if (u.lengthSqr() < interpolationEpsilon || v.lengthSqr() < interpolationEpsilon) {
            return slerp(from, to, t);
        }
        u = u.normalize();
        v = v.subtract(u.scale(v.dot(u))).normalize();
        double su = lerp(from.u.length(), to.u.length(), t);
        double sv = lerp(from.v.length(), to.v.length(), t);
        return new QuadBasis(lerpVec(from.origin, to.origin, t), u.scale(su), v.scale(sv));
    }

    /**
     * Frame-rate-independent exponential smoothing toward {@code target}:
     * blends with {@code 1 − exp(−λ·dt)} so any sampling of the same total
     * time converges to the same result. {@code lambda} is the damping rate
     * (higher → snappier), {@code dt} the caller-supplied frame delta.
     */
    public static QuadBasis damp(QuadBasis current, QuadBasis target, double lambda, double dt) {
        double blend = 1.0 - Math.exp(-Math.max(0.0, lambda) * Math.max(0.0, dt));
        return slerp(current, target, blend);
    }

    private static Quaternionf frameRotation(QuadBasis basis) {
        // a pure rotation matrix — getNormalizedRotation requires unit axes,
        // the pixel scale lives in the lerped axis lengths instead
        Vec3 u = basis.u.normalize();
        Vec3 v = basis.v.normalize();
        Vec3 n = u.cross(v);
        Matrix3f frame = new Matrix3f().set(
            new Vector3f((float) u.x, (float) u.y, (float) u.z),
            new Vector3f((float) v.x, (float) v.y, (float) v.z),
            new Vector3f((float) n.x, (float) n.y, (float) n.z)
        );
        return frame.getNormalizedRotation(new Quaternionf());
    }

    private static Vec3 rotateAxis(Quaternionf rotation, float x, float y, float z) {
        return new Vec3(rotation.transform(new Vector3f(x, y, z))).normalize();
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static Vec3 lerpVec(Vec3 from, Vec3 to, double t) {
        return new Vec3(lerp(from.x, to.x, t), lerp(from.y, to.y, t), lerp(from.z, to.z, t));
    }

    // endregion

    // region depth-based scale

    /**
     * World units covered by one gui pixel at this quad's center depth —
     * {@link Projection#worldPerPixel} evaluated at {@link #center(int, int)}.
     */
    public double worldPerPixel(Projection projection, int wPx, int hPx) {
        return projection.worldPerPixel(center(wPx, hPx));
    }

    /**
     * The {@code pixelsPerBlock} that keeps a face-hugging quad's on-screen
     * size constant as the viewing depth changes: a quad drawn with
     * {@code pixelsPerBlockAtDepth(ref, dRef, d)} at depth {@code d} covers
     * the same screen area as one drawn with {@code ref} at depth
     * {@code dRef}. Perspective projections only; depths are clamped to a
     * small positive epsilon.
     */
    public static double pixelsPerBlockAtDepth(double pixelsPerBlockAtReference, double referenceDepth, double depth) {
        if (pixelsPerBlockAtReference <= 0) {
            throw new IllegalArgumentException(
                "pixelsPerBlockAtReference must be positive: " + pixelsPerBlockAtReference
            );
        }
        if (referenceDepth <= 0) {
            throw new IllegalArgumentException("referenceDepth must be positive: " + referenceDepth);
        }
        return pixelsPerBlockAtReference * referenceDepth / Math.max(depth, 1.0e-6);
    }

    // endregion

    /**
     * One accepted pick on a quad: the panel-local pixel position plus the
     * ray parameter {@code t} (distance along a unit-length ray).
     */
    public record QuadHit(FloatPos uv, double t) {}

    /**
     * Intersects the ray with this quad's plane and maps the hit point to
     * panel-local pixels — the shared pick geometry behind crosshair pointing
     * and gaze selection.
     * <p>
     * {@code normal} is the <em>explicit</em> plane normal (the pick side the
     * caller granted, e.g. a layouter's resolved facing) — it may differ from
     * {@link #normal()} and defines the plane together with {@link #origin()}.
     * Back-face culling is the caller's rule (see the pointing geometry); this
     * method only rejects parallel rays, hits behind the ray origin, and hits
     * outside the {@code wPx × hPx} bounds.
     *
     * @param origin  ray origin (usually the camera position)
     * @param dir     ray direction
     * @param normal  the plane normal defining the pick plane
     * @param wPx     panel width in pixels
     * @param hPx     panel height in pixels
     * @return the hit (panel-local uv plus ray distance), or null when the
     *         ray is parallel to the plane, hits it behind the origin, or
     *         lands outside the quad
     */
    public @Nullable QuadHit hit(Vec3 origin, Vec3 dir, Vec3 normal, int wPx, int hPx) {
        double denom = dir.dot(normal);
        if (Math.abs(denom) < 1.0e-7) return null;
        double t = this.origin.subtract(origin).dot(normal) / denom;
        if (t <= 0) return null;
        Vec3 displacement = origin.add(dir.scale(t)).subtract(this.origin);
        double uu = displacement.dot(u) / u.lengthSqr();
        double vv = displacement.dot(v) / v.lengthSqr();
        if (uu < 0 || vv < 0 || uu > wPx || vv > hPx) return null;
        return new QuadHit(new FloatPos(uu, vv), t);
    }

    /** Panel +x axis in world space for each face (right as seen from outside). */
    public static Vec3 faceUAxis(Direction face) {
        return switch (face) {
            case NORTH -> new Vec3(-1, 0, 0);
            case SOUTH -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(0, 0, 1);
            case EAST -> new Vec3(0, 0, -1);
            case UP, DOWN -> new Vec3(1, 0, 0);
        };
    }

    /** Panel +y axis in world space for each face (down as seen from outside). */
    public static Vec3 faceVAxis(Direction face) {
        return switch (face) {
            case NORTH, SOUTH, WEST, EAST -> new Vec3(0, -1, 0);
            case UP -> new Vec3(0, 0, 1);
            case DOWN -> new Vec3(0, 0, -1);
        };
    }
}
