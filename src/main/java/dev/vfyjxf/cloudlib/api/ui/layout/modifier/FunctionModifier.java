package dev.vfyjxf.cloudlib.api.ui.layout.modifier;

import org.appliedenergistics.yoga.YogaNode;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FunctionModifier implements Modifier {

    private final Consumer<YogaNode> consumer;

    public FunctionModifier(Consumer<YogaNode> consumer) {
        this.consumer = consumer;
    }

    @Override
    public <R> R foldIn(R initial, BiFunction<R, Modifier, R> operation) {
        return initial;
    }

    @Override
    public <R> R foldOut(R initial, BiFunction<R, Modifier, R> operation) {
        return initial;
    }

    @Override
    public boolean any(Predicate<Modifier> predicate) {
        return false;
    }

    @Override
    public boolean all(Predicate<Modifier> predicate) {
        return true;
    }

    @Override
    public void apply(YogaNode node) {
        consumer.accept(node);
    }
}
