package dev.vfyjxf.cloudlib.api.ui.traits;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * A series of event listeners.
 */
public interface ITrait {

    <T> T foldIn(T initial, BiFunction<T, ITraitElement, T> operator);

    <T> T foldOut(T initial, BiFunction<T, ITraitElement, T> operator);

    boolean any(Predicate<ITraitElement> predicate);

    boolean all(Predicate<ITraitElement> predicate);

    default ITrait then(ITrait trait) {
        if (trait == ITraitElement.EMPTY) return this;
        return new CombinedTrait(this, trait);
    }

    default ITrait then(ITrait... traits) {
        ITrait trait = this;
        for (ITrait t : traits) trait = trait.then(t);
        return trait;
    }

    default void init() {

    }

    default void update() {

    }

}
