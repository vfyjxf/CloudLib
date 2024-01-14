package dev.vfyjxf.cloudlib.api.event;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

public interface IEventHolder<E extends IEventHolder<E>> {

    IEventManager<E> events();

    default IEventContext context() {
        return events().createContext();
    }

    default <T> T listener(IEventDefinition<T> definition) {
        return events().get(definition).invoker();
    }

    @CanIgnoreReturnValue
    default <T> T registerListener(IEventDefinition<T> definition, T listener) {
        return events().get(definition).register(listener);
    }

    default <T> void unregisterListener(IEventDefinition<T> definition, T listener) {
        events().get(definition).unregister(listener);
    }

    default <T> void clearListeners(IEventDefinition<T> definition) {
        events().get(definition).clearListeners();
    }

    default <T> void clearAllListeners() {
        events().clearAllListeners();
    }

}
