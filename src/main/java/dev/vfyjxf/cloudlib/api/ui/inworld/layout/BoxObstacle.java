package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.SegmentCollision;
import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** An axis-aligned box obstacle — the common case for blocks and machines. */
public record BoxObstacle(AABB box) implements ObstacleShape {

    public BoxObstacle {
        if (!(box.maxX > box.minX) || !(box.maxY > box.minY) || !(box.maxZ > box.minZ)) {
            throw new IllegalArgumentException("box extent");
        }
    }

    @Override
    public boolean intersects(Pose pose, double width, double height, double clearance) {
        Vec3 boxCenter = box.getCenter();
        Vec3 halfExtent = new Vec3(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ).scale(0.5);
        Vec3 delta = pose.origin().subtract(boxCenter);
        Vec3[] boxAxes = {new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0)};
        Vec3[] panelAxes = {
            pose.basis().right(), pose.basis().up(), pose.basis().normal()
        };
        List<Vec3> axes = new ArrayList<>(15);
        axes.addAll(List.of(boxAxes));
        axes.addAll(List.of(panelAxes));
        for (Vec3 boxAxis : boxAxes) {
            for (Vec3 panelAxis : panelAxes) {
                axes.add(boxAxis.cross(panelAxis));
            }
        }

        for (Vec3 axis : axes) {
            if (!(axis.length() < 1.0E-9)) {
                Vec3 normal = Vecs.unit(axis);
                double panelRadius = Math.abs(normal.dot(panelAxes[0])) * width * 0.5
                        + Math.abs(normal.dot(panelAxes[1])) * height * 0.5
                        + clearance;
                double boxRadius = Math.abs(normal.x) * halfExtent.x
                        + Math.abs(normal.y) * halfExtent.y
                        + Math.abs(normal.z) * halfExtent.z;
                if (Math.abs(delta.dot(normal)) >= panelRadius + boxRadius - 1.0E-8) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean segment(Vec3 from, Vec3 to, double padding) {
        return SegmentCollision.hits(from, to, box, padding);
    }

    @Override
    public AABB bounds() {
        return box;
    }

    @Override
    public List<Vec3> previewVertices() {
        List<Vec3> corners = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            corners.add(new Vec3(
                    (i & 1) == 0 ? box.minX : box.maxX,
                    (i & 2) == 0 ? box.minY : box.maxY,
                    (i & 4) == 0 ? box.minZ : box.maxZ));
        }
        return corners;
    }
}
