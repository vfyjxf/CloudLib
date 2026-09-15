package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The world the solver reasons about: a list of {@link Obstacle}s used
 * for panel collision, leader line-of-sight and occlusion.
 * <ul>
 *   <li>{@link #free} — is a panel quad collision-free;</li>
 *   <li>{@link #lineFree} — is a world polyline collision-free (the first
 *       segment may pass through the source's own obstacles);</li>
 *   <li>{@link #visible} — is a point visible from {@code eye} (opaque
 *       obstacles not in {@code exemptIds} block it).</li>
 * </ul>
 * Repeated queries share a lazily built {@link #index()}: each obstacle's
 * bounds are computed once and index arrays sorted by minX let a query
 * binary-search the prefix that can possibly overlap its AABB.
 */
public record WorldObstacles(List<Obstacle> obstacles) {

    public WorldObstacles {
        obstacles = List.copyOf(obstacles);
    }

    /**
     * Spatial index over this snapshot: bounds computed once per shape,
     * plus per-predicate index arrays sorted by minX — {@link Index#end}
     * bounds a scan to obstacles whose bounds can still overlap the
     * query box, so a point/segment test skips the bulk of the field.
     */
    public static final class Index {

        public final Obstacle[] obstacles;
        public final AABB[] bounds;
        private final double[] minX;
        /** Obstacle indices with {@code solid}, sorted by minX. */
        public final int[] solid;
        /** Obstacle indices with {@code opaque}, sorted by minX. */
        public final int[] opaque;
        /** Obstacle indices with {@code solid || opaque}, sorted by minX. */
        public final int[] solidOrOpaque;

        private Index(
                Obstacle[] obstacles, AABB[] bounds, double[] minX, int[] solid, int[] opaque, int[] solidOrOpaque) {
            this.obstacles = obstacles;
            this.bounds = bounds;
            this.minX = minX;
            this.solid = solid;
            this.opaque = opaque;
            this.solidOrOpaque = solidOrOpaque;
        }

        public double minX(int i) {
            return minX[i];
        }

        /**
         * Iteration bound for a sorted order: the count of leading
         * indices whose minX is still below {@code maxX} — scan
         * {@code order[0..end)} and stop; everything past it starts to
         * the right of the query box.
         */
        public int end(int[] order, double maxX) {
            int lo = 0, hi = order.length;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (minX[order[mid]] <= maxX) lo = mid + 1;
                else hi = mid;
            }
            return lo;
        }
    }

    /**
     * Bounded identity cache: a {@code WorldObstacles} record hashes
     * its whole obstacle list, so an identity-keyed map keeps {@link
     * #index()} O(1); the cap bounds how many stale snapshots are held.
     */
    private static final Map<WorldObstacles, Index> indexCache = new IdentityHashMap<>();

    public Index index() {
        Index index = indexCache.get(this);
        if (index == null) {
            if (indexCache.size() >= 64) indexCache.clear();
            index = buildIndex(this);
            indexCache.put(this, index);
        }
        return index;
    }

    private static Index buildIndex(WorldObstacles world) {
        List<Obstacle> list = world.obstacles();
        int n = list.size();
        Obstacle[] obstacles = list.toArray(new Obstacle[0]);
        AABB[] bounds = new AABB[n];
        double[] minX = new double[n];
        Integer[] idx = new Integer[n];
        int m = 0;
        for (int i = 0; i < n; i++) {
            AABB box = obstacles[i].shape().bounds();
            if (box == null) continue;
            bounds[i] = box;
            minX[i] = box.minX;
            idx[m++] = i;
        }
        Integer[] valid = new Integer[m];
        System.arraycopy(idx, 0, valid, 0, m);
        java.util.Arrays.sort(valid, java.util.Comparator.comparingDouble(i -> minX[i]));
        int[] solid = order(valid, obstacles, true, false);
        int[] opaque = order(valid, obstacles, false, true);
        int[] solidOrOpaque = order(valid, obstacles, true, true);
        return new Index(obstacles, bounds, minX, solid, opaque, solidOrOpaque);
    }

    private static int[] order(Integer[] valid, Obstacle[] obstacles, boolean solid, boolean opaque) {
        int[] out = new int[valid.length];
        int n = 0;
        for (int i : valid) {
            if (obstacles[i].solid() == solid || obstacles[i].opaque() == opaque) out[n++] = i;
        }
        return java.util.Arrays.copyOf(out, n);
    }

    public boolean free(Pose pose, double width, double height, double clearance) {
        // Broad-phase: the panel's world AABB — |right|·w/2 + |up|·h/2
        // per axis around the origin, inflated by the clearance.
        Vec3 right = pose.basis().right().scale(width * .5);
        Vec3 up = pose.basis().up().scale(height * .5);
        double hx = Math.abs(right.x) + Math.abs(up.x) + clearance;
        double hy = Math.abs(right.y) + Math.abs(up.y) + clearance;
        double hz = Math.abs(right.z) + Math.abs(up.z) + clearance;
        Vec3 origin = pose.origin();
        AABB panel = new AABB(origin.x - hx, origin.y - hy, origin.z - hz, origin.x + hx, origin.y + hy, origin.z + hz);
        Index index = index();
        for (int k = 0, end = index.end(index.solid, panel.maxX); k < end; k++) {
            int j = index.solid[k];
            if (panel.intersects(index.bounds[j])
                    && index.obstacles[j].shape().intersects(pose, width, height, clearance)) {
                return false;
            }
        }
        return true;
    }

    public boolean lineFree(List<Vec3> path, Set<String> exemptIds, double padding) {
        double inflate = Math.max(0, padding);
        Index index = index();
        for (int i = 1; i < path.size(); i++) {
            Vec3 a = path.get(i - 1);
            Vec3 b = path.get(i);
            AABB segmentBox = new AABB(
                    Math.min(a.x, b.x),
                    Math.min(a.y, b.y),
                    Math.min(a.z, b.z),
                    Math.max(a.x, b.x),
                    Math.max(a.y, b.y),
                    Math.max(a.z, b.z));
            AABB broad = inflate == 0 ? segmentBox : segmentBox.inflate(inflate);
            for (int k = 0, end = index.end(index.solid, broad.maxX); k < end; k++) {
                int j = index.solid[k];
                Obstacle obstacle = index.obstacles[j];
                if ((i != 1 || !exemptIds.contains(obstacle.id()))
                        && broad.intersects(index.bounds[j])
                        && obstacle.shape().segment(a, b, padding)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean visible(Vec3 eye, Vec3 point, Set<String> exemptIds) {
        AABB segmentBox = new AABB(
                Math.min(eye.x, point.x),
                Math.min(eye.y, point.y),
                Math.min(eye.z, point.z),
                Math.max(eye.x, point.x),
                Math.max(eye.y, point.y),
                Math.max(eye.z, point.z));
        Index index = index();
        for (int k = 0, end = index.end(index.opaque, segmentBox.maxX); k < end; k++) {
            int j = index.opaque[k];
            Obstacle obstacle = index.obstacles[j];
            if (segmentBox.intersects(index.bounds[j])
                    && !exemptIds.contains(obstacle.id())
                    && obstacle.shape().segment(eye, point, -1.0E-5)) {
                return false;
            }
        }
        return true;
    }
}
