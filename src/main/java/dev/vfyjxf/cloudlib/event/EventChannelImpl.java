package dev.vfyjxf.cloudlib.event;


import dev.vfyjxf.cloudlib.api.event.Event;
import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

public class EventChannelImpl<T> implements EventChannel<T> {

    private final EventHandler<T> handler;
    private final MutableMap<EventDefinition<?>, Event<?>> listeners = Maps.mutable.empty();
    private final MutableList<Checker<T>> checkers = Lists.mutable.empty();

    public EventChannelImpl(EventHandler<T> handler) {
        this.handler = handler;
    }

    @Override
    public EventHandler<T> handler() {
        return handler;
    }

    @Override
    public void register(Object listenerOwner) {
        //TODO:WIP
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
