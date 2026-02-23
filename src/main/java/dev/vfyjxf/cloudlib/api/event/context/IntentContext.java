package dev.vfyjxf.cloudlib.api.event.context;

import dev.vfyjxf.cloudlib.api.event.EventChannel;

public final class IntentContext {

    private final EventChannel<?> source;

    public IntentContext(EventChannel<?> source) {
        this.source = source;
    }

    public EventChannel<?> source() {
        return source;
    }

}
