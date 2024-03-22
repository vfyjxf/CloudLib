package dev.vfyjxf.cloudlib.ui.state;

import dev.vfyjxf.cloudlib.api.ui.state.IState;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class MutableState<T> implements IState<T> {

    protected final MutableList<Consumer<T>> listeners = Lists.mutable.empty();
    protected T value;

    public MutableState(T value) {
        this.value = value;
    }

    @Override
    public @NotNull T get() {
        if (value == null) throw new NullPointerException("Value is null!");
        return value;
    }

    @Override
    public @Nullable T getNullable() {
        return value;
    }

    @Override
    public <U> IState<U> map(Function<T, U> mapper) {
        return new MutableState<>(mapper.apply(value));
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
