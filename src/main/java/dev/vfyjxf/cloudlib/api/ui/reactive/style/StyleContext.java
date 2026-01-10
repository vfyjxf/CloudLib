package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Context provided to {@link StyleProperty} implementations for applying styles.
 * <p>
 * StyleContext acts as a dispatcher that routes different types of properties
 * to their appropriate targets:
 * <ul>
 *   <li>{@link LayoutProperty} - Applied to {@link YogaNode} for layout calculations</li>
 *   <li>{@link VisualProperty} - Applied to {@link VisualContext} for rendering</li>
 *   <li>Generic {@link StyleProperty} - Stored for later retrieval</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * YogaNode node = YogaNode.create();
 * StyleContext context = new StyleContext(node);
 *
 * // Apply a style - layout properties go to node, visual to context
 * style.apply(context);
 *
 * // Get visual properties for rendering
 * VisualContext visual = context.getVisualContext();
 * Integer bgColor = visual.getBackgroundColor();
 * }</pre>
 *
 * @see StyleProperty
 * @see LayoutProperty
 * @see VisualProperty
 * @see VisualContext
 */
@ApiStatus.Experimental
public class StyleContext {

    @Nullable
    private final YogaNode yogaNode;
    private final VisualContext visualContext;
    private final List<StyleProperty> appliedProperties;

    /**
     * Creates a StyleContext without a YogaNode.
     * <p>
     * Layout properties will be collected but not applied.
     */
    public StyleContext() {
        this(null);
    }

    /**
     * Creates a StyleContext with a YogaNode for layout.
     *
     * @param yogaNode the Yoga node to apply layout properties to, or null
     */
    public StyleContext(@Nullable YogaNode yogaNode) {
        this.yogaNode = yogaNode;
        this.visualContext = new VisualContext();
        this.appliedProperties = new ArrayList<>();
    }

    /**
     * Gets the Yoga node for layout operations.
     *
     * @return the Yoga node, or null if not set
     */
    @Nullable
    public YogaNode getYogaNode() {
        return yogaNode;
    }

    /**
     * Gets the visual context for rendering properties.
     *
     * @return the visual context
     */
    public VisualContext getVisualContext() {
        return visualContext;
    }

    /**
     * Returns true if this context has a YogaNode.
     *
     * @return true if YogaNode is available
     */
    public boolean hasYogaNode() {
        return yogaNode != null;
    }

    /**
     * Applies a layout property to the Yoga node.
     * <p>
     * Called by {@link LayoutProperty#apply(StyleContext)}.
     *
     * @param property the layout property to apply
     */
    public void applyLayout(LayoutProperty property) {
        appliedProperties.add(property);
        if (yogaNode != null) {
            property.applyToNode(yogaNode);
        }
    }

    /**
     * Applies a visual property to the visual context.
     * <p>
     * Called by {@link VisualProperty#apply(StyleContext)}.
     *
     * @param property the visual property to apply
     */
    public void applyVisual(VisualProperty property) {
        appliedProperties.add(property);
        property.applyToWidget(visualContext);
    }

    /**
     * Records a generic style property.
     * <p>
     * For properties that don't implement LayoutProperty or VisualProperty.
     *
     * @param property the property to record
     */
    public void applyGeneric(StyleProperty property) {
        appliedProperties.add(property);
    }

    /**
     * Returns all properties that have been applied to this context.
     *
     * @return list of applied properties
     */
    public List<StyleProperty> getAppliedProperties() {
        return appliedProperties;
    }

    /**
     * Checks if a property with the given name has been applied.
     *
     * @param name the property name
     * @return true if a property with that name was applied
     */
    public boolean hasProperty(String name) {
        return appliedProperties.stream()
                .anyMatch(p -> p.name().equals(name));
    }

    /**
     * Gets the last applied property with the given name.
     *
     * @param name the property name
     * @param <T>  the expected property type
     * @return the property, or null if not found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends StyleProperty> T getProperty(String name) {
        for (int i = appliedProperties.size() - 1; i >= 0; i--) {
            StyleProperty prop = appliedProperties.get(i);
            if (prop.name().equals(name)) {
                return (T) prop;
            }
        }
        return null;
    }

    /**
     * Resets the context to initial state.
     */
    public void reset() {
        appliedProperties.clear();
        visualContext.reset();
    }

    // ==================== Legacy compatibility methods ====================
    // These methods provide backwards compatibility with existing code
    // that directly accesses StyleContext for visual properties.

