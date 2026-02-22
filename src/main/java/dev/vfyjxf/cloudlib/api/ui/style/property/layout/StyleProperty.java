package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single style property that can be applied to UI elements.
 * <p>
 * This interface is designed to be extensible - external code can implement
 * their own StyleProperty to add custom styling capabilities.
 * <p>
 * StyleProperty now supports inspection via {@link #collectInspection(InspectionInfoCollector)},
 * allowing properties to contribute to the Inspector debug view.
 *
 * @see UIStyle
 * @see UIStyles
 */
public interface StyleProperty {

    /**
     * Gets the unique type of this property.
     * <p>
     * This is used for property deduplication and debugging.
     * Properties with the same type will override each other
     * when styles are merged.
     *
     * @return the property type
     */
    StyleType<?> type();

    /**
     * Applies this property to the given style context.
     * <p>
     * The context provides access to the underlying styling system
     * and allows the property to modify the element's appearance.
     *
     * @param context the style context to apply to
     */
    void apply(StyleContext context);

    /**
     * Gets the current value of this property for inspection.
     * <p>
     * This is used by the default inspection implementation.
     * Override to provide a typed value for inspection display.
     *
     * @return the current value, or null if not applicable
     */
    default @Nullable Object inspectionValue() {
        return null;
    }

    /**
     * Collects inspection information for this property.
     * <p>
     * The default implementation uses the StyleType metadata to add
     * the property to the collector. Override to customize the inspection
     * display or add multiple sub-properties.
     *
     * @param collector the collector to add properties to
     */
    @SuppressWarnings("unchecked")
    default void collectInspection(InspectionInfoCollector collector) {
        StyleType<?> type = type();
        Object value = inspectionValue();
        if (value != null) {
            String formatted = ((StyleType<Object>) type).format(value);
            String defaultFormatted = type.defaultValue() != null
                ? ((StyleType<Object>) type).format(type.defaultValue()) : null;
            collector.addFormatted(type.displayName(), formatted, defaultFormatted, type.category());
        }
    }
}
