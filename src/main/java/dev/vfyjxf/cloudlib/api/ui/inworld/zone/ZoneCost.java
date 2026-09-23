package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The unified cost function (Z1): one comparable score for a candidate rect,
 * {@code score = Σ wᵢ · termᵢ} with the nine weights from
 * {@link ZoneWeights}. Every term is normalized to {@code [0, 1]} — the
 * javadoc of each {@link Term} states its normalization basis — so weights
 * are directly comparable and the score is bounded by the weight sum.
 * <p>
 * The terms, all pure functions of the candidate and the context:
 * <ul>
 *   <li>{@link Term#anchor anchor} — squared distance from the candidate's
 *       center to the anchor, normalized by the squared safe-rect diagonal</li>
 *   <li>{@link Term#overlap overlap} — the candidate's area covered by
 *       already-placed rects, summed over all placements and clamped at 1
 *       (normalized by the candidate's own area)</li>
 *   <li>{@link Term#hud hud} — the candidate's area covered by exclusion
 *       rects, same normalization as overlap</li>
 *   <li>{@link Term#attention attention} — the attention field's
 *       {@link AttentionField#cost(Rect)}, already in {@code [0, 1]}</li>
 *   <li>{@link Term#edge edge} — the candidate's area outside the safe rect,
 *       normalized by the candidate's own area; defensive (the candidate
 *       lattice pre-clamps, so this is 0 on the happy path)</li>
 *   <li>{@link Term#leader leader} — the estimated leader length: the
 *       distance from the anchor to the midpoint of the candidate's edge
 *       facing the anchor, normalized by the safe-rect diagonal; 0 when the
 *       anchor lies inside the candidate (no leader needed)</li>
 *   <li>{@link Term#temporal temporal} — squared center distance from the
 *       previous frame's rect, normalized by the squared safe-rect diagonal;
 *       0 when there is no previous rect</li>
 *   <li>{@link Term#crossing crossing} — the count of the candidate's leader
 *       segment (anchor → facing-edge midpoint) properly crossing
 *       already-placed leader segments, normalized by the number of placed
 *       segments; 0 when none are placed. Shared endpoints are not crossings;
 *       collinear overlap is</li>
 *   <li>{@link Term#topology topology} — the fraction of previous-frame
 *       adjacency relations involving this element that the candidate breaks
 *       (a leftOf/above pair counts as broken when the current geometry no
 *       longer satisfies it); 0 when no previous adjacency involves this
 *       element</li>
 * </ul>
 * "Left of" is {@code a.right() <= b.x()} and "above" is
 * {@code a.bottom() <= b.y()} (gui y grows downward) — both touching-tolerant.
 */
public final class ZoneCost {

    /** The nine cost terms; each javadoc states its normalization basis. */
    public enum Term {
        anchor, overlap, hud, attention, edge, leader, temporal, crossing, topology
    }

    /** A leader line segment, in screen coordinates. */
    public record Segment(double x1, double y1, double x2, double y2) {

        public Segment {
            requireFinite("x1", x1);
            requireFinite("y1", y1);
            requireFinite("x2", x2);
            requireFinite("y2", y2);
        }
    }

    /**
     * One previous-frame adjacency relation: {@code first} was left of
     * {@code second} (in a {@code previousLeftOf} set) or above it (in a
     * {@code previousAbove} set). An ordered pair — the containing set
     * carries the relation's meaning.
     */
    public record Adjacency(String first, String second) {

        public Adjacency {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");
        }
    }

    /**
     * Everything the cost terms see of the world.
     *
     * @param elementId the candidate's owning element id (topology term)
     * @param anchor the element's anchor screen position
     * @param safeRect the safe rectangle (edge term; every normalization
     *        basis that uses the diagonal uses this rect's)
     * @param attention the center attention field
     * @param placed the already-placed elements' rects by id (overlap and
     *        topology terms)
     * @param exclusions the exclusion (HUD) rects (hud term)
     * @param previous the element's previous-frame rect, when it had one
     *        (temporal term)
     * @param placedLeaders the already-placed leader segments (crossing term)
     * @param previousLeftOf the previous frame's left-of relations (topology
     *        term)
     * @param previousAbove the previous frame's above relations (topology
     *        term)
     */
    public record Context(
        String elementId,
        FloatPos anchor,
        Rect safeRect,
        AttentionField attention,
        Map<String, Rect> placed,
        List<Rect> exclusions,
        @Nullable Rect previous,
        List<Segment> placedLeaders,
        Set<Adjacency> previousLeftOf,
        Set<Adjacency> previousAbove
    ) {

        public Context {
            Objects.requireNonNull(elementId, "elementId");
            Objects.requireNonNull(anchor, "anchor");
            if (!Double.isFinite(anchor.x()) || !Double.isFinite(anchor.y())) {
                throw new IllegalArgumentException("anchor must be finite: " + anchor.x() + ", " + anchor.y());
            }
            Objects.requireNonNull(safeRect, "safeRect");
            Objects.requireNonNull(attention, "attention");
            placed = Map.copyOf(placed);
            exclusions = List.copyOf(exclusions);
            placedLeaders = List.copyOf(placedLeaders);
            previousLeftOf = Set.copyOf(previousLeftOf);
            previousAbove = Set.copyOf(previousAbove);
        }

        /** A context with no placements, exclusions, leaders or adjacency — everything empty. */
        public static Context of(String elementId, FloatPos anchor, Rect safeRect, AttentionField attention) {
            return new Context(
                elementId,
                anchor,
                safeRect,
                attention,
                Map.of(),
                List.of(),
                null,
                List.of(),
                Set.of(),
                Set.of()
            );
        }
    }

    private static final double epsilon = 1.0e-9;

    private final ZoneWeights weights;

    public ZoneCost(ZoneWeights weights) {
        this.weights = Objects.requireNonNull(weights, "weights");
    }

    /** A cost function with {@link ZoneWeights#defaults()}. */
    public static ZoneCost withDefaults() {
        return new ZoneCost(ZoneWeights.defaults());
    }

    public ZoneWeights weights() {
        return weights;
    }

    /**
     * The weighted total: {@code Σ wᵢ · termᵢ}, every term normalized to
     * {@code [0, 1]} — so the score lies in {@code [0, Σ wᵢ]}.
     */
    public double cost(Rect candidate, Context context) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(context, "context");
        return weights.anchor() * term(Term.anchor, candidate, context)
                + weights.overlap() * term(Term.overlap, candidate, context)
                + weights.hud() * term(Term.hud, candidate, context)
                + weights.attention() * term(Term.attention, candidate, context)
                + weights.edge() * term(Term.edge, candidate, context)
                + weights.leader() * term(Term.leader, candidate, context)
                + weights.temporal() * term(Term.temporal, candidate, context)
                + weights.crossing() * term(Term.crossing, candidate, context)
                + weights.topology() * term(Term.topology, candidate, context);
    }

    /** One normalized term value in {@code [0, 1]}. */
    public double term(Term term, Rect candidate, Context context) {
        Objects.requireNonNull(term, "term");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(context, "context");
        return switch (term) {
            case anchor -> anchorTerm(candidate, context);
            case overlap -> coveredFraction(candidate, context.placed().values());
            case hud -> coveredFraction(candidate, context.exclusions());
            case attention -> context.attention().cost(candidate);
            case edge -> outsideFraction(candidate, context.safeRect());
            case leader -> leaderTerm(candidate, context);
            case temporal -> temporalTerm(candidate, context);
            case crossing -> crossingTerm(candidate, context);
            case topology -> topologyTerm(candidate, context);
        };
    }

    // region terms

    /** Squared center-anchor distance over the squared safe-rect diagonal. */
    private static double anchorTerm(Rect candidate, Context context) {
        return overDiagonalSq(squaredDistance(candidate.centerX(), candidate.centerY(), context.anchor()), context);
    }

    /** Squared center-previous distance over the squared safe-rect diagonal; 0 without a previous rect. */
    private static double temporalTerm(Rect candidate, Context context) {
        Rect previous = context.previous();
        if (previous == null) {
            return 0.0;
        }
        return overDiagonalSq(
            squaredDistance(candidate.centerX(), candidate.centerY(), previous.centerX(), previous.centerY()),
            context
        );
    }

    /** Summed covered area over the candidate's own area, clamped at 1. */
    private static double coveredFraction(Rect candidate, Iterable<Rect> obstacles) {
        double area = rectArea(candidate);
        if (area <= 0.0) {
            return 0.0;
        }
        double covered = 0.0;
        for (Rect obstacle : obstacles) {
            covered += rectArea(candidate.intersection(obstacle));
        }
        return clamp01(covered / area);
    }

    /** Area outside the safe rect over the candidate's own area. */
    private static double outsideFraction(Rect candidate, Rect safeRect) {
        double area = rectArea(candidate);
        if (area <= 0.0) {
            return 0.0;
        }
        double inside = rectArea(candidate.intersection(safeRect));
        return clamp01((area - inside) / area);
    }

    /** Leader length over the safe-rect diagonal; 0 when the anchor is inside the candidate. */
    private static double leaderTerm(Rect candidate, Context context) {
        FloatPos side = anchorSideMidpoint(candidate, context.anchor());
        double dx = side.x() - context.anchor().x();
        double dy = side.y() - context.anchor().y();
        return overDiagonal(Math.sqrt(dx * dx + dy * dy), context);
    }

    /** Crossings with placed leaders over the placed-leader count. */
    private static double crossingTerm(Rect candidate, Context context) {
        List<Segment> placed = context.placedLeaders();
        if (placed.isEmpty()) {
            return 0.0;
        }
        FloatPos side = anchorSideMidpoint(candidate, context.anchor());
        Segment leader = new Segment(context.anchor().x(), context.anchor().y(), side.x(), side.y());
        int crossings = 0;
        for (Segment other : placed) {
            if (segmentsIntersect(leader, other)) {
                crossings++;
            }
        }
        return clamp01((double) crossings / placed.size());
    }

    /** Broken previous-frame adjacency pairs over the pairs involving this element. */
    private static double topologyTerm(Rect candidate, Context context) {
        int broken = 0;
        int total = 0;
        for (Adjacency pair : context.previousLeftOf()) {
            Rect other = otherRect(pair, context);
            if (other == null) {
                continue;
            }
            total++;
            boolean holds = pair.first().equals(context.elementId())
                    ? candidate.right() <= other.x()
                    : other.right() <= candidate.x();
            if (!holds) {
                broken++;
            }
        }
        for (Adjacency pair : context.previousAbove()) {
            Rect other = otherRect(pair, context);
            if (other == null) {
                continue;
            }
            total++;
            boolean holds = pair.first().equals(context.elementId())
                    ? candidate.bottom() <= other.y()
                    : other.bottom() <= candidate.y();
            if (!holds) {
                broken++;
            }
        }
        return total == 0 ? 0.0 : clamp01((double) broken / total);
    }

    /**
     * The rect of the pair's other element — the id that is not this
     * candidate's element — when the pair involves this element and the other
     * element is placed; null otherwise (self-pairs and pairs about other
     * elements are not this candidate's business).
     */
    private static @Nullable Rect otherRect(Adjacency pair, Context context) {
        if (pair.first().equals(pair.second())) {
            return null;
        }
        if (pair.first().equals(context.elementId())) {
            return context.placed().get(pair.second());
        }
        if (pair.second().equals(context.elementId())) {
            return context.placed().get(pair.first());
        }
        return null;
    }

    // endregion

    // region geometry

    /**
     * The midpoint of the candidate's edge facing the anchor: the nearer of
     * the two vertical edges when the horizontal separation dominates, else
     * the nearer horizontal edge (gui y grows down, so an anchor above the
     * rect faces the rect's top edge). When the anchor lies inside the rect,
     * the anchor itself (no leader needed).
     */
    private static FloatPos anchorSideMidpoint(Rect rect, FloatPos anchor) {
        double gapLeft = rect.x() - anchor.x();
        double gapRight = anchor.x() - rect.right();
        double dx = Math.max(gapLeft, gapRight);
        double gapAbove = rect.y() - anchor.y();
        double gapBelow = anchor.y() - rect.bottom();
        double dy = Math.max(gapAbove, gapBelow);
        if (dx <= 0.0 && dy <= 0.0) {
            return new FloatPos(anchor.x(), anchor.y());
        }
        if (dx >= dy) {
            return gapLeft >= gapRight
                    ? new FloatPos(rect.x(), rect.centerY())
                    : new FloatPos(rect.right(), rect.centerY());
        }
        return gapAbove >= gapBelow
                ? new FloatPos(rect.centerX(), rect.y())
                : new FloatPos(rect.centerX(), rect.bottom());
    }

    /**
     * The leader segment a committed rect declares: from its anchor to the
     * rect's facing-edge midpoint — exactly the segment the crossing term
     * builds for a candidate, so a previous frame's committed leaders and a
     * candidate's estimated leader live on one definition. Degenerate (zero
     * length) when the anchor lies inside the rect (no leader needed).
     */
    public static Segment leaderOf(FloatPos anchor, Rect rect) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(rect, "rect");
        FloatPos side = anchorSideMidpoint(rect, anchor);
        return new Segment(anchor.x(), anchor.y(), side.x(), side.y());
    }

    /**
     * Whether the segments properly cross: a strict orientation-test
     * intersection, or collinear overlap. Shared or touching endpoints are
     * not crossings; degenerate (zero-length) segments never cross.
     */
    private static boolean segmentsIntersect(Segment a, Segment b) {
        if (length(a) < epsilon || length(b) < epsilon) {
            return false;
        }
        if (pointNear(a.x1(), a.y1(), b.x1(), b.y1())
                || pointNear(a.x1(), a.y1(), b.x2(), b.y2())
                || pointNear(a.x2(), a.y2(), b.x1(), b.y1())
                || pointNear(a.x2(), a.y2(), b.x2(), b.y2())) {
            return false;
        }
        double d1 = cross(b.x2() - b.x1(), b.y2() - b.y1(), a.x1() - b.x1(), a.y1() - b.y1());
        double d2 = cross(b.x2() - b.x1(), b.y2() - b.y1(), a.x2() - b.x1(), a.y2() - b.y1());
        double d3 = cross(a.x2() - a.x1(), a.y2() - a.y1(), b.x1() - a.x1(), b.y1() - a.y1());
        double d4 = cross(a.x2() - a.x1(), a.y2() - a.y1(), b.x2() - a.x1(), b.y2() - a.y1());
        if (((d1 > epsilon && d2 < -epsilon) || (d1 < -epsilon && d2 > epsilon))
                && ((d3 > epsilon && d4 < -epsilon) || (d3 < -epsilon && d4 > epsilon))) {
            return true;
        }
        return collinearOverlap(a, b, d1, d2, d3, d4);
    }

    private static boolean collinearOverlap(Segment a, Segment b, double... orientations) {
        for (double orientation : orientations) {
            if (Math.abs(orientation) > epsilon) {
                return false;
            }
        }
        return Math.max(a.x1(), a.x2()) > Math.min(b.x1(), b.x2()) - epsilon
                && Math.max(b.x1(), b.x2()) > Math.min(a.x1(), a.x2()) - epsilon
                && Math.max(a.y1(), a.y2()) > Math.min(b.y1(), b.y2()) - epsilon
                && Math.max(b.y1(), b.y2()) > Math.min(a.y1(), a.y2()) - epsilon;
    }

    private static double length(Segment s) {
        return Math.hypot(s.x2() - s.x1(), s.y2() - s.y1());
    }

    private static boolean pointNear(double ax, double ay, double bx, double by) {
        return Math.hypot(ax - bx, ay - by) < epsilon;
    }

    private static double cross(double ax, double ay, double bx, double by) {
        return ax * by - ay * bx;
    }

    private static double squaredDistance(double x, double y, FloatPos point) {
        double dx = x - point.x();
        double dy = y - point.y();
        return dx * dx + dy * dy;
    }

    private static double squaredDistance(double ax, double ay, double bx, double by) {
        double dx = ax - bx;
        double dy = ay - by;
        return dx * dx + dy * dy;
    }

    private static double diagonal(Context context) {
        return Math.sqrt(diagonalSq(context));
    }

    private static double diagonalSq(Context context) {
        Rect safe = context.safeRect();
        double w = safe.width();
        double h = safe.height();
        return w * w + h * h;
    }

    /**
     * A value normalized by the safe-rect diagonal — clamped to [0, 1] and
     * inert (0) on a degenerate empty safe rect, whose diagonal is 0.
     */
    private static double overDiagonal(double value, Context context) {
        double diagonal = diagonal(context);
        return diagonal <= 0.0 ? 0.0 : clamp01(value / diagonal);
    }

    /** @see #overDiagonal */
    private static double overDiagonalSq(double value, Context context) {
        double diagonalSq = diagonalSq(context);
        return diagonalSq <= 0.0 ? 0.0 : clamp01(value / diagonalSq);
    }

    private static double rectArea(Rect rect) {
        return (double) rect.width() * rect.height();
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }

    // endregion
}
