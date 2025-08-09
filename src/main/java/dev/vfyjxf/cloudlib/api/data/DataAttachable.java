package dev.vfyjxf.cloudlib.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Represents an object that can have data attached to it.
 * It is recommended to use neoforge's {@link net.neoforged.neoforge.attachment.IAttachmentHolder} if you want to attach a serializable data.
 * <p>
 * We call {@link DataType} and {@link net.neoforged.neoforge.attachment.AttachmentType} as "attachable".
 */
@SuppressWarnings("ConstantConditions")
public interface DataAttachable {

    @NotNull
    AttachableDataContainer attachableDataContainer();

    default <T> void attachData(DataType<T> type, T value) {
        attachableDataContainer().attach(type, value);
    }

    default <T> T getData(DataType<T> type) {
        return attachableDataContainer().get(type);
    }

    @Nullable
    default <T> T getNullable(DataType<T> type) {
        return attachableDataContainer().getNullable(type);
    }

    default <T> T getOrDefault(DataType<T> type, T defaultValue) {
        return attachableDataContainer().getOrDefault(type, defaultValue);
    }

    default <T> T getOrDefault(DataType<T> type, Supplier<T> supplier) {
        T value = getData(type);
        if (value == null) {
            return supplier.get();
        }
        return value;
    }

    default <T> T getIfAbsentPut(DataType<T> type, Supplier<T> supplier) {
        T value = getData(type);
        if (value == null) {
            value = supplier.get();
            attachData(type, value);
        }
        return value;
    }

    default <T> T getIfAbsentPut(DataType<T> type, @Nullable T value) {
        T existing = getData(type);
        if (existing == null) {
            attachData(type, value);
            return value;
        }
        return existing;
    }

    default <T> T detachData(DataType<T> type) {
        return attachableDataContainer().detach(type);
    }

    default void clearData() {
        attachableDataContainer().clear();
    }

    default boolean isDataEmpty() {
        return attachableDataContainer().isEmpty();
    }

    default boolean hasAnyData() {
        return !isDataEmpty();
    }

    default boolean hasData(DataType<?> type) {
        return attachableDataContainer().has(type);
    }

}
