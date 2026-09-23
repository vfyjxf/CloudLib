package dev.vfyjxf.cloudlib.api.event.context;

import dev.vfyjxf.cloudlib.api.event.EventChannel;
import org.jspecify.annotations.Nullable;

public final class IntentContext {

    private final @Nullable EventChannel<?> source;

    public IntentContext(@Nullable EventChannel<?> source) {
        this.source = source;
    }

    public @Nullable EventChannel<?> source() {
        return source;
    }
}
