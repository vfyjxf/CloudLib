package dev.vfyjxf.cloudlib.utils;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
     * @param bottom   the bottom class
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

    public static boolean isFunctionalInterface(Class<?> clazz) {
        if (clazz.isInterface()) {
            Method[] methods = clazz.getMethods();
            int abstractCount = 0;
            for (int i = 0; i < methods.length && abstractCount <= 1; i++) {
                Method method = methods[i];
                if (!Modifier.isAbstract(method.getModifiers())) continue;
                boolean fromObject = switch (method.getName()) {
                    case "hashCode" -> method.getReturnType() == int.class && method.getParameterCount() == 0;
                    case "equals" -> method.getReturnType() == boolean.class && method.getParameterCount() == 1 &&
                            method.getParameterTypes()[0] == Object.class;
                    case "toString" -> method.getReturnType() == String.class && method.getParameterCount() == 0;
                    case "clone" -> method.getReturnType() == Object.class && method.getParameterCount() == 0;
                    case "finalize" -> method.getReturnType() == void.class && method.getParameterCount() == 0;
                    default -> false;
                };
                if (!fromObject) abstractCount++;
            }
            return abstractCount == 1;
        }
        return false;
    }

    private ClassUtils() {
    }
}
