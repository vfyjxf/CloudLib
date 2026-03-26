package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;

import java.util.Objects;

public record ContextKey<T>(Namespace id, Class<T> type) {

    public static <T> ContextKey<T> create(Namespace id, Class<T> type) {
        return new ContextKey<>(id, type);
    }

    public ContextKey(Namespace id, Class<T> type) {
        this.id = Checks.checkNotNull(id, "id");
        this.type = Checks.checkNotNull(type, "type");
    }

    public T cast(Object value) {
        return type.cast(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextKey<?> that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
