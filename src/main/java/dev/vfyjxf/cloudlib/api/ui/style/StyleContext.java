package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The per-widget style state {@link StyleValue}s apply onto.
 * <p>
 * Holds three slots:
 * <ul>
 *   <li>{@link #layoutStyle()} — the accumulated taffy layout style</li>
 *   <li>{@link #visualContext()} — colors, textures, border, shadow, text style</li>
 *   <li>{@link #valuesByKey} — the last value applied under every key</li>
 * </ul>
 * Application is a single path — {@link #set(StyleKey, Object)} records the
 * value, runs the key's {@code applier}, and notifies listeners. What the
 * applier writes depends on the key's {@code StyleScope}.
 */
public class StyleContext {

    /**
     * The holder.
     */
    private final Widget widget;

    private TaffyStyle layoutStyle;
    private final VisualContext visualContext;
    private final List<StyleValue<?>> applied;
    private final LinkedHashMap<StyleKey<?>, Object> valuesByKey;

    /**
     * Change listeners registered for specific style keys.
     */
    @Nullable
    private Map<StyleKey<?>, MutableList<StyleChangeListener<?>>> changeListeners;

    /**
     * Creates a StyleContext for a widget.
     *
     * @param widget the holder widget.
     */
    public StyleContext(Widget widget) {
        this.widget = widget;
        this.layoutStyle = new TaffyStyle();
        this.visualContext = new VisualContext();
        this.applied = new ArrayList<>();
        this.valuesByKey = new LinkedHashMap<>();
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
     * Layout-scoped appliers write into this object. A layout subsystem can
     * later call {@code tree.setStyle(nodeId, context.layoutStyle())}.
     */
    public TaffyStyle layoutStyle() {
        return layoutStyle;
    }

    /**
     * Gets the visual context for rendering properties.
     */
    public VisualContext visualContext() {
        return visualContext;
    }

    /**
     * Type-safe getter for a {@link StyleKey}.
     * <p>
     * The returned value is the last value set via {@link #set(StyleKey, Object)}.
     * If never set, returns {@link StyleKey#initialValue()}.
     */
    public <T> @Nullable T get(StyleKey<T> key) {
        Objects.requireNonNull(key, "key");

        Object value = valuesByKey.get(key);
        if (value != null || valuesByKey.containsKey(key)) {
            @SuppressWarnings("unchecked")
            T cast = (T) value;
            return cast;
        }

        return key.initialValue();
    }

    /**
     * Type-safe setter — the single application path for every style value.
     * <p>
     * Records the value under the key, runs the key's applier (which writes
     * into {@link #layoutStyle()} / {@link #visualContext()} for layout and
     * visual keys), appends to the applied list, and notifies listeners.
     */
    public <T> void set(StyleKey<T> key, T value) {
        Objects.requireNonNull(key, "key");

        @SuppressWarnings("unchecked")
        T oldValue = (T) valuesByKey.get(key);

        valuesByKey.put(key, value);
        applied.add(key.of(value));
        key.applier().apply(this, value);

        notifyChangeListeners(key, oldValue, value);
    }

    /** Applies a {@link StyleValue} — equivalent to {@code set(v.key(), v.value())}. */
    public void apply(StyleValue<?> value) {
        applyUntyped(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyUntyped(StyleValue value) {
        set(value.key(), value.value());
    }

    // region change listeners

    /**
     * Registers a change listener for a specific style key.
     * <p>
     * The listener is called whenever the value changes via {@link #set}.
     */
    public <T> StyleContext addChangeListener(StyleKey<T> key, StyleChangeListener<T> listener) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(listener, "listener");

        if (changeListeners == null) {
            changeListeners = new LinkedHashMap<>();
        }
        changeListeners
                .computeIfAbsent(key, k -> org.eclipse.collections.impl.factory.Lists.mutable.empty())
                .add(listener);
        return this;
    }

    /**
     * Removes a change listener for a specific style key.
     */
    public <T> boolean removeChangeListener(StyleKey<T> key, StyleChangeListener<T> listener) {
        if (changeListeners == null) return false;
        MutableList<StyleChangeListener<?>> listeners = changeListeners.get(key);
        if (listeners == null) return false;
        return listeners.remove(listener);
    }

    /**
     * Removes all change listeners for a specific style key.
     */
    public void removeChangeListeners(StyleKey<?> key) {
        if (changeListeners != null) {
            changeListeners.remove(key);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void notifyChangeListeners(StyleKey<T> key, @Nullable T oldValue, T newValue) {
        if (changeListeners == null) return;
        MutableList<StyleChangeListener<?>> listeners = changeListeners.get(key);
        if (listeners == null || listeners.isEmpty()) return;

        for (StyleChangeListener listener : listeners) {
            listener.onChanged(oldValue, newValue);
        }
    }

    // endregion

    /**
     * Every value applied to this context, in order.
     */
    public List<StyleValue<?>> applied() {
        return applied;
    }

    /**
     * Collects style information for inspection/debugging — emits every applied
     * value as {@code key-id = formatted-value}.
     */
    public void collectStyleInspection(InspectionInfoCollector collector) {
        for (StyleValue<?> value : applied) {
            collectFormatted(collector, value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void collectFormatted(InspectionInfoCollector collector, StyleValue value) {
        collector.add(value.key().id(), ((StyleKey) value.key()).format(value.value()));
    }

    /**
     * Checks if a value has been applied under the given key.
     */
    public boolean has(StyleKey<?> key) {
        return valuesByKey.containsKey(key);
    }

    /**
     * Resets the context to initial state.
     */
    public void reset() {
        applied.clear();
        valuesByKey.clear();
        layoutStyle = new TaffyStyle();
        visualContext.reset();
    }
}
