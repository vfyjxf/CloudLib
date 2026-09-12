package dev.vfyjxf.cloudlib.api.performer;

/**
 * Common priorities of performers.
 */
public final class PerformerPriorities {

    public static final int high = 100;
    public static final int normal = 0;
    public static final int low = -100;

    private PerformerPriorities() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
