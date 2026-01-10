package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

/**
 * A style property that applies visual styling to UI elements.
 * <p>
 * Visual properties control the appearance of UI elements but do not affect
 * layout calculations. These properties are applied to the widget's rendering
 * context.
 * <p>
 * Common visual properties include:
 * <ul>
 *   <li>Background: color, image, gradient</li>
 *   <li>Border: color, radius (visual, not layout border)</li>
 *   <li>Text: color, font size, font style</li>
 *   <li>Opacity: transparency</li>
 *   <li>Cursor: mouse cursor style</li>
 *   <li>Shadow: drop shadows</li>
 * </ul>
 * <p>
 * Example of custom visual property:
 * <pre>{@code
 * public class ShadowProperty implements VisualProperty {
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
 *     public void applyToWidget(VisualContext context) {
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
 * @see StyleProperty
 * @see LayoutProperty
 * @see VisualContext
 */
@ApiStatus.Experimental
public interface VisualProperty extends StyleProperty {

    /**
     * Applies this visual property to the given visual context.
     * <p>
     * This method is called when rendering the widget.
     *
     * @param context the visual context to apply styles to
     */
    void applyToWidget(VisualContext context);

    /**
     * Default implementation that delegates to {@link StyleContext#applyVisual(VisualProperty)}.
     */
    @Override
    default void apply(StyleContext context) {
        context.applyVisual(this);
    }
}
