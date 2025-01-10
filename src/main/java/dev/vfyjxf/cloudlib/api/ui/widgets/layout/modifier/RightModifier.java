package dev.vfyjxf.cloudlib.api.ui.widgets.layout.modifier;

import dev.vfyjxf.cloudlib.api.ui.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;

import java.util.function.BiFunction;
import java.util.function.Predicate;

//TODO: Implement the RightModifier
public class RightModifier implements Modifier {

    public RightModifier(Modifier modifier){

    }

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

    public LeftModifier right(int value) {
        return new LeftModifier(null);
    }

    public Modifier width(int value) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
