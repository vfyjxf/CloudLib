package dev.vfyjxf.cloudlib.api.event.context;

import dev.vfyjxf.cloudlib.api.event.EventChannel;
import org.jspecify.annotations.Nullable;

public final class CancelableContext {

    private final @Nullable EventChannel<?> channel;
    private boolean cancelled = false;

    public CancelableContext(@Nullable EventChannel<?> channel) {
        this.channel = channel;
    }

    public @Nullable EventChannel<?> channel() {
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
