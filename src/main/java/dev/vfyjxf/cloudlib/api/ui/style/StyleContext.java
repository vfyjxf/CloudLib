package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.NodeId;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Context provided to {@link StyleProperty} implementations for applying styles.
 * <p>
 * StyleContext acts as a dispatcher that routes different types of properties
 * to their appropriate targets:
 * <ul>
 *   <li>{@link LayoutProperty} - Applied to {@link NodeId} for layout calculations</li>
 *   <li>{@link VisualProperty} - Applied to {@link VisualContext} for rendering</li>
 *   <li>Generic {@link StyleProperty} - Stored for later retrieval</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * NodeId nodeId = ...;
 * StyleContext context = new StyleContext(nodeId);
 *
 * // Apply a style - layout properties go to node, visual to context
 * style.apply(context);
 *
 * // Get visual properties for rendering
 * VisualContext visual = context.visualContext();
 * Integer bgColor = visual.backgroundColor();
 * }</pre>
 *
 * @see StyleProperty
 * @see LayoutProperty
 * @see VisualProperty
 * @see VisualContext
 */
public class StyleContext {

    /**
     * The holder.
     */
    private final Widget widget;
    private TaffyStyle layoutStyle;
    private final VisualContext visualContext;
    private final List<StyleProperty> appliedProperties;
    private final LinkedHashMap<StyleType<?>, Object> valuesByType;

    /**
     * Change listeners registered for specific style types.
     */
    @Nullable
    private Map<StyleType<?>, MutableList<StyleChangeListener<?>>> changeListeners;

    /**
     * Creates a StyleContext for a taffy layout node.
     *
     * @param widget the holder widget.
     */
    public StyleContext(Widget widget) {
        this.widget = widget;
        this.layoutStyle = new TaffyStyle();
        this.visualContext = new VisualContext();
        this.appliedProperties = new ArrayList<>();
        this.valuesByType = new LinkedHashMap<>();
    }

    /**
     * Gets the holder widget.
     */
    public Widget widget() {
        return widget;
    }

    /**
     * Gets the accumulated taffy layout style for this node.
     * <p>
     * Layout properties write into this object. A layout subsystem can later
     * call {@code tree.setStyle(nodeId, context.layoutStyle())}.
     */
    public TaffyStyle layoutStyle() {
        return layoutStyle;
    }

    /**
     * Gets the visual context for rendering properties.
     *
     * @return the visual context
     */
    public VisualContext visualContext() {
        return visualContext;
    }

    /**
     * Type-safe getter for a {@link StyleType}. This allows advanced users to bypass the Property API.
     * <p>
     * The returned value is the last value set via {@link #set(StyleType, Object)}.
     * If never set, returns {@link StyleType#initValue()}.
     */
    public <T extends @Nullable Object> T get(StyleType<T> type) {
        Objects.requireNonNull(type, "type");

        Object value = valuesByType.get(type);
        if (value != null || valuesByType.containsKey(type)) {
            @SuppressWarnings("unchecked")
            T cast = (T) value;
            return cast;
        }

        return type.initValue() == null ? null : type.initValue().get();
    }

    /**
     * Type-safe setter for a {@link StyleType}. This allows advanced users to bypass the Property API.
     * <p>
     * Note: If the {@link StyleType} has an applier, this will also immediately write the value into
     * {@link #layoutStyle()} / {@link #visualContext()}.
     */
    public <T> void set(StyleType<T> type, T value) {
        Objects.requireNonNull(type, "type");

        // Capture old value for change notification
        @SuppressWarnings("unchecked")
        T oldValue = (T) valuesByType.get(type);

        valuesByType.put(type, value);

        StyleType.Applier<T> applier = type.applier();
        if (applier != null) {
            applier.apply(this, value);
        }

        // Notify change listeners
        notifyChangeListeners(type, oldValue, value);
    }

    //region change listeners

    /**
     * Registers a change listener for a specific style type.
     * <p>
     * The listener will be called whenever the property value changes via {@link #set(StyleType, Object)}.
     *
     * @param type     the style type to listen for
     * @param listener the listener to register
     * @param <T>      the type of the property value
     * @return this context for chaining
     */
    public <T> StyleContext addChangeListener(StyleType<T> type, StyleChangeListener<T> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");

        if (changeListeners == null) {
            changeListeners = new LinkedHashMap<>();
        }
        changeListeners.computeIfAbsent(type, k -> org.eclipse.collections.impl.factory.Lists.mutable.empty())
                       .add(listener);
        return this;
    }

    /**
     * Removes a change listener for a specific style type.
     *
     * @param type     the style type
     * @param listener the listener to remove
     * @param <T>      the type of the property value
     * @return true if the listener was removed
     */
    public <T> boolean removeChangeListener(StyleType<T> type, StyleChangeListener<T> listener) {
        if (changeListeners == null) return false;
        MutableList<StyleChangeListener<?>> listeners = changeListeners.get(type);
        if (listeners == null) return false;
        return listeners.remove(listener);
    }

    /**
     * Removes all change listeners for a specific style type.
     *
     * @param type the style type
     */
    public void removeChangeListeners(StyleType<?> type) {
        if (changeListeners != null) {
            changeListeners.remove(type);
        }
    }

    /**
     * Notifies all registered listeners about a property change.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void notifyChangeListeners(StyleType<T> type, @Nullable T oldValue, T newValue) {
        if (changeListeners == null) return;
        MutableList<StyleChangeListener<?>> listeners = changeListeners.get(type);
        if (listeners == null || listeners.isEmpty()) return;

        for (StyleChangeListener listener : listeners) {
            listener.onChanged(oldValue, newValue);
        }
    }

    //endregion

    /**
     * Applies a layout property to the taffy style.
     * <p>
     * Called by {@link LayoutProperty#apply(StyleContext)}.
     */
    public void applyLayout(LayoutProperty property) {
        appliedProperties.add(property);
        property.applyToStyle(layoutStyle);
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
     * Collects style information for inspection/debugging.
     * <p>
     * This method iterates through all applied properties and collects
     * their inspection information using StyleProperty.collectInspection().
     *
     * @param collector the collector to add style properties to
     */
    public void collectStyleInspection(InspectionInfoCollector collector) {
        for (StyleProperty property : appliedProperties) {
            property.collectInspection(collector);
        }
    }

    /**
     * Checks if a property with the given name has been applied.
     *
     * @param type the property type
     * @return true if a property with that type was applied
     */
    public boolean hasProperty(StyleType<?> type) {
        return valuesByType.containsKey(type)
                || appliedProperties.stream()
                                    .anyMatch(p -> p.type().equals(type));
    }

    /**
     * Gets the last applied property with the given type.
     *
     * @param type the property type
     * @param <T>  the expected property type
     * @return the property, or null if not found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends StyleProperty> T getProperty(StyleType<?> type) {
        for (int i = appliedProperties.size() - 1; i >= 0; i--) {
            StyleProperty prop = appliedProperties.get(i);
            if (prop.type().equals(type)) {
                return (T) prop;
            }
        }
        return null;
    }

    public <T extends StyleProperty> void setProperty(String name, T property) {

    }

    /**
     * Resets the context to initial state.
     */
    public void reset() {
        appliedProperties.clear();
        valuesByType.clear();
        layoutStyle = new TaffyStyle();
        visualContext.reset();
    }

}
