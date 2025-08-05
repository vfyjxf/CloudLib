package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;

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

class SnapshotState<T> implements MutableState<T> {
    private final Snapshot.MutableRef<T> snapshot;

    SnapshotState(CheckStrategy<T> strategy, T initialValue) {
        this.snapshot = Snapshot.mutableRefOf(strategy);
        this.snapshot.currentState(initialValue);
    }

    SnapshotState(Snapshot.MutableRef<T> snapshot) {this.snapshot = snapshot;}

    @Override
    public T set(T value) {
        return null;
    }

    @Override
    public T value() {
        return null;
    }

    @Override
    public boolean changed() {
        return false;
    }
}
