package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A fan of {@code N} evenly spaced rays from an anchor point (128 by default)
 * that answers "which directions around this anchor are free" by combining
 * {@link IntervalSet} angular arithmetic: every blocking rectangle subtracts
 * the angular span it subtends from the anchor, and the complement within
 * {@code [0, 2π)} — coalesced across the wrap-around — is the free angular
 * space.
 * <p>
 * Angles are radians in the standard atan2 convention measured from the
 * positive x axis; because GUI y grows downward, increasing angles go
 * clockwise on screen. A rectangle blocks an arc only when it comes within
 * {@link #radius()} of the anchor (a distant occluder never shadows the ring a
 * label actually occupies); a rectangle containing the anchor blocks
 * everything. Free arcs may be reported with {@code end > 2π} to represent a
 * span crossing the wrap-around, and candidate directions are the arc
 * midpoints ordered widest-arc first.
 */
public final class RayFan {

    /** The default number of rays in the fan. */
    public static final int defaultRayCount = 128;

    /** The full-turn angle, in radians. */
    public static final double fullTurn = 2 * Math.PI;

    private final double anchorX;
    private final double anchorY;
    private final int rayCount;
    private final double radius;
    private final IntervalSet blocked = new IntervalSet();

    /**
     * @throws IllegalArgumentException if ray count or radius is not positive
     */
    public RayFan(double anchorX, double anchorY, int rayCount, double radius) {
        if (rayCount <= 0) {
            throw new IllegalArgumentException("ray count must be positive: " + rayCount);
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be positive: " + radius);
        }
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.rayCount = rayCount;
        this.radius = radius;
    }

    /** A fan with the default ray count. */
    public RayFan(double anchorX, double anchorY, double radius) {
        this(anchorX, anchorY, defaultRayCount, radius);
    }

    public double anchorX() {
        return anchorX;
    }

    public double anchorY() {
        return anchorY;
    }

    public int rayCount() {
        return rayCount;
    }

    /** The reach of the fan: only occluders within this distance block arcs. */
    public double radius() {
        return radius;
    }

    /** The angle of ray {@code index}, in {@code [0, 2π)}. */
    public double rayAngle(int index) {
        return fullTurn * index / rayCount;
    }

    /** Removes all blocked arcs. */
    public void reset() {
        blocked.clear();
    }

    /** Subtracts the arcs blocked by each rectangle. */
    public void blockAll(Iterable<Rect> occluders) {
        for (Rect occluder : occluders) {
            block(occluder);
        }
    }

    /**
     * Subtracts the angular span the rectangle subtends from the anchor —
     * spanning the wrap-around when the rectangle straddles the positive x
     * axis, covering the whole turn when it contains the anchor, and doing
     * nothing when it is empty or lies farther than {@link #radius()} away.
     */
    public void block(Rect occluder) {
        if (occluder.width() <= 0 || occluder.height() <= 0) {
            return;
        }
        double dx0 = occluder.x() - anchorX;
        double dx1 = occluder.right() - anchorX;
        double dy0 = occluder.y() - anchorY;
        double dy1 = occluder.bottom() - anchorY;
        boolean containsAnchor = dx0 <= 0 && dx1 >= 0 && dy0 <= 0 && dy1 >= 0;
        if (containsAnchor) {
            blocked.add(0, fullTurn);
            return;
        }
        if (distanceSquaredToNearestPoint(dx0, dx1, dy0, dy1) > radius * radius) {
            return;
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double dx : new double[]{dx0, dx1}) {
            for (double dy : new double[]{dy0, dy1}) {
                double angle = normalize(Math.atan2(dy, dx));
                min = Math.min(min, angle);
                max = Math.max(max, angle);
            }
        }
        if (max <= min) {
            return;
        }
        if (max - min <= Math.PI) {
            blocked.add(min, max);
        } else {
            // Corner span crosses the wrap-around: the blocked span is the complement.
            blocked.add(0, min);
            blocked.add(max, fullTurn);
        }
    }

    private static double distanceSquaredToNearestPoint(double dx0, double dx1, double dy0, double dy1) {
        double nx = Math.max(dx0, Math.min(0, dx1));
        double ny = Math.max(dy0, Math.min(0, dy1));
        return nx * nx + ny * ny;
    }

    /** Normalizes an angle into {@code [0, 2π)}. */
    private static double normalize(double angle) {
        double normalized = angle % fullTurn;
        if (normalized < 0) {
            normalized += fullTurn;
        }
        return normalized;
    }

    /** Whether the direction (any angle, normalized internally) is blocked. */
    public boolean isBlocked(double angle) {
        return blocked.contains(normalize(angle));
    }

    /**
     * The free angular intervals within one turn, wrap-coalesced: a free span
     * crossing the positive x axis is a single interval that may end beyond
     * {@code 2π} and is reported first. Empty when everything is blocked.
     */
    public List<IntervalSet.Interval> freeArcs() {
        List<IntervalSet.Interval> free = blocked.complement(0, fullTurn);
        if (free.size() < 2) {
            return free;
        }
        IntervalSet.Interval first = free.getFirst();
        IntervalSet.Interval last = free.getLast();
        boolean firstAtZero = first.start() <= 0;
        boolean lastAtTurn = last.end() >= fullTurn;
        if (firstAtZero && lastAtTurn) {
            List<IntervalSet.Interval> coalesced = new ArrayList<>(free.size() - 1);
            coalesced.add(new IntervalSet.Interval(last.start(), first.end() + fullTurn));
            coalesced.addAll(free.subList(1, free.size() - 1));
            return coalesced;
        }
        return free;
    }

    /**
     * Candidate directions: the midpoint of each free arc, normalized into
     * {@code [0, 2π)}, widest arc first (ties by earlier start).
     */
    public List<Double> candidateDirections() {
        List<IntervalSet.Interval> arcs = new ArrayList<>(freeArcs());
        arcs.sort((a, b) -> {
            int byLength = Double.compare(b.length(), a.length());
            return byLength != 0 ? byLength : Double.compare(a.start(), b.start());
        });
        List<Double> directions = new ArrayList<>(arcs.size());
        for (IntervalSet.Interval arc : arcs) {
            directions.add(normalize((arc.start() + arc.end()) / 2));
        }
        return directions;
    }

    /**
     * The widest free direction, or {@code null} when everything is blocked.
     */
    public @Nullable Double bestDirection() {
        List<Double> candidates = candidateDirections();
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    /**
     * The angles of the fan's rays that are not blocked, in ascending angular
     * order — the discrete sampling counterpart of {@link #freeArcs()}.
     */
    public List<Double> freeRays() {
        List<Double> free = new ArrayList<>();
        for (int i = 0; i < rayCount; i++) {
            if (!isBlocked(rayAngle(i))) {
                free.add(rayAngle(i));
            }
        }
        return free;
    }
}
