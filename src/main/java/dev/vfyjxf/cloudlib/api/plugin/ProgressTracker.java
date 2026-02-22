package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

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
        Checks.checkArgument(weight > 0, "weight must be positive, got: %s", weight);
        var phase = new Phase(weight);
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

    //endregion

    //region internal

    private void notifyListener() {
        var l = listener;
        if (l != null) l.accept(this);
    }

    private final class Phase implements DispatchProgress {

        final float weight;
        volatile int total;
        final AtomicInteger completed = new AtomicInteger();

        Phase(float weight) {
            this.weight = weight;
        }

        @Override
        public void begin(int totalSteps) {
            this.total = totalSteps;
        }

        @Override
        public void advance(Namespace pluginId, int completed, int total) {
            this.completed.incrementAndGet();
            notifyListener();
        }

        @Override
        public void complete() {
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
    }

    //endregion

}
