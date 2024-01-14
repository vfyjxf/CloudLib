package dev.vfyjxf.cloudlib.event;

import dev.vfyjxf.cloudlib.api.event.IEventContext;
import dev.vfyjxf.cloudlib.api.event.IEventManager;

public class EventContext implements IEventContext {

    private final IEventManager<?> manager;
    private boolean cancelled = false;
    private boolean interrupted = false;

    public EventContext(IEventManager<?> manager) {
        this.manager = manager;
    }


    @Override
    public IEventManager<?> getManager() {
        return manager;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
        interrupted = true;
    }

    @Override
    public void interrupt() {
        interrupted = true;
    }

    @Override
    public boolean isInterrupted() {
        return interrupted;
    }
}
