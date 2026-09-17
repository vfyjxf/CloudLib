package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.ExclusionContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.InworldExclusions;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * FloatingMiddleware that nudges the floating element out of a set of
 * obstacle rects (e.g. other panels already committed to the screen).
 * <p>
 * Obstacle sources are pluggable: {@link #create} takes any custom rect
 * supplier, while {@link #exclusions} reads the screen's registered
 * {@link InworldExclusions exclusion-area providers} each run — the same
 * rectangles every other avoidance consumer sees. The escape logic itself is
 * the pure function {@link #escapeObstacles}, so sources and geometry can be
 * exercised without a live client.
 * <p>
 * For every obstacle the element intersects, the minimal single-axis escape
 * push is applied, preferring the axis that keeps the element closest to its
 * placement. Pushes are bounded by a per-frame budget — when clearing the
 * obstacle would displace the element too far, the overlap is accepted
 * instead of teleporting the element away from its reference.
 */
public final class AvoidRectsMiddleware implements FloatingMiddleware {

    private final Function<FloatingState, List<Rect>> obstacles;
    private final int padding;
    private final int maxPush;

    /**
     * Creates a middleware avoiding custom, dynamically supplied obstacle rects.
     *
     * @param obstacles live supplier of rects the element should not cover
     */
    public static AvoidRectsMiddleware create(Supplier<? extends List<Rect>> obstacles) {
        return new AvoidRectsMiddleware(state -> obstacles.get(), 2, 64);
    }

    /**
     * Creates a middleware avoiding custom obstacle rects with clearance padding.
     *
     * @param obstacles live supplier of obstacle rects
     * @param padding   extra clearance kept around each obstacle
     */
    public static AvoidRectsMiddleware create(Supplier<? extends List<Rect>> obstacles, int padding) {
        return new AvoidRectsMiddleware(state -> obstacles.get(), padding, 64);
    }

    /**
     * Creates a middleware avoiding custom obstacle rects with full options.
     *
     * @param obstacles live supplier of obstacle rects
     * @param padding   extra clearance kept around each obstacle
     * @param maxPush   total displacement budget; overlaps that would cost
     *                  more to clear are accepted instead
     */
    public static AvoidRectsMiddleware create(Supplier<? extends List<Rect>> obstacles, int padding, int maxPush) {
        return new AvoidRectsMiddleware(state -> obstacles.get(), padding, maxPush);
    }

    /**
     * Creates a middleware whose obstacles are the screen's registered
     * {@link InworldExclusions exclusion areas}, with default padding and
     * push budget.
     */
    public static AvoidRectsMiddleware exclusions() {
        return exclusions(2, 64);
    }

    /**
     * Creates a middleware whose obstacles are the screen's registered
     * {@link InworldExclusions exclusion areas}.
     *
     * @param padding extra clearance kept around each exclusion area
     */
    public static AvoidRectsMiddleware exclusions(int padding) {
        return exclusions(padding, 64);
    }

    /**
     * Creates a middleware whose obstacles are the screen's registered
     * {@link InworldExclusions exclusion areas}. The exclusion context is
     * derived from the run's {@link FloatingState#boundary()} — the
     * gui-scaled screen rect — with a partial tick of zero, since middleware
     * runs during layout rather than render.
     *
     * @param padding extra clearance kept around each exclusion area
     * @param maxPush total displacement budget; overlaps that would cost
     *                more to clear are accepted instead
     */
    public static AvoidRectsMiddleware exclusions(int padding, int maxPush) {
        return new AvoidRectsMiddleware(AvoidRectsMiddleware::inworldObstacles, padding, maxPush);
    }

    private static List<Rect> inworldObstacles(FloatingState state) {
        Rect boundary = state.boundary();
        if (boundary.width() <= 0 || boundary.height() <= 0) {
            return List.of();
        }
        return InworldExclusions.collect(new ExclusionContext(boundary.width(), boundary.height(), 0f));
    }

    private AvoidRectsMiddleware(Function<FloatingState, List<Rect>> obstacles, int padding, int maxPush) {
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
        List<Rect> obs = obstacles.apply(state);
        if (obs == null || obs.isEmpty()) return Result.done();

        FloatPos escaped = escapeObstacles(
                state.x(),
                state.y(),
                state.floatingRect().width(),
                state.floatingRect().height(),
                obs,
                padding,
                maxPush,
                state.boundary());

        state.setX(escaped.x());
        state.setY(escaped.y());
        return Result.done();
    }

    /**
     * The pure escape core: pushes a {@code width × height} element at
     * {@code (x, y)} out of {@code obstacles} and returns where it lands,
     * clamped into {@code boundary}. See the class docs for the push
     * strategy; this function has no state and never touches the client.
     *
     * @param x         the element's current x position
     * @param y         the element's current y position
     * @param width     the element's width
     * @param height    the element's height
     * @param obstacles the obstacle rects to escape, in gui-scaled pixels
     * @param padding   extra clearance kept around each obstacle
     * @param maxPush   total displacement budget; overlaps that would cost
     *                  more to clear are accepted instead
     * @param boundary  the rect the element is clamped into, when it fits at all
     * @return the escaped position
     */
    public static FloatPos escapeObstacles(
            double x, double y, int width, int height, List<Rect> obstacles, int padding, int maxPush, Rect boundary) {
        double budget = maxPush;
        for (int pass = 0; pass < 3 && budget > 0; pass++) {
            boolean moved = false;
            for (Rect ob : obstacles) {
                Rect pad = new Rect(
                        ob.x() - padding, ob.y() - padding, ob.width() + padding * 2, ob.height() + padding * 2);
                Rect cur = new Rect((int) Math.round(x), (int) Math.round(y), width, height);
                Rect in = cur.intersection(pad);
                if (in.width() <= 0 || in.height() <= 0) continue;
                // a graze is acceptable — chasing pixel-perfect separation
                // jiggles the panel more than the overlap hurts readability
                if (in.width() <= 4 && in.height() <= 4) continue;

                // minimal escape distances (positive = pixels needed to clear)
                double pushLeft = cur.right() - pad.x();
                double pushRight = pad.right() - cur.x();
                double pushUp = cur.bottom() - pad.y();
                double pushDown = pad.bottom() - cur.y();
                double min = Math.min(Math.min(pushLeft, pushRight), Math.min(pushUp, pushDown));
                if (min > budget) continue; // clearing it costs too much — accept the overlap

                if (min == pushLeft) x -= pushLeft;
                else if (min == pushRight) x += pushRight;
                else if (min == pushUp) y -= pushUp;
                else y += pushDown;
                budget -= min;
                moved = true;
            }
            if (!moved) break;
        }

        // keep the result inside the boundary when it fits at all
        if (width <= boundary.width())
            x = Math.max(boundary.x() + padding, Math.min(x, boundary.right() - padding - width));
        if (height <= boundary.height())
            y = Math.max(boundary.y() + padding, Math.min(y, boundary.bottom() - padding - height));

        return new FloatPos(x, y);
    }
}
