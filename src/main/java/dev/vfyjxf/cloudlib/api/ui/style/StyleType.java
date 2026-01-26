package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

/**
 * A globally-registered style "slot" identified by {@link #id}.
 * <p>
 * {@link StyleType} is used as the unique key for deduplicating {@link StyleProperty} instances,
 * and can also be used directly for type-safe access via {@link StyleContext#get(StyleType)} / {@link StyleContext#set(StyleType, Object)}.
 */
public record StyleType<T>(String id, @Nullable Supplier<T> initValue, @Nullable Applier<T> applier) {

    @FunctionalInterface
    public interface Applier<T> {
        void apply(StyleContext context, T value);
    }

    private static final LinkedHashMap<String, StyleType<?>> TYPES = new LinkedHashMap<>();

    public StyleType {
        if (TYPES.put(id, this) != null) {
            throw new IllegalArgumentException("Duplicate style type ID: " + id);
        }
    }

    public static <T> StyleType<T> of(String id, @Nullable Supplier<T> initValue) {
        return new StyleType<>(id, initValue, null);
    }

    public static <T> StyleType<T> of(String id, @Nullable Supplier<T> initValue, @Nullable Applier<T> applier) {
        return new StyleType<>(id, initValue, applier);
    }

    public static StyleType<?> get(String id) {
        return TYPES.get(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        StyleType<?> styleType = (StyleType<?>) o;
        return id.equals(styleType.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
