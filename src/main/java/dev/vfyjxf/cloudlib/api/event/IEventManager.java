package dev.vfyjxf.cloudlib.api.event;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface IEventManager<E extends IEventHolder<E>> {

    IEventHolder<E> holder();

    IEventContext createContext();

    default <T> void register(IEventDefinition<T> definition, T listener) {
        get(definition).register(listener);
    }

    <T> IEvent<T> get(IEventDefinition<T> definition);

    void clearAllListeners();

}
