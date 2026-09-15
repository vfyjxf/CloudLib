package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.Vec3;

/**
 * A pinhole camera with an orthonormal basis — the default
 * {@link Projector}. {@link #minecraft} builds one from the player's
 * yaw/pitch conventions; {@link #lookAt} points it at a target.
 */
public record ViewCamera(
        Vec3 eye,
        Vec3 right,
        Vec3 up,
        Vec3 forward,
        double width,
        double height,
        double verticalFovDegrees,
        double near)
        implements Projector {

    public ViewCamera {
        if (width <= 0.0 || height <= 0.0 || near <= 0.0 || verticalFovDegrees <= 1.0 || verticalFovDegrees >= 175.0) {
            throw new IllegalArgumentException("camera parameters");
        }
        if (Math.abs(right.length() - 1.0) > 1.0E-6
                || Math.abs(up.length() - 1.0) > 1.0E-6
                || Math.abs(forward.length() - 1.0) > 1.0E-6
                || Math.abs(right.dot(up)) > 1.0E-6
                || Math.abs(right.dot(forward)) > 1.0E-6
                || Math.abs(up.dot(forward)) > 1.0E-6) {
            throw new IllegalArgumentException("orthonormal camera basis required");
        }
    }

    /**
     * A camera at {@code eye} looking along Minecraft's yaw/pitch
     * convention (yaw 0 faces +Z, positive yaw turns toward −X).
     */
    public static ViewCamera minecraft(
            Vec3 eye, double yawDegrees, double pitchDegrees, double width, double height, double fovDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        return new ViewCamera(eye, right, right.cross(forward), forward, width, height, fovDegrees, 0.05);
    }

    public static ViewCamera lookAt(Vec3 eye, Vec3 target, double width, double height, double fovDegrees) {
        Vec3 forward = Vecs.unit(target.subtract(eye));
        double yaw = Math.toDegrees(Math.atan2(-forward.x, forward.z));
        double pitch = Math.toDegrees(Math.asin(-forward.y));
        return minecraft(eye, yaw, pitch, width, height, fovDegrees);
    }

    double focal() {
        return height * 0.5 / Math.tan(Math.toRadians(verticalFovDegrees) * 0.5);
    }

    @Override
    public Projection project(Vec3 point) {
        Vec3 rel = point.subtract(eye);
        double depth = rel.dot(forward);
        if (!Double.isFinite(depth) || depth <= near) {
            return new Projection(GuiVec.zero, depth, false);
        }
        return new Projection(
                new GuiVec(
                        width * 0.5 + rel.dot(right) * focal() / depth, height * 0.5 - rel.dot(up) * focal() / depth),
                depth,
                true);
    }

    @Override
    public Vec3 unproject(GuiVec point, double depth) {
        return eye.add(forward.scale(depth))
                .add(right.scale((point.x() - width * 0.5) * depth / focal()))
                .add(up.scale((height * 0.5 - point.y()) * depth / focal()));
    }
}