    /**
     * @deprecated Use {@link VisualContext#setBackgroundColor(int)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setBackgroundColor(int color) {
        visualContext.setBackgroundColor(color);
    }

    /**
     * @deprecated Use {@link VisualContext#getBackgroundColor()} via {@link #getVisualContext()}
     */
    @Deprecated
    @Nullable
    public Integer getBackgroundColor() {
        return visualContext.getBackgroundColor();
    }

    /**
     * @deprecated Use {@link VisualContext#setBorder(double, int)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setBorder(double width, int color) {
        visualContext.setBorder(width, color);
    }

    /**
     * @deprecated Use {@link VisualContext#setBorderRadius(double)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setBorderRadius(double radius) {
        visualContext.setBorderRadius(radius);
    }

    /**
     * @deprecated Use {@link VisualContext#setBorderRadius(double, double, double, double)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setBorderRadius(double topLeft, double topRight, double bottomRight, double bottomLeft) {
        visualContext.setBorderRadius(topLeft, topRight, bottomRight, bottomLeft);
    }

    /**
     * @deprecated Use {@link VisualContext#setTextColor(int)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setTextColor(int color) {
        visualContext.setTextColor(color);
    }

    /**
     * @deprecated Use {@link VisualContext#setFontSize(double)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setFontSize(double size) {
        visualContext.setFontSize(size);
    }

    /**
     * @deprecated Use {@link VisualContext#setBold(boolean)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setBold(boolean bold) {
        visualContext.setBold(bold);
    }

    /**
     * @deprecated Use {@link VisualContext#setItalic(boolean)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setItalic(boolean italic) {
        visualContext.setItalic(italic);
    }

    /**
     * @deprecated Use {@link VisualContext#setUnderline(boolean)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setUnderline(boolean underline) {
        visualContext.setUnderline(underline);
    }

    /**
     * @deprecated Use {@link VisualContext#setStrikethrough(boolean)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setStrikethrough(boolean strikethrough) {
        visualContext.setStrikethrough(strikethrough);
    }

    /**
     * @deprecated Use {@link VisualContext#setCursor(Cursor)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setCursor(Cursor cursor) {
        visualContext.setCursor(cursor);
    }

    /**
     * @deprecated Use {@link VisualContext#setOpacity(double)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setOpacity(double opacity) {
        visualContext.setOpacity(opacity);
    }

    /**
     * @deprecated Use {@link VisualContext#setVisible(boolean)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setVisible(boolean visible) {
        visualContext.setVisible(visible);
    }

    /**
     * @deprecated Use {@link VisualContext#setCustomProperty(String, Object)} via {@link #getVisualContext()}
     */
    @Deprecated
    public void setCustom(String key, Object value) {
        visualContext.setCustomProperty(key, value);
    }

    /**
     * @deprecated Use {@link VisualContext#getCustomProperty(String, Class)} via {@link #getVisualContext()}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCustom(String key) {
        return (T) visualContext.getCustomProperty(key, Object.class);
    }

    // ==================== Legacy padding/margin/size methods for backwards compatibility ====================

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setPadding(double all) {
        setPadding(all, all, all, all);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setPadding(double vertical, double horizontal) {
        setPadding(vertical, horizontal, vertical, horizontal);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setPadding(double top, double right, double bottom, double left) {
        visualContext.setCustomProperty("_legacy_padding", new double[]{top, right, bottom, left});
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setMargin(double all) {
        setMargin(all, all, all, all);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setMargin(double vertical, double horizontal) {
        setMargin(vertical, horizontal, vertical, horizontal);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setMargin(double top, double right, double bottom, double left) {
        visualContext.setCustomProperty("_legacy_margin", new double[]{top, right, bottom, left});
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setWidth(double width) {
        visualContext.setCustomProperty("_legacy_width", width);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setHeight(double height) {
        visualContext.setCustomProperty("_legacy_height", height);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setSize(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setMinWidth(double minWidth) {
        visualContext.setCustomProperty("_legacy_minWidth", minWidth);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setMinHeight(double minHeight) {
        visualContext.setCustomProperty("_legacy_minHeight", minHeight);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setMaxWidth(double maxWidth) {
        visualContext.setCustomProperty("_legacy_maxWidth", maxWidth);
    }

    /**
     * @deprecated Layout properties should use {@link LayoutProperty} and apply to YogaNode
     */
    @Deprecated
    public void setMaxHeight(double maxHeight) {
        visualContext.setCustomProperty("_legacy_maxHeight", maxHeight);
    }
}
