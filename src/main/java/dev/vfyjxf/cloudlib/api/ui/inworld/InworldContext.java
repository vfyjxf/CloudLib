package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The client context handed to {@link InworldProvider}s on each evaluation pass.
 */
public record InworldContext(
        ClientLevel level,
        LocalPlayer player,
        Camera camera,
        Projection projection,
        long tick
) {

    /**
     * The block the crosshair currently rests on, i.e. the result of the
     * vanilla pick raycast. Useful for "show a panel on the block I look at".
     */
    public @Nullable BlockHitResult crosshairTarget() {
        HitResult hit = player.pick(player.blockInteractionRange(), 0f, false);
        return hit instanceof BlockHitResult blockHit ? blockHit : null;
    }

    /** The block entity under the crosshair, if any. */
    public @Nullable BlockEntity crosshairBlockEntity() {
        BlockHitResult hit = crosshairTarget();
        return hit == null ? null : level.getBlockEntity(hit.getBlockPos());
    }

    /** Camera distance to a world point. */
    public double distanceTo(Vec3 pos) {
        return camera.getPosition().distanceTo(pos);
    }
}
