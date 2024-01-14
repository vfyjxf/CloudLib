package dev.vfyjxf.cloudlib.utils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;

@SuppressWarnings("unchecked")
public class Singletons {

    private static final IdentityHashMap<Class<?>, Object> SINGLETONS = new IdentityHashMap<>();
    private static final Table<Class<?>, Class<?>, Object> GENERIC_SINGLETONS = HashBasedTable.create();


    public static <T> void attachInstance(Class<T> type, T instance) {
        SINGLETONS.put(type, instance);
    }

    public static <T> void attachInstance(Class<T> type, Class<T> genericType, T instance) {
        GENERIC_SINGLETONS.put(type, genericType, instance);
    }

    @Nullable
    public static <T> T getNullable(Class<T> type) {
        return (T) SINGLETONS.get(type);
    }

    @Nullable
    static <T, G> T getNullable(Class<T> type, Class<G> genericType) {
        return (T) GENERIC_SINGLETONS.get(type, genericType);
    }

    public static <T> T get(Class<T> type) {
        T t = getNullable(type);
        if (t == null) {
            throw new RuntimeException("Singleton of type " + type.getCanonicalName() + " not registered");
        }
        return t;
    }

    public static <T, G> T get(Class<T> type, Class<G> genericType) {
        T t = getNullable(type, genericType);
        if (t == null) {
            throw new RuntimeException("Singleton of type " + type.getCanonicalName() + " with generic type " + genericType.getCanonicalName() + " not registered");
        }
        return t;
    }

}
