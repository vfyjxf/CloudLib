package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Obstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Camera-to-segment triangle tests for leader occlusion — a thin
 * foreground wall between two line samples must still block the route.
 */
public final class LeaderVisibility {

    private LeaderVisibility() {}

    /**
     * Is a leader line unobstructed? The world path is checked against
     * solid segments and the camera→segment triangle against opaque
     * obstacles. Screen-only routes are lifted to world space at the
     * source's depth first.
     */
    public static boolean clear(LayoutFrame frame, PanelPlacement panel, List<GuiVec> screen, List<Vec3> world) {
        if (screen.isEmpty()) return true;
        List<Vec3> path = world;
        if (path.isEmpty()) {
            Projection source = frame.camera().project(panel.source().frame().origin());
            if (!source.valid()) return false;
            path = new ArrayList<>(screen.size());
            for (GuiVec point : screen) {
                path.add(frame.camera().unproject(point, source.depth()));
            }
        }
        Vec3 eye = frame.camera().eye();
        WorldObstacles.Index index = frame.world().index();

        // Path-level prune: an obstacle whose bounds miss the swept
        // region (path ∪ eye) can block neither a segment nor the
        // camera triangle of any segment — skip it for the whole path.
        double minX = Math.min(eye.x, path.get(0).x);
        double minY = Math.min(eye.y, path.get(0).y);
        double minZ = Math.min(eye.z, path.get(0).z);
        double maxX = Math.max(eye.x, path.get(0).x);
        double maxY = Math.max(eye.y, path.get(0).y);
        double maxZ = Math.max(eye.z, path.get(0).z);
        for (Vec3 p : path) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
        AABB swept = new AABB(minX - .003, minY - .003, minZ - .003, maxX + .003, maxY + .003, maxZ + .003);
        int[] order = index.solidOrOpaque;
        int[] survivors = new int[order.length];
        int count = 0;
        for (int k = 0, end = index.end(order, swept.maxX); k < end; k++) {
            int j = order[k];
            if (swept.intersects(index.bounds[j])) {
                survivors[count++] = j;
            }
        }

        for (int i = 1; i < path.size(); i++) {
            Vec3 a = path.get(i - 1);
            Vec3 b = path.get(i);
            AABB segmentBox = new AABB(
                    Math.min(a.x, b.x) - .003,
                    Math.min(a.y, b.y) - .003,
                    Math.min(a.z, b.z) - .003,
                    Math.max(a.x, b.x) + .003,
                    Math.max(a.y, b.y) + .003,
                    Math.max(a.z, b.z) + .003);
            AABB triangleBox = new AABB(
                    Math.min(eye.x, Math.min(a.x, b.x)),
                    Math.min(eye.y, Math.min(a.y, b.y)),
                    Math.min(eye.z, Math.min(a.z, b.z)),
                    Math.max(eye.x, Math.max(a.x, b.x)),
                    Math.max(eye.y, Math.max(a.y, b.y)),
                    Math.max(eye.z, Math.max(a.z, b.z)));
            double limit = Math.max(segmentBox.maxX, triangleBox.maxX);
            for (int k = 0; k < count; k++) {
                int j = survivors[k];
                if (index.minX(j) > limit) break;
                AABB box = index.bounds[j];
                Obstacle obstacle = index.obstacles[j];
                if (!(i == 1 && panel.source().obstacleIds().contains(obstacle.id()))
                        && segmentBox.intersects(box)
                        && obstacle.shape().segment(a, b, .003)) {
                    return false;
                }
                if (obstacle.opaque()
                        && triangleBox.intersects(box)
                        && obstacle.shape().occludes(eye, a, b)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * SAT test: does the triangle {@code eye–a–b} intersect {@code box}?
     * Used to detect occlusion the sparse segment samples would miss.
     */
    public static boolean triangleBox(Vec3 eye, Vec3 a, Vec3 b, AABB box) {
        Vec3 center = box.getCenter();
        Vec3 half = new Vec3(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ)
                .scale(.5)
                .subtract(new Vec3(1e-5, 1e-5, 1e-5));
        half = new Vec3(Math.max(half.x, 0), Math.max(half.y, 0), Math.max(half.z, 0));
        Vec3 v0 = eye.subtract(center), v1 = a.subtract(center), v2 = b.subtract(center);
        Vec3 e0 = v1.subtract(v0), e1 = v2.subtract(v1), e2 = v0.subtract(v2);
        if (separated(1, 0, 0, v0, v1, v2, half)
                || separated(0, 1, 0, v0, v1, v2, half)
                || separated(0, 0, 1, v0, v1, v2, half)) {
            return false;
        }
        Vec3 normal = e0.cross(e1);
        if (normal.dot(normal) < 1e-20) {
            return SegmentCollision.hits(eye, a, box, -1e-5) || SegmentCollision.hits(eye, b, box, -1e-5);
        }
        if (separated(normal.x, normal.y, normal.z, v0, v1, v2, half)) return false;
        for (Vec3 edge : List.of(e0, e1, e2)) {
            if (separated(0, edge.z, -edge.y, v0, v1, v2, half)
                    || separated(-edge.z, 0, edge.x, v0, v1, v2, half)
                    || separated(edge.y, -edge.x, 0, v0, v1, v2, half)) {
                return false;
            }
        }
        return true;
    }

    private static boolean separated(double x, double y, double z, Vec3 a, Vec3 b, Vec3 c, Vec3 half) {
        if (x * x + y * y + z * z < 1e-24) return false;
        double p = a.x * x + a.y * y + a.z * z;
        double q = b.x * x + b.y * y + b.z * z;
        double r = c.x * x + c.y * y + c.z * z;
        double radius = Math.abs(x) * half.x + Math.abs(y) * half.y + Math.abs(z) * half.z;
        return Math.min(p, Math.min(q, r)) > radius || Math.max(p, Math.max(q, r)) < -radius;
    }
}
