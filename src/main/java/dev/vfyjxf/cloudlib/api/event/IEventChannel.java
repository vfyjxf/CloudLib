package dev.vfyjxf.cloudlib.api.event;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface IEventChannel {

    IEventHandler handler();

    default IEventContext.Common context(){
        return new IEventContext.Common(this);
    }

    default IEventContext.Cancelable cancelable(){
        return new IEventContext.Cancelable(this);
    }

    default IEventContext.Interruptible interruptible(){
        return new IEventContext.Interruptible(this);
    }

    default <T> void register(IEventDefinition<T> definition, T listener) {
        get(definition).register(listener);
    }

    <T> IEvent<T> get(IEventDefinition<T> definition);

    void clearAllListeners();

}
