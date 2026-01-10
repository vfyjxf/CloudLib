package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Core reactive state interface representing an observable value.
 * <p>
 * ReactiveState is the foundation of the reactive UI system. When a state's value
 * changes, all subscribed elements will be notified and potentially rebuilt.
 * <p>
 * There are three main types of reactive state:
 * <ul>
 *   <li>{@link Signal} - A mutable state that can be read and written directly (stable)</li>
 *   <li>{@link Computed} - A derived state computed from other states (stability depends on sources)</li>
 *   <li>{@link Volatile} - An inherently unstable state that may change at any time</li>
 * </ul>
 * <p>
 * <b>Stability:</b>
 * <ul>
 *   <li>Stable states only change when explicitly updated (Signal.set())</li>
 *   <li>Unstable/volatile states may return different values on each read</li>
 *   <li>Computed states inherit stability from their dependencies</li>
 * </ul>
 *
 * @param <T> the type of the state value
 * @see Signal
 * @see Computed
 * @see Volatile
 */
public sealed interface ReactiveState<T> permits Signal, Computed, Volatile {

    /**
     * Gets the current value of this state.
     * <p>
     * When called within a reactive context (e.g., during widget build),
     * this automatically subscribes the current scope to this state.
     *
     * @return the current value
     */
    T get();

    /**
     * Gets the current value without tracking dependencies.
     * <p>
     * Unlike {@link #get()}, this method does not subscribe the current
     * scope to this state, making it useful for side effects or one-time reads.
     *
     * @return the current value
     */
    T peek();

    /**
     * Subscribes to changes of this state.
     *
     * @param listener the callback to invoke when the value changes
     * @return a subscription that can be used to unsubscribe
     */
    Subscription subscribe(Consumer<T> listener);

    /**
     * Subscribes to changes with access to both old and new values.
     *
     * @param listener the callback receiving (oldValue, newValue)
     * @return a subscription that can be used to unsubscribe
     */
    Subscription subscribeWithOld(ChangeListener<T> listener);

    /**
     * Maps this state to a new state with a transformed value.
     * <p>
     * The resulting state is a {@link Computed} that automatically
     * updates when this state changes.
     *
     * @param mapper the transformation function
     * @param <R>    the type of the mapped value
     * @return a new computed state
     */
    default <R> Computed<R> map(Function<T, R> mapper) {
        return Computed.of(() -> mapper.apply(this.get()));
    }

    /**
     * Combines this state with another state.
     *
     * @param other    the other state to combine with
     * @param combiner the function to combine both values
     * @param <U>      the type of the other state
     * @param <R>      the type of the combined result
     * @return a new computed state representing the combination
     */
    default <U, R> Computed<R> combine(ReactiveState<U> other, CombineFunction<T, U, R> combiner) {
        return Computed.of(() -> combiner.apply(this.get(), other.get()));
    }

    /**
     * Creates a derived state that only emits distinct consecutive values.
     *
     * @return a new state that filters out duplicate consecutive values
     */
    default Computed<T> distinct() {
        return Computed.distinct(this);
    }

    /**
     * Checks if this state has been modified since last check.
     * <p>
     * This is primarily used internally by the reactive system to determine
     * which elements need to be rebuilt.
     *
     * @return true if the state has changed
     */
    @ApiStatus.Internal
    boolean isDirty();

    /**
     * Clears the dirty flag.
     * <p>
     * Called after the reactive system has processed all updates.
     */
    @ApiStatus.Internal
    void clearDirty();
    
    /**
     * Gets the current version number of this state.
     * <p>
     * The version is incremented each time the value changes.
     * This allows multiple observers to independently track changes
     * without interfering with each other (unlike dirty flags).
     *
     * @return the current version number
     */
    @ApiStatus.Internal
    default long getVersion() {
        return 0; // Default implementation for backwards compatibility
    }

    /**
     * Returns whether this state is stable.
     * <p>
     * Stable states only change when explicitly updated (e.g., Signal.set()).
     * Unstable/volatile states may return different values on each read.
     * <p>
     * This information is used by the reactive system to optimize updates:
     * <ul>
     *   <li>Stable states can be safely cached</li>
     *   <li>Unstable states require recomputation on every access</li>
     * </ul>
     *
     * @return true if this state is stable, false if it may change at any time
     */
    default boolean isStable() {
        return true; // Default: most states are stable
    }

    /**
     * Returns whether this state is inherently volatile.
     * <p>
     * Volatile states are those that may change without any notification,
     * such as {@code System.currentTimeMillis()} or external data sources.
     * <p>
     * This is the inverse of {@link #isStable()}.
     *
     * @return true if this state is volatile
     */
    default boolean isVolatile() {
        return !isStable();
    }

    /**
     * Listener interface for state changes.
     *
     * @param <T> the type of the state value
     */
    @FunctionalInterface
    interface ChangeListener<T> {
        void onChange(@Nullable T oldValue, @NotNull T newValue);
    }

    /**
     * Function interface for combining two values.
     *
     * @param <T> first value type
     * @param <U> second value type
     * @param <R> result type
     */
    @FunctionalInterface
    interface CombineFunction<T, U, R> {
        R apply(T t, U u);
    }

    /**
     * Subscription handle that can be used to unsubscribe from a state.
     */
    interface Subscription {
        /**
         * Stops receiving updates from the state.
         */
        void unsubscribe();

        /**
         * Checks if this subscription is still active.
         *
         * @return true if still subscribed
         */
        boolean isActive();
    }
}
