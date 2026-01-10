package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

/**
 * A style property that applies to the layout engine (Yoga).
 * <p>
 * Layout properties control the positioning, sizing, and arrangement of UI elements
 * using the Yoga flexbox layout system. These properties are applied directly to
 * {@link YogaNode} instances.
 * <p>
 * Common layout properties include:
 * <ul>
 *   <li>Size: width, height, minWidth, maxWidth, etc.</li>
 *   <li>Padding: internal spacing</li>
 *   <li>Margin: external spacing</li>
 *   <li>Flex: flexGrow, flexShrink, flexBasis, flexDirection</li>
 *   <li>Alignment: alignItems, alignSelf, alignContent, justifyContent</li>
 *   <li>Gap: spacing between flex children</li>
 *   <li>Position: positionType, position edges</li>
 * </ul>
 * <p>
 * Example of custom layout property:
 * <pre>{@code
 * public class FlexGrowProperty implements LayoutProperty {
 *     private final float grow;
 *
 *     public FlexGrowProperty(float grow) {
 *         this.grow = grow;
 *     }
 *
 *     @Override
 *     public void applyToNode(YogaNode node) {
 *         node.setFlexGrow(grow);
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "flex-grow";
 *     }
 * }
 * }</pre>
 *
 * @see StyleProperty
 * @see VisualProperty
 */
@ApiStatus.Experimental
public interface LayoutProperty extends StyleProperty {

    /**
     * Applies this layout property to the given Yoga node.
     * <p>
     * This method is called when building the layout tree.
     *
     * @param node the Yoga node to apply layout to
     */
    void applyToNode(YogaNode node);

    /**
     * Default implementation that delegates to {@link StyleContext#applyLayout(LayoutProperty)}.
     */
    @Override
    default void apply(StyleContext context) {
        context.applyLayout(this);
    }
}
