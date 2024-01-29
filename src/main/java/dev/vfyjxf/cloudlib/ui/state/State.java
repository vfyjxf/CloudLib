package dev.vfyjxf.cloudlib.ui.state;

import dev.vfyjxf.cloudlib.api.ui.state.IState;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class State<T> implements IState<T> {

    protected final MutableList<Consumer<T>> listeners = Lists.mutable.empty();
    protected T value;

    public State(T value) {
        this.value = value;
    }

    @Override
    public @Nullable T get() {
        return value;
    }

    @Override
    public IState<T> set(T value) {
        this.value = value;
        listeners.forEach(listener -> listener.accept(value));
        return this;
    }

    @Override
    public IState<T> onSet(Consumer<T> consumer) {
        listeners.add(consumer);
        return this;
    }

}
