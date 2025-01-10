package dev.vfyjxf.cloudlib.api.actor;

import org.jetbrains.annotations.NotNull;

class MutableActorImpl<T> implements MutableActor<T> {
    private T actor;

    public MutableActorImpl(T actor) {
        this.actor = actor;
    }

    @Override
    public void put(T actor) {
        this.actor = actor;
    }

    @Override
    public void remove(@NotNull T actor) {
        if (this.actor == actor) {
            this.actor = null;
        }
    }

    @Override
    public T actor() {
        return actor;
    }
}
