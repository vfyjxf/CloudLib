package dev.vfyjxf.cloudlib.api.utils;

import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * A reference to a value that may or may not be present.
 * Like {@link java.util.Optional} or Option in scala.
 * {@link java.util.Optional} isn't recommended for using as a field in a class,
 *
 * @param <T> the type of the value
 */
public sealed interface Maybe<T extends @Nullable Object> {

    //region factories

    static <T> Maybe<T> none() {
        return Empty.empty();
    }

    static <T> Maybe<T> empty() {
        return Empty.empty();
    }

    static <T> Maybe<T> of(T value) {
        return new Some<>(value);
    }

    static <T> Maybe<T> ofNonNull(T value) {
        if (value == null) {
            throw new NullPointerException("value is null");
        }
        return new Some<>(value);
    }

    //endregion

    //region utils

    //TODO:add more utils
    default boolean isEmpty() {
        return this == Empty.INSTANCE;
    }

    default boolean defined() {
        return !isEmpty();
    }

    default <R> Maybe<R> map(Function<? super T, ? extends R> mapper) {
        return isEmpty() ? empty() : new Some<>(mapper.apply(get()));
    }

    default T orElse(T other) {
        return isEmpty() ? other : get();
    }

    //endregion

    T get();

    enum Empty implements Maybe<Object> {
        INSTANCE;

        @SuppressWarnings("unchecked")
        static <T> Maybe<T> empty() {
            return (Maybe<T>) INSTANCE;
        }

        @Override
        public Object get() {
            throw new NoSuchElementException("Maybe is empty");
        }

    }

    record Some<T>(T value) implements Maybe<T> {

        @Override
        public T get() {
            return value;
        }
    }


}
