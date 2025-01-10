package dev.vfyjxf.cloudlib.api.ui.modifier;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A special modifier that use list to store all modifier's functions
 */
public class ListModifier<T extends Modifier> implements Modifier {

    private final Modifier outer;
    private final MutableList<Modifier> modifiers = Lists.mutable.empty();

    public ListModifier(Modifier outer) {
        this.outer = outer;
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

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    @Override
    public Modifier then(Consumer<Widget> function) {
        return combine().then(function);
    }

    @Override
    public Modifier then(Modifier other) {
        return combine().then(other);
    }

    public T add(Modifier modifier) {
        modifiers.add(modifier);
        return self();
    }

    public T add(Consumer<Widget> function) {
        modifiers.add(new FunctionModifier(function));
        return self();
    }

    public Modifier combine() {
        var modifier = outer;
        for (Modifier current : modifiers) {
            modifier = modifier.then(current);
        }
        return modifier;
    }

    @Override
    public void apply(Widget widget) {

    }
}
