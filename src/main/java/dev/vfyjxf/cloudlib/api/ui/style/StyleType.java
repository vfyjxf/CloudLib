package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A globally-registered style "slot" identified by {@link #id}.
 * <p>
 * {@link StyleType} is used as the unique key for deduplicating {@link StyleProperty} instances,
 * and can also be used directly for type-safe access via {@link StyleContext#get(StyleType)} / {@link StyleContext#set(StyleType, Object)}.
 * <p>
 * StyleType now also carries metadata for inspection support:
 * <ul>
 *   <li>{@link #displayName} - Human-readable name for Inspector UI</li>
 *   <li>{@link #category} - Category for grouping (layout/visual)</li>
 *   <li>{@link #formatter} - Custom value formatter for inspection display</li>
 * </ul>
 *
 * @param <T> the type of value this style holds
 */
public final class StyleType<T> {

    //region static registry

    private static final LinkedHashMap<String, StyleType<?>> TYPES = new LinkedHashMap<>();

    /**
     * Gets a registered StyleType by ID.
     *
     * @param id the style type ID
     * @return the StyleType, or null if not found
     */
    public static @Nullable StyleType<?> get(String id) {
        return TYPES.get(id);
    }

    /**
     * Gets all registered StyleTypes.
     *
     * @return an unmodifiable view of all registered types
     */
    public static Iterable<StyleType<?>> all() {
        return TYPES.values();
    }

    //endregion

    //region fields

    private final String id;
    private final String displayName;
    private final String category;
    private final @Nullable Supplier<T> initValue;
    private final @Nullable Applier<T> applier;
    private final @Nullable Function<T, String> formatter;

    //endregion

    //region constructors

    private StyleType(
        String id,
        String displayName,
        String category,
        @Nullable Supplier<T> initValue,
        @Nullable Applier<T> applier,
        @Nullable Function<T, String> formatter
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.category = Objects.requireNonNull(category, "category");
        this.initValue = initValue;
        this.applier = applier;
        this.formatter = formatter;

        if (TYPES.put(id, this) != null) {
            throw new IllegalArgumentException("Duplicate style type ID: " + id);
        }
    }

    //endregion

    //region factory methods

    /**
     * Creates a new StyleType builder.
     *
     * @param id the unique identifier
     * @param <T> the value type
     * @return a new builder
     */
    public static <T> Builder<T> builder(String id) {
        return new Builder<>(id);
    }

    /**
     * Creates a simple StyleType with only id and initValue.
     * Uses id as displayName, "layout" as category.
     *
     * @param id        the unique identifier
     * @param initValue the initial value supplier (may be null)
     * @param <T>       the value type
     * @return a new StyleType
     */
    public static <T> StyleType<T> of(String id, @Nullable Supplier<T> initValue) {
        return new StyleType<>(id, id, InspectionProperty.CATEGORY_LAYOUT, initValue, null, null);
    }

    /**
     * Creates a StyleType with id, initValue, and applier.
     * Uses id as displayName, "layout" as category.
     *
     * @param id        the unique identifier
     * @param initValue the initial value supplier (may be null)
     * @param applier   the applier function (may be null)
     * @param <T>       the value type
     * @return a new StyleType
     */
    public static <T> StyleType<T> of(String id, @Nullable Supplier<T> initValue, @Nullable Applier<T> applier) {
        return new StyleType<>(id, id, InspectionProperty.CATEGORY_LAYOUT, initValue, applier, null);
    }

    /**
     * Creates a visual StyleType (uses "visual" category).
     *
     * @param id        the unique identifier
     * @param initValue the initial value supplier (may be null)
     * @param <T>       the value type
     * @return a new StyleType
     */
    public static <T> StyleType<T> visual(String id, @Nullable Supplier<T> initValue) {
        return new StyleType<>(id, id, InspectionProperty.CATEGORY_VISUAL, initValue, null, null);
    }

    /**
     * Creates a visual StyleType with applier.
     *
     * @param id        the unique identifier
     * @param initValue the initial value supplier (may be null)
     * @param applier   the applier function (may be null)
     * @param <T>       the value type
     * @return a new StyleType
     */
    public static <T> StyleType<T> visual(String id, @Nullable Supplier<T> initValue, @Nullable Applier<T> applier) {
        return new StyleType<>(id, id, InspectionProperty.CATEGORY_VISUAL, initValue, applier, null);
    }

    //endregion

    //region getters

    /**
     * Gets the unique identifier.
     */
    public String id() {
        return id;
    }

    /**
     * Gets the human-readable display name for inspection UI.
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Gets the category for grouping in inspection (e.g., "layout", "visual").
     */
    public String category() {
        return category;
    }

    /**
     * Gets the initial value supplier.
     */
    public @Nullable Supplier<T> initValue() {
        return initValue;
    }

    /**
     * Gets the applier function.
     */
    public @Nullable Applier<T> applier() {
        return applier;
    }

    /**
     * Gets the value formatter for inspection display.
     */
    public @Nullable Function<T, String> formatter() {
        return formatter;
    }

    /**
     * Gets the default value (evaluates initValue supplier).
     *
     * @return the default value, or null if no initValue
     */
    public @Nullable T defaultValue() {
        return initValue != null ? initValue.get() : null;
    }

    //endregion

    //region formatting

    /**
     * Formats a value for inspection display.
     *
     * @param value the value to format
     * @return the formatted string
     */
    public String format(@Nullable T value) {
        if (value == null) {
            return "null";
        }
        if (formatter != null) {
            return formatter.apply(value);
        }
        return defaultFormat(value);
    }

    private static String defaultFormat(Object value) {
        return switch (value) {
            case String s -> "\"" + s + "\"";
            case Enum<?> e -> e.name();
            default -> value.toString();
        };
    }

    //endregion

    //region equals/hashCode

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StyleType<?> styleType = (StyleType<?>) o;
        return id.equals(styleType.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StyleType[" + id + "]";
    }

    //endregion

    //region inner types

    /**
     * Functional interface for applying style values to a context.
     *
     * @param <T> the value type
     */
    @FunctionalInterface
    public interface Applier<T> {
        void apply(StyleContext context, T value);
    }

    /**
     * Builder for creating StyleType instances with full customization.
     *
     * @param <T> the value type
     */
    public static final class Builder<T> {
        private final String id;
        private String displayName;
        private String category = InspectionProperty.CATEGORY_LAYOUT;
        private @Nullable Supplier<T> initValue;
        private @Nullable Applier<T> applier;
        private @Nullable Function<T, String> formatter;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = id; // default to id
        }

        /**
         * Sets the display name for inspection UI.
         */
        public Builder<T> displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        /**
         * Sets the category for grouping.
         */
        public Builder<T> category(String category) {
            this.category = Objects.requireNonNull(category, "category");
            return this;
        }

        /**
         * Sets the initial value supplier.
         */
        public Builder<T> initValue(@Nullable Supplier<T> initValue) {
            this.initValue = initValue;
            return this;
        }

        /**
         * Sets the initial value directly.
         */
        public Builder<T> initValue(@Nullable T value) {
            this.initValue = value == null ? null : () -> value;
            return this;
        }

        /**
         * Sets the applier function.
         */
        public Builder<T> applier(@Nullable Applier<T> applier) {
            this.applier = applier;
            return this;
        }

        /**
         * Sets the value formatter for inspection display.
         */
        public Builder<T> formatter(@Nullable Function<T, String> formatter) {
            this.formatter = formatter;
            return this;
        }

        /**
         * Builds the StyleType.
         */
        public StyleType<T> build() {
            return new StyleType<>(id, displayName, category, initValue, applier, formatter);
        }
    }

    //endregion
}
