package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Core reactive state interface representing an observable value.
 * <p>
 * ReactiveState is the foundation of the reactive UI system. When a state's value
 * changes, all subscribed elements will be notified and potentially rebuilt.
 * <p>
 * There are two main types of reactive state:
 * <ul>
 *   <li>{@link Signal} - A mutable state that can be read and written directly</li>
 *   <li>{@link Computed} - A derived state computed from other states</li>
 * </ul>
 *
 * @param <T> the type of the state value
 * @see Signal
 * @see Computed
 */
public sealed interface ReactiveState<T> permits Signal, Computed {

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
