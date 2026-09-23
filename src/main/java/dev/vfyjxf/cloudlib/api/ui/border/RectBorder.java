package dev.vfyjxf.cloudlib.api.ui.border;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import org.jspecify.annotations.Nullable;

/**
 * A screen-space UI frame measured for attachment — the border-measurement
 * half of the trace API.
 * <p>
 * Wraps a gui-px rect and resolves points on it: edge/corner/center
 * positions, parametric edge points, perimeter walking, and
 * {@link ScreenPort}s (position + outward direction) for connection
 * routing. {@link #nearestPort(FloatPos)} always lands on the perimeter —
 * a point projecting inside the rect snaps to its nearest edge instead of
 * the surface.
 * <p>
 * Measurement is the product; drawing is opt-in: {@link #stroke} /
 * {@link #strokeEdge} render the frame through a {@link SceneCanvas} only
 * when a caller asks for it.
 */
public final class RectBorder {

    private static final double eps = 1.0e-6;

    private final FloatRect rect;

    private RectBorder(FloatRect rect) {
        this.rect = rect;
    }

    public static RectBorder of(FloatRect rect) {
        return new RectBorder(rect);
    }

    public static RectBorder of(Rect rect) {
        return new RectBorder(new FloatRect(rect.x(), rect.y(), rect.width(), rect.height()));
    }

    public static RectBorder of(double x, double y, double width, double height) {
        return new RectBorder(new FloatRect(x, y, width, height));
    }

    /** The measured rect. */
    public FloatRect rect() {
        return rect;
    }

    public FloatPos center() {
        return rect.center();
    }

    // region measurement

    /** World-free edge length in px — top/bottom → width, left/right → height. */
    public double edgeLength(Edge edge) {
        return switch (edge) {
            case top, bottom -> rect.width();
            case left, right -> rect.height();
        };
    }

    public double perimeter() {
        return 2 * (rect.width() + rect.height());
    }

    /** The unit outward normal of an edge — e.g. {@code top → (0,-1)}. */
    public FloatPos outward(Edge edge) {
        return switch (edge) {
            case top -> new FloatPos(0, -1);
            case bottom -> new FloatPos(0, 1);
            case left -> new FloatPos(-1, 0);
            case right -> new FloatPos(1, 0);
        };
    }

    /**
     * A parametric point on an edge — {@code t} in [0,1], left→right on
     * top/bottom, top→bottom on left/right.
     */
    public FloatPos edgePoint(Edge edge, double t) {
        double x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        return switch (edge) {
            case top -> new FloatPos(x + t * w, y);
            case bottom -> new FloatPos(x + t * w, y + h);
            case left -> new FloatPos(x, y + t * h);
            case right -> new FloatPos(x + w, y + t * h);
        };
    }

    public FloatPos edgeCenter(Edge edge) {
        return edgePoint(edge, 0.5);
    }

    public FloatPos corner(Corner corner) {
        return new FloatPos(rect.x() + corner.u() * rect.width(), rect.y() + corner.v() * rect.height());
    }

    /**
     * Walks the perimeter clockwise from the top-left corner — {@code s}
     * is an arc length in px, wrapped into [0, {@link #perimeter()}).
     */
    public FloatPos perimeterPoint(double s) {
        double w = rect.width(), h = rect.height(), p = perimeter();
        if (p <= 0) return new FloatPos(rect.x(), rect.y());
        s = ((s % p) + p) % p;
        if (s < w) return new FloatPos(rect.x() + s, rect.y());
        s -= w;
        if (s < h) return new FloatPos(rect.right(), rect.y() + s);
        s -= h;
        if (s < w) return new FloatPos(rect.right() - s, rect.bottom());
        s -= w;
        return new FloatPos(rect.x(), rect.bottom() - s);
    }

    // endregion

    // region ports

    /** An edge port — point on the edge, outward = the edge normal. */
    public ScreenPort port(Edge edge, double t) {
        return new ScreenPort(edgePoint(edge, t), outward(edge));
    }

    /** A corner port — outward is the bisector of the two meeting edges' normals. */
    public ScreenPort cornerPort(Corner corner) {
        FloatPos a = outward(corner.edgeA());
        FloatPos b = outward(corner.edgeB());
        return new ScreenPort(corner(corner), normalize(a.x + b.x, a.y + b.y));
    }

    /** The center port — no preferred exit direction. */
    public ScreenPort centerPort() {
        return new ScreenPort(center(), new FloatPos(0, 0));
    }

    /**
     * The perimeter port closest to {@code toward}. Points inside the rect
     * snap to their nearest edge; outside points clamp onto the boundary
     * (a corner hit takes the bisector of the two meeting normals).
     */
    public ScreenPort nearestPort(FloatPos toward) {
        double x = rect.x(), y = rect.y(), r = rect.right(), b = rect.bottom();
        double w = rect.width(), h = rect.height();
        if (rect.contains(toward.x, toward.y) && w > 0 && h > 0) {
            // inside — nearest edge wins
            double dl = toward.x - x, dr = r - toward.x;
            double dt = toward.y - y, db = b - toward.y;
            double m = Math.min(Math.min(dl, dr), Math.min(dt, db));
            if (m == dl) return port(Edge.left, (toward.y - y) / h);
            if (m == dr) return port(Edge.right, (toward.y - y) / h);
            if (m == dt) return port(Edge.top, (toward.x - x) / w);
            return port(Edge.bottom, (toward.x - x) / w);
        }
        double cx = Math.max(x, Math.min(r, toward.x));
        double cy = Math.max(y, Math.min(b, toward.y));
        double ox = 0, oy = 0;
        if (cx <= x + eps) ox -= 1;
        if (cx >= r - eps) ox += 1;
        if (cy <= y + eps) oy -= 1;
        if (cy >= b - eps) oy += 1;
        return new ScreenPort(new FloatPos(cx, cy), normalize(ox, oy));
    }

    /** Resolves a {@link BorderPoint} spec into a port. {@code toward} feeds {@link BorderPoint#nearest()}. */
    public ScreenPort port(BorderPoint point, @Nullable FloatPos toward) {
        return switch (point.kind()) {
            case nearest -> toward == null ? centerPort() : nearestPort(toward);
            case edge -> port(point.edge(), point.t());
            case corner -> cornerPort(point.corner());
            case center -> centerPort();
        };
    }

    // endregion

    // region opt-in drawing

    /** Strokes the whole frame, 1 gui px wide. */
    public void stroke(SceneCanvas canvas, int argb) {
        stroke(canvas, argb, 1f);
    }

    /** Strokes the whole frame. */
    public void stroke(SceneCanvas canvas, int argb, float widthPx) {
        for (Edge edge : Edge.values()) strokeEdge(canvas, edge, argb, widthPx);
    }

    /** Strokes one edge of the frame. */
    public void strokeEdge(SceneCanvas canvas, Edge edge, int argb, float widthPx) {
        FloatPos a = edgePoint(edge, 0);
        FloatPos b = edgePoint(edge, 1);
        canvas.line((float) a.x, (float) a.y, (float) b.x, (float) b.y, widthPx, argb);
    }

    // endregion

    private static FloatPos normalize(double x, double y) {
        double len = Math.sqrt(x * x + y * y);
        return len < eps ? new FloatPos(0, 0) : new FloatPos(x / len, y / len);
    }
}
