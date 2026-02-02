package dev.vfyjxf.cloudlib.api.data;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A type-safe key for storing and retrieving in-memory data in a {@link DataContainer}.
 *
 * <p>DataKey provides a strongly-typed mechanism for attaching arbitrary data to objects.
 * Each key has a unique identity based on its {@link Namespace} identifier and
 * can optionally specify a default value provider.
 *
 * <p>This API is designed for transient, in-memory data storage only.
 * For persistent, serializable data, use NeoForge's attachment system.
 *
 * @param <T> the type of data this key holds
 * @see DataContainer
 * @see DataAttachable
 */
public record DataKey<T>(
    Namespace id,
    @Nullable Function<@Nullable DataAttachable, T> defaultValueFunction
) {

    /**
     * Creates a simple key with no default value.
     *
     * @param id  the unique identifier for this key
     * @param <T> the type of data
     * @return a new DataKey
     */
    public static <T> DataKey<T> create(Namespace id) {
        return new DataKey<>(id, null);
    }

    /**
     * Creates a key with a constant default value.
     *
     * @param id           the unique identifier for this key
     * @param defaultValue the default value to return when data is not present
     * @param <T>          the type of data
     * @return a new DataKey
     */
    public static <T> DataKey<T> create(Namespace id, T defaultValue) {
        return new DataKey<>(id, holder -> defaultValue);
    }

    /**
     * Creates a key with a lazily computed default value.
     *
     * <p>The supplier is called each time a default value is needed,
     * useful for mutable default values like collections.
     *
     * @param id              the unique identifier for this key
     * @param defaultSupplier supplier for the default value
     * @param <T>             the type of data
     * @return a new DataKey
     */
    public static <T> DataKey<T> create(Namespace id, Supplier<T> defaultSupplier) {
        Checks.checkNotNull(defaultSupplier, "defaultSupplier");
        return new DataKey<>(id, holder -> defaultSupplier.get());
    }

    /**
     * Creates a key with a context-aware default value.
     *
     * <p>The function receives the DataAttachable holder and can compute
     * a default value based on the holder's state.
     *
     * @param id              the unique identifier for this key
     * @param defaultFunction function to compute the default value
     * @param <T>             the type of data
     * @return a new DataKey
     */
    public static <T> DataKey<T> createComputed(Namespace id, Function<@Nullable DataAttachable, T> defaultFunction) {
        Checks.checkNotNull(defaultFunction, "defaultFunction");
        return new DataKey<>(id, defaultFunction);
    }

    public DataKey(Namespace id, @Nullable Function<@Nullable DataAttachable, T> defaultValueFunction) {
        this.id = Checks.checkNotNull(id, "id");
        this.defaultValueFunction = defaultValueFunction;
    }

    /**
     * Checks if this key has a default value provider.
     *
     * @return true if a default value is available
     */
    public boolean hasDefaultValue() {
        return defaultValueFunction != null;
    }

    /**
     * Gets the default value for this key.
     *
     * @param holder the data holder requesting the default (may be null)
     * @return the default value, or null if no default is configured
     */
    public @Nullable T defaultValue(@Nullable DataAttachable holder) {
        return defaultValueFunction != null ? defaultValueFunction.apply(holder) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataKey<?> dataKey)) return false;
        return Objects.equals(id, dataKey.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DataKey[" + id + "]";
    }
}
