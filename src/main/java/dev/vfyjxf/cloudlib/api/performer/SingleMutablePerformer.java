package dev.vfyjxf.cloudlib.api.performer;

import org.jetbrains.annotations.NotNull;

class SingleMutablePerformer<T> implements MutablePerformer<T> {
    private T performer;

    public SingleMutablePerformer(T performer) {
        this.performer = performer;
    }

    @Override
    public void put(T actor) {
        this.performer = actor;
    }

    @Override
    public void remove(@NotNull T actor) {
        if (this.performer == actor) {
            this.performer = null;
        }
    }

    @Override
    public T performer() {
        return performer;
    }
}
