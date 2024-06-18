package dev.vfyjxf.cloudlib.api.ui.traits;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface ITraitElement extends ITrait {

    @Override
    default <T> T foldIn(T initial, BiFunction<T, ITraitElement, T> operator) {
        return operator.apply(initial, this);
    }

    @Override
    default <T> T foldOut(T initial, BiFunction<T, ITraitElement, T> operator) {
        return operator.apply(initial, this);
    }

    @Override
    default boolean any(Predicate<ITraitElement> predicate) {
        return predicate.test(this);
    }

    @Override
    default boolean all(Predicate<ITraitElement> predicate) {
        return predicate.test(this);
    }
}
