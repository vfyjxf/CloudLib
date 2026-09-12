package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Rect;

import java.util.List;
import java.util.function.Supplier;

/**
 * FloatingMiddleware that nudges the floating element out of a set of
 * obstacle rects (e.g. other panels already committed to the screen).
 * <p>
 * For every obstacle the element intersects, the minimal single-axis escape
 * push is applied, preferring the axis that keeps the element closest to its
 * placement. Pushes are bounded by a per-frame budget — when clearing the
 * obstacle would displace the element too far, the overlap is accepted
 * instead of teleporting the element away from its reference.
 */
public final class AvoidRectsMiddleware implements FloatingMiddleware {

    private final Supplier<? extends List<Rect>> obstacles;
    private final int padding;
    private final int maxPush;

    public static AvoidRectsMiddleware create(Supplier<? extends List<Rect>> obstacles) {
        return new AvoidRectsMiddleware(obstacles, 2, 64);
    }

    public static AvoidRectsMiddleware create(Supplier<? extends List<Rect>> obstacles, int padding) {
        return new AvoidRectsMiddleware(obstacles, padding, 64);
    }

    public static AvoidRectsMiddleware create(Supplier<? extends List<Rect>> obstacles, int padding, int maxPush) {
        return new AvoidRectsMiddleware(obstacles, padding, maxPush);
    }

    private AvoidRectsMiddleware(Supplier<? extends List<Rect>> obstacles, int padding, int maxPush) {
        this.obstacles = obstacles;
        this.padding = padding;
        this.maxPush = maxPush;
    }

    @Override
    public String name() {
        return "avoidRects";
    }

    @Override
    public Result run(FloatingState state) {
        List<Rect> obs = obstacles.get();
        if (obs == null || obs.isEmpty()) return Result.done();

        double x = state.x();
        double y = state.y();
        int w = state.floatingRect().width();
        int h = state.floatingRect().height();

        double budget = maxPush;
        for (int pass = 0; pass < 3 && budget > 0; pass++) {
            boolean moved = false;
            for (Rect ob : obs) {
                Rect pad = new Rect(
                        ob.x() - padding, ob.y() - padding,
                        ob.width() + padding * 2, ob.height() + padding * 2);
                Rect cur = new Rect((int) Math.round(x), (int) Math.round(y), w, h);
                Rect in = cur.intersection(pad);
                if (in.width() <= 0 || in.height() <= 0) continue;
                //a graze is acceptable — chasing pixel-perfect separation
                //jiggles the panel more than the overlap hurts readability
                if (in.width() <= 4 && in.height() <= 4) continue;

                //minimal escape distances (positive = pixels needed to clear)
                double pushLeft = cur.right() - pad.x();
                double pushRight = pad.right() - cur.x();
                double pushUp = cur.bottom() - pad.y();
                double pushDown = pad.bottom() - cur.y();
                double min = Math.min(Math.min(pushLeft, pushRight), Math.min(pushUp, pushDown));
                if (min > budget) continue;   //clearing it costs too much — accept the overlap

                if (min == pushLeft) x -= pushLeft;
                else if (min == pushRight) x += pushRight;
                else if (min == pushUp) y -= pushUp;
                else y += pushDown;
                budget -= min;
                moved = true;
            }
            if (!moved) break;
        }

        //keep the result inside the boundary when it fits at all
        Rect b = state.boundary();
        if (w <= b.width()) x = Math.max(b.x() + padding, Math.min(x, b.right() - padding - w));
        if (h <= b.height()) y = Math.max(b.y() + padding, Math.min(y, b.bottom() - padding - h));

        state.setX(x);
        state.setY(y);
        return Result.done();
    }
}
