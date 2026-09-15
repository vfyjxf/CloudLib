package dev.vfyjxf.cloudlib.api.ui.border;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import org.jetbrains.annotations.Nullable;

/**
 * A point selected on a measured frame — where something (a trace link, a
 * decoration) attaches to a UI border.
 * <p>
 * {@link #nearest()} lets the frame pick the border point closest to the
 * other end of the connection, re-solved every frame; the explicit kinds
 * pin an exact spot: {@link #edge(Edge, double)} is a parametric point
 * along one edge ({@code t} runs left→right on top/bottom, top→bottom on
 * left/right), {@link #corner(Corner)} a corner, {@link #center()} the
 * frame's center.
 */
public record BorderPoint(Kind kind, @Nullable Edge edge, double t, @Nullable Corner corner) {

    public enum Kind {
        /** The border point closest to the other end — resolved per frame. */
        nearest,
        /** A parametric point on one edge. */
        edge,
        /** A corner. */
        corner,
        /** The frame's center. */
        center
    }

    private static final BorderPoint nearest = new BorderPoint(Kind.nearest, null, 0, null);
    private static final BorderPoint center = new BorderPoint(Kind.center, null, 0, null);

    public BorderPoint {
        if (kind == Kind.edge && edge == null) {
            throw new IllegalArgumentException("edge point requires an edge");
        }
        if (kind == Kind.corner && corner == null) {
            throw new IllegalArgumentException("corner point requires a corner");
        }
        t = Math.max(0, Math.min(1, t));
    }

    /** The border point closest to whatever connects here. */
    public static BorderPoint nearest() {
        return nearest;
    }

    /** The middle of an edge. */
    public static BorderPoint edge(Edge edge) {
        return edge(edge, 0.5);
    }

    /**
     * A parametric point on an edge — {@code t} in [0,1], measured
     * left→right on the top/bottom edges and top→bottom on the
     * left/right edges.
     */
    public static BorderPoint edge(Edge edge, double t) {
        return new BorderPoint(Kind.edge, edge, t, null);
    }

    public static BorderPoint corner(Corner corner) {
        return new BorderPoint(Kind.corner, null, 0, corner);
    }

    /** The frame's center — the attach point sits on the surface, not the border. */
    public static BorderPoint center() {
        return center;
    }
}
