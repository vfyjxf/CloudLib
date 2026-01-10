package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A derived reactive state that computes its value from other states.
 * <p>
 * Computed states are lazily evaluated and cached. When any dependency
 * changes, the computed value is invalidated and will be recomputed
 * on the next access.
 * <p>
 * Example:
 * <pre>{@code
 * Signal<String> firstName = Signal.of("John");
 * Signal<String> lastName = Signal.of("Doe");
 *
 * Computed<String> fullName = Computed.of(() ->
 *     firstName.get() + " " + lastName.get()
 * );
 *
 * // fullName automatically updates when firstName or lastName changes
 * }</pre>
 *
 * @param <T> the type of the computed value
 */
public non-sealed class Computed<T> implements ReactiveState<T> {

    private final Supplier<T> computation;
    private T cachedValue;
    private boolean valid = false;
    private boolean dirty = false;
    private final Set<ReactiveState<?>> dependencies = new HashSet<>();
    private final Set<Subscription> dependencySubscriptions = new HashSet<>();
    private final CopyOnWriteArrayList<SubscriptionImpl<T>> subscriptions = new CopyOnWriteArrayList<>();
    private long version = 0;

    private Computed(Supplier<T> computation) {
        this.computation = computation;
    }

    /**
     * Creates a new computed state with the given computation.
     * <p>
     * The computation function should access other reactive states
     * using their {@code get()} method, which will automatically
     * track dependencies.
     *
     * @param computation the function to compute the value
     * @param <T>         the type of the computed value
     * @return a new computed state
     */
    public static <T> Computed<T> of(Supplier<T> computation) {
        return new Computed<>(computation);
    }

    /**
     * Creates a computed state that only emits distinct consecutive values.
     *
     * @param source the source state to observe
     * @param <T>    the type of the value
     * @return a new computed state that filters duplicate values
     */
    public static <T> Computed<T> distinct(ReactiveState<T> source) {
        return new DistinctComputed<>(source);
    }

    @Override
    public T get() {
        // Track this computed as a dependency of the caller
        Tracker.capture(this);
        return computeIfNeeded();
    }

    @Override
    public T peek() {
        return computeIfNeeded();
    }

    private T computeIfNeeded() {
        // If we have volatile dependencies, always recompute
        if (valid && hasVolatileDependency()) {
            valid = false;
        }
        if (!valid) {
            recompute();
        }
        return cachedValue;
    }

    /**
     * Checks if any dependency is volatile (always-dirty).
     */
    private boolean hasVolatileDependency() {
        for (ReactiveState<?> dep : dependencies) {
            if (dep instanceof Volatile<?> vol && vol.isAlwaysDirty()) {
                return true;
            }
        }
        return false;
    }

    private void recompute() {
        // Clear old dependency subscriptions
        for (var sub : dependencySubscriptions) {
            sub.unsubscribe();
        }
        dependencySubscriptions.clear();
        dependencies.clear();

        // Track dependencies during computation
        T oldValue = cachedValue;
        try (var scope = Tracker.start()) {
            cachedValue = computation.get();
            valid = true;
            dirty = !Objects.equals(oldValue, cachedValue);

            // Subscribe to all captured dependencies
            for (ReactiveState<?> dep : scope.captured()) {
                dependencies.add(dep);
                var sub = dep.subscribe(v -> invalidate());
                dependencySubscriptions.add(sub);
            }
        }

        // Notify if value changed and increment version
        if (dirty) {
            version++;
            notifySubscribers(oldValue, cachedValue);
        }
    }

    /**
     * Invalidates the cached value, causing recomputation on next access.
     */
    public void invalidate() {
        if (valid) {
            valid = false;
            dirty = true;
        }
    }

    @Override
    public Subscription subscribe(Consumer<T> listener) {
        SubscriptionImpl<T> sub = new SubscriptionImpl<>(this, (old, newVal) -> listener.accept(newVal));
        subscriptions.add(sub);
        return sub;
    }

    @Override
    public Subscription subscribeWithOld(ChangeListener<T> listener) {
        SubscriptionImpl<T> sub = new SubscriptionImpl<>(this, listener);
        subscriptions.add(sub);
        return sub;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void clearDirty() {
        this.dirty = false;
    }

    @Override
    public long getVersion() {
        // Must trigger recompute to get accurate version
        // Otherwise version won't update when dependencies change
        computeIfNeeded();
        return version;
    }

    /**
     * Returns whether this computed state is stable.
     * <p>
     * A computed state is stable if and only if all of its dependencies are stable.
     * If any dependency is volatile, this computed state is also volatile.
     *
     * @return true if all dependencies are stable
     */
    @Override
    public boolean isStable() {
        // Must compute to track dependencies
        computeIfNeeded();
        // Stable if all dependencies are stable
        for (ReactiveState<?> dep : dependencies) {
            if (!dep.isStable()) {
                return false;
            }
        }
        return true;
    }

    private void notifySubscribers(@Nullable T oldValue, @NotNull T newValue) {
        for (SubscriptionImpl<T> sub : subscriptions) {
            if (sub.isActive()) {
                sub.notify(oldValue, newValue);
            }
        }
    }

    void removeSubscription(SubscriptionImpl<T> sub) {
        subscriptions.remove(sub);
    }

    private static class SubscriptionImpl<T> implements Subscription {
        private final Computed<T> computed;
        private final ChangeListener<T> listener;
        private volatile boolean active = true;

        SubscriptionImpl(Computed<T> computed, ChangeListener<T> listener) {
            this.computed = computed;
            this.listener = listener;
        }

        void notify(@Nullable T oldValue, @NotNull T newValue) {
            if (active) {
                listener.onChange(oldValue, newValue);
            }
        }

        @Override
        public void unsubscribe() {
            if (active) {
                active = false;
                computed.removeSubscription(this);
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    /**
     * A computed state that filters out duplicate consecutive values.
     */
    private static class DistinctComputed<T> extends Computed<T> {
        private T lastEmittedValue;
        private boolean hasEmitted = false;

        DistinctComputed(ReactiveState<T> source) {
            super(source::get);
        }

        @Override
        public T get() {
            T value = super.get();
            if (!hasEmitted || !Objects.equals(lastEmittedValue, value)) {
                lastEmittedValue = value;
                hasEmitted = true;
            }
            return value;
        }
    }

    @Override
    public String toString() {
        return "Computed{valid=" + valid + ", cached=" + cachedValue + ", dirty=" + dirty + "}";
    }
}
