package dev.vfyjxf.cloudlib.api.ui.layout.modifier;

import org.appliedenergistics.yoga.YogaNode;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class CombinedModifier implements Modifier {

    private final Modifier outer;
    private final Modifier inner;

    public CombinedModifier(Modifier outer, Modifier inner) {
        this.outer = outer;
        this.inner = inner;
    }

    @Override
    public <R> R foldIn(R initial, BiFunction<R, Modifier, R> operation) {
        return inner.foldIn(outer.foldIn(initial, operation), operation);
    }

    @Override
    public <R> R foldOut(R initial, BiFunction<R, Modifier, R> operation) {
        return outer.foldOut(inner.foldOut(initial, operation), operation);
    }

    @Override
    public boolean any(Predicate<Modifier> predicate) {
        return outer.any(predicate) || inner.any(predicate);
    }

    @Override
    public boolean all(Predicate<Modifier> predicate) {
        return outer.all(predicate) && inner.all(predicate);
    }

    @Override
    public void apply(YogaNode node) {
        outer.apply(node);
        inner.apply(node);
    }
}
