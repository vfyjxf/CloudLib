package dev.vfyjxf.cloudlib.api.event;


import dev.vfyjxf.cloudlib.api.event.context.BubbleContext;
import dev.vfyjxf.cloudlib.api.event.context.CancelableContext;
import dev.vfyjxf.cloudlib.api.event.context.CommonContext;
import dev.vfyjxf.cloudlib.api.event.context.InterruptibleContext;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

import java.util.function.BooleanSupplier;

/**
 * The event channel is the main class to manage events.
 * It manages all event objects held by the corresponding EventHandler and provides the ability to register and post events.
 *
 * @param <T> the base type of the events
 */
public sealed interface EventChannel<T> permits EventChannelImpl {

    static <T> EventChannel<T> create(EventHandler<T> handler) {
        return new EventChannelImpl<>(handler);
    }

    EventHandler<T> handler();

    default CommonContext common() {
        return new CommonContext(this);
    }

    default CancelableContext cancelable() {
        return new CancelableContext(this);
    }

    default InterruptibleContext interruptible() {
        return new InterruptibleContext(this);
    }

    default BubbleContext bubble() {
        return new BubbleContext(this);
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

    default <E extends T> void registerManaged(EventDefinition<E> definition, E listener, Object reference) {
        get(definition).registerManaged(listener, reference);
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

final class EventChannelImpl<T> implements EventChannel<T> {

    private final EventHandler<T> handler;
    private final MutableMap<EventDefinition<?>, Event<?>> listeners = Maps.mutable.empty();
    private final MutableList<Checker<T>> checkers = MutableLists.empty();

    public EventChannelImpl(EventHandler<T> handler) {
        this.handler = handler;
    }

    @Override
    public EventHandler<T> handler() {
        return handler;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends T> Event<E> get(EventDefinition<E> definition) {
        if (!checkers.isEmpty() && checkers.noneSatisfy(checker -> checker.check(definition.type()))) {
            throw new IllegalArgumentException("Event type: " + definition.type() + " not allowed");
        }
        return (Event<E>) listeners.getIfAbsentPut(definition, definition::create);
    }

    @Override
    public void clearAllListeners() {
        for (Event<?> source : listeners) {
            source.clearListeners();
        }
    }

    @Override
    public void checkEvent(Checker<T> checker) {
        if (checkers.contains(checker)) {
            throw new IllegalArgumentException("Event checker already registered");
        }
        checkers.add(checker);
    }
}