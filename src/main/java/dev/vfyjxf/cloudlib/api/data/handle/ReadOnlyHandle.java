package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.snapshot.Observable;
import org.jetbrains.annotations.Contract;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Read-only view over a {@link Handle}'s value: read + subscribe, no mutation.
 *
 * @param <T> the value type
 */
public interface ReadOnlyHandle<T> extends Observable {

    @Contract(pure = true)
    T get();

    /** Fired by {@link Handle#set} and {@link Handle#apply} (not {@link Handle#load}). */
    Subscription onChange(Consumer<? super T> listener);

    /** Fired with {@code (previous, current)} on change. */
    Subscription onChange(BiConsumer<? super T, ? super T> listener);
}
