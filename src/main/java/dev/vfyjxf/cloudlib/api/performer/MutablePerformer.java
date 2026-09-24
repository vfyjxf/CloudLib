package dev.vfyjxf.cloudlib.api.performer;

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

    /**
     * Removes a performer object from this performer reference.
     * <p>
     * When the removal leaves the reference without a performer, {@link #performer()} reports that
     * state with an {@link IllegalStateException} instead of returning the removed performer.
     *
     * @param performer the performer
     */
    void remove(T performer);
}
