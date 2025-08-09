package dev.vfyjxf.cloudlib.api.data;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

public class AttachableDataContainer {

    private final MutableMap<DataType<?>, Object> data = Maps.mutable.withInitialCapacity(1);

    public <T> void attach(DataType<T> key, T value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(DataType<T> key) {
        Object value = data.get(key);
        if (value == null) {
            throw new NullPointerException("DataType " + key + " not found in AttachableDataContainer");
        }
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getNullable(DataType<T> key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(DataType<T> key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }


    @SuppressWarnings("unchecked")
    public <T> T detach(DataType<T> key) {
        if (data.containsKey(key)) {
            return (T) data.remove(key);
        } else throw new NullPointerException("DataType " + key + " not found in AttachableDataContainer");
    }

    public void clear() {
        data.clear();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean has(DataType<?> key) {
        return data.containsKey(key);
    }

}
