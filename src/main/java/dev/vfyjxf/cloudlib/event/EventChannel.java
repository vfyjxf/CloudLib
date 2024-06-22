package dev.vfyjxf.cloudlib.event;

import dev.vfyjxf.cloudlib.api.event.IEvent;
import dev.vfyjxf.cloudlib.api.event.IEventChannel;
import dev.vfyjxf.cloudlib.api.event.IEventDefinition;
import dev.vfyjxf.cloudlib.api.event.IEventHandler;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

@SuppressWarnings("unchecked")
public class EventChannel<E extends IEventHandler<E>> implements IEventChannel<E> {

    private final E handler;
    private final MutableMap<IEventDefinition<?>, IEvent<?>> listeners = Maps.mutable.empty();

    public EventChannel(E handler) {
        this.handler = handler;
    }

    @Override
    public E handler() {
        return handler;
    }

    @Override
    public <T> IEvent<T> get(IEventDefinition<T> definition) {
        IEvent<T> event = (IEvent<T>) listeners.get(definition);
        if (event == null) {
            event = definition.create();
            listeners.put(definition, event);
        }
        return event;
    }

    @Override
    public void clearAllListeners() {
        for (IEvent<?> listener : listeners) {
            listener.clearListeners();
        }
    }

}
