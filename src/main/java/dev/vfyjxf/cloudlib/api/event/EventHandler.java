package dev.vfyjxf.cloudlib.api.event;


import dev.vfyjxf.cloudlib.api.event.context.BubbleContext;
import dev.vfyjxf.cloudlib.api.event.context.CancelableContext;
import dev.vfyjxf.cloudlib.api.event.context.CommonContext;
import dev.vfyjxf.cloudlib.api.event.context.InterruptibleContext;

/**
 * The event handler.
 *
 * @param <T> The type of event this handler will handle
 */
public interface EventHandler<T> {

    EventChannel<T> events();

    default CommonContext common() {
        return events().common();
    }

    default CancelableContext cancelable() {
        return events().cancelable();
    }

    default InterruptibleContext interruptible() {
        return events().interruptible();
    }

    default BubbleContext bubble() {
        return events().bubble();
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

    default <E extends T> void clearListeners(EventDefinition<E> definition) {
        events().get(definition).clearListeners();
    }

    default void clearAllListeners() {
        events().clearAllListeners();
    }

}
