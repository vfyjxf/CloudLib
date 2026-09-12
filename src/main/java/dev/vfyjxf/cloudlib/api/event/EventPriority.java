package dev.vfyjxf.cloudlib.api.event;

/**
 * The priorities of events.
 */
public final class EventPriority {

    public static final int highest = 100;
    public static final int high = 50;
    public static final int normal = 10;
    public static final int low = 5;
    public static final int lowest = 0;

    private EventPriority() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
