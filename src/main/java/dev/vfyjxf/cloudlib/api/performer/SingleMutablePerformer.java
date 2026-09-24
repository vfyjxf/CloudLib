package dev.vfyjxf.cloudlib.api.performer;

import org.jspecify.annotations.Nullable;

class SingleMutablePerformer<T> implements MutablePerformer<T> {
    private @Nullable T performer;

    public SingleMutablePerformer(T performer) {
        this.performer = performer;
    }

    @Override
    public void put(T performer) {
        this.performer = performer;
    }

    @Override
    public void remove(T performer) {
        if (this.performer == performer) {
            this.performer = null;
        }
    }

    @Override
    public T performer() {
        @Nullable
        T current = performer;
        if (current == null) {
            throw new IllegalStateException("performer was removed from this MutablePerformer");
        }
        return current;
    }
}
