package dev.vfyjxf.cloudlib.api.ui.border;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;

/**
 * The four corners of a frame, in reading order (top-left → bottom-left).
 * Each corner knows the two edges that meet at it and its (u, v) position
 * factors — {@code (0,0)} is top-left, {@code (1,1)} bottom-right.
 */
public enum Corner {
    topLeft(Edge.top, Edge.left, 0, 0), topRight(Edge.top, Edge.right, 1, 0), bottomRight(
        Edge.bottom,
        Edge.right,
        1,
        1
    ), bottomLeft(Edge.bottom, Edge.left, 0, 1);

    private final Edge edgeA;
    private final Edge edgeB;
    private final double u;
    private final double v;

    Corner(Edge edgeA, Edge edgeB, double u, double v) {
        this.edgeA = edgeA;
        this.edgeB = edgeB;
        this.u = u;
        this.v = v;
    }

    /** One of the two edges meeting at this corner (the horizontal one for top/bottom pairs). */
    public Edge edgeA() {
        return edgeA;
    }

    /** The other edge meeting at this corner. */
    public Edge edgeB() {
        return edgeB;
    }

    /** Horizontal position factor — 0 on the left, 1 on the right. */
    public double u() {
        return u;
    }

    /** Vertical position factor — 0 at the top, 1 at the bottom. */
    public double v() {
        return v;
    }

    /** The corner diagonally opposite this one. */
    public Corner opposite() {
        return switch (this) {
            case topLeft -> bottomRight;
            case bottomRight -> topLeft;
            case topRight -> bottomLeft;
            case bottomLeft -> topRight;
        };
    }
}
