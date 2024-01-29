package dev.vfyjxf.cloudlib.api.event;

public interface IEventDefinition<T> {

    Class<T> type();

    IEvent<T> create();

    IEvent<T> global();

}
