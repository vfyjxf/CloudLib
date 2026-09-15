package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.Vec3;

/**
 * Billboard orientation — the panel normal aims at the camera eye. With
 * {@code yawOnly} the panel only rotates around the world Y axis (it stays
 * upright instead of pitching at the viewer).
 */
public record FaceCamera(boolean yawOnly) implements OrientationPolicy {

    @Override
    public PanelBasis basis(IntentContext context, Vec3 panelCenter) {
        Vec3 normal = context.frame().camera().eye().subtract(panelCenter);
        Vec3 upHint =
                yawOnly ? new Vec3(0.0, 1.0, 0.0) : context.frame().viewBasis().up();
        if (yawOnly) {
            normal = normal.subtract(upHint.scale(normal.dot(upHint)));
        }

        if (normal.length() < 1.0E-6) {
            return context.previousPose() == null
                    ? context.frame().viewBasis()
                    : context.previousPose().basis();
        }

        normal = Vecs.unit(normal);
        Vec3 right = upHint.cross(normal);
        if (right.length() < 1.0E-6) {
            right = context.frame()
                    .viewBasis()
                    .right()
                    .subtract(normal.scale(context.frame().viewBasis().right().dot(normal)));
        }

        right = Vecs.unit(right);
        return new PanelBasis(right, normal.cross(right), normal);
    }
}
