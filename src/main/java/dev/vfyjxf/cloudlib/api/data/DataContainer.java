package dev.vfyjxf.cloudlib.api.data;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

public class DataContainer {

    private final MutableMap<DataKey<?>, Object> data = Maps.mutable.withInitialCapacity(1);

    public <T> void attach(DataKey<T> key, T value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(DataKey<T> key) {
        Object value = data.get(key);
        if (value == null) {
            throw new NullPointerException("DataKey " + key + " not found in DataContainer");
        }
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getNullable(DataKey<T> key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(DataKey<T> key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }


    @SuppressWarnings("unchecked")
    public <T> T detach(DataKey<T> key) {
        if (data.containsKey(key)) {
            return (T) data.remove(key);
        } else throw new NullPointerException("DataKey " + key + " not found in DataContainer");
    }

    public void clear() {
        data.clear();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean has(DataKey<?> key) {
        return data.containsKey(key);
    }

}
