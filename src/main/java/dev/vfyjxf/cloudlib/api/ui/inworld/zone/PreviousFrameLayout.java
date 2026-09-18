package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The previous frame's committed layout in the zone vocabulary (Z2): the
 * committed placement map, each committed placement's leader segment, and the
 * left-of/above adjacency tables derived from it. The coordinator computes
 * this snapshot once per frame, after commit, from the committed grants —
 * and only when at least one registered element consumes zone context, so a
 * population without zone declarations pays nothing. The driving adapter
 * hands the snapshot to the next frame's {@code LayoutEnvironment}, where
 * the zone ranker picks it up.
 * <p>
 * Scoring against the <em>previous committed frame</em> (never this frame's
 * incremental placements) is what keeps the element-side zone cost frame-order
 * independent and deterministic: every zone element scores its candidates
 * against the same frozen snapshot regardless of its position in the
 * arbitration order. The coordinator's fit check remains the hard constraint
 * for the current frame; the zone cost is advisory ranking only.
 * <p>
 * Adjacency semantics — center projection plus edge-gap threshold: {@code a}
 * is left of {@code b} iff
 * <ol>
 *   <li>{@code a.centerX() < b.centerX()} (center projection: the order is
 *       never ambiguous when centers coincide — equal centers form no
 *       relation), and</li>
 *   <li>the vertical projections overlap by more than 0 px
 *       ({@code min(a.bottom, b.bottom) > max(a.y, b.y)} — a pair that only
 *       touches edge-to-edge in the projection is not adjacent), and</li>
 *   <li>the horizontal edge gap {@code b.x() - a.right() <= gapThresholdPx}
 *       (negative gaps — overlapping rects — satisfy it; arbitration keeps
 *       committed rects disjoint, but ghost-policy rects may overlap and
 *       still count as neighbors).</li>
 * </ol>
 * {@code a} above {@code b} is the same with the axes swapped (center y,
 * horizontal projection, vertical gap {@code b.y() - a.bottom()}). Both
 * tables are computed over unordered pairs in the snapshot's placement
 * order; a diagonal pair may appear in both tables.
 *
 * @param placements the committed rects by element id
 * @param leaders each committed placement's leader segment, in placement
 *        order; see {@link Leader}
 * @param leftOf the left-of relations of the committed layout
 * @param above the above relations of the committed layout
 */
public record PreviousFrameLayout(
        Map<String, Rect> placements,
        List<Leader> leaders,
        Set<ZoneCost.Adjacency> leftOf,
        Set<ZoneCost.Adjacency> above) {

    /** The default edge-gap threshold below which two rects count as adjacent, in gui pixels. */
    public static final double defaultAdjacencyGapPx = 8.0;

    public PreviousFrameLayout {
        placements = Map.copyOf(placements);
        leaders = List.copyOf(leaders);
        leftOf = Set.copyOf(leftOf);
        above = Set.copyOf(above);
    }

    /**
     * The snapshot of a committed layout at the default adjacency threshold.
     *
     * @param placements the committed placements in arbitration order
     *        (duplicated ids are a caller bug; the last one wins)
     */
    public static PreviousFrameLayout of(List<Placement> placements) {
        return of(placements, defaultAdjacencyGapPx);
    }

    /**
     * The snapshot of a committed layout.
     *
     * @param placements the committed placements in arbitration order
     * @param adjacencyGapPx the edge-gap threshold of the adjacency tables;
     *        must be finite and non-negative
     */
    public static PreviousFrameLayout of(List<Placement> placements, double adjacencyGapPx) {
        Objects.requireNonNull(placements, "placements");
        if (!Double.isFinite(adjacencyGapPx) || adjacencyGapPx < 0.0) {
            throw new IllegalArgumentException("adjacencyGapPx must be finite and non-negative: " + adjacencyGapPx);
        }
        Map<String, Rect> byId = new LinkedHashMap<>();
        List<Leader> leaderSegments = new ArrayList<>(placements.size());
        for (Placement placement : placements) {
            byId.put(placement.elementId(), placement.rect());
            leaderSegments.add(
                    new Leader(placement.elementId(), ZoneCost.leaderOf(placement.anchor(), placement.rect())));
        }
        Set<ZoneCost.Adjacency> leftOf = new LinkedHashSet<>();
        Set<ZoneCost.Adjacency> above = new LinkedHashSet<>();
        for (int i = 0; i < placements.size(); i++) {
            for (int j = i + 1; j < placements.size(); j++) {
                Placement first = placements.get(i);
                Placement second = placements.get(j);
                if (first.elementId().equals(second.elementId())) {
                    continue;
                }
                if (leftOf(first.rect(), second.rect(), adjacencyGapPx)) {
                    leftOf.add(new ZoneCost.Adjacency(first.elementId(), second.elementId()));
                }
                if (leftOf(second.rect(), first.rect(), adjacencyGapPx)) {
                    leftOf.add(new ZoneCost.Adjacency(second.elementId(), first.elementId()));
                }
                if (above(first.rect(), second.rect(), adjacencyGapPx)) {
                    above.add(new ZoneCost.Adjacency(first.elementId(), second.elementId()));
                }
                if (above(second.rect(), first.rect(), adjacencyGapPx)) {
                    above.add(new ZoneCost.Adjacency(second.elementId(), first.elementId()));
                }
            }
        }
        return new PreviousFrameLayout(byId, leaderSegments, leftOf, above);
    }

    /**
     * The committed leader segments except {@code elementId}'s own — the
     * crossing term scores a candidate's leader against the <em>other</em>
     * elements' leaders; a panel keeping its own leader collinear with its
     * previous one is staying put, not crossing.
     */
    public List<ZoneCost.Segment> leadersExcluding(String elementId) {
        Objects.requireNonNull(elementId, "elementId");
        List<ZoneCost.Segment> others = new ArrayList<>(leaders.size());
        for (Leader leader : leaders) {
            if (!leader.elementId().equals(elementId)) {
                others.add(leader.segment());
            }
        }
        return others;
    }

    /** {@code a} left of {@code b} at the default threshold. */
    private static boolean leftOf(Rect a, Rect b, double gapThresholdPx) {
        return a.centerX() < b.centerX()
                && Math.min(a.bottom(), b.bottom()) > Math.max(a.y(), b.y())
                && b.x() - a.right() <= gapThresholdPx;
    }

    /** {@code a} above {@code b} at the default threshold. */
    private static boolean above(Rect a, Rect b, double gapThresholdPx) {
        return a.centerY() < b.centerY()
                && Math.min(a.right(), b.right()) > Math.max(a.x(), b.x())
                && b.y() - a.bottom() <= gapThresholdPx;
    }

    /** One committed placement: the element's id, its anchor and its granted rect. */
    public record Placement(String elementId, FloatPos anchor, Rect rect) {

        public Placement {
            Objects.requireNonNull(elementId, "elementId");
            Objects.requireNonNull(anchor, "anchor");
            if (!Double.isFinite(anchor.x()) || !Double.isFinite(anchor.y())) {
                throw new IllegalArgumentException("anchor must be finite: " + anchor.x() + ", " + anchor.y());
            }
            Objects.requireNonNull(rect, "rect");
        }
    }

    /** One committed placement's leader segment, tagged with its owner. */
    public record Leader(String elementId, ZoneCost.Segment segment) {

        public Leader {
            Objects.requireNonNull(elementId, "elementId");
            Objects.requireNonNull(segment, "segment");
        }
    }
}
