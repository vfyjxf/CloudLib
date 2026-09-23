package dev.vfyjxf.cloudlib.internal.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.inworld.OcclusionProbe;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Objects;

/**
 * The {@link OcclusionProbe} over live level geometry: each segment is clipped
 * through {@link ClipContext} with the visual block shape and no fluids —
 * visual shape (not the collision shape) because a panel half-hidden behind a
 * slab's visible face should fade while one tucked against a full block's
 * side should not, and missing a fluid should never count as occlusion.
 * Kept deliberately thin; it is the only occlusion code that touches
 * Minecraft types and is not headless-testable.
 */
public final class LevelOcclusionProbe implements OcclusionProbe {

    private final BlockGetter level;

    public LevelOcclusionProbe(BlockGetter level) {
        this.level = Objects.requireNonNull(level, "level");
    }

    @Override
    public boolean segmentClear(Vec3 from, Vec3 to) {
        BlockHitResult hit = level.clip(
            new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())
        );
        return hit.getType() == HitResult.Type.MISS;
    }
}
