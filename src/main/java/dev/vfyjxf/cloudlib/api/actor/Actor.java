package dev.vfyjxf.cloudlib.api.actor;

import org.jetbrains.annotations.NotNull;

/**
 * An actor represents an object that can be used to perform actions.
 *
 * @param <T> the type of the actor
 */
public interface Actor<T> {

    /**
     * @param actor the actor
     * @param <T>   the type of the actor
     * @return an immutable actor
     */
    static <T> Actor<T> of(@NotNull T actor) {
        return () -> actor;
    }

    /**
     * @return the actor
     */
    T actor();

}
