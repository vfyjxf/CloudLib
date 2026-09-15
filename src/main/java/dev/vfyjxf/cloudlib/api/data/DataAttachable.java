package dev.vfyjxf.cloudlib.api.data;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * An interface for objects that can hold type-safe data using {@link DataKey} instances.
 *
 * <p>DataAttachable provides a clean API for attaching arbitrary data to objects at runtime.
 * It is backed by a {@link DataContainer} which handles the actual storage.
 *
 *
 * <h2>Compared to NeoForge Attachments</h2>
 * <p>This API is designed for in-memory, transient data storage. For persistent,
 * serializable data, consider using NeoForge's attachment system via
 * {@link net.neoforged.neoforge.attachment.IAttachmentHolder}.
 *
 * @see DataKey
 * @see DataContainer
 */
public interface DataAttachable {

    /**
     * Returns the underlying data container.
     *
     * <p>The returned container can be used for direct access to all data operations.
     * For simple operations, prefer using the convenience methods on DataAttachable itself.
     *
     * @return the data container backing this holder
     */
    DataContainer data();

    /**
     * Stores a value for the given key.
     *
     * @param key   the key to store under
     * @param value the value to store
     * @param <T>   the type of the value
     * @return this holder for chaining (if supported by implementation)
     */
    default <T> DataAttachable set(DataKey<T> key, T value) {
        data().set(key, value);
        return this;
    }

    /**
     * Retrieves the value for the given key.
     *
     * <p>Returns the default value from the key if not explicitly set.
     *
     * @param key the key to look up
     * @param <T> the type of the value
     * @return the stored value, default value, or null
     */
    default <T extends @Nullable Object> T get(DataKey<T> key) {
        return data().get(key);
    }

    /**
     * Retrieves the value as an Optional.
     *
     * <p>Unlike {@link #get(DataKey)}, this does NOT use default values.
     *
     * @param key the key to look up
     * @param <T> the type of the value
     * @return an Optional containing the value if present
     */
    default <T> Optional<T> find(DataKey<T> key) {
        return data().find(key);
    }

    /**
     * Retrieves the value, throwing if not present.
     *
     * @param key the key to look up
     * @param <T> the type of the value
     * @return the stored value
     * @throws IllegalStateException if not present and no default
     */
    default <T> T require(DataKey<T> key) {
        return data().require(key);
    }

    /**
     * Retrieves the value, or returns the provided default.
     *
     * @param key          the key to look up
     * @param defaultValue the default value if not present
     * @param <T>          the type of the value
     * @return the stored value or the provided default
     */
    default <T> T getOrDefault(DataKey<T> key, T defaultValue) {
        return data().getOrDefault(key, defaultValue);
    }

    /**
     * Removes the value for the given key.
     *
     * @param key the key to remove
     * @param <T> the type of the value
     * @return the removed value, or null if not present
     */
    default <T> @Nullable T remove(DataKey<T> key) {
        return data().remove(key);
    }

    /**
     * Gets the value if present, otherwise computes and stores a new value.
     *
     * @param key      the key to look up or create
     * @param supplier the supplier for creating a new value
     * @param <T>      the type of the value
     * @return the existing or newly created value
     */
    default <T> T computeIfAbsent(DataKey<T> key, Supplier<T> supplier) {
        return data().computeIfAbsent(key, supplier);
    }

    /**
     * Updates the value using a function.
     *
     * @param key          the key to update
     * @param defaultValue the default if not present
     * @param operator     the operator to transform the value
     * @param <T>          the type of the value
     * @return the updated value
     */
    default <T> T update(DataKey<T> key, T defaultValue, UnaryOperator<T> operator) {
        return data().update(key, defaultValue, operator);
    }

    /**
     * Checks if a value is present for the given key.
     *
     * @param key the key to check
     * @return true if a value is explicitly stored for this key
     */
    default boolean has(DataKey<?> key) {
        return data().has(key);
    }

}
