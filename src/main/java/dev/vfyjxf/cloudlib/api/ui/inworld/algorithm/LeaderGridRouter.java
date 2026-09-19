package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The candidate-grid A* behind every orthogonal (po) leader: one collision-free,
 * bend-penalized polyline from a feature anchor to a panel port, around
 * rectangle obstacles, in bounded microsecond time.
 * <p>
 * <strong>The grid.</strong> For ≤10 leaders and a handful of rectangle
 * obstacles the search space collapses to a few hundred candidate points: the
 * x/y coordinates of both stub ends plus every obstacle edge inflated by
 * {@link Config#clearancePx} — the non-uniform grid on which an orthogonal
 * shortest path's bends provably lie. Three obstacles make an ~8×8 grid; the
 * A* over (node, entry-direction) states carries integer costs, expands
 * neighbors in a fixed order and breaks queue ties by insertion sequence, so
 * equal-cost paths resolve identically on every run and every machine.
 * <p>
 * <strong>The cost.</strong> Manhattan length + a {@link Config#bendPenaltyPx}
 * (24 px) surcharge per turn + an {@link Config#edgePenaltyPx} (6 px) surcharge
 * per segment hugging an inflated obstacle boundary. The bend penalty is the
 * difference between a tidy one-fold line and the staircase a bare shortest
 * path finder produces — a 5 px equivalent (the diagramming-tool default)
 * buys exactly the ugly paths this replaces.
 * <p>
 * <strong>The frozen ends.</strong> The first segment leaves the anchor along
 * the caller's axis direction for at least {@link Config#stubPx} (the
 * out-stub) and the last segment enters the port along the port's outward
 * normal for exactly {@link Config#lastSegmentPx}, stopping
 * {@link Config#arrivalGapPx} short of the border — the leader never touches
 * the label, the endpoint marker does. The A* runs between the two stub ends;
 * no bend may occur at either. The one exemption is the port's own panel
 * along that final approach: the port sits on its border by construction, so
 * only that one forced segment may enter its inflated margin.
 * <p>
 * <strong>Lane centering.</strong> After the search, each fully interior
 * segment slides to the midpoint of the free corridor it runs beside — the
 * middle of the aisle, not its wall — whenever the slide keeps the whole
 * polyline collision-free. Sliding an interior segment never changes the
 * path's Manhattan length.
 * <p>
 * Pure and static: same inputs, same output list, no wall clock, no
 * randomness. {@link #route} returns {@code null} when no collision-free
 * orthogonal path exists — the caller falls back to a simple elbow rather
 * than drawing nothing.
 */
public final class LeaderGridRouter {

    /**
     * The routing knobs, in px.
     *
     * @param bendPenaltyPx the cost of one 90° turn, in px-equivalents;
     *        non-negative
     * @param edgePenaltyPx the cost of one segment hugging an inflated
     *        obstacle boundary; non-negative
     * @param clearancePx the obstacle inflation — lines keep this distance
     *        from every panel; non-negative
     * @param stubPx the minimum straight run off the anchor before the first
     *        bend; positive
     * @param lastSegmentPx the exact length of the final approach segment into
     *        the port; positive
     * @param arrivalGapPx the gap left between the polyline's last point and
     *        the port (the endpoint marker's slot); non-negative
     */
    public record Config(
            double bendPenaltyPx,
            double edgePenaltyPx,
            double clearancePx,
            double stubPx,
            double lastSegmentPx,
            double arrivalGapPx) {

        public Config {
            if (!Double.isFinite(bendPenaltyPx) || bendPenaltyPx < 0) {
                throw new IllegalArgumentException("bendPenaltyPx must be finite and non-negative: " + bendPenaltyPx);
            }
            if (!Double.isFinite(edgePenaltyPx) || edgePenaltyPx < 0) {
                throw new IllegalArgumentException("edgePenaltyPx must be finite and non-negative: " + edgePenaltyPx);
            }
            if (!Double.isFinite(clearancePx) || clearancePx < 0) {
                throw new IllegalArgumentException("clearancePx must be finite and non-negative: " + clearancePx);
            }
            if (!Double.isFinite(stubPx) || stubPx <= 0) {
                throw new IllegalArgumentException("stubPx must be finite and positive: " + stubPx);
            }
            if (!Double.isFinite(lastSegmentPx) || lastSegmentPx <= 0) {
                throw new IllegalArgumentException("lastSegmentPx must be finite and positive: " + lastSegmentPx);
            }
            if (!Double.isFinite(arrivalGapPx) || arrivalGapPx < 0) {
                throw new IllegalArgumentException("arrivalGapPx must be finite and non-negative: " + arrivalGapPx);
            }
        }

        /** The defaults: {@code 24, 6, 12, 16, 20, 6}. */
        public static Config ofDefaults() {
            return new Config(24.0, 6.0, 12.0, 16.0, 20.0, 6.0);
        }
    }

    private static final double epsilon = 1.0e-9;

    /** Integer cost scale: costs are carried in tenths of a px. */
    private static final int costScale = 10;

    // neighbor expansion order: +x, +y, −x, −y — fixed for determinism
    private static final int[] moveDx = {1, 0, -1, 0};
    private static final int[] moveDy = {0, 1, 0, -1};

    private LeaderGridRouter() {}

    /**
     * Routes one orthogonal polyline from {@code anchor} to {@code port}.
     *
     * @param dirX the frozen first-segment direction (axis unit, off the
     *        anchor), x component
     * @param dirY the frozen first-segment direction, y component
     * @param port the attachment point on the target panel's border
     * @param normalX the port's outward normal (axis unit); the final segment
     *        arrives along −normal, x component
     * @param normalY the port's outward normal, y component
     * @param obstacles the rects to avoid (other panels, HUD areas), in the
     *        caller's canonical order
     * @return the polyline anchor → … → port+gap, or {@code null} when no
     *         collision-free path exists on this grid
     */
    public static List<FloatPos> route(
            FloatPos anchor,
            double dirX,
            double dirY,
            FloatPos port,
            double normalX,
            double normalY,
            List<FloatRect> obstacles,
            Config config) {
        requireAxisUnit(dirX, dirY, "dir");
        requireAxisUnit(normalX, normalY, "normal");
        List<double[]> inflated = new ArrayList<>(obstacles.size());
        List<Integer> ownIndices = new ArrayList<>();
        for (int k = 0; k < obstacles.size(); k++) {
            FloatRect rect = obstacles.get(k);
            inflated.add(new double[] {
                rect.x() - config.clearancePx(),
                rect.y() - config.clearancePx(),
                rect.x() + rect.width() + config.clearancePx(),
                rect.y() + rect.height() + config.clearancePx()
            });
            if (portOnBorder(port, rect)) {
                ownIndices.add(k);
            }
        }
        List<double[]> ownInflated = new ArrayList<>();
        for (int k : ownIndices) {
            ownInflated.add(inflated.get(k));
        }

        FloatPos drawnEnd =
                new FloatPos(port.x() + normalX * config.arrivalGapPx(), port.y() + normalY * config.arrivalGapPx());
        FloatPos stubEnd = new FloatPos(anchor.x() + dirX * config.stubPx(), anchor.y() + dirY * config.stubPx());
        FloatPos approach = new FloatPos(
                drawnEnd.x() + normalX * config.lastSegmentPx(), drawnEnd.y() + normalY * config.lastSegmentPx());

        // the two forced segments: the anchor stub clears everything, the
        // port approach may enter its own panel's inflated margin only
        if (segmentBlocked(anchor, stubEnd, inflated, null)
                || segmentBlocked(approach, drawnEnd, inflated, ownInflated)) {
            return null;
        }

        double[] gx = gridCoords(stubEnd.x(), approach.x(), inflated, true);
        double[] gy = gridCoords(stubEnd.y(), approach.y(), inflated, false);
        int w = gx.length;
        int h = gy.length;
        int startNode = nodeOf(gx, gy, stubEnd);
        int goalNode = nodeOf(gx, gy, approach);
        int startDir = dirCode(dirX, dirY);
        int goalDir = dirCode(-normalX, -normalY);

        boolean[] nodeBlocked = new boolean[w * h];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                nodeBlocked[j * w + i] = pointBlocked(gx[i], gy[j], inflated);
            }
        }

        int states = w * h * 4;
        int[] g = new int[states];
        int[] parent = new int[states];
        Arrays.fill(g, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);
        boolean[] closed = new boolean[states];

        int start = startNode * 4 + startDir;
        int goal = goalNode * 4 + goalDir;
        List<FloatPos> bends = new ArrayList<>();
        if (start != goal) {
            int goalFound = search(start, goal, gx, gy, nodeBlocked, inflated, g, parent, closed, config);
            if (goalFound < 0) {
                return null;
            }
            bends = bendPoints(goalFound, parent, gx, gy);
        }

        List<FloatPos> points = new ArrayList<>();
        points.add(anchor);
        points.add(stubEnd);
        points.addAll(bends);
        points.add(approach);
        points.add(drawnEnd);
        return centerLanes(mergeCollinear(points), inflated);
    }

    /**
     * Whether any straight segment of the polyline crosses any obstacle
     * interior inflated by {@code clearance}; the port's own panel is exempt
     * along the final segment only, mirroring {@link #route}'s rule.
     */
    public static boolean polylineBlocked(
            List<FloatPos> points, FloatPos port, List<FloatRect> obstacles, double clearance) {
        List<double[]> inflated = new ArrayList<>(obstacles.size());
        List<double[]> own = new ArrayList<>();
        for (FloatRect rect : obstacles) {
            double[] bounds = new double[] {
                rect.x() - clearance,
                rect.y() - clearance,
                rect.x() + rect.width() + clearance,
                rect.y() + rect.height() + clearance
            };
            inflated.add(bounds);
            if (portOnBorder(port, rect)) {
                own.add(bounds);
            }
        }
        for (int i = 0; i + 1 < points.size(); i++) {
            List<double[]> exemption = i + 2 == points.size() ? own : null;
            if (segmentBlocked(points.get(i), points.get(i + 1), inflated, exemption)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the straight segment {@code a→b} crosses any inflated obstacle
     * interior (touching a boundary is not crossing). {@code exempt} bounds
     * are skipped entirely.
     */
    public static boolean segmentBlocked(FloatPos a, FloatPos b, List<double[]> inflated, List<double[]> exempt) {
        for (double[] rect : inflated) {
            if (exempt != null && containsBounds(exempt, rect)) continue;
            if (segmentIntersectsRect(a, b, rect)) {
                return true;
            }
        }
        return false;
    }

    // region search

    private record Entry(int cost, int seq, int state) implements Comparable<Entry> {

        @Override
        public int compareTo(Entry other) {
            int byCost = Integer.compare(cost, other.cost);
            return byCost != 0 ? byCost : Integer.compare(seq, other.seq);
        }
    }

    /** The A* itself; returns the goal state index, or −1 when unreachable. */
    private static int search(
            int start,
            int goal,
            double[] gx,
            double[] gy,
            boolean[] nodeBlocked,
            List<double[]> inflated,
            int[] g,
            int[] parent,
            boolean[] closed,
            Config config) {
        int w = gx.length;
        int goalNode = goal / 4;
        int startNode = start / 4;
        int startDir = start % 4;
        int bend = toCost(config.bendPenaltyPx());
        g[start] = 0;
        PriorityQueue<Entry> open = new PriorityQueue<>();
        int seq = 0;
        open.add(new Entry(manhattan(gx, gy, startNode, goalNode), seq++, start));
        while (!open.isEmpty()) {
            Entry entry = open.poll();
            int state = entry.state();
            if (closed[state]) continue;
            closed[state] = true;
            if (state == goal) {
                return state;
            }
            int node = state / 4;
            int inDir = state % 4;
            int i = node % w;
            int j = node / w;
            for (int move = 0; move < 4; move++) {
                if (move == opposite(inDir)) continue;
                if (state == start && move != startDir) continue; // no bend on the stub
                int ni = i + moveDx[move];
                int nj = j + moveDy[move];
                if (ni < 0 || ni >= w || nj < 0 || nj >= gy.length) continue;
                int nextNode = nj * w + ni;
                if (nodeBlocked[nextNode]) continue;
                int step = edgeCost(i, j, ni, nj, gx, gy, inflated, config);
                if (step < 0) continue;
                if (move != inDir) step += bend;
                int nextState = nextNode * 4 + move;
                if (closed[nextState]) continue;
                int nextG = g[state] + step;
                if (nextG < g[nextState]) {
                    g[nextState] = nextG;
                    parent[nextState] = state;
                    open.add(new Entry(nextG + manhattan(gx, gy, nextNode, goalNode), seq++, nextState));
                }
            }
        }
        return -1;
    }

    /**
     * The bend points of the found path, in travel order: a node where the
     * outgoing move direction differs from the entry direction.
     */
    private static List<FloatPos> bendPoints(int goalState, int[] parent, double[] gx, double[] gy) {
        int w = gx.length;
        List<Integer> chain = new ArrayList<>();
        for (int state = goalState; state != -1; state = parent[state]) {
            chain.add(state);
        }
        // chain is goal → start; node k (entry dir dir_k) is a bend when the
        // move leaving it — the entry dir of state k−1 in the chain — differs
        List<FloatPos> bends = new ArrayList<>();
        for (int k = chain.size() - 1; k >= 1; k--) {
            int state = chain.get(k);
            int leaving = chain.get(k - 1) % 4;
            if (state % 4 != leaving) {
                int node = state / 4;
                bends.add(new FloatPos(gx[node % w], gy[node / w]));
            }
        }
        return bends;
    }

    // endregion

    // region cost model

    /**
     * The cost of the grid edge (i,j)→(ni,nj) in integer cost units, or −1
     * when the segment crosses an inflated obstacle interior. Running along
     * an inflated boundary adds the hug penalty per hugged rect. The overlap
     * that decides is the one along the segment: the x-span for a horizontal
     * run, the y-span for a vertical one — the perpendicular extent is
     * always degenerate and is covered by the strictly-inside span test.
     */
    private static int edgeCost(
            int i, int j, int ni, int nj, double[] gx, double[] gy, List<double[]> inflated, Config config) {
        boolean horizontal = j == nj;
        double x0 = Math.min(gx[i], gx[ni]);
        double x1 = Math.max(gx[i], gx[ni]);
        double y0 = Math.min(gy[j], gy[nj]);
        double y1 = Math.max(gy[j], gy[nj]);
        double length = horizontal ? x1 - x0 : y1 - y0;
        int hug = 0;
        for (double[] rect : inflated) {
            boolean spanOpen = horizontal
                    ? rect[1] + epsilon < y0 && y0 < rect[3] - epsilon
                    : rect[0] + epsilon < x0 && x0 < rect[2] - epsilon;
            boolean overlap = horizontal
                    ? Math.max(x0, rect[0]) < Math.min(x1, rect[2]) - epsilon
                    : Math.max(y0, rect[1]) < Math.min(y1, rect[3]) - epsilon;
            if (spanOpen && overlap) {
                return -1;
            }
            boolean onBoundary = horizontal
                    ? Math.abs(y0 - rect[1]) < epsilon || Math.abs(y0 - rect[3]) < epsilon
                    : Math.abs(x0 - rect[0]) < epsilon || Math.abs(x0 - rect[2]) < epsilon;
            if (onBoundary && overlap) {
                hug++;
            }
        }
        return toCost(length) + hug * toCost(config.edgePenaltyPx());
    }

    private static int toCost(double px) {
        return (int) Math.round(px * costScale);
    }

    private static int manhattan(double[] gx, double[] gy, int from, int to) {
        double dx = Math.abs(gx[to % gx.length] - gx[from % gx.length]);
        double dy = Math.abs(gy[to / gx.length] - gy[from / gx.length]);
        return (int) Math.round((dx + dy) * costScale);
    }

    // endregion

    // region geometry

    private static boolean segmentIntersectsRect(FloatPos a, FloatPos b, double[] rect) {
        double rx0 = rect[0], ry0 = rect[1], rx1 = rect[2], ry1 = rect[3];
        boolean aInside =
                a.x() > rx0 + epsilon && a.x() < rx1 - epsilon && a.y() > ry0 + epsilon && a.y() < ry1 - epsilon;
        boolean bInside =
                b.x() > rx0 + epsilon && b.x() < rx1 - epsilon && b.y() > ry0 + epsilon && b.y() < ry1 - epsilon;
        if (aInside || bInside) return true;
        return segCrossesEdge(a, b, rx0, ry0, rx1, ry0)
                || segCrossesEdge(a, b, rx1, ry0, rx1, ry1)
                || segCrossesEdge(a, b, rx1, ry1, rx0, ry1)
                || segCrossesEdge(a, b, rx0, ry1, rx0, ry0);
    }

    private static boolean segCrossesEdge(FloatPos a, FloatPos b, double ex0, double ey0, double ex1, double ey1) {
        double d1 = cross(ex1 - ex0, ey1 - ey0, a.x() - ex0, a.y() - ey0);
        double d2 = cross(ex1 - ex0, ey1 - ey0, b.x() - ex0, b.y() - ey0);
        double d3 = cross(b.x() - a.x(), b.y() - a.y(), ex0 - a.x(), ey0 - a.y());
        double d4 = cross(b.x() - a.x(), b.y() - a.y(), ex1 - a.x(), ey1 - a.y());
        return ((d1 > epsilon && d2 < -epsilon) || (d1 < -epsilon && d2 > epsilon))
                && ((d3 > epsilon && d4 < -epsilon) || (d3 < -epsilon && d4 > epsilon));
    }

    private static double cross(double ax, double ay, double bx, double by) {
        return ax * by - ay * bx;
    }

    private static boolean pointBlocked(double x, double y, List<double[]> inflated) {
        for (double[] rect : inflated) {
            if (x > rect[0] + epsilon && x < rect[2] - epsilon && y > rect[1] + epsilon && y < rect[3] - epsilon) {
                return true;
            }
        }
        return false;
    }

    private static boolean portOnBorder(FloatPos port, FloatRect rect) {
        boolean within = port.x() >= rect.x() - epsilon
                && port.x() <= rect.x() + rect.width() + epsilon
                && port.y() >= rect.y() - epsilon
                && port.y() <= rect.y() + rect.height() + epsilon;
        boolean onBorder = Math.abs(port.x() - rect.x()) < epsilon
                || Math.abs(port.x() - (rect.x() + rect.width())) < epsilon
                || Math.abs(port.y() - rect.y()) < epsilon
                || Math.abs(port.y() - (rect.y() + rect.height())) < epsilon;
        return within && onBorder;
    }

    private static boolean containsBounds(List<double[]> bounds, double[] candidate) {
        for (double[] rect : bounds) {
            if (rect[0] == candidate[0]
                    && rect[1] == candidate[1]
                    && rect[2] == candidate[2]
                    && rect[3] == candidate[3]) {
                return true;
            }
        }
        return false;
    }

    private static double[] gridCoords(double a, double b, List<double[]> inflated, boolean xAxis) {
        List<Double> coords = new ArrayList<>(inflated.size() * 2 + 2);
        coords.add(a);
        coords.add(b);
        for (double[] rect : inflated) {
            coords.add(xAxis ? rect[0] : rect[1]);
            coords.add(xAxis ? rect[2] : rect[3]);
        }
        double[] sorted = new double[coords.size()];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = coords.get(i);
        }
        Arrays.sort(sorted);
        int distinct = 0;
        for (int i = 0; i < sorted.length; i++) {
            if (i == 0 || sorted[i] - sorted[distinct - 1] > epsilon) {
                sorted[distinct++] = sorted[i];
            }
        }
        return Arrays.copyOf(sorted, distinct);
    }

    private static int nodeOf(double[] gx, double[] gy, FloatPos point) {
        int i = indexOf(gx, point.x());
        int j = indexOf(gy, point.y());
        if (i < 0 || j < 0) {
            throw new IllegalStateException("endpoint coordinate missing from the candidate grid: " + point);
        }
        return j * gx.length + i;
    }

    private static int indexOf(double[] coords, double value) {
        for (int i = 0; i < coords.length; i++) {
            if (Math.abs(coords[i] - value) < epsilon) {
                return i;
            }
        }
        return -1;
    }

    private static int dirCode(double dx, double dy) {
        if (dx > 0.5) return 0;
        if (dy > 0.5) return 1;
        if (dx < -0.5) return 2;
        return 3;
    }

    private static int opposite(int dir) {
        return (dir + 2) % 4;
    }

    private static void requireAxisUnit(double x, double y, String name) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || Math.abs(Math.abs(x) + Math.abs(y) - 1.0) > 1.0e-6) {
            throw new IllegalArgumentException(name + " must be an axis unit vector: (" + x + ", " + y + ")");
        }
    }

    // endregion

    // region assembly & lane centering

    private static List<FloatPos> mergeCollinear(List<FloatPos> points) {
        List<FloatPos> merged = new ArrayList<>();
        for (FloatPos point : points) {
            if (!merged.isEmpty()
                    && Math.abs(merged.get(merged.size() - 1).x() - point.x()) < epsilon
                    && Math.abs(merged.get(merged.size() - 1).y() - point.y()) < epsilon) {
                continue;
            }
            merged.add(point);
        }
        for (int i = 1; i < merged.size() - 1; ) {
            FloatPos a = merged.get(i - 1);
            FloatPos q = merged.get(i);
            FloatPos b = merged.get(i + 1);
            double cross = (q.x() - a.x()) * (b.y() - q.y()) - (q.y() - a.y()) * (b.x() - q.x());
            double dot = (q.x() - a.x()) * (b.x() - q.x()) + (q.y() - a.y()) * (b.y() - q.y());
            if (Math.abs(cross) < epsilon && dot > 0) {
                merged.remove(i);
            } else {
                i++;
            }
        }
        return merged;
    }

    /**
     * Lane centering: every fully interior segment (bends on both sides) whose
     * perpendicular corridor is bounded by obstacles on both sides slides to
     * that corridor's midpoint when the whole polyline stays collision-free.
     */
    private static List<FloatPos> centerLanes(List<FloatPos> points, List<double[]> inflated) {
        if (points.size() < 5 || inflated.isEmpty()) {
            return points;
        }
        List<FloatPos> out = new ArrayList<>(points);
        for (int run = 1; run + 1 < out.size() - 1; run++) {
            FloatPos a = out.get(run);
            FloatPos b = out.get(run + 1);
            boolean vertical = Math.abs(a.x() - b.x()) < epsilon;
            double s0 = vertical ? Math.min(a.y(), b.y()) : Math.min(a.x(), b.x());
            double s1 = vertical ? Math.max(a.y(), b.y()) : Math.max(a.x(), b.x());
            double current = vertical ? a.x() : a.y();
            double lo = Double.NEGATIVE_INFINITY;
            double hi = Double.POSITIVE_INFINITY;
            for (double[] rect : inflated) {
                boolean beside = vertical
                        ? rect[1] < s1 - epsilon && rect[3] > s0 + epsilon
                        : rect[0] < s1 - epsilon && rect[2] > s0 + epsilon;
                if (!beside) continue;
                double lowBound = vertical ? rect[0] : rect[1];
                double highBound = vertical ? rect[2] : rect[3];
                if (highBound <= current + epsilon) {
                    lo = Math.max(lo, highBound);
                }
                if (lowBound >= current - epsilon) {
                    hi = Math.min(hi, lowBound);
                }
            }
            if (!Double.isFinite(lo) || !Double.isFinite(hi) || hi - lo < epsilon) {
                continue;
            }
            double mid = (lo + hi) * 0.5;
            if (Math.abs(mid - current) < 0.5) continue;
            List<FloatPos> slid = new ArrayList<>(out);
            slid.set(run, vertical ? new FloatPos(mid, a.y()) : new FloatPos(a.x(), mid));
            slid.set(run + 1, vertical ? new FloatPos(mid, b.y()) : new FloatPos(b.x(), mid));
            if (!polylineCrosses(slid, inflated)) {
                out = slid;
            }
        }
        return out;
    }

    private static boolean polylineCrosses(List<FloatPos> points, List<double[]> inflated) {
        for (int i = 0; i + 1 < points.size(); i++) {
            for (double[] rect : inflated) {
                if (segmentIntersectsRect(points.get(i), points.get(i + 1), rect)) {
                    return true;
                }
            }
        }
        return false;
    }

    // endregion
}
