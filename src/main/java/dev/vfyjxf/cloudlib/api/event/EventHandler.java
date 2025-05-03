package dev.vfyjxf.cloudlib.api.event;


/**
 * The event handler.
 *
 * @param <T> The type of event this handler will handle
 */
public interface EventHandler<T> {

    EventChannel<T> events();

    default EventContext.Common common() {
        return events().common();
    }

    default EventContext.Cancelable cancelable() {
        return events().cancelable();
    }

    default EventContext.Interruptible interruptible() {
        return events().interruptible();
    }

    default <E extends T> E listeners(EventDefinition<E> definition) {
        return events().get(definition).invoker();
    }

    default <E extends T> E register(EventDefinition<E> definition, E listener) {
        return events().get(definition).register(listener);
    }

    default <E extends T> EventHandler<T> onEvent(EventDefinition<E> definition, E listener) {
        events().register(definition, listener);
        return this;
    }

    default <E extends T> EventHandler<T> when(EventDefinition<E> definition, E listener) {
        events().register(definition, listener);
        return this;
    }

    default <E extends T> void unregister(EventDefinition<E> definition, E listener) {
        events().get(definition).unregister(listener);
    }

    default <E extends T> void clear(EventDefinition<E> definition) {
        events().get(definition).clearListeners();
    }

    default void clearAll() {
        events().clearAllListeners();
    }

}
