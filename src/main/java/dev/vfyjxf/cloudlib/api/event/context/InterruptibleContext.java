package dev.vfyjxf.cloudlib.api.event.context;

import dev.vfyjxf.cloudlib.api.event.EventChannel;

public final class InterruptibleContext {

    private final EventChannel<?> channel;
    private boolean interrupted = false;

    public InterruptibleContext(EventChannel<?> channel) {
        this.channel = channel;
    }

    public EventChannel<?> channel() {
        return channel;
    }

    public boolean cancelled() {
        return false;
    }

    public boolean interrupted() {
        return interrupted;
    }

    public void interrupt() {
        interrupted = true;
    }

}
