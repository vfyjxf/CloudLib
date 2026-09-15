package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;

import java.util.List;

/**
 * A convex screen polygon with its SAT projections precomputed — the
 * overlap test then costs O(edges) per trial rectangle instead of
 * re-deriving axes and projections each time.
 */
final class ScreenObstacle {

    final double left, top, right, bottom;
    private final double[] axisX, axisY, axisMin, axisMax;

    ScreenObstacle(List<GuiVec> polygon) {
        GuiRect box = LayoutMath.bounds(polygon);
        left = box.x();
        top = box.y();
        right = left + box.width();
        bottom = top + box.height();
        boolean axisAligned = polygon.size() == 4;
        for (int i = 0; i < polygon.size(); i++) {
            GuiVec a = polygon.get(i);
            GuiVec b = polygon.get((i + 1) % polygon.size());
            if (a.x() != b.x() && a.y() != b.y()) axisAligned = false;
        }
        int count = axisAligned ? 0 : polygon.size();
        axisX = new double[count];
        axisY = new double[count];
        axisMin = new double[count];
        axisMax = new double[count];
        for (int i = 0; i < count; i++) {
            GuiVec a = polygon.get(i);
            GuiVec b = polygon.get((i + 1) % count);
            double dx = b.x() - a.x();
            double dy = b.y() - a.y();
            double length = Math.hypot(dx, dy);
            axisX[i] = -dy / length;
            axisY[i] = dx / length;
            axisMin[i] = Double.POSITIVE_INFINITY;
            axisMax[i] = -axisMin[i];
            for (GuiVec point : polygon) {
                double projection = point.x() * axisX[i] + point.y() * axisY[i];
                axisMin[i] = Math.min(axisMin[i], projection);
                axisMax[i] = Math.max(axisMax[i], projection);
            }
        }
    }

    /** Does the axis-aligned rect at {@code (x, y, width, height)} overlap this obstacle, given {@code gap}? */
    boolean overlaps(double x, double y, double width, double height, double gap) {
        if (x + width + gap <= left + 1e-7
                || right + gap <= x + 1e-7
                || y + height + gap <= top + 1e-7
                || bottom + gap <= y + 1e-7) {
            return false;
        }
        double centerX = x + width * .5;
        double centerY = y + height * .5;
        for (int i = 0; i < axisX.length; i++) {
            double center = centerX * axisX[i] + centerY * axisY[i];
            double radius = Math.abs(axisX[i]) * width * .5 + Math.abs(axisY[i]) * height * .5;
            if (center + radius + gap <= axisMin[i] + 1e-7 || axisMax[i] + gap <= center - radius + 1e-7) {
                return false;
            }
        }
        return true;
    }
}
