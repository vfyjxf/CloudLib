package dev.vfyjxf.cloudlib.api.ui.debug;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a single inspected property of a widget.
 *
 * @param name         the property name
 * @param value        the current value (may be null)
 * @param defaultValue the default value for comparison (may be null)
 * @param category     the category for grouping properties
 */
public record InspectionProperty(
    String name,
    @Nullable Object value,
    @Nullable Object defaultValue,
    String category
) {

    /** Default category for basic widget properties. */
    public static final String CATEGORY_BASIC = "basic";
    /** Category for layout-related properties. */
    public static final String CATEGORY_LAYOUT = "layout";
    /** Category for state-related properties. */
    public static final String CATEGORY_STATE = "state";
    /** Category for visual-related properties. */
    public static final String CATEGORY_VISUAL = "visual";
    /** Category for data-related properties. */
    public static final String CATEGORY_DATA = "data";

    /** Creates a property with default category. */
    public static InspectionProperty of(String name, @Nullable Object value) {
        return new InspectionProperty(name, value, null, CATEGORY_BASIC);
    }

    /** Creates a property with specified category. */
    public static InspectionProperty of(String name, @Nullable Object value, String category) {
        return new InspectionProperty(name, value, null, category);
    }

    /** Creates a property with default value for comparison. */
    public static InspectionProperty withDefault(String name, @Nullable Object value, @Nullable Object defaultValue) {
        return new InspectionProperty(name, value, defaultValue, CATEGORY_BASIC);
    }

    /** Creates a property with default value and category. */
    public static InspectionProperty withDefault(String name, @Nullable Object value, @Nullable Object defaultValue, String category) {
        return new InspectionProperty(name, value, defaultValue, category);
    }

    /**
     * Checks if the current value differs from the default value.
     *
     * @return true if value differs from default, or if no default is set
     */
    public boolean isNonDefault() {
        if (defaultValue == null) {
            return value != null;
        }
        return !defaultValue.equals(value);
    }

    /** Gets a formatted string representation of the value. */
    public String formattedValue() {
        return switch (value) {
            case null -> "null";
            case String s -> "\"" + s + "\"";
            case Number n -> value.toString();
            case Boolean b -> b.toString();
            case Enum<?> e -> e.name();
            default -> value.toString();
        };
    }

    @Override
    public String toString() {
        return name + "=" + formattedValue();
    }
}
