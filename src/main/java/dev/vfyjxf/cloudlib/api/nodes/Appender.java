package dev.vfyjxf.cloudlib.api.nodes;

public interface Appender<E> {
    /**
     * @param element the element to append
     * @return {@code true} if more elements can be sent,
     * and {@code false} if not.
     */
    boolean append(E element);

    /**
     * Checks whether the next stage is known to not want
     * any more elements sent to it.
     *
     * @return {@code true} if this Appender is known not to want any
     * more elements sent to it, {@code false} if otherwise
     * @apiNote This is best-effort only, once this returns {@code true} it
     * should never return {@code false} again for the same instance.
     * @implSpec The implementation in this interface returns {@code false}.
     */
    default boolean rejecting() {
        return false;
    }


}
