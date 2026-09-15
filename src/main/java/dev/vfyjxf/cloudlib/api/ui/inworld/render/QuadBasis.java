package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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

    /**
     * A panel hugging a block face. {@code u}/{@code v} (0..1) position the
     * panel center on the face; {@code pixelsPerBlock} sets the world scale.
     */
    public static QuadBasis face(
            BlockPos pos, Direction face, double u, double v, double pixelsPerBlock, int wPx, int hPx) {
        double s = 1.0 / pixelsPerBlock;
        Vec3 n = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 uAxis = faceUAxis(face);
        Vec3 vAxis = faceVAxis(face);

        Vec3 facePoint = Vec3.atCenterOf(pos)
                .add(n.scale(0.5))
                .add(uAxis.scale(u - 0.5))
                .add(vAxis.scale(v - 0.5))
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
        double s = 1.0 / pixelsPerBlock;
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 down = new Vec3(camera.getUpVector()).scale(-1);
        Vec3 uu = right.scale(s);
        Vec3 vv = down.scale(s);
        Vec3 origin = center.subtract(uu.scale(wPx * 0.5)).subtract(vv.scale(hPx * 0.5));
        return new QuadBasis(origin, uu, vv);
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

    /**
     * Camera ray → panel-local pixels, or null when the ray misses the quad
     * (parallel, behind the origin, or outside the w×h bounds).
     */
    public @Nullable dev.vfyjxf.cloudlib.api.math.FloatPos rayUv(Vec3 rayOrigin, Vec3 rayDir, int wPx, int hPx) {
        double denom = rayDir.dot(normal());
        if (Math.abs(denom) < 1.0e-7) return null;
        double t = origin.subtract(rayOrigin).dot(normal()) / denom;
        if (t <= 0) return null;
        Vec3 hit = rayOrigin.add(rayDir.scale(t)).subtract(origin);
        double uu = hit.dot(u) / u.lengthSqr();
        double vv = hit.dot(v) / v.lengthSqr();
        if (uu < 0 || vv < 0 || uu > wPx || vv > hPx) return null;
        return new dev.vfyjxf.cloudlib.api.math.FloatPos(uu, vv);
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
