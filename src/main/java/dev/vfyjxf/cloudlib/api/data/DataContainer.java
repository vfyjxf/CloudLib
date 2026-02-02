package dev.vfyjxf.cloudlib.api.data;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A container for storing type-safe data using {@link DataKey} instances.
 *
 * <p>DataContainer is the actual storage implementation that backs {@link DataAttachable}.
 * It provides a type-safe map-like structure where keys determine the type of values.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe storage and retrieval</li>
 *   <li>Optional default value support via {@link DataKey}</li>
 *   <li>Atomic compute operations</li>
 *   <li>Null-safe API with explicit optional returns</li>
 * </ul>
 *
 * @see DataKey
 * @see DataAttachable
 */
public class DataContainer {

    private final MutableMap<DataKey<?>, Object> data = Maps.mutable.withInitialCapacity(2);
    private @Nullable DataAttachable owner;

    /**
     * Creates a standalone data container with no owner.
     */
    public DataContainer() {
        this(null);
    }

    /**
     * Creates a data container with a specific owner.
     *
     * @param owner the owner of this container, used for context-aware default values
     */
    public DataContainer(@Nullable DataAttachable owner) {
        this.owner = owner;
    }

    /**
     * Sets the owner of this container.
     */
    public void setOwner(@Nullable DataAttachable owner) {
        this.owner = owner;
    }

    //region core operations

    /**
     * Stores a value for the given key.
     *
     * @param key   the key to store under
     * @param value the value to store
     * @param <T>   the type of the value
     * @return this container for chaining
     */
    public <T> DataContainer set(DataKey<T> key, T value) {
        data.put(key, value);
        return this;
    }

    /**
     * Retrieves the value for the given key.
     *
     * <p>If the key is not present:
     * <ul>
     *   <li>If the key has a default value, returns the default</li>
     *   <li>Otherwise, returns null</li>
     * </ul>
     *
     * @param key the key to look up
     * @param <T> the type of the value
     * @return the stored value, the default value, or null
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(DataKey<T> key) {
        Object value = data.get(key);
        if (value != null) {
            return (T) value;
        }
        return key.defaultValue(owner);
    }

    /**
     * Retrieves the value for the given key as an Optional.
     *
     * <p>Unlike {@link #get(DataKey)}, this does NOT fall back to default values.
     * Use this when you want to distinguish between "not set" and "set to default".
     *
     * @param key the key to look up
     * @param <T> the type of the value
     * @return an Optional containing the value if present
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> find(DataKey<T> key) {
        return Optional.ofNullable((T) data.get(key));
    }

    /**
     * Retrieves the value, throwing an exception if not present.
     *
     * @param key the key to look up
     * @param <T> the type of the value
     * @return the stored value
     * @throws IllegalStateException if the key is not present and has no default
     */
    public <T> T require(DataKey<T> key) {
        T value = get(key);
        if (value == null) {
            throw new IllegalStateException("Required data not found: " + key);
        }
        return value;
    }

    /**
     * Retrieves the value, or returns the provided default.
     *
     * @param key          the key to look up
     * @param defaultValue the default value if not present
     * @param <T>          the type of the value
     * @return the stored value or the provided default
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(DataKey<T> key, T defaultValue) {
        Object value = data.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Removes the value for the given key.
     *
     * @param key the key to remove
     * @param <T> the type of the value
     * @return the removed value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T remove(DataKey<T> key) {
        return (T) data.remove(key);
    }

    //endregion

    //region compute operations

    /**
     * Gets the value if present, otherwise computes and stores a new value.
     *
     * <p>This is atomic - the supplier is only called if the key is absent.
     *
     * @param key      the key to look up or create
     * @param supplier the supplier for creating a new value
     * @param <T>      the type of the value
     * @return the existing or newly created value
     */
    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(DataKey<T> key, Supplier<T> supplier) {
        Object value = data.get(key);
        if (value == null) {
            T newValue = supplier.get();
            data.put(key, newValue);
            return newValue;
        }
        return (T) value;
    }

    /**
     * Computes a new value if the key is present.
     *
     * @param key      the key to look up
     * @param operator the operator to transform the value
     * @param <T>      the type of the value
     * @return the new value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T computeIfPresent(DataKey<T> key, UnaryOperator<T> operator) {
        Object value = data.get(key);
        if (value != null) {
            T newValue = operator.apply((T) value);
            data.put(key, newValue);
            return newValue;
        }
        return null;
    }

    /**
     * Updates the value using a function, creating if absent.
     *
     * @param key          the key to update
     * @param defaultValue the default if not present
     * @param operator     the operator to transform the value
     * @param <T>          the type of the value
     * @return the updated value
     */
    @SuppressWarnings("unchecked")
    public <T> T update(DataKey<T> key, T defaultValue, UnaryOperator<T> operator) {
        Object existing = data.get(key);
        T value = existing != null ? (T) existing : defaultValue;
        T newValue = operator.apply(value);
        data.put(key, newValue);
        return newValue;
    }

    /**
     * Merges a value with the existing value.
     *
     * @param key           the key to merge into
     * @param value         the value to merge
     * @param mergeFunction the function to combine old and new values
     * @param <T>           the type of the value
     * @return the merged value
     */
    @SuppressWarnings("unchecked")
    public <T> T merge(DataKey<T> key, T value, Function<T, UnaryOperator<T>> mergeFunction) {
        Object existing = data.get(key);
        T newValue = existing != null ? mergeFunction.apply((T) existing).apply(value) : value;
        data.put(key, newValue);
        return newValue;
    }

    //endregion

    //region query operations

    /**
     * Checks if a value is present for the given key.
     *
     * @param key the key to check
     * @return true if a value is explicitly stored for this key
     */
    public boolean has(DataKey<?> key) {
        return data.containsKey(key);
    }

    /**
     * @return true if this container has no stored data
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * @return the number of stored entries
     */
    public int size() {
        return data.size();
    }

    /**
     * Removes all stored data.
     */
    public void clear() {
        data.clear();
    }

    //endregion

    //region legacy api

    /**
     * @deprecated Use {@link #set(DataKey, Object)}
     */
    @Deprecated(forRemoval = true)
    public <T> void attach(DataKey<T> key, T value) {
        set(key, value);
    }

    /**
     * @deprecated Use {@link #get(DataKey)}
     */
    @Deprecated(forRemoval = true)
    public <T> @Nullable T getNullable(DataKey<T> key) {
        return get(key);
    }

    /**
     * @deprecated Use {@link #remove(DataKey)}
     */
    @Deprecated(forRemoval = true)
    public <T> @Nullable T detach(DataKey<T> key) {
        return remove(key);
    }

    //endregion
}
