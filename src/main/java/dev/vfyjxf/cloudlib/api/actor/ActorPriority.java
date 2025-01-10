package dev.vfyjxf.cloudlib.api.actor;

/**
 * Common priorities of actors.
 */
public final class ActorPriority {

    public static final int HIGH = 100;
    public static final int DEFAULT = 0;
    public static final int LOW = -100;

    private ActorPriority() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
