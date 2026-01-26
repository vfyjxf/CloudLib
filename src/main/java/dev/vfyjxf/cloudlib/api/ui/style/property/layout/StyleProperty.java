package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;

/**
 * Represents a single style property that can be applied to UI elements.
 * <p>
 * This interface is designed to be extensible - external code can implement
 * their own StyleProperty to add custom styling capabilities.
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
}
