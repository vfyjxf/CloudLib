package dev.vfyjxf.cloudlib.api.data;

import java.util.function.Function;

/**
 * The unique key for an attachable data.
 * <p>
 * An in memory data storage system for an attachable object.
 *
 * @param <T> The type of the data
 */
public interface DataKey<T> {

    static <T> DataKey<T> valueOf(String key, T defaultValue) {
        return new DataKeys.BasicDataKey<>(key, defaultValue);
    }

    static <T> DataKey<T> providerOf(String key, Function<DataAttachable, T> defaultValueProvider) {
        return new DataKeys.DataKeyImpl<>(key, defaultValueProvider);
    }

    String key();

    T defaultValue(DataAttachable holder);

}
