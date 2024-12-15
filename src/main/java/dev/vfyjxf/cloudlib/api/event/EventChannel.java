package dev.vfyjxf.cloudlib.api.event;


import dev.vfyjxf.cloudlib.event.EventChannelImpl;

import java.util.function.BooleanSupplier;

public interface EventChannel<T> {

    static <T> EventChannel<T> create(EventHandler<T> handler) {
        return new EventChannelImpl<>(handler);
    }

    EventHandler<T> handler();

    default EventContext.Common context() {
        return new EventContext.Common(this);
    }

    default EventContext.Cancelable cancelable() {
        return new EventContext.Cancelable(this);
    }

    default EventContext.Interruptible interruptible() {
        return new EventContext.Interruptible(this);
    }

    void register(Object listenerOwner);

    default <E extends T> void register(EventDefinition<E> definition, E listener) {
        get(definition).register(listener);
    }

    default <E extends T> void registerManaged(EventDefinition<E> definition, E listener, int lifetime) {
        get(definition).registerManaged(listener, lifetime);
    }

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
