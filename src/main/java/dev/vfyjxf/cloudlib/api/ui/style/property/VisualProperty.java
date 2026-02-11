package dev.vfyjxf.cloudlib.api.ui.style.property;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;

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
 *   <li>Border: visual border styling (color, etc.) - for layout border, use layout properties</li>
 *   <li>Text: color, font style</li>
 *   <li>Opacity: transparency</li>
 *   <li>Shadow: drop shadows (if your renderer supports it)</li>
 * </ul>
 *
 * @see StyleProperty
 * @see LayoutProperty
 * @see VisualContext
 */
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
