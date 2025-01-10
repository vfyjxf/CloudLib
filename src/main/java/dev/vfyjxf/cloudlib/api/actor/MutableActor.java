package dev.vfyjxf.cloudlib.api.actor;

import org.jetbrains.annotations.NotNull;

public interface MutableActor<T> extends Actor<T> {

    static <T> MutableActor<T> mutableOf(T actor) {
        return new MutableActorImpl<>(actor);
    }

    void put(T actor);

    void remove(@NotNull T actor);

}
