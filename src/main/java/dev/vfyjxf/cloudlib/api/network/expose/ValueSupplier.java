package dev.vfyjxf.cloudlib.api.network.expose;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @param <T> the type of the exposed value
 */
public interface ValueSupplier<T extends @Nullable Object> extends Supplier<T> {
    /**
     * Must be pure
     *
     * @return the current value of this provider, or null while it holds no value yet
     */
    @Contract(pure = true)
    @Override
    @Nullable
    T get();
}
