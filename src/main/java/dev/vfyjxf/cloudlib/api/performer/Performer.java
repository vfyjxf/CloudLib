package dev.vfyjxf.cloudlib.api.performer;

import org.jetbrains.annotations.NotNull;

/**
 * A performer represents an object that can be used to perform actions.
 *
 * @param <T> the type of the actor
 */
public interface Performer<T> {

    /**
     * @param actor the actor
     * @param <T>   the type of the actor
     * @return an immutable actor
     */
    static <T> Performer<T> of(@NotNull T actor) {
        return () -> actor;
    }

    /**
     * @return the actor
     */
    T performer();

}
