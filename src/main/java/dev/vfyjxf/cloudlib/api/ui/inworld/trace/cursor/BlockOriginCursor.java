package dev.vfyjxf.cloudlib.api.ui.inworld.trace.cursor;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The block-source origin cursor: {@link #resolve} attaches at the block's top
 * face center — the point a hovering source mark sits over.
 * <p>
 * The static helpers are the pure block-corner geometry behind block-bound
 * traces: {@link #cornerPos} enumerates a block box's (inflated) corners, and
 * {@link #nearestCorner}/{@link #anchorCorner} pick the corner nearest a
 * target, restricted to the camera-facing half so a back corner never bleeds
 * through the block's own face.
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
                pos.getZ() + (((i >> 2) & 1) == 0 ? -e : 1 + e));
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
            BlockPos pos, double inflate, Projection proj, Vec3 camPos, double targetX, double targetY) {
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
}
