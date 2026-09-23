package dev.vfyjxf.cloudlib.api.performer;

/**
 * A performer represents an object that can be used to perform actions.
 *
 * @param <T> the type of the performer
 */
public interface Performer<T> {

    /**
     * @param performer the performer
     * @param <T>       the type of the performer
     * @return an immutable performer
     */
    static <T> Performer<T> of(T performer) {
        return () -> performer;
    }

    /**
     * @return the performer
     */
    T performer();
}
