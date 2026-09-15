package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Contact-coordinate free-slot search: candidate top-left positions are
 * generated along obstacle edges and the viewport rails, filtered by
 * prepared SAT masks, then diversified — the farthest-point selection
 * keeps spatially distinct options rather than 24 near-identical slots.
 */
final class FreeSpace {

    private FreeSpace() {}

    /**
     * Per-call workspaces, grown on demand and reused across {@link #slots}
     * calls — the solver allocates megabytes of short-lived arrays per
     * frame otherwise. Solving is single-threaded (render thread), so a
     * thread-local scratch is safe.
     */
    private static final class Scratch {
        double[] cand = new double[512];
        double[] dist = new double[512];
        double[] hits = new double[1024];
        double[] values = new double[512];
        double[] xs = new double[128];
        double[] ys = new double[128];
        double[] px = new double[1024];
        double[] py = new double[1024];
        double[] pdist = new double[1024];
        double[] pNearest = new double[1024];
        int[] order = new int[1024];
        int[] rank = new int[512];
        int[] selected = new int[64];
        boolean[] used = new boolean[1024];

        @SuppressWarnings("unchecked")
        final List<ScreenObstacle>[] columns = new List[64];

        Scratch() {
            for (int i = 0; i < columns.length; i++) columns[i] = new ArrayList<>();
        }

        static double[] grow(double[] a, int need) {
            return a.length >= need ? a : new double[Math.max(need, a.length * 2)];
        }

        static int[] grow(int[] a, int need) {
            return a.length >= need ? a : new int[Math.max(need, a.length * 2)];
        }

        static boolean[] grow(boolean[] a, int need) {
            return a.length >= need ? a : new boolean[Math.max(need, a.length * 2)];
        }
    }

    private static final ThreadLocal<Scratch> scratch = ThreadLocal.withInitial(Scratch::new);

