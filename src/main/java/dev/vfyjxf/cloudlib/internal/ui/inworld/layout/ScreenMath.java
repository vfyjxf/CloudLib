package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projector;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Screen-space polygon primitives shared by the layout kernel: convex
 * clipping (Cyrus–Beck), signed area, centroid, projection helpers and
 * the convex hull / polygon offset used for exclusion zones.
 */
public final class ScreenMath {

    private ScreenMath() {}

    /**
     * Cyrus–Beck clip of the segment {@code a → b} against a convex
     * polygon (either winding). Returns true when any part of the segment
     * lies inside; a degenerate {@code a == b} segment tests the point.
     */
    public static boolean intersects(GuiVec a, GuiVec b, List<GuiVec> polygon) {
        return intersects(a, b, polygon, Math.signum(area(polygon)));
    }

    /** {@link #intersects} with the polygon's signed winding supplied by the caller. */
    public static boolean intersects(GuiVec a, GuiVec b, List<GuiVec> polygon, double winding) {
        double tEnter = 0.0;
        double tExit = 1.0;
        GuiVec direction = b.sub(a);
        double clearance = 1.0E-7;

        for (int i = 0; i < polygon.size(); i++) {
            GuiVec vertex = polygon.get(i);
            GuiVec edge = polygon.get((i + 1) % polygon.size()).sub(vertex);
            double startSide = winding * edge.cross(a.sub(vertex));
            double directionSide = winding * edge.cross(direction);
            double slack = clearance * edge.len();
            if (Math.abs(directionSide) < 1.0E-12) {
                if (startSide <= slack) {
                    return false;
                }
            } else {
                double t = (slack - startSide) / directionSide;
                if (directionSide > 0.0) {
                    tEnter = Math.max(tEnter, t);
                } else {
                    tExit = Math.min(tExit, t);
                }
                if (tEnter >= tExit) {
                    return false;
                }
            }
        }
        return tEnter < tExit && tExit > 0.0 && tEnter < 1.0;
    }

    /** Point-in-convex-polygon test via the degenerate segment {@code point → point}. */
    public static boolean inside(GuiVec point, List<GuiVec> polygon) {
        return intersects(point, point, polygon);
    }

    /** Signed polygon area (positive = CCW). */
    public static double area(List<GuiVec> polygon) {
        double sum = 0.0;
        for (int i = 0; i < polygon.size(); i++) {
            sum += polygon.get(i).cross(polygon.get((i + 1) % polygon.size()));
        }
        return sum * 0.5;
    }

    public static GuiVec centroid(List<GuiVec> polygon) {
        GuiVec sum = GuiVec.zero;
        for (GuiVec vertex : polygon) {
            sum = sum.add(vertex);
        }
        return sum.mul(1.0 / polygon.size());
    }

    /**
     * Projects all world points; returns an empty list when any point
     * fails (behind the camera or clipped).
     */
    public static List<GuiVec> projectAll(Projector camera, List<Vec3> points) {
        List<GuiVec> projected = new ArrayList<>(points.size());
        for (Vec3 point : points) {
            Projection projection = camera.project(point);
            if (!projection.valid()) {
                return List.of();
            }
            projected.add(projection.point());
        }
        return projected;
    }

    public static boolean inViewport(GuiVec point, Projector camera, double margin) {
        return Double.isFinite(point.x())
                && Double.isFinite(point.y())
                && point.x() >= margin
                && point.y() >= margin
                && point.x() <= camera.width() - margin
                && point.y() <= camera.height() - margin;
    }

    /** Andrew's monotone-chain convex hull. */
    public static List<GuiVec> hull(List<GuiVec> points) {
        List<GuiVec> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparingDouble(GuiVec::x).thenComparingDouble(GuiVec::y));
        List<GuiVec> hull = new ArrayList<>();
        for (GuiVec point : sorted) {
            while (hull.size() >= 2
                    && hull.get(hull.size() - 1)
                                    .sub(hull.get(hull.size() - 2))
                                    .cross(point.sub(hull.get(hull.size() - 1)))
                            <= 1.0E-9) {
                hull.remove(hull.size() - 1);
            }
            hull.add(point);
        }
        int lowerSize = hull.size();
        for (int i = sorted.size() - 2; i >= 0; i--) {
            GuiVec point = sorted.get(i);
            while (hull.size() > lowerSize
                    && hull.get(hull.size() - 1)
                                    .sub(hull.get(hull.size() - 2))
                                    .cross(point.sub(hull.get(hull.size() - 1)))
                            <= 1.0E-9) {
                hull.remove(hull.size() - 1);
            }
            hull.add(point);
        }
        if (hull.size() > 1) {
            hull.remove(hull.size() - 1);
        }
        return hull;
    }

    /** Offsets a convex polygon outward by {@code distance} (miter-joined). */
    public static List<GuiVec> offset(List<GuiVec> polygon, double distance) {
        List<GuiVec> result = new ArrayList<>(polygon.size());
        double winding = Math.signum(area(polygon));
        for (int i = 0; i < polygon.size(); i++) {
            GuiVec prev = polygon.get((i + polygon.size() - 1) % polygon.size());
            GuiVec vertex = polygon.get(i);
            GuiVec next = polygon.get((i + 1) % polygon.size());
            GuiVec dirPrev = vertex.sub(prev).unit();
            GuiVec dirNext = next.sub(vertex).unit();
            GuiVec normalPrev = new GuiVec(dirPrev.y() * winding, -dirPrev.x() * winding);
            GuiVec normalNext = new GuiVec(dirNext.y() * winding, -dirNext.x() * winding);
            GuiVec miter = normalPrev.add(normalNext);
            double scale = distance / Math.max(0.1, 1.0 + normalPrev.dot(normalNext));
            result.add(vertex.add(miter.mul(scale)));
        }
        return result;
    }
}
