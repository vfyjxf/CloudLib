package dev.vfyjxf.cloudlib.api.util;

import org.jspecify.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * A reference to a value that may or may not be present.
 * Like {@link java.util.Optional} or Option in scala.
 * {@link java.util.Optional} isn't recommended for using as a field in a class,
 * <p>
 * A defined {@code Maybe} always carries a non-null value: {@link #of} rejects null and
 * {@link #ofNullable} maps null to {@link #empty()}.
 *
 * @param <T> the type of the value
 */
public sealed interface Maybe<T extends @Nullable Object> {

    // region factories

    static <T extends @Nullable Object> Maybe<T> none() {
        return Empty.empty();
    }

    static <T extends @Nullable Object> Maybe<T> empty() {
        return Empty.empty();
    }

    /** @return a defined {@code Maybe} holding {@code value} */
    static <T> Maybe<T> of(T value) {
        return ofNonNull(value);
    }

    static <T> Maybe<T> ofNonNull(T value) {
        if (value == null) {
            throw new NullPointerException("value is null");
        }
        return new Some<>(value);
    }

    /** @return {@link #empty()} if {@code value} is null, a defined {@code Maybe} holding it otherwise */
    static <T> Maybe<T> ofNullable(@Nullable T value) {
        if (value == null) {
            return empty();
        }
        return new Some<>(value);
    }

    // endregion

    // region utils

    // TODO:add more utils
    default boolean isEmpty() {
        return this == Empty.instance;
    }

    default boolean defined() {
        return !isEmpty();
    }

    default <R> Maybe<R> map(Function<? super T, ? extends R> mapper) {
        return isEmpty() ? empty() : ofNullable(mapper.apply(get()));
    }

    default @Nullable T orElse(@Nullable T other) {
        return isEmpty() ? other : get();
    }

    // endregion

    /**
     * @return the value of this {@code Maybe}, never null
     * @throws NoSuchElementException if this {@code Maybe} is empty
     */
    T get();

    @SuppressWarnings("rawtypes")
    enum Empty implements Maybe {
        instance;

        @SuppressWarnings("unchecked")
        static <T> Maybe<T> empty() {
            return (Maybe<T>) instance;
        }

        @Override
        public Void get() {
            throw new NoSuchElementException("Maybe is empty");
        }
    }

    record Some<T>(T value) implements Maybe<T> {

        public Some {
            if (value == null) {
                throw new NullPointerException("value is null");
            }
        }

        @Override
        public T get() {
            return value;
        }
    }
}
