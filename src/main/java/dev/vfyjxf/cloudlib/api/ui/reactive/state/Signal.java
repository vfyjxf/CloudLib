package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A mutable reactive state that can be read and written directly.
 * <p>
 * Signal is the primary way to hold mutable state in the reactive system.
 * When the value changes, all subscribers are notified and dependent
 * computed states are invalidated.
 * <p>
 * Example:
 * <pre>{@code
 * Signal<Integer> count = Signal.of(0);
 *
 * // Read the value
 * int current = count.get();
 *
 * // Set a new value
 * count.set(current + 1);
 *
 * // Update based on current value
 * count.update(n -> n + 1);
 * }</pre>
 *
 * @param <T> the type of the state value
 */
public non-sealed class Signal<T> implements ReactiveState<T> {

    private T value;
    private boolean dirty = false;
    private long version = 0;
    private final CopyOnWriteArrayList<SubscriptionImpl<T>> subscriptions = new CopyOnWriteArrayList<>();

    private Signal(T initialValue) {
        this.value = initialValue;
    }

    /**
     * Creates a new signal with the given initial value.
     *
     * @param initialValue the initial value
     * @param <T>          the type of the value
     * @return a new signal
     */
    public static <T> Signal<T> of(T initialValue) {
        Signal<T> signal = new Signal<>(initialValue);
        StateCapture.register(signal);
        return signal;
    }

    /**
     * Creates a new signal with null as the initial value.
     *
     * @param <T> the type of the value
     * @return a new signal with null value
     */
    public static <T> Signal<T> empty() {
        Signal<T> signal = new Signal<>(null);
        StateCapture.register(signal);
        return signal;
    }

    @Override
    public T get() {
        // Single unified tracking point for all reactive systems
        Tracker.capture(this);
        return value;
    }

    @Override
    public T peek() {
        return value;
    }

    /**
     * Sets a new value and notifies all subscribers if the value changed.
     *
     * @param newValue the new value
     * @return the old value
     */
    public T set(T newValue) {
        T oldValue = this.value;
        if (!Objects.equals(oldValue, newValue)) {
            this.value = newValue;
            this.dirty = true;
            this.version++;
            notifySubscribers(oldValue, newValue);
        }
        return oldValue;
    }

    /**
     * Updates the value using a transformation function.
     *
     * @param updater the function to transform the current value
     * @return the new value
     */
    public T update(UnaryOperator<T> updater) {
        T newValue = updater.apply(this.value);
        set(newValue);
        return newValue;
    }

    /**
     * Silently sets the value without notifying subscribers.
     * <p>
     * Use with caution - this can lead to inconsistent UI state.
     *
     * @param newValue the new value
     */
    public void setSilent(T newValue) {
        this.value = newValue;
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
        return version;
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
        private final Signal<T> signal;
        private final ChangeListener<T> listener;
        private volatile boolean active = true;

        SubscriptionImpl(Signal<T> signal, ChangeListener<T> listener) {
            this.signal = signal;
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
                signal.removeSubscription(this);
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    @Override
    public String toString() {
        return "Signal{value=" + value + ", dirty=" + dirty + "}";
    }
}
