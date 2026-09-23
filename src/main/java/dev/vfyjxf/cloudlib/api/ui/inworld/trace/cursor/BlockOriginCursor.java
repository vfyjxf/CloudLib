package dev.vfyjxf.cloudlib.api.ui.inworld.trace.cursor;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The block-source origin cursor: {@link #resolve} attaches at the block's top
 * face center — the point a hovering source mark sits over.
 * <p>
 * The static helpers are the pure block-corner geometry behind block-bound
 * traces: {@link #cornerPos} enumerates a block box's (inflated) corners, and
 * {@link #nearestCorner}/{@link #anchorCorner} pick the corner nearest a
 * target, restricted to the camera-facing half so a back corner never bleeds
 * through the block's own face. {@link #screenAnchorCorner} is the screen-side
 * counterpart with no camera-facing restriction — a screen leader attaches to
 * the projected outline, not to a world-side pin.
 */
public final class BlockOriginCursor implements SourceCursor {

    @Override
    public Optional<Vec3> resolve(TraceSource source, TraceContext context) {
        BlockPos pos = source.blockPos();
        if (pos == null) return Optional.empty();
        return Optional.of(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5));
    }

    /** Box corner i of {@code pos}, inflated by {@code e}. */
    public static Vec3 cornerPos(BlockPos pos, int i, double e) {
        return new Vec3(
            pos.getX() + ((i & 1) == 0 ? -e : 1 + e),
            pos.getY() + (((i >> 1) & 1) == 0 ? -e : 1 + e),
            pos.getZ() + (((i >> 2) & 1) == 0 ? -e : 1 + e)
        );
    }

    /**
     * The corner of {@code pos} nearest {@code target}, restricted to the
     * camera-facing half — a back corner would bleed through the block's own
     * face. Null when the block has no camera-facing corner (camera inside).
     */
    public static @Nullable Vec3 nearestCorner(BlockPos pos, Vec3 target, Vec3 camPos) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 toCam = camPos.subtract(center);
        Vec3 best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            Vec3 c = cornerPos(pos, i, 0.003);
            if (c.subtract(center).dot(toCam) <= 0) continue;
            double d = c.distanceToSqr(target);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    /**
     * The projected corner of {@code pos}'s box nearest a screen point —
     * restricted to the camera-facing half so a back corner never bleeds
     * through the block's own face.
     */
    public static @Nullable FloatPos anchorCorner(
        BlockPos pos,
        double inflate,
        Projection proj,
        Vec3 camPos,
        double targetX,
        double targetY
    ) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 toCam = camPos.subtract(center);
        FloatPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            Vec3 corner = cornerPos(pos, i, inflate);
            if (corner.subtract(center).dot(toCam) <= 0) continue;
            FloatPos s = proj.worldToScreen(corner);
            if (s == null) continue;
            double d = (s.x - targetX) * (s.x - targetX) + (s.y - targetY) * (s.y - targetY);
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }

    /** One frame's screen-side corner pick — the hysteresis token carried between frames. */
    public record ScreenCorner(int index, FloatPos screen) {}

    /**
     * The corner-switch dead zone in px: a challenger must beat the incumbent
     * corner's projection by at least this much to take over.
     */
    private static final double screenCornerHysteresisPx = 4.0;

    /**
     * The corner of {@code pos}'s box whose projection lands nearest a screen
     * point — all eight corners are candidates, the projection itself
     * dropping the ones the camera cannot place (behind it). The screen-side
     * counterpart of {@link #anchorCorner}: a screen leader attaches to what
     * the eye sees of the block on screen, and from an oblique camera the
     * corner nearest the panel can sit on the camera-back half — the
     * face-bleed rule that protects world-side pins would exclude it and
     * detach the leader from the block's projected outline.
     * <p>
     * The caller-held {@code incumbent} (last frame's pick) rides a dead-zone
     * hysteresis: a challenger only takes over when its projection is at
     * least {@value #screenCornerHysteresisPx} px nearer the target, so two
     * near-equidistant corners do not flip with camera micro-motion. An
     * incumbent the camera can no longer project is simply dropped.
     *
     * @return this frame's pick, or null when no corner projects at all
     */
    public static @Nullable ScreenCorner screenAnchorCorner(
        BlockPos pos,
        double inflate,
        Projection proj,
        double targetX,
        double targetY,
        @Nullable ScreenCorner incumbent
    ) {
        ScreenCorner best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            FloatPos s = proj.worldToScreen(cornerPos(pos, i, inflate));
            if (s == null) continue;
            double d = Math.hypot(s.x() - targetX, s.y() - targetY);
            if (d < bestDistance) {
                bestDistance = d;
                best = new ScreenCorner(i, s);
            }
        }
        if (best == null) return null;
        if (incumbent != null && incumbent.index() != best.index()) {
            FloatPos incumbentScreen = proj.worldToScreen(cornerPos(pos, incumbent.index(), inflate));
            if (incumbentScreen != null) {
                double incumbentDistance = Math.hypot(incumbentScreen.x() - targetX, incumbentScreen.y() - targetY);
                if (incumbentDistance - bestDistance < screenCornerHysteresisPx) {
                    return new ScreenCorner(incumbent.index(), incumbentScreen);
                }
            }
        }
        return best;
    }
}
