package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a single style property that can be applied to UI elements.
 * <p>
 * This interface is designed to be extensible - external code can implement
 * their own StyleProperty to add custom styling capabilities.
 * <p>
 * Example of custom property:
 * <pre>{@code
 * public class ShadowProperty implements StyleProperty {
 *     private final double offsetX;
 *     private final double offsetY;
 *     private final double blur;
 *     private final int color;
 *
 *     public ShadowProperty(double offsetX, double offsetY, double blur, int color) {
 *         this.offsetX = offsetX;
 *         this.offsetY = offsetY;
 *         this.blur = blur;
 *         this.color = color;
 *     }
 *
 *     @Override
 *     public void apply(StyleContext context) {
 *         context.setShadow(offsetX, offsetY, blur, color);
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "shadow";
 *     }
 * }
 * }</pre>
 *
 * @see Style
 * @see Styles
 */
@ApiStatus.Experimental
public interface StyleProperty {

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
     * Gets the unique name of this property.
     * <p>
     * This is used for property deduplication and debugging.
     * Properties with the same name will override each other
     * when styles are merged.
     *
     * @return the property name
     */
    String name();

    /**
     * Returns a string representation of this property's value.
     * <p>
     * Used for debugging and style inspection.
     *
     * @return a string representation of the value
     */
    default String valueToString() {
        return toString();
    }
}
