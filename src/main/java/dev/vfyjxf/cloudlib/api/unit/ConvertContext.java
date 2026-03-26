package dev.vfyjxf.cloudlib.api.unit;

import dev.vfyjxf.cloudlib.util.Checks;

import java.util.Map;
import java.util.Optional;

public final class ConvertContext {

    private static final ConvertContext emptyContext = new ConvertContext(Map.of());

    private final Map<ContextKey<?>, Object> values;

    static ConvertContext create(Map<ContextKey<?>, Object> values) {
        Checks.checkNotNull(values, "values");
        return values.isEmpty() ? emptyContext : new ConvertContext(values);
    }

    public static ConvertContext empty() {
        return emptyContext;
    }

    private ConvertContext(Map<ContextKey<?>, Object> values) {
        this.values = Map.copyOf(values);
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

    public <T> T getOrDefault(ContextKey<T> key, T defaultValue) {
        Checks.checkNotNull(key, "key");
        Checks.checkNotNull(defaultValue, "defaultValue");
        return find(key).orElse(defaultValue);
    }

    public Map<ContextKey<?>, Object> entries() {
        return values;
    }

    public <T> ConvertContext with(ContextKey<T> key, T value) {
        Checks.checkNotNull(key, "key");
        Checks.checkNotNull(value, "value");
        var newValues = new java.util.HashMap<>(values);
        newValues.put(key, value);
        return new ConvertContext(newValues);
    }
}
