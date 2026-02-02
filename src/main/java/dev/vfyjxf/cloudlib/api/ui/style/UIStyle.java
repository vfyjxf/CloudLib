package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A composable style definition that contains multiple {@link StyleProperty} instances.
 * <p>
 * Style supports:
 * <ul>
 *   <li>Static creation via {@link #of(StyleProperty...)}</li>
 *   <li>Chain-style composition via {@link #with(StyleProperty...)} and {@link #merge(UIStyle)}</li>
 *   <li>Property lookup and inspection</li>
 *   <li>Application to UI elements via {@link StyleContext}</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.style.Styles.*;
 *
 * // Create styles
 * var cardStyle = Style.of(
 *     padding(12),
 *     background(0xFFFFFFFF),
 *     border(1, 0xFF666666),
 *     rounded(4)
 * );
 *
 * var buttonStyle = Style.of(
 *     padding(8, 16),
 *     background(0xFF0066CC),
 *     rounded(4)
 * );
 *
 * // Compose styles
 * var hoveredButton = buttonStyle.with(
 *     background(0xFF0088FF)
 * );
 *
 * // Merge styles (later properties override earlier ones)
 * var combined = cardStyle.merge(buttonStyle);
 * }</pre>
 *
 * @see StyleProperty
 * @see UIStyles
 */
public final class UIStyle {

    /**
     * Empty style with no properties.
     */
    public static final UIStyle EMPTY = new UIStyle(Collections.emptyList());

    private final List<StyleProperty> properties;
    @Nullable
    private Map<String, StyleProperty> propertyMap;

    private UIStyle(List<StyleProperty> properties) {
        this.properties = Collections.unmodifiableList(new ArrayList<>(properties));
    }

    /**
     * Creates a new style with the given properties.
     *
     * @param properties the style properties
     * @return a new Style instance
     */
    public static UIStyle of(StyleProperty... properties) {
        if (properties.length == 0) {
            return EMPTY;
        }
        return new UIStyle(deduplicateProperties(Arrays.asList(properties)));
    }

    /**
     * Creates a new style with a single property.
     *
     * @param property the style property
     * @return a new Style instance
     */
    public static UIStyle of(StyleProperty property) {
        return new UIStyle(Collections.singletonList(property));
    }

    /**
     * Creates a new style from a list of properties.
     *
     * @param properties the style properties
     * @return a new Style instance
     */
    public static UIStyle of(List<StyleProperty> properties) {
        if (properties.isEmpty()) {
            return EMPTY;
        }
        return new UIStyle(properties);
    }

    /**
     * Creates an empty style builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new style by adding properties to this style.
     * <p>
     * If a property with the same name already exists, the new property
     * will override it in the resulting style.
     *
     * @param properties the properties to add
     * @return a new Style with the combined properties
     */
    public UIStyle with(StyleProperty... properties) {
        if (properties.length == 0) {
            return this;
        }
        List<StyleProperty> combined = new ArrayList<>(this.properties);
        combined.addAll(Arrays.asList(properties));
        return new UIStyle(deduplicateProperties(combined));
    }

    /**
     * Merges another style into this style.
     * <p>
     * Properties from the other style will override properties with
     * the same name in this style.
     *
     * @param other the style to merge
     * @return a new Style with merged properties
     */
    public UIStyle merge(UIStyle other) {
        if (other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }
        List<StyleProperty> combined = new ArrayList<>(this.properties);
        combined.addAll(other.properties);
        return new UIStyle(deduplicateProperties(combined));
    }

    /**
     * Creates a new style by transforming this style.
     *
     * @param transformer the transformation function
     * @return a new transformed Style
     */
    public UIStyle transform(UnaryOperator<Builder> transformer) {
        Builder builder = toBuilder();
        return transformer.apply(builder).build();
    }

    /**
     * Creates a new style without the specified property.
     *
     * @param propertyName the name of the property to remove
     * @return a new Style without the property
     */
    public UIStyle without(String propertyName) {
        List<StyleProperty> filtered = properties.stream()
                                                 .filter(p -> !p.type().id().equals(propertyName))
                                                 .collect(Collectors.toList());
        return new UIStyle(filtered);
    }

    /**
     * Gets a property by name.
     *
     * @param name the property name
     * @return the property, or null if not found
     */
    @Nullable
    public StyleProperty get(String name) {
        return getPropertyMap().get(name);
    }

    /**
     * Gets a typed property by name.
     *
     * @param name the property name
     * @param type the expected property type
     * @param <T>  the property type
     * @return the property, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends StyleProperty> T get(String name, Class<T> type) {
        StyleProperty property = get(name);
        if (type.isInstance(property)) {
            return (T) property;
        }
        return null;
    }

    /**
     * Checks if this style has a property with the given name.
     *
     * @param name the property name
     * @return true if the property exists
     */
    public boolean has(String name) {
        return getPropertyMap().containsKey(name);
    }

    /**
     * Gets all properties in this style.
     *
     * @return an unmodifiable list of properties
     */
    public List<StyleProperty> getProperties() {
        return properties;
    }

    /**
     * Checks if this style is empty.
     *
     * @return true if no properties
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * Gets the number of properties.
     *
     * @return the property count
     */
    public int size() {
        return properties.size();
    }

    /**
     * Applies this style to a StyleContext.
     *
     * @param context the context to apply to
     */
    public void apply(StyleContext context) {
        for (StyleProperty property : properties) {
            property.apply(context);
        }
    }

    /**
     * Creates a new StyleContext and applies this style to it.
     *
     * @return a new StyleContext with this style applied
     */
    public StyleContext createContext() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Converts this style to a builder for modification.
     *
     * @return a new builder with this style's properties
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.properties.addAll(this.properties);
        return builder;
    }

    private Map<String, StyleProperty> getPropertyMap() {
        if (propertyMap == null) {
            propertyMap = new LinkedHashMap<>();
            for (StyleProperty property : properties) {
                propertyMap.put(property.type().id(), property);
            }
        }
        return propertyMap;
    }

    /**
     * Removes duplicate properties, keeping the last occurrence.
     */
    private static List<StyleProperty> deduplicateProperties(List<StyleProperty> properties) {
        Map<String, StyleProperty> map = new LinkedHashMap<>();
        for (StyleProperty property : properties) {
            map.put(property.type().id(), property);
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UIStyle style)) return false;
        return Objects.equals(properties, style.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "Style.EMPTY";
        }
        StringBuilder sb = new StringBuilder("Style.of(\n");
        for (int i = 0; i < properties.size(); i++) {
            StyleProperty property = properties.get(i);
            sb.append("    ").append(property.type().id()).append(": ").append(property);
            if (i < properties.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Mutable builder for Style.
     */
    public static final class Builder {

        private final List<StyleProperty> properties = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a property to the builder.
         *
         * @param property the property to add
         * @return this builder
         */
        public Builder add(StyleProperty property) {
            properties.add(property);
            return this;
        }

        /**
         * Adds multiple properties to the builder.
         *
         * @param properties the properties to add
         * @return this builder
         */
        public Builder add(StyleProperty... properties) {
            this.properties.addAll(Arrays.asList(properties));
            return this;
        }

        /**
         * Adds all properties from another style.
         *
         * @param style the style to add from
         * @return this builder
         */
        public Builder add(UIStyle style) {
            this.properties.addAll(style.getProperties());
            return this;
        }

        /**
         * Removes a property by name.
         *
         * @param type the property type to remove
         * @return this builder
         */
        public Builder remove(StyleType<?> type) {
            properties.removeIf(p -> p.type().equals(type));
            return this;
        }

        /**
         * Clears all properties.
         *
         * @return this builder
         */
        public Builder clear() {
            properties.clear();
            return this;
        }

        /**
         * Builds the final Style.
         *
         * @return the built Style
         */
        public UIStyle build() {
            if (properties.isEmpty()) {
                return EMPTY;
            }
            return new UIStyle(deduplicateProperties(properties));
        }
    }
}
