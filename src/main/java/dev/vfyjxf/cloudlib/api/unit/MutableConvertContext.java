package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.util.Checks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class MutableConvertContext {

    private final Map<ContextKey<?>, Object> values = new HashMap<>(2);

    public static MutableConvertContext create() {
        return new MutableConvertContext();
    }

    private MutableConvertContext() {
    }

    public <T> MutableConvertContext set(ContextKey<T> key, T value) {
        Checks.checkNotNull(key, "key");
        Checks.checkNotNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("Value type mismatch for key " + key.id());
        }
        values.put(key, value);
        return this;
    }

    public <T> Optional<T> find(ContextKey<T> key) {
        Checks.checkNotNull(key, "key");
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(key.cast(value));
    }

    public <T> T require(ContextKey<T> key) {
        return find(key).orElseThrow(() -> new IllegalStateException("Required context key not found: " + key.id()));
    }

    public ConvertContext freeze() {
        return ConvertContext.create(values);
    }
}
