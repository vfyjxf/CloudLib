package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LeaderVisibility;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The geometric form of a world {@link Obstacle}.
 * <ul>
 *   <li>{@link #intersects} — swept panel-vs-shape SAT with clearance;</li>
 *   <li>{@link #segment} — segment-vs-shape test with padding (line of sight
 *       and leader world routing);</li>
 *   <li>{@link #previewVertices} — a point cloud projecting to the shape's
 *       screen silhouette;</li>
 *   <li>{@link #occludes} — whether the shape blocks the camera→segment
 *       triangle (thin-wall occlusion between line samples).</li>
 * </ul>
 */
public interface ObstacleShape {

    boolean intersects(Pose pose, double width, double height, double clearance);

    boolean segment(Vec3 from, Vec3 to, double padding);

    List<Vec3> previewVertices();

    /** A conservative bounding volume for broad-phase culls. */
    default AABB bounds() {
        List<Vec3> vertices = previewVertices();
        if (vertices.isEmpty()) return new AABB(0, 0, 0, 0, 0, 0);
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = -minX, maxY = -minX, maxZ = -minX;
        for (Vec3 p : vertices) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    default boolean occludes(Vec3 eye, Vec3 a, Vec3 b) {
        if (!(this instanceof BoxObstacle) && previewVertices().isEmpty()) return true;
        return LeaderVisibility.triangleBox(eye, a, b, bounds());
    }
}
