package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;

class StandardMutableState<T> implements MutableState<T> {

    private final CheckStrategy<T> checkStrategy;
    private T value;
    private boolean changed;

    StandardMutableState(CheckStrategy<T> checkStrategy) {
        this.checkStrategy = checkStrategy;
    }

    @Override
    public T set(T value) {
        var old = this.value;
        this.value = value;
        changed = checkStrategy.matches(old, value);
        return old;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public boolean changed() {
        var changed = this.changed;
        this.changed = false;
        return changed;
    }
}
