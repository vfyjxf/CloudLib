package dev.vfyjxf.cloudlib.api.lifecycle;

import java.util.Objects;

public record LifecycleState<T>(String id, Kind kind, Class<T> type) {

    public LifecycleState {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(type, "type");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Lifecycle state id must not be blank");
        }
    }

    boolean isContext() {
        return kind == Kind.context;
    }

    boolean isEvent() {
        return kind == Kind.event;
    }

    Object normalizeValue(Object value) {
        if (type == Void.class) {
            return Unit.instance;
        }
        return type.cast(Objects.requireNonNull(value, "value"));
    }

    @Override
    public String toString() {
        return id;
    }

    public enum Kind {
        context,
        event
    }

    enum Unit {
        instance
    }
}
