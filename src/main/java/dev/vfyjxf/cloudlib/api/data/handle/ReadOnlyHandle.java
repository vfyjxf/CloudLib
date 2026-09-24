package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.snapshot.Observable;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Read-only view over a {@link Handle}'s value: read + subscribe, no mutation.
 *
 * @param <T> the value type
 */
public interface ReadOnlyHandle<T> extends Observable {

    /**
     * @return the value of this handle, or null while the cell holds no value
     */
    @Contract(pure = true)
    @Nullable
    T get();

    /** Fired by {@link Handle#set} and {@link Handle#apply} (not {@link Handle#load}). */
    Subscription onChange(Consumer<? super T> listener);

    /**
     * Fired with {@code (previous, current)} on change. {@code previous} is null when the cell held
     * no value before this change.
     */
    Subscription onChange(BiConsumer<? super @Nullable T, ? super T> listener);
}
