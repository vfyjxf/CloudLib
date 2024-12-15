package dev.vfyjxf.cloudlib.api.ui.layout.modifier;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public abstract class BasicModifier<THIS extends BasicModifier<THIS>> implements Modifier {
    @Override
    public <R> R foldIn(R initial, BiFunction<R, Modifier, R> operation) {
        return null;
    }

    @Override
    public <R> R foldOut(R initial, BiFunction<R, Modifier, R> operation) {
        return null;
    }

    @Override
    public boolean any(Predicate<Modifier> predicate) {
        return false;
    }

    @Override
    public boolean all(Predicate<Modifier> predicate) {
        return false;
    }

    @Override
    public abstract void apply(Widget widget);
}
