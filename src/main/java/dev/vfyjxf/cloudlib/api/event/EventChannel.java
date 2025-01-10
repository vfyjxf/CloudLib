package dev.vfyjxf.cloudlib.api.event;


import dev.vfyjxf.cloudlib.event.EventChannelImpl;

import java.util.function.BooleanSupplier;

/**
 * The event channel is the main class to manage events.
 * It manages all event objects held by the corresponding EventHandler and provides the ability to register and post events.
 *
 * @param <T> the base type of the events
 */
public interface EventChannel<T> {

    static <T> EventChannel<T> create(EventHandler<T> handler) {
        return new EventChannelImpl<>(handler);
    }

    EventHandler<T> handler();

    default EventContext.Common common() {
        return new EventContext.Common(this);
    }

    default EventContext.Cancelable cancelable() {
        return new EventContext.Cancelable(this);
    }

    default EventContext.Interruptible interruptible() {
        return new EventContext.Interruptible(this);
    }

    default <E extends T> void register(EventDefinition<E> definition, E listener) {
        get(definition).register(listener);
    }

    /**
     * @param definition the event definition
     * @param listener   the listener to register
     * @param lifetime   the times the listener will be called
     * @param <E>        the type of the event
     */
    default <E extends T> void registerManaged(EventDefinition<E> definition, E listener, int lifetime) {
        get(definition).registerManaged(listener, lifetime);
    }

    /**
     * Register a listener managed by a condition
     * <p>
     * When the condition is true, the listener will be removed before the next event call
     *
     * @param definition the event definition
     * @param listener   the listener to register
     * @param condition  if true, the listener will be removed
     * @param <E>        the type of the event
     */
    default <E extends T> void registerManaged(EventDefinition<E> definition, E listener, BooleanSupplier condition) {
        get(definition).registerManaged(listener, condition);
    }

    default <E extends T> void unregister(EventDefinition<E> definition) {
        get(definition).clearListeners();
    }

    <E extends T> Event<E> get(EventDefinition<E> definition);

    void clearAllListeners();

    void checkEvent(Checker<T> checker);

    @FunctionalInterface
    interface Checker<T> {
        boolean check(Class<? extends T> type);
    }

}
