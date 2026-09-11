package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * World ↔ screen coordinate conversion for the in-world UI layer.
 * <p>
 * A {@code Projection} captures the camera transform of one rendered frame:
 * the world→view matrix — note that vanilla's level {@code modelViewMatrix}
 * is camera <em>rotation only</em>; the caller must compose in the
 * {@code −cameraPos} translation themselves ({@code m.translate(-cam)}) — and
 * the view→clip matrix (the game projection matrix). All conversions are
 * expressed in <b>gui-scaled</b>
 * screen pixels — the same coordinate space {@link dev.vfyjxf.cloudlib.api.ui.base.Scene}
 * works in.
 *
 * <h3>Conversion formulas</h3>
 * <ul>
 *   <li><b>world → screen:</b>
 *     {@code clip = viewToClip · worldToView · (world, 1)};
 *     {@code ndc = clip.xyz / clip.w};
 *     {@code screen = ((ndc.x + 1) / 2 · W, (1 − ndc.y) / 2 · H)}.
 *     The point is in front of the camera iff {@code clip.w > 0}.</li>
 *   <li><b>screen → world ray:</b>
 *     {@code ndc = (2·sx/W − 1, 1 − 2·sy/H, 1, 1)};
 *     {@code world = (worldToClip)⁻¹ · ndc} normalized by {@code w};
 *     {@code dir = normalize(world − cameraPos)}.</li>
 *   <li><b>ray → face-plane:</b>
 *     {@code t = (p0 − o)·n / (d·n)};
 *     {@code u = (hit − p0)·uAxis}, {@code v = (hit − p0)·vAxis}.</li>
 * </ul>
 */
public final class Projection {

    private final Matrix4f worldToClip;
    private final Matrix4f clipToWorld;
    private final Vec3 cameraPos;
    private final int screenWidth;
    private final int screenHeight;

    private Projection(
            Matrix4f worldToView, Matrix4f viewToClip,
            Vec3 cameraPos, int screenWidth, int screenHeight
    ) {
        this.worldToClip = new Matrix4f(viewToClip).mul(worldToView);
        this.clipToWorld = new Matrix4f(worldToClip).invert();
        this.cameraPos = cameraPos;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Captures the current frame's projection.
     *
     * @param worldToView the level render pose matrix (world → camera space)
     * @param viewToClip  the projection matrix (camera space → clip space)
     * @param cameraPos   the camera's world position
     * @param screenW     gui-scaled screen width
     * @param screenH     gui-scaled screen height
     */
    public static Projection capture(
            Matrix4f worldToView, Matrix4f viewToClip,
            Vec3 cameraPos, int screenW, int screenH
    ) {
        return new Projection(worldToView, viewToClip, cameraPos, screenW, screenH);
    }

    //region getters

    public Vec3 cameraPos() {
        return cameraPos;
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    //endregion

    //region world → screen

    /**
     * Projects a world position to gui-scaled screen coordinates.
     *
     * @return the screen position, or {@code null} when the point is behind
     * the camera (clip.w ≤ 0 — projected coordinates would flip)
     */
    public @Nullable FloatPos worldToScreen(Vec3 world) {
        Vector4f clip = worldToClip.transform(new Vector4f(
                (float) world.x, (float) world.y, (float) world.z, 1.0f));
        if (clip.w <= 1.0e-6f) return null;
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        return new FloatPos(
                (ndcX + 1f) * 0.5f * screenWidth,
                (1f - ndcY) * 0.5f * screenHeight
        );
    }

    /**
     * Projects a world position to screen coordinates with depth.
     *
     * @return screen x/y plus the point's view-space depth, or null when behind the camera
     */
    public @Nullable float[] worldToScreenDepth(Vec3 world) {
        Vector4f clip = worldToClip.transform(new Vector4f(
                (float) world.x, (float) world.y, (float) world.z, 1.0f));
        if (clip.w <= 1.0e-6f) return null;
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        return new float[]{
                (ndcX + 1f) * 0.5f * screenWidth,
                (1f - ndcY) * 0.5f * screenHeight,
                clip.w
        };
    }

    /** @return true when the world position is in front of the camera */
    public boolean inFront(Vec3 world) {
        Vector4f clip = worldToClip.transform(new Vector4f(
                (float) world.x, (float) world.y, (float) world.z, 1.0f));
        return clip.w > 1.0e-6f;
    }

    /** Camera → point distance in blocks. */
    public double distance(Vec3 world) {
        return cameraPos.distanceTo(world);
    }

    //endregion

    //region screen → world

    /**
     * Builds the normalized world-space ray direction through a gui-scaled
     * screen point. The ray originates at {@link #cameraPos()}.
     */
    public Vec3 rayDirection(double screenX, double screenY) {
        float ndcX = (float) (screenX / screenWidth * 2.0 - 1.0);
        float ndcY = (float) (1.0 - screenY / screenHeight * 2.0);
        Vector4f far = clipToWorld.transform(new Vector4f(ndcX, ndcY, 1f, 1f));
        Vec3 world = new Vec3(far.x / far.w, far.y / far.w, far.z / far.w);
        return world.subtract(cameraPos).normalize();
    }

    /** The ray through the screen center — the crosshair ray. */
    public Vec3 crosshairDirection() {
        return rayDirection(screenWidth * 0.5, screenHeight * 0.5);
    }

    /**
     * Intersects a ray with a panel's face plane and converts the hit point
     * into panel-local pixel coordinates.
     * <p>
     * {@code uAxis}/{@code vAxis} are the panel's px → world basis vectors
     * (unit face direction × 1/pixelsPerBlock — the same axes used to place the
     * panel in the world). Dividing the hit displacement by the squared axis
     * length converts world units back to panel pixels:
     * {@code u = (hit − p0)·u / |u|²}.
     *
     * @param origin   ray origin (usually {@link #cameraPos()})
     * @param dir      ray direction
     * @param originPx the plane origin (panel top-left corner) in world space
     * @param uAxis    panel +x basis (px → world)
     * @param vAxis    panel +y basis (px → world)
     * @param normal   plane normal
     * @param widthPx  panel width in pixels
     * @param heightPx panel height in pixels
     * @return panel-local hit position in pixels, or null when the ray misses
     */
    public static @Nullable FloatPos rayPlane(
            Vec3 origin, Vec3 dir,
            Vec3 originPx, Vec3 uAxis, Vec3 vAxis, Vec3 normal,
            double widthPx, double heightPx
    ) {
        double denom = dir.dot(normal);
        if (Math.abs(denom) < 1.0e-7) return null;
        double t = originPx.subtract(origin).dot(normal) / denom;
        if (t <= 0) return null;
        Vec3 hit = origin.add(dir.scale(t)).subtract(originPx);
        double u = hit.dot(uAxis) / uAxis.lengthSqr();
        double v = hit.dot(vAxis) / vAxis.lengthSqr();
        if (u < 0 || v < 0 || u > widthPx || v > heightPx) return null;
        return new FloatPos(u, v);
    }

    //endregion
}
