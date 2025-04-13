package dev.vfyjxf.cloudlib.api.event;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.vfyjxf.cloudlib.api.performer.Performer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * The event object, which holds all listener and manage them.
 * <p>
 * <b>Difference with {@link Performer}:</b> {@link Performer}'s interface usually has multiple methods that need to be implemented,
 * while the majority of the objects held by the {@link Event} are objects of a functional interface.
 * <p>
 * {@link Event} will be handled by {@link EventHandler},but {@link Performer} will be handled by anything.
 *
 * @param <T> the invoker type,it <b>must</b> be a functional interface.
 */
@ApiStatus.NonExtendable
public interface Event<T> {

    static <T> Event<T> create(Function<List<T>, T> combiner) {
        return EventFactory.createEvent(combiner);
    }

    /**
     * @return the combined invoker of the event
     */
    T invoker();

    /**
     * Register a listener with default priority.
     *
     * @param listener register the listener
     * @return the listener
     */
    @CanIgnoreReturnValue
    default T register(T listener) {
        return register(listener, EventPriority.DEFAULT);
    }

    /**
     * @param listener the listener to register
     * @param priority the priority of the listener
     * @return the listener
     */
    @CanIgnoreReturnValue
    T register(T listener, @Range(from = 0, to = Integer.MAX_VALUE) int priority);

    /**
     * Register a listener which managed by the lifetime.
     *
     * @param listener the listener to register
     * @param lifetime the lifetime of the listener in invoke calls times
     * @return the wrapped listener,which can count the invoke times
     */
    @Contract("_, _ -> new")
    @CanIgnoreReturnValue
    T registerManaged(T listener, @Range(from = 1, to = Integer.MAX_VALUE) int lifetime);

    /**
     * Register a listener which lifetime managed by the condition.
     *
     * @param listener  the listener to register
     * @param condition the condition to check if the listener should be removed before next invoke
     * @return the listener
     */
    @CanIgnoreReturnValue
    T registerManaged(T listener, BooleanSupplier condition);

    /**
     * Register a listener which managed by the reference.
     * <p>
     * the listener will be removed when the reference is collected by the GC.
     *
     * @param listener  the listener to register
     * @param reference the reference to manage the listener
     * @return the listener
     */
    @CanIgnoreReturnValue
    T registerManaged(T listener, Object reference);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister.
     */
    void unregister(T listener);

    /**
     * @param listener the listener to check
     * @return if the listener is registered
     */
    boolean isRegistered(T listener);

    /**
     * Unregister all listener.
     */
    void clearListeners();

}
