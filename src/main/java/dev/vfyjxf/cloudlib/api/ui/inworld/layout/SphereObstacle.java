package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A spherical obstacle — cheap distance tests for rounded source geometry. */
public record SphereObstacle(Vec3 center, double radius) implements ObstacleShape {

    @Override
    public boolean intersects(Pose pose, double width, double height, double clearance) {
        Vec3 local = pose.basis().inverse(center.subtract(pose.origin()));
        double clampedX = Math.max(-width * 0.5, Math.min(width * 0.5, local.x));
        double clampedY = Math.max(-height * 0.5, Math.min(height * 0.5, local.y));
        return new Vec3(local.x - clampedX, local.y - clampedY, local.z).length() < radius + clearance;
    }

    @Override
    public boolean segment(Vec3 from, Vec3 to, double padding) {
        Vec3 direction = to.subtract(from);
        double t = Math.max(
                0.0, Math.min(1.0, center.subtract(from).dot(direction) / Math.max(1.0E-12, direction.dot(direction))));
        return center.distanceTo(from.add(direction.scale(t))) < radius + padding;
    }

    @Override
    public AABB bounds() {
        return new AABB(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius);
    }

    @Override
    public List<Vec3> previewVertices() {
        return List.of(center.add(new Vec3(-radius, -radius, -radius)), center.add(new Vec3(radius, radius, radius)));
    }
}
