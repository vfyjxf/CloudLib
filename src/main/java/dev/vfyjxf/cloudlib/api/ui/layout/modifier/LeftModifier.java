package dev.vfyjxf.cloudlib.api.ui.layout.modifier;

import org.appliedenergistics.yoga.YogaNode;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class LeftModifier implements Modifier {

    public LeftModifier(Modifier modifier) {
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
    public void apply(YogaNode node) {

    }

    public RightModifier left(int value) {
        return new RightModifier(null);
    }

    public Modifier width(int value) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
