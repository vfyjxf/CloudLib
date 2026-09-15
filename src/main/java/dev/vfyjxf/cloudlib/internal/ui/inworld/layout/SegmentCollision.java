package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Slab test for a segment against an AABB inflated by {@code clearance}. */
public final class SegmentCollision {

    private SegmentCollision() {}

    public static boolean hits(Vec3 from, Vec3 to, AABB box, double clearance) {
        double enter = 0, exit = 1;
        for (int axis = 0; axis < 3; axis++) {
            double a = value(from, axis);
            double b = value(to, axis);
            double min = value(box, axis, false) - clearance;
            double max = value(box, axis, true) + clearance;
            if (min > max) return false;
            double direction = b - a;
            if (Math.abs(direction) < 1e-12) {
                if (a <= min || a >= max) return false;
            } else {
                double near = (min - a) / direction;
                double far = (max - a) / direction;
                if (near > far) {
                    double swap = near;
                    near = far;
                    far = swap;
                }
                enter = Math.max(enter, near);
                exit = Math.min(exit, far);
                if (enter >= exit - 1e-10) return false;
            }
        }
        return exit > 1e-7 && enter < .9999999;
    }

    private static double value(Vec3 v, int axis) {
        return axis == 0 ? v.x : axis == 1 ? v.y : v.z;
    }

    private static double value(AABB box, int axis, boolean max) {
        return switch (axis) {
            case 0 -> max ? box.maxX : box.minX;
            case 1 -> max ? box.maxY : box.minY;
            default -> max ? box.maxZ : box.minZ;
        };
    }
}
