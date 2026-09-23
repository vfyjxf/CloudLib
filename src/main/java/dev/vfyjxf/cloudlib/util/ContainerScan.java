package dev.vfyjxf.cloudlib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Soft-aim container targeting: finds item-handling blocks inside a cone
 * around a ray instead of demanding the ray hit them exactly. Used both for
 * the inventory panel's anchor pick (crosshair merely <em>near</em> a chest
 * engages it) and for drag-sweep tolerance (a near-miss across a container
 * still collects it into the trail).
 * <p>
 * A candidate must be within reach, inside the cone, expose an
 * {@code IItemHandler} block capability, and have line of sight — the ray to
 * its centre must not be blocked by a different block first.
 */
public final class ContainerScan {

    private ContainerScan() {}

    /**
     * Nearest-to-ray container inside the cone and reach. Angle decides;
     * distance breaks near-ties. Returns null when nothing qualifies.
     */
    public static @Nullable BlockPos nearest(
        Level level,
        Entity entity,
        Vec3 eye,
        Vec3 dir,
        double reach,
        double coneCos
    ) {
        BlockPos base = BlockPos.containing(eye);
        int R = (int) Math.ceil(reach) + 1;
        double maxDistSq = (reach + 1.5) * (reach + 1.5);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dx = -R; dx <= R; dx++) {
            for (int dy = -R; dy <= R; dy++) {
                for (int dz = -R; dz <= R; dz++) {
                    // cheap sphere cull before any vector or capability work
                    if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;
                    double tx = base.getX() + dx + 0.5 - eye.x;
                    double ty = base.getY() + dy + 0.5 - eye.y;
                    double tz = base.getZ() + dz + 0.5 - eye.z;
                    double dist = Math.sqrt(tx * tx + ty * ty + tz * tz);
                    if (dist < 1.2 || dist > reach + 0.7) continue;
                    double cos = (tx * dir.x + ty * dir.y + tz * dir.z) / dist;
                    if (cos < coneCos) continue;
                    cursor.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    if (level.getCapability(Capabilities.ItemHandler.BLOCK, cursor, null) == null) continue;
                    if (!lineOfSight(level, entity, eye, tx, ty, tz, cursor)) continue;
                    double score = Math.acos(Math.min(cos, 1.0)) + dist * 0.02;
                    if (score < bestScore) {
                        bestScore = score;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    /**
     * Every item-handling block inside the cone and reach — the multi-panel
     * counterpart of {@link #nearest}: each position gets its own offer.
     */
    public static List<BlockPos> all(Level level, Entity entity, Vec3 eye, Vec3 dir, double reach, double coneCos) {
        return all(
            level,
            entity,
            eye,
            dir,
            reach,
            coneCos,
            pos -> level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null
        );
    }

    /**
     * Every position inside the cone and reach matching {@code eligible} —
     * same spatial and line-of-sight rules as {@link #all}, arbitrary
     * eligibility (e.g. "any block section provider contributes").
     */
    public static List<BlockPos> all(
        Level level,
        Entity entity,
        Vec3 eye,
        Vec3 dir,
        double reach,
        double coneCos,
        Predicate<BlockPos> eligible
    ) {
        BlockPos base = BlockPos.containing(eye);
        int R = (int) Math.ceil(reach) + 1;
        double maxDistSq = (reach + 1.5) * (reach + 1.5);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<BlockPos> found = new ArrayList<>();
        for (int dx = -R; dx <= R; dx++) {
            for (int dy = -R; dy <= R; dy++) {
                for (int dz = -R; dz <= R; dz++) {
                    if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;
                    double tx = base.getX() + dx + 0.5 - eye.x;
                    double ty = base.getY() + dy + 0.5 - eye.y;
                    double tz = base.getZ() + dz + 0.5 - eye.z;
                    double dist = Math.sqrt(tx * tx + ty * ty + tz * tz);
                    if (dist < 1.2 || dist > reach + 0.7) continue;
                    double cos = (tx * dir.x + ty * dir.y + tz * dir.z) / dist;
                    if (cos < coneCos) continue;
                    cursor.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    if (!eligible.test(cursor)) continue;
                    if (!lineOfSight(level, entity, eye, tx, ty, tz, cursor)) continue;
                    found.add(cursor.immutable());
                }
            }
        }
        return found;
    }

    /**
     * True while the position stays a visible container inside the (usually
     * wider) cone — the incumbent-hold check that stops cone-edge flicker.
     */
    public static boolean holds(
        Level level,
        Entity entity,
        Vec3 eye,
        Vec3 dir,
        BlockPos pos,
        double reach,
        double coneCos
    ) {
        double tx = pos.getX() + 0.5 - eye.x;
        double ty = pos.getY() + 0.5 - eye.y;
        double tz = pos.getZ() + 0.5 - eye.z;
        double dist = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (dist < 0.8 || dist > reach + 0.7) return false;
        double cos = (tx * dir.x + ty * dir.y + tz * dir.z) / dist;
        if (cos < coneCos) return false;
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null) return false;
        return lineOfSight(level, entity, eye, tx, ty, tz, pos);
    }

    /** The ray to the block centre must not be blocked by a different block first. */
    private static boolean lineOfSight(
        Level level,
        Entity entity,
        Vec3 eye,
        double tx,
        double ty,
        double tz,
        BlockPos pos
    ) {
        BlockHitResult los = level.clip(
            new ClipContext(eye, eye.add(tx, ty, tz), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)
        );
        return los.getType() != HitResult.Type.BLOCK || los.getBlockPos().equals(pos);
    }
}
