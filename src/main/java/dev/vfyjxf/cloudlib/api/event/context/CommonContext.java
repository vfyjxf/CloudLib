package dev.vfyjxf.cloudlib.api.event.context;

import dev.vfyjxf.cloudlib.api.event.EventChannel;

/**
 * Cancellable/Interruptible
 */
public final class CommonContext {

    private final EventChannel<?> channel;
    private boolean cancelled = false;
    private boolean interrupted = false;

    public CommonContext(EventChannel<?> channel) {
        this.channel = channel;
    }

    public EventChannel<?> channel() {
        return channel;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public boolean interrupted() {
        return interrupted;
    }

    public void cancel() {
        cancelled = true;
        interrupted = true;
    }

    public void interrupt() {
        interrupted = true;
    }

}
