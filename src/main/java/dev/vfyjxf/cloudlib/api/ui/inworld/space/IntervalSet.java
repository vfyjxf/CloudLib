package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import java.util.ArrayList;
import java.util.List;

/**
 * A mutable set of disjoint half-open intervals {@code [start, end)} over the
 * doubles — the algebra behind angular free-space queries. {@link #add}
 * unions (merging overlapping and adjacent intervals), {@link #subtract}
 * removes a span (splitting or trimming what it hits). The stored intervals
 * stay sorted by start and disjoint, so iteration order is deterministic.
 * <p>
 * Used with angles in {@code [0, 2π)}: callers subtract the arcs occupied by
 * occluders and read the complement as free angular intervals (see
 * {@link RayFan}, which also handles the wrap-around at {@code 2π}).
 */
public final class IntervalSet {

    /** A half-open interval {@code [start, end)}. */
    public record Interval(double start, double end) {
        public Interval {
            if (end < start) {
                throw new IllegalArgumentException("interval end before start: " + start + " > " + end);
            }
        }

        public double length() {
            return end - start;
        }

        public boolean contains(double value) {
            return value >= start && value < end;
        }
    }

    private final List<Interval> intervals = new ArrayList<>();

    /** An empty set. */
    public IntervalSet() {}

    /** A set holding the single interval {@code [start, end)}. */
    public static IntervalSet of(double start, double end) {
        IntervalSet set = new IntervalSet();
        set.add(start, end);
        return set;
    }

    /** Unions the interval {@code [start, end)} into the set. */
    public void add(double start, double end) {
        if (start >= end) {
            return;
        }
        List<Interval> merged = new ArrayList<>();
        Interval pending = new Interval(start, end);
        for (Interval existing : intervals) {
            if (existing.end() < pending.start()) {
                merged.add(existing);
            } else if (existing.start() > pending.end()) {
                merged.add(pending);
                pending = existing;
            } else {
                pending = new Interval(
                    Math.min(pending.start(), existing.start()),
                    Math.max(pending.end(), existing.end())
                );
            }
        }
        merged.add(pending);
        replaceAll(merged);
    }

    /** Removes the interval {@code [start, end)} from the set. */
    public void subtract(double start, double end) {
        if (start >= end) {
            return;
        }
        List<Interval> result = new ArrayList<>();
        for (Interval existing : intervals) {
            if (existing.end() <= start || existing.start() >= end) {
                result.add(existing);
                continue;
            }
            if (existing.start() < start) {
                result.add(new Interval(existing.start(), start));
            }
            if (existing.end() > end) {
                result.add(new Interval(end, existing.end()));
            }
        }
        replaceAll(result);
    }

    private void replaceAll(List<Interval> replacement) {
        intervals.clear();
        intervals.addAll(replacement);
    }

    /** Whether {@code value} falls inside any interval. */
    public boolean contains(double value) {
        for (Interval interval : intervals) {
            if (interval.contains(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return intervals.isEmpty();
    }

    /** Removes all intervals. */
    public void clear() {
        intervals.clear();
    }

    /** An immutable snapshot of the intervals, sorted by start. */
    public List<Interval> intervals() {
        return List.copyOf(intervals);
    }

    /** The total length of all intervals. */
    public double totalLength() {
        double total = 0;
        for (Interval interval : intervals) {
            total += interval.length();
        }
        return total;
    }

    /**
     * The complement of this set within {@code [lo, hi)}, as sorted
     * intervals. Does not coalesce across the {@code lo}/{@code hi} boundary —
     * wrap-aware callers do that themselves.
     */
    public List<Interval> complement(double lo, double hi) {
        if (hi <= lo) {
            return List.of();
        }
        List<Interval> free = new ArrayList<>();
        double cursor = lo;
        for (Interval interval : intervals) {
            if (interval.end() <= lo || interval.start() >= hi) {
                continue;
            }
            double start = Math.max(interval.start(), lo);
            if (start > cursor) {
                free.add(new Interval(cursor, start));
            }
            cursor = Math.max(cursor, interval.end());
            if (cursor >= hi) {
                return free;
            }
        }
        if (cursor < hi) {
            free.add(new Interval(cursor, hi));
        }
        return free;
    }
}
