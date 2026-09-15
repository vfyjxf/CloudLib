package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Metrics;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projector;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tier;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Layout geometry: world-quad corners and their screen projection,
 * polygon SAT overlap, viewport/readability checks, panel visibility
 * sampling and segment/path crossing tests for leader routing.
 */
public final class LayoutMath {

    private LayoutMath() {}

    /** The four world-space corners of a {@code width} × {@code height} panel quad at {@code pose}. */
    public static List<Vec3> corners(Pose pose, double width, double height) {
        Vec3 right = pose.basis().right().scale(width * 0.5);
        Vec3 up = pose.basis().up().scale(height * 0.5);
        return List.of(
                pose.origin().subtract(right).add(up),
                pose.origin().add(right).add(up),
                pose.origin().add(right).subtract(up),
                pose.origin().subtract(right).subtract(up));
    }

    /** The projected screen polygon of a world panel quad — empty when any corner clips. */
    public static List<GuiVec> projected(Projector camera, Pose pose, double width, double height) {
        return ScreenMath.projectAll(camera, corners(pose, width, height));
    }

    /** The axis-aligned bounds of a screen polygon, clamped to a minimal 1e-6 extent. */
    public static GuiRect bounds(List<GuiVec> polygon) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = minX;
        double maxX = -minX;
        double maxY = -minX;
        for (GuiVec point : polygon) {
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
        }
        return new GuiRect(minX, minY, Math.max(1.0E-6, maxX - minX), Math.max(1.0E-6, maxY - minY));
    }

    /** SAT overlap of two convex polygons with a {@code gap} separation requirement. */
    public static boolean overlap(List<GuiVec> a, List<GuiVec> b, double gap) {
        if (a.size() < 3 || b.size() < 3) {
            return false;
        }
        for (List<GuiVec> polygon : List.of(a, b)) {
            for (int i = 0; i < polygon.size(); i++) {
                GuiVec edge = polygon.get((i + 1) % polygon.size()).sub(polygon.get(i));
                GuiVec axis = new GuiVec(-edge.y(), edge.x()).unit();
                double minA = Double.POSITIVE_INFINITY;
                double maxA = -minA;
                double minB = minA;
                double maxB = -minA;
                for (GuiVec point : a) {
                    double value = point.dot(axis);
                    minA = Math.min(minA, value);
                    maxA = Math.max(maxA, value);
                }
                for (GuiVec point : b) {
                    double value = point.dot(axis);
                    minB = Math.min(minB, value);
                    maxB = Math.max(maxB, value);
                }
                if (maxA + gap <= minB + 1.0E-7 || maxB + gap <= minA + 1.0E-7) {
                    return false;
                }
            }
        }
        return true;
    }

    /** All polygon vertices inside the viewport (with margin). */
    public static boolean inView(List<GuiVec> polygon, Projector camera, double margin) {
        if (polygon.size() < 3) {
            return false;
        }
        for (GuiVec point : polygon) {
            if (!ScreenMath.inViewport(point, camera, margin)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The fraction of a panel quad visible from the camera — a 5×3 sample
     * grid tested against world occlusion.
     */
    public static double visibility(LayoutFrame frame, Pose pose, double width, double height, Set<String> exemptIds) {
        int visible = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                Vec3 point = pose.at(new Vec3((col / 4.0 - 0.5) * width, (row / 2.0 - 0.5) * height, 0.0));
                if (frame.world().visible(frame.camera().eye(), point, exemptIds)) {
                    visible++;
                }
            }
        }
        return visible / 15.0;
    }

    /**
     * Readability gate: a world panel's projected quad must keep a minimum
     * on-screen footprint (plus a fill ratio against its bounding box).
     */
    public static boolean readable(List<GuiVec> polygon, Metrics metrics, Tier tier) {
        if (polygon.size() != 4) {
            return false;
        }
        double width =
                Math.min(polygon.get(0).distance(polygon.get(1)), polygon.get(3).distance(polygon.get(2)));
        double height =
                Math.min(polygon.get(0).distance(polygon.get(3)), polygon.get(1).distance(polygon.get(2)));
        return width >= metrics.minProjectedWidth()
                && (tier == Tier.compact ? height >= 20.0 : height >= metrics.minProjectedHeight())
                && Math.abs(ScreenMath.area(polygon)) >= width * height * 0.12;
    }

    /** Strict interior crossing of two open segments. */
    public static boolean segmentCross(GuiVec a1, GuiVec a2, GuiVec b1, GuiVec b2) {
        GuiVec a = a2.sub(a1);
        GuiVec b = b2.sub(b1);
        double denominator = a.cross(b);
        if (Math.abs(denominator) < 1.0E-9) {
            return false;
        }
        double tA = b1.sub(a1).cross(b) / denominator;
        double tB = b1.sub(a1).cross(a) / denominator;
        return tA > 1.0E-5 && tA < 0.99999 && tB > 1.0E-5 && tB < 0.99999;
    }

    public static boolean pathsCross(List<GuiVec> a, List<GuiVec> b) {
        for (int i = 1; i < a.size(); i++) {
            for (int j = 1; j < b.size(); j++) {
                if (segmentCross(a.get(i - 1), a.get(i), b.get(j - 1), b.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Any segment of {@code path} entering the convex {@code polygon}. */
    public static boolean pathEnters(List<GuiVec> path, List<GuiVec> polygon) {
        if (polygon.isEmpty()) return false;
        double minX = Double.POSITIVE_INFINITY, minY = minX;
        double maxX = -minX, maxY = -minX;
        for (GuiVec v : polygon) {
            minX = Math.min(minX, v.x());
            minY = Math.min(minY, v.y());
            maxX = Math.max(maxX, v.x());
            maxY = Math.max(maxY, v.y());
        }
        double winding = 0.0;
        for (int i = 1; i < path.size(); i++) {
            GuiVec a = path.get(i - 1);
            GuiVec b = path.get(i);
            if (Math.max(a.x(), b.x()) < minX
                    || Math.min(a.x(), b.x()) > maxX
                    || Math.max(a.y(), b.y()) < minY
                    || Math.min(a.y(), b.y()) > maxY) {
                continue;
            }
            if (winding == 0.0) winding = Math.signum(ScreenMath.area(polygon));
            if (ScreenMath.intersects(a, b, polygon, winding)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The view basis implied by a projector — unprojected from three
     * screen samples so it works for both pinhole and matrix cameras.
     */
    public static PanelBasis viewBasis(Projector camera) {
        GuiVec center = new GuiVec(camera.width() * 0.5, camera.height() * 0.5);
        Vec3 eye1 = camera.unproject(center, 1.0);
        Vec3 right = camera.unproject(center.add(new GuiVec(1.0, 0.0)), 1.0).subtract(eye1);
        Vec3 up = camera.unproject(center.add(new GuiVec(0.0, -1.0)), 1.0).subtract(eye1);
        return PanelBasis.of(right, up);
    }
}
