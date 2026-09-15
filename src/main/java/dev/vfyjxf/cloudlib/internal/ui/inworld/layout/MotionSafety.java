package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;

import java.util.ArrayList;
import java.util.List;

/**
 * Continuous SAT for linearly translated/resized screen rectangles — a
 * panel sliding between two rects may only be declared safe when the
 * whole sweep stays clear.
 */
final class MotionSafety {

    private MotionSafety() {}

    /** Do two axis-aligned rects, each swept from→to, collide at any time? */
    static boolean collide(GuiRect aFrom, GuiRect aTo, GuiRect bFrom, GuiRect bTo, double gap) {
        return collide(aFrom, aTo, bFrom.polygon(), bTo.polygon(), List.of(new GuiVec(1, 0), new GuiVec(0, 1)), gap);
    }

    /** Does a rect swept from→to collide with a convex polygon at any time? */
    static boolean collide(GuiRect from, GuiRect to, List<GuiVec> polygon, double gap) {
        List<GuiVec> axes = new ArrayList<>(List.of(new GuiVec(1, 0), new GuiVec(0, 1)));
        for (int i = 0; i < polygon.size(); i++) {
            GuiVec edge = polygon.get((i + 1) % polygon.size()).sub(polygon.get(i));
            if (edge.len() > 1e-9) axes.add(new GuiVec(-edge.y(), edge.x()).unit());
        }
        return collide(from, to, polygon, polygon, axes, gap);
    }

    private static boolean collide(
            GuiRect aFrom, GuiRect aTo, List<GuiVec> bFrom, List<GuiVec> bTo, List<GuiVec> axes, double gap) {
        double[] time = {0, 1};
        for (GuiVec axis : axes) {
            double[] a0 = project(aFrom.polygon(), axis);
            double[] a1 = project(aTo.polygon(), axis);
            double[] b0 = project(bFrom, axis);
            double[] b1 = project(bTo, axis);
            if (!clip(time, a0[1] + gap - b0[0], a1[1] + gap - b1[0])
                    || !clip(time, b0[1] + gap - a0[0], b1[1] + gap - a1[0])) {
                return false;
            }
        }
        return time[0] <= time[1];
    }

    private static double[] project(List<GuiVec> polygon, GuiVec axis) {
        double min = Double.POSITIVE_INFINITY;
        double max = -min;
        for (GuiVec point : polygon) {
            double value = point.dot(axis);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return new double[] {min, max};
    }

    private static boolean clip(double[] time, double start, double end) {
        // Strictly smaller tolerance than the static SAT keeps the certificate conservative.
        start -= 1e-9;
        end -= 1e-9;
        double speed = end - start;
        if (Math.abs(speed) < 1e-12) return start >= 0;
        double crossing = -start / speed;
        if (speed > 0) time[0] = Math.max(time[0], crossing);
        else time[1] = Math.min(time[1], crossing);
        return time[0] <= time[1];
    }
}
