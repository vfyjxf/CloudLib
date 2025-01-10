package dev.vfyjxf.cloudlib.api.event;

/**
 * The priorities of events.
 */
public final class EventPriority {

    public static final int HIGHEST = 100;
    public static final int HIGH = 50;
    public static final int DEFAULT = 10;
    public static final int LOW = 5;
    public static final int LOWEST = 0;


    private EventPriority() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
