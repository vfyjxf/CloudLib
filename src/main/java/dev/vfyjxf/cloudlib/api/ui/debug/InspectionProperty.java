package dev.vfyjxf.cloudlib.api.ui.debug;

import org.jetbrains.annotations.Nullable;

/**
 * A single inspected property. Values are pre-formatted as strings
 * to avoid recursive {@code toString()} calls on complex objects.
 */
public record InspectionProperty(
    String name,
    String value,
    @Nullable String defaultValue,
    String category
) {

    public static final String categoryBasic = "basic";
    public static final String categoryLayout = "layout";
    public static final String categoryState = "state";
    public static final String categoryVisual = "visual";
    public static final String categoryData = "data";

    /** Whether the current value differs from the default. */
    public boolean isNonDefault() {
        if (defaultValue == null) return true;
        return !defaultValue.equals(value);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    // Format an arbitrary object into a display string.
    static String format(@Nullable Object obj) {
        return switch (obj) {
            case null -> "null";
            case String s -> "\"" + s + "\"";
            case Boolean b -> b.toString();
            case Number n -> n.toString();
            case Enum<?> e -> e.name();
            default -> obj.toString();
        };
    }
}
