package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.ui.state.MutableState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Listenable reference get.
 */
public interface IState<T> {


    static <V> IState<V> stateOf(V value) {
        return new MutableState<>(value);
    }

    @NotNull
    T get();

    @Nullable
    T getNullable();

    default boolean isNull() {
        return getNullable() == null;
    }

    default boolean notNull() {
        return getNullable() != null;
    }

    default T getOrDefault(T defaultValue) {
        T value = getNullable();
        if (value == null) return defaultValue;
        return value;
    }

    default T getElse(Supplier<T> supplier) {
        T value = getNullable();
        if (value == null) return supplier.get();
        return value;
    }

    IState<T> set(T value);

    default IState<T> set(Function<T, T> mapper) {
        if (isNull()) return this;
        return set(mapper.apply(get()));
    }

    IState<T> onSet(Consumer<T> consumer);

    <U> IState<U> map(Function<T, U> mapper);

}
