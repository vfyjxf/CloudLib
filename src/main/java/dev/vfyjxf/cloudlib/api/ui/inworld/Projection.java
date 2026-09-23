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
 *     {@code u = (hit − p0)·uAxis}, {@code v = (hit − p0)·vAxis} —
 *     see {@link dev.vfyjxf.cloudlib.api.ui.inworld.render.QuadBasis#hit}.</li>
 *   <li><b>camera basis:</b> the rows of {@code worldToView} are the camera's
 *     right/up/backward axes in world space, so callers that only need
 *     orientation (offscreen direction, orientation quads) can read them via
 *     {@link #cameraRight()}/{@link #cameraUp()}/{@link #cameraForward()}
 *     without touching a {@code Camera}.</li>
 *   <li><b>world units per pixel:</b> at view depth {@code d} the visible
 *     vertical extent is {@code 2·d·tan(fovY/2)} world units over
 *     {@code screenHeight} pixels, so {@code worldPerPixel = 2·d·tan(fovY/2)/H}
 *     with {@code tan(fovY/2) = 1/viewToClip.m11} (perspective
 *     projections).</li>
 * </ul>
 */
public final class Projection {

    private static final double behindEpsilon = 1.0e-6;

    private final Matrix4f worldToView;
    private final Matrix4f viewToClip;
    private final Matrix4f worldToClip;
    private final Matrix4f clipToWorld;
    private final Vec3 cameraPos;
    private final int screenWidth;
    private final int screenHeight;

    private Projection(Matrix4f worldToView, Matrix4f viewToClip, Vec3 cameraPos, int screenWidth, int screenHeight) {
        this.worldToView = new Matrix4f(worldToView);
        this.viewToClip = new Matrix4f(viewToClip);
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
        Matrix4f worldToView,
        Matrix4f viewToClip,
        Vec3 cameraPos,
        int screenW,
        int screenH
    ) {
        return new Projection(worldToView, viewToClip, cameraPos, screenW, screenH);
    }

    // region getters

    public Vec3 cameraPos() {
        return cameraPos;
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    // endregion

    // region matrices and camera basis

    /**
     * A copy of the world→view matrix (the level render pose). Mutating the
     * returned matrix does not affect this projection.
     */
    public Matrix4f worldToView() {
        return new Matrix4f(worldToView);
    }

    /** A copy of the view→clip (projection) matrix. */
    public Matrix4f viewToClip() {
        return new Matrix4f(viewToClip);
    }

    /**
     * A copy of the combined world→clip matrix
     * ({@code viewToClip · worldToView}) — the view-projection transform
     * callers need for custom projections of world geometry.
     */
    public Matrix4f worldToClip() {
        return new Matrix4f(worldToClip);
    }

    /**
     * A copy of the inverse {@link #worldToClip()} matrix — unprojects clip
     * space back to world space.
     */
    public Matrix4f clipToWorld() {
        return new Matrix4f(clipToWorld);
    }

    /**
     * The camera's right axis in world space — {@code +x} of view space, the
     * direction of increasing screen x. Extracted from the rows of
     * {@link #worldToView()}; works for any valid view matrix, including the
     * straight-down/up pitch where a world-up look-at construction would
     * degenerate.
     */
    public Vec3 cameraRight() {
        return normalizeRow(0);
    }

    /** The camera's up axis in world space — {@code +y} of view space. */
    public Vec3 cameraUp() {
        return normalizeRow(1);
    }

    /** The camera's forward (look) axis in world space — {@code −z} of view space. */
    public Vec3 cameraForward() {
        return normalizeRow(2).scale(-1);
    }

    private Vec3 normalizeRow(int row) {
        return new Vec3(worldToView.get(0, row), worldToView.get(1, row), worldToView.get(2, row)).normalize();
    }

    // endregion

    // region depth and pixel scale

    /**
     * The world position's signed view-space depth: positive in front of the
     * camera, negative behind, measured along {@link #cameraForward()}.
     */
    public double viewDepth(Vec3 world) {
        return world.subtract(cameraPos).dot(cameraForward());
    }

    /**
     * World units covered by one gui pixel at the given (signed) view depth —
     * the perspective scale factor that keeps on-screen sizes proportional:
     * {@code 2·|d|·tan(fovY/2) / screenHeight}. Perspective projections only.
     */
    public double worldPerPixelAtDepth(double viewDepth) {
        double tanHalfFovY = 1.0 / viewToClip.m11();
        return 2.0 * Math.abs(viewDepth) * tanHalfFovY / screenHeight;
    }

    /**
     * World units covered by one gui pixel at the depth of {@code world} —
     * see {@link #worldPerPixelAtDepth(double)}.
     */
    public double worldPerPixel(Vec3 world) {
        return worldPerPixelAtDepth(viewDepth(world));
    }

    // endregion

    // region world → screen

    /**
     * Projects a world position to gui-scaled screen coordinates.
     *
     * @return the screen position, or {@code null} when the point is behind
     * the camera (clip.w ≤ 0 — projected coordinates would flip)
     */
    public @Nullable FloatPos worldToScreen(Vec3 world) {
        Vector4f clip = worldToClip.transform(new Vector4f((float) world.x, (float) world.y, (float) world.z, 1.0f));
        if (clip.w <= (float) behindEpsilon) return null;
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        return new FloatPos((ndcX + 1f) * 0.5f * screenWidth, (1f - ndcY) * 0.5f * screenHeight);
    }

    /**
     * Projects a world position to screen coordinates with depth.
     *
     * @return screen x/y plus the point's view-space depth, or null when behind the camera
     */
    public @Nullable ScreenPoint worldToScreenDepth(Vec3 world) {
        Vector4f clip = worldToClip.transform(new Vector4f((float) world.x, (float) world.y, (float) world.z, 1.0f));
        if (clip.w <= (float) behindEpsilon) return null;
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        return new ScreenPoint((ndcX + 1f) * 0.5f * screenWidth, (1f - ndcY) * 0.5f * screenHeight, clip.w);
    }

    /** A world→screen projection with its view-space depth ({@link #worldToScreenDepth}). */
    public record ScreenPoint(float x, float y, float depth) {}

    /** @return true when the world position is in front of the camera */
    public boolean inFront(Vec3 world) {
        Vector4f clip = worldToClip.transform(new Vector4f((float) world.x, (float) world.y, (float) world.z, 1.0f));
        return clip.w > (float) behindEpsilon;
    }

    /** Camera → point distance in blocks. */
    public double distance(Vec3 world) {
        return cameraPos.distanceTo(world);
    }

    // endregion

    // region screen → world

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

    // endregion
}
