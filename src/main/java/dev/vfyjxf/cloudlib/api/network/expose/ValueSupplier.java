package dev.vfyjxf.cloudlib.api.network.expose;

import org.jetbrains.annotations.Contract;

import java.util.function.Supplier;

public interface ValueSupplier<T> extends Supplier<T> {
    /**
     * Must be pure
     *
     * @return the current value of this provider
     */
    @Contract(pure = true)
    @Override
    T get();
}
