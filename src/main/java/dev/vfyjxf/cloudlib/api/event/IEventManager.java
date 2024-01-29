package dev.vfyjxf.cloudlib.api.event;

import dev.vfyjxf.cloudlib.utils.Singletons;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface IEventManager<E extends IEventHolder<E>> {

    static IEventManager<?> global() {
        //TODO:global event bus.
        return Singletons.get(IEventManager.class);
    }

    IEventHolder<E> holder();

    IEventContext createContext();

    default <T> void register(IEventDefinition<T> definition, T listener) {
        get(definition).register(listener);
    }

    <T> IEvent<T> get(IEventDefinition<T> definition);

    void clearAllListeners();

}
