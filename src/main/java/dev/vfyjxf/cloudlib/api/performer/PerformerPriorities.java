package dev.vfyjxf.cloudlib.api.performer;

/**
 * Common priorities of performers.
 */
public final class PerformerPriorities {

    public static final int HIGH = 100;
    public static final int DEFAULT = 0;
    public static final int LOW = -100;

    private PerformerPriorities() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
