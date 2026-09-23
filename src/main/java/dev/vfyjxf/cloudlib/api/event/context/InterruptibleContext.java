package dev.vfyjxf.cloudlib.api.event.context;

import dev.vfyjxf.cloudlib.api.event.EventChannel;
import org.jspecify.annotations.Nullable;

public final class InterruptibleContext {

    private final @Nullable EventChannel<?> channel;
    private boolean interrupted = false;

    public InterruptibleContext(@Nullable EventChannel<?> channel) {
        this.channel = channel;
    }

    public @Nullable EventChannel<?> channel() {
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
