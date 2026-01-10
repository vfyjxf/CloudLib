package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A reactive state that is inherently unstable - its value may change at any time
 * without explicit updates.
 * <p>
 * Unlike {@link Signal} which requires explicit {@code set()} calls, Volatile states
 * represent values that are naturally changing, such as:
 * <ul>
 *   <li>Current time ({@code System.currentTimeMillis()})</li>
 *   <li>Mouse position (continuously changes)</li>
 *   <li>Random values</li>
 *   <li>External data sources that may change without notification</li>
 *   <li>Game tick counters</li>
 * </ul>
 * <p>
 * Volatile states are always considered "dirty" from the perspective of the reactive
 * system, which means any {@link Computed} depending on a Volatile will always
 * recompute when accessed.
 * <p>
 * Example:
 * <pre>{@code
 * // Current time - changes every read
 * Volatile<Long> currentTime = Volatile.of(System::currentTimeMillis);
 *
 * // Random value - different each time
 * Volatile<Double> random = Volatile.of(Math::random);
 *
 * // Polled external state with refresh interval
 * Volatile<Integer> externalData = Volatile.polled(
 *     () -> fetchFromServer(),
 *     Duration.ofSeconds(5)
 * );
 * }</pre>
 *
 * @param <T> the type of the state value
 * @see Signal for stable mutable state
 * @see Computed for derived state
 */
