package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.ui.state.State;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Listenable reference get.
 */
public interface IState<T> {

    static <V> IState<V> of(V value) {
        return new State<>(value);
    }

    @Nullable
    T get();

    default boolean isNull() {
        return get() == null;
    }

    default boolean notNull() {
        return get() != null;
    }

    default T getOrDefault(T defaultValue) {
        T value = get();
        if (value == null) return defaultValue;
        return value;
    }

    default T getElse(Supplier<T> supplier) {
        T value = get();
        if (value == null) return supplier.get();
        return value;
    }

    /**
     * @throws IllegalStateException if get is null
     */
    default T getNotNull() {
        T value = get();
        if (value == null) throw new IllegalStateException("Value is null");
        return value;
    }

    IState<T> set(T value);

    default IState<T> set(Function<T, T> mapper) {
        return set(mapper.apply(get()));
    }

    IState<T> onSet(Consumer<T> consumer);

    default <U> IState<U> map(Function<T, U> mapper){
        return new State<>(mapper.apply(get()));
    }

}
