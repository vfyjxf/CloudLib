package dev.vfyjxf.cloudlib.api.event;


public interface IEventHandler<E extends IEventHandler<E>> {

    IEventChannel<E> channel();

    default IEventContext.Common context() {
        return channel().context();
    }

    default IEventContext.Cancelable cancelableCtx() {
        return channel().cancelable();
    }

    default IEventContext.Interruptible interruptibleCtx() {
        return channel().interruptible();
    }

    default <T> T listeners(IEventDefinition<T> definition) {
        return channel().get(definition).invoker();
    }


    default <T> T registerListener(IEventDefinition<T> definition, T listener) {
        return channel().get(definition).register(listener);
    }

    default <T> void unregisterListener(IEventDefinition<T> definition, T listener) {
        channel().get(definition).unregister(listener);
    }

    default <T> void clearListeners(IEventDefinition<T> definition) {
        channel().get(definition).clearListeners();
    }

    default void clearAllListeners() {
        channel().clearAllListeners();
    }

}
