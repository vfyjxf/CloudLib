package dev.vfyjxf.cloudlib.api.ui.traits;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface ITraitElement extends ITrait {

    ITraitElement EMPTY = new ITraitElement() {
        @Override
        public <T> T foldIn(T initial, BiFunction<T, ITraitElement, T> operator) {
            return operator.apply(initial, this);
        }

        @Override
        public <T> T foldOut(T initial, BiFunction<T, ITraitElement, T> operator) {
            return operator.apply(initial, this);
        }

        @Override
        public boolean any(Predicate<ITraitElement> predicate) {
            return predicate.test(this);
        }

        @Override
        public boolean all(Predicate<ITraitElement> predicate) {
            return predicate.test(this);
        }
    };

}
