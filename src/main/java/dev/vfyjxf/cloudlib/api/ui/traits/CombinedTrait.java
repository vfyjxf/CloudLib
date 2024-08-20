package dev.vfyjxf.cloudlib.api.ui.traits;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class CombinedTrait implements ITrait {

    private final ITrait outer;
    private final ITrait inner;

    public CombinedTrait(ITrait outer, ITrait inner) {
        this.outer = outer;
        this.inner = inner;
    }

    public ITrait getOuter() {
        return outer;
    }

    public ITrait getInner() {
        return inner;
    }

    @Override
    public <T> T foldIn(T initial, BiFunction<T, ITraitElement, T> operator) {
        return inner.foldIn(outer.foldIn(initial, operator), operator);
    }

    @Override
    public <T> T foldOut(T initial, BiFunction<T, ITraitElement, T> operator) {
        return outer.foldOut(inner.foldOut(initial, operator), operator);
    }

    @Override
    public boolean any(Predicate<ITraitElement> predicate) {
        return outer.any(predicate) || inner.any(predicate);
    }

    @Override
    public boolean all(Predicate<ITraitElement> predicate) {
        return outer.all(predicate) && inner.all(predicate);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof CombinedTrait that)) return false;

        if (!outer.equals(that.outer)) return false;
        return inner.equals(that.inner);
    }

    @Override
    public int hashCode() {
        int result = outer.hashCode();
        result = 31 * result + inner.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "[" +
                foldIn("", (acc, element) -> {
                    if(acc.isEmpty())return element.toString();
                    else return acc + ", " + element.toString();
                })
                + "]";
    }
}