public non-sealed class Volatile<T> implements ReactiveState<T> {

    private final Supplier<T> supplier;
    private final boolean alwaysDirty;
    private final CopyOnWriteArrayList<SubscriptionImpl<T>> subscriptions = new CopyOnWriteArrayList<>();

    // For polled/cached variants
    @Nullable
    private T cachedValue;
    @Nullable
    private Long lastUpdateTime;
    @Nullable
    private final Duration refreshInterval;

    private long version = 0;
    private boolean manualDirty = false;

    private Volatile(Supplier<T> supplier, boolean alwaysDirty, @Nullable Duration refreshInterval) {
        this.supplier = supplier;
        this.alwaysDirty = alwaysDirty;
        this.refreshInterval = refreshInterval;
    }

    /**
     * Creates a volatile state that fetches a fresh value on every access.
     * <p>
     * Use this for values that inherently change on every read, like
     * {@code System.currentTimeMillis()} or {@code Math.random()}.
     *
     * @param supplier the function to get the current value
     * @param <T>      the type of the value
     * @return a new volatile state
     */
    public static <T> Volatile<T> of(Supplier<T> supplier) {
        Volatile<T> vol = new Volatile<>(supplier, true, null);
        StateCapture.register(vol);
        return vol;
    }

    /**
     * Creates a volatile state that caches the value but can be invalidated.
     * <p>
     * The cached value will be used until {@link #invalidate()} is called,
     * at which point the next access will fetch a fresh value.
     * <p>
     * This is useful for external data sources where you want to control
     * when refreshes happen.
     *
     * @param supplier the function to get the current value
     * @param <T>      the type of the value
     * @return a new volatile state with caching
     */
    public static <T> Volatile<T> cached(Supplier<T> supplier) {
        Volatile<T> vol = new Volatile<>(supplier, false, null);
        StateCapture.register(vol);
        return vol;
    }

    /**
     * Creates a volatile state that automatically refreshes at a fixed interval.
     * <p>
     * The value is cached and refreshed when:
     * <ul>
     *   <li>The refresh interval has elapsed since the last update</li>
     *   <li>{@link #invalidate()} is called manually</li>
     * </ul>
     *
     * @param supplier        the function to get the current value
     * @param refreshInterval the minimum time between refreshes
     * @param <T>             the type of the value
     * @return a new polled volatile state
     */
    public static <T> Volatile<T> polled(Supplier<T> supplier, Duration refreshInterval) {
        Volatile<T> vol = new Volatile<>(supplier, false, refreshInterval);
        StateCapture.register(vol);
        return vol;
    }

    /**
     * Creates a volatile state representing the current time in milliseconds.
     *
     * @return a volatile state for current time
     */
    public static Volatile<Long> currentTimeMillis() {
        return of(System::currentTimeMillis);
    }

    /**
     * Creates a volatile state representing the current nano time.
     *
     * @return a volatile state for nano time
     */
    public static Volatile<Long> nanoTime() {
        return of(System::nanoTime);
    }

    /**
     * Creates a volatile state for random values.
     *
     * @return a volatile state that returns a new random value each access
     */
    public static Volatile<Double> random() {
        return of(Math::random);
    }

    @Override
    public T get() {
        Tracker.capture(this);
        return computeValue();
    }

    @Override
    public T peek() {
        return computeValue();
    }

    private T computeValue() {
        // Always fetch if alwaysDirty
        if (alwaysDirty) {
            T newValue = supplier.get();
            notifyIfChanged(newValue);
            return newValue;
        }

        // Check if we need to refresh based on time interval
        if (refreshInterval != null && lastUpdateTime != null) {
            long elapsed = System.currentTimeMillis() - lastUpdateTime;
            if (elapsed >= refreshInterval.toMillis()) {
                cachedValue = null; // Force refresh
            }
        }

        // Use cached value if available
        if (cachedValue != null && !manualDirty) {
            return cachedValue;
        }

        // Fetch new value
        T oldValue = cachedValue;
        cachedValue = supplier.get();
        lastUpdateTime = System.currentTimeMillis();
        manualDirty = false;

        if (oldValue == null || !oldValue.equals(cachedValue)) {
            version++;
            notifySubscribers(oldValue, cachedValue);
        }

        return cachedValue;
    }

    private void notifyIfChanged(T newValue) {
        // For always-dirty states, we can't really track "changes"
        // since every read is fresh. We just increment version.
        version++;
    }

    /**
     * Invalidates the cached value, causing the next access to fetch fresh data.
     * <p>
     * This is primarily useful for {@link #cached(Supplier)} and {@link #polled(Supplier, Duration)}
     * variants. For {@link #of(Supplier)} variants, this has no effect as they
     * always fetch fresh values.
     */
    public void invalidate() {
        if (!alwaysDirty) {
            T oldValue = cachedValue;
            cachedValue = null;
            manualDirty = true;
            // Notify that invalidation happened
            if (oldValue != null) {
                // Force recomputation and notify
                T newValue = computeValue();
                if (!oldValue.equals(newValue)) {
                    notifySubscribers(oldValue, newValue);
                }
            }
        }
    }

    /**
     * Forces notification to all subscribers with the current value.
     * <p>
     * This is useful for volatile states that need to trigger updates
     * even when the underlying value might be the same.
     */
    public void pulse() {
        T current = computeValue();
        version++;
        notifySubscribers(current, current);
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

    /**
     * Volatile states are always considered dirty from the reactive system's perspective,
     * as their values may have changed without any explicit update.
     *
     * @return {@code true} if this is an always-dirty volatile, or if it has been invalidated
     */
    @Override
    public boolean isDirty() {
        return alwaysDirty || manualDirty;
    }

    @Override
    public void clearDirty() {
        // For always-dirty states, this is a no-op
        if (!alwaysDirty) {
            manualDirty = false;
        }
    }

    @Override
    public long getVersion() {
        return version;
    }

    /**
     * Volatile states are not stable - their values may change at any time.
     *
     * @return {@code false} always
     */
    @Override
    public boolean isStable() {
        return false;
    }

    /**
     * Returns whether this volatile state is inherently unstable.
     * <p>
     * Always-dirty states (created with {@link #of(Supplier)}) return fresh
     * values on every access and should be treated specially by the reactive
     * system.
     *
     * @return true if this state is always considered dirty
     */
    public boolean isAlwaysDirty() {
        return alwaysDirty;
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
        private final Volatile<T> volatile_;
        private final ChangeListener<T> listener;
        private volatile boolean active = true;

        SubscriptionImpl(Volatile<T> volatile_, ChangeListener<T> listener) {
            this.volatile_ = volatile_;
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
                volatile_.removeSubscription(this);
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    @Override
    public String toString() {
        if (alwaysDirty) {
            return "Volatile{alwaysDirty=true}";
        } else {
            return "Volatile{cached=" + cachedValue + ", refreshInterval=" + refreshInterval + "}";
        }
    }
}
