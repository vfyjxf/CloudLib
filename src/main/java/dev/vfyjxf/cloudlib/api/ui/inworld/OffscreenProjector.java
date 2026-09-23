package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Computes where an off-screen or behind-the-camera world target should be
 * pointed at — the geometry behind edge-sliding indicators (waypoint arrows).
 * <p>
 * <b>Direction.</b> The target's screen direction is the orthogonal projection
 * of {@code target − cameraPos} onto the camera basis:
 * {@code (d·right, −d·up)} in gui pixels — no perspective division anywhere.
 * Perspective division is exactly what must not be used here: for points
 * behind the camera {@code clip.w < 0} and the divided coordinates flip
 * through the origin, while the orthogonal projection keeps the direction
 * continuous as the target sweeps around the camera. For aspect-consistent
 * perspective projections the orthogonal direction is also numerically exact
 * for on-screen points, because the frustum maps view-space x and y to pixels
 * with the same scale factor.
 * <p>
 * <b>Edge landing point.</b> By similar triangles the ray from the screen
 * center toward the target leaves the screen rectangle exactly where the
 * direction vector, scaled up, first touches the rectangle inset by the
 * margin: {@code t = 1 / max(|dx| / halfW, |dy| / halfH)} and
 * {@code edge = center + t · (dx, dy)}. The same construction covers behind
 * targets, whose direction is still valid.
 * <p>
 * The consumer-facing ANGLE edge encoder (four-edge sliding indicators with
 * switch hysteresis) is built on top of this in a later milestone; this class
 * only delivers the direction and edge geometry.
 */
public final class OffscreenProjector {

    private static final double degenerateEpsilon = 1.0e-9;
    private static final double minHalfExtent = 1.0;

    /**
     * One projection result. {@code dirX}/{@code dirY} form the unit
     * direction from the screen center toward the target in gui pixels
     * (+x right, +y down); {@code angle} is {@code atan2(dirY, dirX)} in
     * radians — clockwise from screen right in gui coordinates.
     * {@code screenPos} is the target's actual projection when in front of
     * the camera, null behind it. {@code edgePoint}/{@code edge} describe the
     * margin-inset rectangle crossing and are null while the target projects
     * inside that rectangle.
     */
    public record Result(
        boolean onScreen,
        boolean behind,
        double dirX,
        double dirY,
        double angle,
        @Nullable FloatPos screenPos,
        @Nullable FloatPos edgePoint,
        @Nullable ScreenEdge edge
    ) {

        static Result onScreen(double dirX, double dirY, FloatPos screenPos) {
            return new Result(true, false, dirX, dirY, Math.atan2(dirY, dirX), screenPos, null, null);
        }

        static Result offScreen(
            boolean behind,
            double dirX,
            double dirY,
            @Nullable FloatPos screenPos,
            FloatPos edgePoint,
            ScreenEdge edge
        ) {
            return new Result(false, behind, dirX, dirY, Math.atan2(dirY, dirX), screenPos, edgePoint, edge);
        }
    }

    private final double insetMargin;

    /**
     * @param insetMargin pixels kept free between the screen border and where
     *                    indicators land; clamped per call so at least a 2×2
     *                    pixel rectangle remains
     */
    public OffscreenProjector(double insetMargin) {
        if (insetMargin < 0) {
            throw new IllegalArgumentException("insetMargin must be >= 0: " + insetMargin);
        }
        this.insetMargin = insetMargin;
    }

    /**
     * Projects the target; a target exactly along the camera's backward axis
     * (direction undefined) falls back to a deterministic screen-down
     * direction.
     *
     * @see #project(Projection, Vec3, FloatPos)
     */
    public Result project(Projection projection, Vec3 target) {
        return project(projection, target, null);
    }

    /**
     * Projects the target. {@code lastDirection} is the previous frame's
     * {@link Result#dirX()}/{@link Result#dirY()} direction; it is echoed
     * unchanged when the target sits exactly on the camera axis (directly
     * ahead or behind), where any fresh direction would be noise — passing
     * the previous one keeps a circling target's indicator continuous.
     */
    public Result project(Projection projection, Vec3 target, @Nullable FloatPos lastDirection) {
        double centerX = projection.screenWidth() * 0.5;
        double centerY = projection.screenHeight() * 0.5;
        double halfW = Math.max(projection.screenWidth() * 0.5 - insetMargin, minHalfExtent);
        double halfH = Math.max(projection.screenHeight() * 0.5 - insetMargin, minHalfExtent);

        Vec3 toTarget = target.subtract(projection.cameraPos());
        double right = toTarget.dot(projection.cameraRight());
        double up = toTarget.dot(projection.cameraUp());
        double forward = toTarget.dot(projection.cameraForward());

        // gui-pixel direction toward the target: orthogonal projection on the
        // camera basis, screen y grows downward — never a perspective divide.
        double dx = right;
        double dy = -up;
        double lenSq = dx * dx + dy * dy;
        if (lenSq < degenerateEpsilon) {
            dx = lastDirection != null ? lastDirection.x() : 0;
            dy = lastDirection != null ? lastDirection.y() : 1;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < degenerateEpsilon) {
                dx = 0;
                dy = 1;
            } else {
                dx /= len;
                dy /= len;
            }
        } else {
            double len = Math.sqrt(lenSq);
            dx /= len;
            dy /= len;
        }

        FloatPos screenPos = forward > 0 ? projection.worldToScreen(target) : null;
        if (screenPos != null
                && Math.abs(screenPos.x() - centerX) <= halfW
                && Math.abs(screenPos.y() - centerY) <= halfH) {
            return Result.onScreen(dx, dy, screenPos);
        }

        // similar triangles: scale the direction until it touches the inset
        // rectangle; the binding axis decides the edge.
        double tx = Math.abs(dx) < degenerateEpsilon ? Double.POSITIVE_INFINITY : halfW / Math.abs(dx);
        double ty = Math.abs(dy) < degenerateEpsilon ? Double.POSITIVE_INFINITY : halfH / Math.abs(dy);
        double t = Math.min(tx, ty);
        FloatPos edgePoint = new FloatPos(centerX + dx * t, centerY + dy * t);
        ScreenEdge edge = tx <= ty
                ? (dx < 0 ? ScreenEdge.left : ScreenEdge.right)
                : (dy < 0 ? ScreenEdge.top : ScreenEdge.bottom);
        return Result.offScreen(forward <= 0, dx, dy, screenPos, edgePoint, edge);
    }
}
