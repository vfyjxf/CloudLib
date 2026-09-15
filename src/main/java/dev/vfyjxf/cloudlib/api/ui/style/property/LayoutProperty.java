package dev.vfyjxf.cloudlib.api.ui.style.property;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * A style property that applies to the layout engine (taffy).
 * <p>
 * Layout properties control the positioning, sizing, and arrangement of UI elements
 * using the taffy layout system. These properties are applied to a {@link TaffyStyle}
 * instance which is then consumed by a {@code TaffyTree}.
 * <p>
 * Common layout properties include:
 * <ul>
 *   <li>Size: width, height, minWidth, maxWidth, etc.</li>
 *   <li>Padding: internal spacing</li>
 *   <li>Margin: external spacing</li>
 *   <li>Flex: flexGrow, flexShrink, flexBasis, flexDirection</li>
 *   <li>Alignment: alignItems, alignSelf, alignContent, justifyContent</li>
 *   <li>Gap: spacing between flex children</li>
 *   <li>Position: position, inset edges</li>
 * </ul>
 *
 * @see StyleProperty
 * @see VisualProperty
 */
public interface LayoutProperty extends StyleProperty {

    /**
     * Applies this layout property to the given taffy Style.
     * <p>
     * This method is called when building/updating the layout tree.
     *
     * @param style the taffy style to apply layout to
     */
    void applyToStyle(TaffyStyle style);

    /**
     * Default implementation that delegates to {@link StyleContext#applyLayout(LayoutProperty)}.
     */
    @Override
    default void apply(StyleContext context) {
        context.applyLayout(this);
    }
}
