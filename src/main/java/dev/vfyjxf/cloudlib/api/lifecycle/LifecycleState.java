package dev.vfyjxf.cloudlib.api.lifecycle;

import org.jspecify.annotations.Nullable;

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

    /**
     * Resolves a reported value to the value held by this state.
     *
     * @param value  the reported value, may be null
     * @param reason a short description of why the value was reported, used in error messages
     * @return {@link Unit#instance} for {@link Void} states, {@code value} itself when it is an
     * instance of {@link #type}, or null when a non-void state was reported without a value
     * @throws IllegalArgumentException if the value is not an instance of {@link #type}
     */
    @Nullable
    Object normalizeValue(@Nullable Object value, String reason) {
        if (type == Void.class) {
            return Unit.instance;
        }
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                "Lifecycle state " + id + " reason=" + reason + " expects an instance of " + type.getName()
            );
        }
        return type.cast(value);
    }

    @Override
    public String toString() {
        return id;
    }

    public enum Kind {
        context, event
    }

    enum Unit {
        instance
    }
}
