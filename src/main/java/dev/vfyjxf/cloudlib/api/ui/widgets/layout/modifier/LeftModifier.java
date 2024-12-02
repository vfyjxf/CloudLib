package dev.vfyjxf.cloudlib.api.ui.widgets.layout.modifier;

import dev.vfyjxf.cloudlib.api.ui.layout.modifier.BasicModifier;
import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class LeftModifier implements Modifier {
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
    public void apply(Widget widget) {

    }

    public RightModifier left(int value) {
        return new RightModifier();
    }

    public Modifier width(int value) {
        return new BasicModifier();
    }

}
