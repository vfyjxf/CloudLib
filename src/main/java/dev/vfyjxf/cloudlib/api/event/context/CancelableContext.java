package dev.vfyjxf.cloudlib.api.event.context;

import dev.vfyjxf.cloudlib.api.event.EventChannel;

public final class CancelableContext {

    private final EventChannel<?> channel;
    private boolean cancelled = false;

    public CancelableContext(EventChannel<?> channel) {
        this.channel = channel;
    }

    public EventChannel<?> channel() {
        return channel;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public boolean interrupted() {
        return false;
    }

    public void cancel() {
        cancelled = true;
    }

}
