package dev.vfyjxf.cloudlib.utils;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class Checks {

    public static void checkArgument(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        Preconditions.checkArgument(expression, errorMessageTemplate, errorMessageArgs);
    }

    public static <T> T checkNotNull(@Nullable T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }

    public static <X extends Throwable, T> T checkNotNull(@Nullable T obj, Supplier<? extends X> exceptionSupplier) throws X {
        if (obj == null) {
            throw exceptionSupplier.get();
        }
        return obj;
    }

    public static void checkElementIndex(int index, int size) {
        Preconditions.checkElementIndex(index, size);
    }

    /**
     * Checks the get whether in range [start, end).
     */
    public static void checkRange(int value, int start, int end) {
        Preconditions.checkArgument(value >= start && value < end, "Value must be in range [%s,%s)", start, end);
    }

    public static void checkRange(int value, int size) {
        checkRange(value, 0, size);
    }

    /**
     * Checks the get whether in range [start, end].
     */
    public static void checkRangeClosed(int value, int start, int end) {
        Preconditions.checkArgument(value >= start && value <= end, "Value must be in range [%s,%s]", start, end);
    }

    public static void checkRangeClosed(int value, int size) {
        checkRangeClosed(value, 0, size);
    }

    public static void checkRange(double value, double start, double end) {
        Preconditions.checkArgument(value >= start && value < end, "Value must be in range [%s,%s)", start, end);
    }

    public static void checkRange(double value, double size) {
        checkRange(value, 0, size);
    }

    public static void checkRangeClosed(double value, double start, double end) {
        Preconditions.checkArgument(value >= start && value <= end, "Value must be in range [%s,%s]", start, end);
    }

    public static void checkRangeClosed(double value, double size) {
        checkRangeClosed(value, 0, size);
    }

}
