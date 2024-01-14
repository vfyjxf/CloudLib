package dev.vfyjxf.cloudlib.event;

import dev.vfyjxf.cloudlib.api.event.IEvent;
import dev.vfyjxf.cloudlib.api.event.IEventContext;
import dev.vfyjxf.cloudlib.api.event.IEventDefinition;
import dev.vfyjxf.cloudlib.api.event.IEventHolder;
import dev.vfyjxf.cloudlib.api.event.IEventManager;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

@SuppressWarnings("unchecked")
public class EventManager<E extends IEventHolder<E>> implements IEventManager<E> {

    private final E holder;
    private final MutableMap<IEventDefinition<?>, IEvent<?>> listeners = Maps.mutable.empty();

    public EventManager(E holder) {
        this.holder = holder;
    }

    @Override
    public E holder() {
        return holder;
    }

    @Override
    public IEventContext createContext() {
        return new EventContext(this);
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
