package dev.vfyjxf.cloudlib.api.performer;

import org.jetbrains.annotations.NotNull;

public interface MutablePerformer<T> extends Performer<T> {

    static <T> MutablePerformer<T> mutableOf(T performer) {
        return new SingleMutablePerformer<>(performer);
    }

    /**
     * Puts an actor object to this actor reference.
     *
     * @param actor the actor
     */
    void put(T actor);

    void remove(@NotNull T actor);

}
