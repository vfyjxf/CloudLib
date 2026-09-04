package dev.vfyjxf.cloudlib.api.event;

/**
 * The definition of an event.
 * <p>
 * It defines how multiple listeners collaborate with each other and aggregates multiple values into a single value,
 * and it also determines the semantics of interrupts.
 *
 * @param <T> the type of the event
 */
public sealed interface EventDefinition<T> permits Events.EventDefinitionImpl {

    /**
     * @return The type of the event.Normally, it is an interface.
     */
    Class<T> type();

    /**
     * @return The event instance.
     */
    Event<T> create();

    /**
     * @return get a default event instance.
     */
    Event<T> defaultEvent();

    /**
     * @return The invoker of the global event.
     */
    default T invoker() {
        return defaultEvent().invoker();
    }

    /**
     * @param listener The listener to be registered.
     * @return The listener registered.
     */
    default T register(T listener) {
        return defaultEvent().register(listener);
    }

    /**
     * @param listener The listener to be unregistered.
     */
    default void unregister(T listener) {
        defaultEvent().unregister(listener);
    }

    /**
     * @param listener The listener to be checked.
     * @return Whether the listener is registered.
     */
    default boolean isRegistered(T listener) {
        return defaultEvent().isRegistered(listener);
    }

    /**
     * Unregister all listeners.
     */
    default void unregisterAll() {
        defaultEvent().clearListeners();
    }

}
