package dev.vfyjxf.cloudlib.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Represents an object that can have data attached to it.
 */
@SuppressWarnings("ConstantConditions")
public interface DataAttachable {

    @NotNull
    DataContainer dataContainer();

    default <T> void attach(DataKey<T> key, T value) {
        dataContainer().attach(key, value);
    }

    default <T> @NotNull T getData(DataKey<T> key) {
        return dataContainer().get(key);
    }

    @Nullable
    default <T> T getNullable(DataKey<T> key) {
        return dataContainer().getNullable(key);
    }

    default <T> T getOrDefault(DataKey<T> key, T defaultValue) {
        return dataContainer().getOrDefault(key, defaultValue);
    }

    default <T> T getOrDefault(DataKey<T> key, Supplier<T> supplier) {
        T value = getData(key);
        if (value == null) {
            return supplier.get();
        }
        return value;
    }

    default <T> T getIfAbsentPut(DataKey<T> key, Supplier<T> supplier) {
        T value = getData(key);
        if (value == null) {
            value = supplier.get();
            attach(key, value);
        }
        return value;
    }

    default <T> T getIfAbsentPut(DataKey<T> key, @Nullable T value) {
        T existing = getData(key);
        if (existing == null) {
            attach(key, value);
            return value;
        }
        return existing;
    }

    default <T> T detach(DataKey<T> key) {
        return dataContainer().detach(key);
    }

    default void clear() {
        dataContainer().clear();
    }

    default boolean isEmpty() {
        return dataContainer().isEmpty();
    }

    default boolean hasData() {
        return !isEmpty();
    }

    default boolean has(DataKey<?> key) {
        return dataContainer().has(key);
    }

}