    /**
     * Up to {@code limit} candidate rects of {@code width} × {@code height}
     * that fit inside the viewport margins and clear {@code obstacles},
     * ordered by distance to {@code preferred} with diversity.
     */
    static List<Map.Entry<String, GuiRect>> slots(
            LayoutFrame frame, double width, double height, GuiVec preferred, List<List<GuiVec>> obstacles, int limit) {
        double margin = frame.config().margin();
        double gap = frame.config().gap();
        double right = frame.camera().width() - margin - width;
        double bottom = frame.camera().height() - margin - height;
        if (right < margin || bottom < margin) return List.of();
        double preferredX = preferred.x() - width * .5;
        double preferredY = preferred.y() - height * .5;
        int maxPanels = frame.config().maxPanels();
        // Seed axes are generated for the 4px bucket containing the
        // rect — validity filtering below still uses the real size, so
        // quantization only shifts which edge lines get proposed.
        Axes axes =
                axes(frame, obstacles, Math.ceil(width / 4) * 4, Math.ceil(height / 4) * 4, gap, margin, right, bottom);
        // The preferred point joins the cached seeds as one more seed —
        // its lattice expansion is enumerated by nearest() on demand.
        Scratch s = scratch.get();
        s.xs = Scratch.grow(s.xs, 64);
        s.ys = Scratch.grow(s.ys, 64);
        int xCount = nearest(axes.x, Math.max(margin, Math.min(right, preferredX)), preferredX, maxPanels, 64, s.xs, s);
        int yCount =
                nearest(axes.y, Math.max(margin, Math.min(bottom, preferredY)), preferredY, maxPanels, 64, s.ys, s);
        double[] xValues = s.xs, yValues = s.ys;
        ScreenObstacle[] masks = masks(frame, obstacles);
        // Column prefilter: a rect at x can only overlap masks whose
        // x-range meets [x, x + width + gap] — masks sorted by left so
        // a column binary-searches the prefix that can still reach it.
        for (int xi = 0; xi < xCount; xi++) {
            double x = xValues[xi];
            double bound = x + width + gap - 1e-7;
            int lo = 0, hi = masks.length;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (masks[mid].left < bound) lo = mid + 1;
                else hi = mid;
            }
            List<ScreenObstacle> column = s.columns[xi];
            column.clear();
            for (int k = 0; k < lo; k++) {
                if (masks[k].right + gap > x + 1e-7) column.add(masks[k]);
            }
        }
        s.px = Scratch.grow(s.px, xCount * yCount);
        s.py = Scratch.grow(s.py, xCount * yCount);
        s.pdist = Scratch.grow(s.pdist, xCount * yCount);
        int count = 0;
        for (int xi = 0; xi < xCount; xi++) {
            double x = xValues[xi];
            List<ScreenObstacle> column = s.columns[xi];
            for (int yi = 0; yi < yCount; yi++) {
                double y = yValues[yi];
                boolean blocked = false;
                for (int k = 0; k < column.size(); k++) {
                    if (column.get(k).overlaps(x, y, width, height, gap)) {
                        blocked = true;
                        break;
                    }
                }
                if (!blocked) {
                    s.px[count] = x;
                    s.py[count] = y;
                    s.pdist[count] = squared(x - preferredX, y - preferredY);
                    count++;
                }
            }
        }
        s.order = Scratch.grow(s.order, count);
        for (int i = 0; i < count; i++) s.order[i] = i;
        sortByDistance(s.order, s.px, s.py, s.pdist, count);
        int selectedCount = Math.min(count, limit);
        s.selected = Scratch.grow(s.selected, selectedCount);
        s.used = Scratch.grow(s.used, count);
        Arrays.fill(s.used, 0, count, false);
        s.pNearest = Scratch.grow(s.pNearest, count);
        Arrays.fill(s.pNearest, 0, count, Double.POSITIVE_INFINITY);
        int initial = count <= limit ? count : limit / 2;
        for (int i = 0; i < initial; i++) {
            int index = s.order[i];
            s.selected[i] = index;
            s.used[index] = true;
            if (count > limit) update(s, count, index);
        }
        for (int n = initial; n < selectedCount; n++) {
            int best = -1;
            double distance = -1;
            for (int i = 0; i < count; i++) {
                if (!s.used[i] && s.pNearest[i] > distance) {
                    best = i;
                    distance = s.pNearest[i];
                }
            }
            s.selected[n] = best;
            s.used[best] = true;
            update(s, count, best);
        }
        List<Map.Entry<String, GuiRect>> result = new ArrayList<>(selectedCount);
        for (int n = 0; n < selectedCount; n++) {
            int index = s.selected[n];
            result.add(
                    Map.entry(keyFor(s.px[index], s.py[index]), new GuiRect(s.px[index], s.py[index], width, height)));
        }
        return result;
    }

    /**
     * Prepared masks per obstacle list, sorted by left edge — keyed by
     * identity inside a frame-weak map: the list is a frame-scoped
     * {@code FrameSilhouettes} product shared by every {@link #slots}
     * call of the frame, so the parse+sort runs once per frame.
     */
    private static ScreenObstacle[] masks(LayoutFrame frame, List<List<GuiVec>> obstacles) {
        Map<List<List<GuiVec>>, ScreenObstacle[]> byList =
                maskCache.computeIfAbsent(frame, f -> new IdentityHashMap<>());
        ScreenObstacle[] cached = byList.get(obstacles);
        if (cached != null) return cached;
        ScreenObstacle[] masks = new ScreenObstacle[obstacles.size()];
        for (int i = 0; i < masks.length; i++) masks[i] = new ScreenObstacle(obstacles.get(i));
        Arrays.sort(masks, Comparator.comparingDouble(m -> m.left));
        byList.put(obstacles, masks);
        return masks;
    }

    private static final Map<LayoutFrame, Map<List<List<GuiVec>>, ScreenObstacle[]>> maskCache = new WeakHashMap<>();

    private static double squared(double x, double y) {
        return x * x + y * y;
    }

    /** Nearest-selected distance update for the farthest-point diversification. */
    private static void update(Scratch s, int count, int sel) {
        for (int i = 0; i < count; i++) {
            s.pNearest[i] = Math.min(s.pNearest[i], squared(s.px[i] - s.px[sel], s.py[i] - s.py[sel]));
        }
    }

    /** Quicksort on an index array driven by per-index keys — no boxing. */
    private interface IndexCompare {
        int compare(int a, int b);
    }

    private static void indexSort(int[] order, int lo, int hi, IndexCompare cmp) {
        while (lo < hi) {
            int i = lo, j = hi;
            int pivot = order[(lo + hi) >>> 1];
            while (i <= j) {
                while (cmp.compare(order[i], pivot) < 0) i++;
                while (cmp.compare(order[j], pivot) > 0) j--;
                if (i <= j) {
                    int t = order[i];
                    order[i] = order[j];
                    order[j] = t;
                    i++;
                    j--;
                }
            }
            // Recurse into the smaller side, loop on the larger — bounded stack.
            if (j - lo < hi - i) {
                if (lo < j) indexSort(order, lo, j, cmp);
                lo = i;
            } else {
                if (i < hi) indexSort(order, i, hi, cmp);
                hi = j;
            }
        }
    }

    private static void sortByDistance(int[] order, double[] x, double[] y, double[] dist, int n) {
        indexSort(order, 0, n - 1, (a, b) -> {
            int c = Double.compare(dist[a], dist[b]);
            if (c != 0) return c;
            c = Double.compare(x[a], x[b]);
            return c != 0 ? c : Double.compare(y[a], y[b]);
        });
    }

    private static void sortByRank(int[] order, double[] values, double origin, int n) {
        indexSort(order, 0, n - 1, (a, b) -> compareRank(values[a], values[b], origin));
    }

    private static String keyFor(double x, double y) {
        return "free:"
                + Long.toHexString(Double.doubleToLongBits(x))
                + ":"
                + Long.toHexString(Double.doubleToLongBits(y));
    }

    private static void add(List<Double> values, double value, double min, double max) {
        if (value >= min && value <= max) values.add(value);
    }

    /** Sort to a primitive array and collapse duplicates — the old set semantics. */
    private static double[] dedup(List<Double> values) {
        double[] sorted = new double[values.size()];
        for (int i = 0; i < sorted.length; i++) sorted[i] = values.get(i);
        Arrays.sort(sorted);
        int n = 0;
        for (int i = 0; i < sorted.length; i++) {
            if (n == 0 || sorted[i] != sorted[n - 1]) sorted[n++] = sorted[i];
        }
        return Arrays.copyOf(sorted, n);
    }

    /**
     * One axis of candidate lines: the deduplicated edge seeds plus the
     * pack lattice each seed expands on ({@code seed ± k·step}, bounded
     * by {@code packLimit} steps and by [min, max]). The packed values
     * are never materialized — {@link #nearest} enumerates only the
     * lattice points that can reach the top-{@code limit} selection.
     */
    private record Axis(double[] seeds, double step, double min, double max) {}

    private record Axes(Axis x, Axis y) {}

    private record Dim(double w, double h) {}

    /**
     * Sorted unique edge seeds on both axes for one (frame, obstacles,
     * panel-size) triple — built once per unique size instead of once
     * per {@link #slots} call. Pack expansion is deferred to
     * {@link #nearest} so this stays a small sort, not a 30k-element one.
     */
    private static Axes axes(
            LayoutFrame frame,
            List<List<GuiVec>> obstacles,
            double width,
            double height,
            double gap,
            double margin,
            double right,
            double bottom) {
        Map<List<List<GuiVec>>, Map<Dim, Axes>> byList = axesCache.computeIfAbsent(frame, f -> new IdentityHashMap<>());
        Map<Dim, Axes> byDim = byList.computeIfAbsent(obstacles, l -> new HashMap<>());
        return byDim.computeIfAbsent(new Dim(width, height), dim -> {
            ScreenObstacle[] masks = masks(frame, obstacles);
            List<Double> xe = new ArrayList<>();
            List<Double> ye = new ArrayList<>();
            xe.add(margin);
            xe.add(right);
            ye.add(margin);
            ye.add(bottom);
            for (ScreenObstacle mask : masks) {
                add(xe, mask.left - gap - width, margin, right);
                add(xe, mask.right + gap, margin, right);
                add(ye, mask.top - gap - height, margin, bottom);
                add(ye, mask.bottom + gap, margin, bottom);
            }
            return new Axes(
                    new Axis(dedup(xe), width + gap, margin, right), new Axis(dedup(ye), height + gap, margin, bottom));
        });
    }

    private static final Map<LayoutFrame, Map<List<List<GuiVec>>, Map<Dim, Axes>>> axesCache = new WeakHashMap<>();

    /**
     * The {@code limit} union values (seeds plus each seed's lattice
     * {@code seed ± k·step}, |k| ≤ {@code packLimit}, inside bounds)
     * nearest {@code origin} by (|v − origin|, v), with the union's
     * global extremes appended — same result as sorting the full union,
     * computed without materializing it:
     *
     * <p>Phase 1 scores only each lattice's nearest points to {@code origin},
     * giving a distance bound the true top-{@code limit} must satisfy;
     * phase 2 enumerates just the lattice points inside that bound.
     */
    private static int nearest(
            Axis axis, double extraSeed, double origin, int packLimit, int limit, double[] out, Scratch s) {
        double[] seeds = axis.seeds();
        double step = axis.step();
        double min = axis.min();
        double max = axis.max();
        s.cand = Scratch.grow(s.cand, 2 * (seeds.length + 1));
        int m = 0;
        double unionLo = Double.POSITIVE_INFINITY, unionHi = Double.NEGATIVE_INFINITY;
        for (int i = -1; i < seeds.length; i++) {
            double seed = i < 0 ? extraSeed : seeds[i];
            long kLo = Math.max(-packLimit, (long) Math.ceil((min - seed) / step - 1e-9));
            long kHi = Math.min(packLimit, (long) Math.floor((max - seed) / step + 1e-9));
            if (kLo > kHi) continue;
            double vLo = seed + kLo * step, vHi = seed + kHi * step;
            if (vLo < unionLo) unionLo = vLo;
            if (vHi > unionHi) unionHi = vHi;
            long k0 = Math.max(kLo, Math.min(kHi, (long) Math.round((origin - seed) / step)));
            s.cand[m++] = seed + k0 * step;
            // The lattice points straddling origin are its two closest —
            // both go in so the distance bound stays tight.
            double frac = (origin - seed) / step - k0;
            long k1 = k0 + (frac > 0 ? 1 : -1);
            if (k1 >= kLo && k1 <= kHi) s.cand[m++] = seed + k1 * step;
        }
        // Distance threshold: the limit-th best score among the
        // per-lattice nearest candidates bounds the true top-limit —
        // found by selection, not a sort.
        double bound = Double.POSITIVE_INFINITY;
        if (m >= limit) {
            s.dist = Scratch.grow(s.dist, m);
            for (int i = 0; i < m; i++) s.dist[i] = Math.abs(s.cand[i] - origin);
            bound = select(s.dist, m, limit - 1);
        }
        // Phase 2: every lattice point within the bound.
        int h = 0;
        for (int i = -1; i < seeds.length; i++) {
            double seed = i < 0 ? extraSeed : seeds[i];
            long kLo = Math.max(-packLimit, (long) Math.ceil((min - seed) / step - 1e-9));
            long kHi = Math.min(packLimit, (long) Math.floor((max - seed) / step + 1e-9));
            if (kLo > kHi) continue;
            long a = Math.max(kLo, (long) Math.ceil((origin - bound - seed) / step - 1e-9));
            long b = Math.min(kHi, (long) Math.floor((origin + bound - seed) / step + 1e-9));
            for (long k = a; k <= b; k++) {
                if (h == s.hits.length) s.hits = Scratch.grow(s.hits, h + 1);
                s.hits[h++] = seed + k * step;
            }
        }
        int n = 0;
        if (h <= 4 * limit) {
            // Small enough to dedupe exactly: sorted uniques — all of
            // them when at most limit, else the best limit-2 by rank.
            Arrays.sort(s.hits, 0, h);
            s.values = Scratch.grow(s.values, h);
            int u = 0;
            for (int i = 0; i < h; i++) {
                if (u == 0 || s.hits[i] != s.values[u - 1]) s.values[u++] = s.hits[i];
            }
            if (u <= limit) {
                for (int i = 0; i < u; i++) out[n++] = s.values[i];
                return n;
            }
            s.rank = Scratch.grow(s.rank, u);
            for (int i = 0; i < u; i++) s.rank[i] = i;
            sortByRank(s.rank, s.values, origin, u);
            for (int i = 0; i < limit - 2; i++) out[n++] = s.values[s.rank[i]];
            out[n++] = unionLo;
            out[n++] = unionHi;
            return n;
        }
        // Large hit set: selection instead of a sort. Duplicate lattice
        // values can survive here, but with this many hits the unique
        // count far exceeds limit — the same trade the sorted path makes.
        selectByRank(s.hits, h, limit - 2, origin);
        s.rank = Scratch.grow(s.rank, limit - 2);
        for (int i = 0; i < limit - 2; i++) s.rank[i] = i;
        sortByRank(s.rank, s.hits, origin, limit - 2);
        double last = Double.NaN;
        for (int i = 0; i < limit - 2; i++) {
            double v = s.hits[s.rank[i]];
            if (i == 0 || v != last) out[n++] = v;
            last = v;
        }
        out[n++] = unionLo;
        out[n++] = unionHi;
        return n;
    }

    /** The k-th smallest of the first {@code n} values — quickselect with three-way partitioning. */
    private static double select(double[] a, int n, int k) {
        int lo = 0, hi = n - 1;
        while (lo < hi) {
            double pivot = a[(lo + hi) >>> 1];
            int lt = lo, i = lo, gt = hi;
            while (i <= gt) {
                if (a[i] < pivot) {
                    double t = a[lt];
                    a[lt++] = a[i];
                    a[i++] = t;
                } else if (a[i] > pivot) {
                    double t = a[i];
                    a[i] = a[gt];
                    a[gt--] = t;
                } else {
                    i++;
                }
            }
            if (k < lt) hi = lt - 1;
            else if (k > gt) lo = gt + 1;
            else return pivot;
        }
        return a[lo];
    }

    /** Selects so the first {@code k} values are the best by (|v − origin|, v). */
    private static void selectByRank(double[] a, int n, int k, double origin) {
        int lo = 0, hi = n - 1;
        while (lo < hi) {
            double pivot = a[(lo + hi) >>> 1];
            int lt = lo, i = lo, gt = hi;
            while (i <= gt) {
                int c = compareRank(a[i], pivot, origin);
                if (c < 0) {
                    double t = a[lt];
                    a[lt++] = a[i];
                    a[i++] = t;
                } else if (c > 0) {
                    double t = a[i];
                    a[i] = a[gt];
                    a[gt--] = t;
                } else {
                    i++;
                }
            }
            if (k < lt) hi = lt - 1;
            else if (k > gt) lo = gt + 1;
            else break;
        }
    }

    private static int compareRank(double a, double b, double origin) {
        int c = Double.compare(Math.abs(a - origin), Math.abs(b - origin));
        return c != 0 ? c : Double.compare(a, b);
    }

    /** The stable slot key for a rect — used to recognize a kept position across frames. */
    static String key(GuiRect rect) {
        return keyFor(rect.x(), rect.y());
    }
}
