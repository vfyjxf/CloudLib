package dev.vfyjxf.cloudlib.api.event;

public interface IEventContext {

    IEventManager<?> getManager();

    @SuppressWarnings("unchecked")
    default <T> T holder() {
        return (T) getManager().holder();
    }

    boolean isCancelled();

    void cancel();

    void interrupt();

    boolean isInterrupted();

}
