package dev.vfyjxf.cloudlib.utils;

public final class Functions {

    public interface ToIntFunction<T> {
        int applyAsInt(T value);
    }

    public interface ToLongFunction<T> {
        long applyAsLong(T value);
    }

    public interface ToFloatFunction<T> {
        float applyAsFloat(T value);
    }

    public interface ToDoubleFunction<T> {
        double applyAsDouble(T value);
    }

    public interface ToByteFunction<T> {
        byte applyAsByte(T value);
    }

    public interface ToShortFunction<T> {
        short applyAsShort(T value);
    }

    public interface ToCharFunction<T> {
        char applyAsChar(T value);
    }

    public interface ToBooleanFunction<T> {
        boolean applyAsBoolean(T value);
    }


    private Functions() {
    }
}
