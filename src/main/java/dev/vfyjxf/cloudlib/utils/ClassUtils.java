package dev.vfyjxf.cloudlib.utils;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Predicate;

public final class ClassUtils {

    public static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Nullable
    public static Field getField(Class<?> clazz, String name) {
        Class<?> currentType = clazz;
        while (currentType != null) {
            try {
                Field field = currentType.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                currentType = currentType.getSuperclass();
            }
        }
        return null;
    }

    /**
     * @param bottom  the bottom class
     * @param detector return whether to continue
     */
    public static void walkClassTree(Class<?> bottom, Predicate<Class<?>> detector) {
        Class<?> currentType = bottom;
        while (currentType != null && detector.test(currentType)) {
            currentType = currentType.getSuperclass();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getGenericType(T[] typeCatch) {
        if (typeCatch.length != 0) {
            throw new IllegalArgumentException("Type catch must be empty");
        }
        return (Class<T>) typeCatch.getClass().getComponentType();
    }

    private ClassUtils() {
    }
}
