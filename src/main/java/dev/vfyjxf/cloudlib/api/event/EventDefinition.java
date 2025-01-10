package dev.vfyjxf.cloudlib.api.event;

/**
 * The definition of an event.
 * <p>
 * It defines how multiple listeners collaborate with each other and aggregates multiple values into a single value,
 * and it also determines the semantics of interrupts.
 *
 * @param <T> the type of the event
 */
public interface EventDefinition<T> {

    /**
     * @return The type of the event.Normally, it is an interface.
     */
    Class<T> type();

    /**
     * @return The event instance.
     */
    Event<T> create();

    /**
     * @return The global event instance.
     */
    Event<T> global();

    /**
     * @return The invoker of the global event.
     */
    default T invoker() {
        return global().actor();
    }

    /**
     * @param listener The listener to be registered.
     * @return The listener registered.
     */
    default T register(T listener) {
        return global().register(listener);
    }

    /**
     * @param listener The listener to be unregistered.
     */
    default void unregister(T listener) {
        global().unregister(listener);
    }

    /**
     * @param listener The listener to be checked.
     * @return Whether the listener is registered.
     */
    default boolean isRegistered(T listener) {
        return global().isRegistered(listener);
    }

    /**
     * Unregister all listeners.
     */
    default void unregisterAll() {
        global().clearListeners();
    }

}
