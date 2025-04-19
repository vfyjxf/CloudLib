package dev.vfyjxf.cloudlib.api.performer;

import org.jetbrains.annotations.NotNull;

public interface MutablePerformer<T> extends Performer<T> {

    static <T> MutablePerformer<T> mutableOf(T performer) {
        return new SingleMutablePerformer<>(performer);
    }

    /**
     * Puts a performer object to this performer reference.
     *
     * @param performer the performer
     */
    void put(T performer);

    void remove(@NotNull T performer);

}
