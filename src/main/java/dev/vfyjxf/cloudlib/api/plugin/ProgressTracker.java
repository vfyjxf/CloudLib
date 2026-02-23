package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Tracks dispatch progress across weighted phases and computes an overall percentage.
 * <pre>{@code
 * var tracker = new ProgressTracker();
 * tracker.withListener(t -> updateUI(t.percentage()));
 *
 * dispatcher.dispatch(contentEvent,  tracker.phase(30));
 * dispatcher.dispatch(recipeEvent,   tracker.phase(50));
 * dispatcher.dispatch(storageEvent,  tracker.phase(20));
 * // percentage() goes 0 → 100 across all three phases
 * }</pre>
 * Thread-safe: multiple phases may advance concurrently.
 */
public final class ProgressTracker {

    private final MutableList<Phase> phases = MutableLists.empty();
    private volatile float totalWeight;
    private @Nullable Consumer<ProgressTracker> listener;

    //region configuration

    /**
     * Creates a {@link DispatchProgress} that contributes {@code weight} to the overall percentage.
     */
    public DispatchProgress phase(float weight) {
        return phase(null, weight);
    }

    /**
     * Creates a named {@link DispatchProgress} with equal weight (1). The name appears in {@link #logTimings}.
     */
    public DispatchProgress phase(String name) {
        return phase(name, 1);
    }

    /**
     * Creates a named {@link DispatchProgress} with timing. The name appears in {@link #logTimings}.
     */
    public DispatchProgress phase(@Nullable String name, float weight) {
        Checks.checkArgument(weight > 0, "weight must be positive, got: %s", weight);
        var phase = new Phase(name, weight);
        synchronized (phases) {
            phases.add(phase);
            totalWeight += weight;
        }
        return phase;
    }

    /**
     * Registers a listener invoked on every progress advance.
     */
    public ProgressTracker withListener(Consumer<ProgressTracker> listener) {
        this.listener = listener;
        return this;
    }

    //endregion

    //region query

    /**
     * Overall progress as a percentage in [0, 100].
     */
    public float percentage() {
        float tw = totalWeight;
        if (tw == 0) return 0;
        float sum = 0;
        synchronized (phases) {
            for (var phase : phases) {
                sum += phase.localProgress() * phase.weight;
            }
        }
        return Math.min(100.0f, sum / tw * 100.0f);
    }

    public boolean isComplete() {
        synchronized (phases) {
            return phases.notEmpty() && phases.allSatisfy(Phase::isDone);
        }
    }

    public int phaseCount() {
        synchronized (phases) {
            return phases.size();
        }
    }

    /**
     * Returns timing information for all completed phases.
     */
    public ImmutableList<PhaseTiming> timings() {
        synchronized (phases) {
            return phases.collect(Phase::toTiming).toImmutable();
        }
    }

    /**
     * Total wall-clock time across all phases in milliseconds.
     */
    public long totalElapsedMs() {
        synchronized (phases) {
            long total = 0;
            for (var phase : phases) {
                total += phase.elapsedMs();
            }
            return total;
        }
    }

    /**
     * Logs timing for each phase and the total.
     */
    public void logTimings(Logger logger) {
        logTimings(logger, null);
    }

    /**
     * Logs a formatted summary with cumulative percentage, per-phase timing and an optional title.
     * <pre>
     * ──────────────────── Loading ───────────────────────
     *   [  3%] Content Types                          12ms
     *   [ 51%] Recipe Systems                        156ms
     *   [100%] Storage Types                          89ms
     * ──────────────────── 3 phases in 257ms ─────────────
     * </pre>
     */
    public void logTimings(Logger logger, @Nullable String title) {
        synchronized (phases) {
            if (phases.isEmpty()) return;

            int lineWidth = 58;

            // header
            if (title != null) {
                int contentLen = title.length() + 2; // " title "
                int remaining = Math.max(0, lineWidth - contentLen);
                int left = remaining / 2;
                int right = remaining - left;
                logger.info("{}", "─".repeat(left) + " " + title + " " + "─".repeat(right));
            }

            // column widths
            int maxNameLen = 0;
            long maxMs = 0;
            for (int i = 0; i < phases.size(); i++) {
                maxNameLen = Math.max(maxNameLen, nameOf(i).length());
                maxMs = Math.max(maxMs, phases.get(i).elapsedMs());
            }
            int msWidth = Long.toString(maxMs).length();

            // phase rows
            float cumulativeWeight = 0;
            float tw = totalWeight;
            for (int i = 0; i < phases.size(); i++) {
                var phase = phases.get(i);
                cumulativeWeight += phase.weight;
                int pct = tw > 0 ? Math.round(cumulativeWeight / tw * 100f) : 0;
                logger.info("{}", String.format("  [%3d%%] %-" + maxNameLen + "s  %" + msWidth + "dms",
                        pct, nameOf(i), phase.elapsedMs()));
            }

            // footer
            var summary = String.format("%d phases in %dms", phases.size(), totalElapsedMs());
            int pad = Math.max(3, (lineWidth - summary.length() - 2) / 2);
            logger.info("{}", "─".repeat(pad) + " " + summary + " " + "─".repeat(pad));
        }
    }

    private String nameOf(int index) {
        var name = phases.get(index).name;
        return name != null ? name : "phase-" + index;
    }

    //endregion

    //region internal

    private void notifyListener() {
        var l = listener;
        if (l != null) l.accept(this);
    }

    private final class Phase implements DispatchProgress {

        final @Nullable String name;
        final float weight;
        volatile int total;
        final AtomicInteger completed = new AtomicInteger();
        volatile long startNanos;
        volatile long endNanos;

        Phase(@Nullable String name, float weight) {
            this.name = name;
            this.weight = weight;
        }

        @Override
        public void begin(int totalSteps) {
            this.total = totalSteps;
            this.startNanos = System.nanoTime();
        }

        @Override
        public void advance(Namespace pluginId, int completed, int total) {
            this.completed.incrementAndGet();
            notifyListener();
        }

        @Override
        public void complete() {
            this.endNanos = System.nanoTime();
            completed.set(total);
            notifyListener();
        }

        float localProgress() {
            int t = total;
            return t == 0 ? 0 : Math.min(1.0f, (float) completed.get() / t);
        }

        boolean isDone() {
            int t = total;
            return t > 0 && completed.get() >= t;
        }

        long elapsedMs() {
            long s = startNanos, e = endNanos;
            if (s == 0) return 0;
            long end = e != 0 ? e : System.nanoTime();
            return (end - s) / 1_000_000;
        }

        PhaseTiming toTiming() {
            return new PhaseTiming(name, elapsedMs(), isDone());
        }
    }

    /**
     * Timing snapshot for a single phase.
     */
    public record PhaseTiming(@Nullable String name, long elapsedMs, boolean complete) {
    }

    //endregion

}
