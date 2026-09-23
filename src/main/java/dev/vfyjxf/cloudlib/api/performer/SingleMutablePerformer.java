package dev.vfyjxf.cloudlib.api.performer;

import dev.vfyjxf.cloudlib.util.Checks;
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
        return Checks.checkNotNull(performer, "performer");
    }
}
