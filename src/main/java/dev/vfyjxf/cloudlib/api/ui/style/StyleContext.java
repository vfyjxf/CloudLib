package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParseContext;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.internal.ui.style.Cascade;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
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
     * The computed custom-property table — {@code --name → token stream}.
     * Populated by theme resolution (values already {@code var()}-substituted)
     * and by inline writes via {@link #setVar}.
     */
    private final Map<String, Tokens> vars = new LinkedHashMap<>();

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

    // region custom properties (--*)

    /**
     * The computed custom-property table — every {@code --name} currently bound
     * on this widget, values already {@code var()}-substituted.
     */
    public Map<String, Tokens> vars() {
        return Collections.unmodifiableMap(vars);
    }

    /** The resolved token stream bound to {@code --name}, or {@code null}. */
    public @Nullable Tokens varRaw(String name) {
        return vars.get(name);
    }

    public boolean hasVar(String name) {
        return vars.containsKey(name);
    }

    /**
     * Reads a custom property through a {@link StyleVar} lens — parses the
     * resolved tokens, falling back to {@link StyleVar#fallback()} when the
     * property is unset or unparseable.
     */
    public <T> @Nullable T var(StyleVar<T> var) {
        Tokens tokens = vars.get(var.name());
        if (tokens == null || tokens.isEmpty()) {
            return var.fallback();
        }
        T parsed = var.parse(tokens.values(), StyleParseContext.plain());
        return parsed != null ? parsed : var.fallback();
    }

    /**
     * Binds a custom property — the application path for both theme-resolved
     * bindings and java-side inline writes.
     * <p>
     * {@code var()} references inside {@code value} are substituted against the
     * vars already in this table; an unresolvable substitution keeps the raw
     * tokens (the binding may become meaningful once later vars arrive).
     */
    public void setVar(String name, Tokens value) {
        vars.put(name, Cascade.substituteVars(value, vars));
    }

    /** Typed write — serializes through the lens's writer. */
    public <T> void setVar(StyleVar<T> var, T value) {
        setVar(var.name(), var.writeTokens(value));
    }

    /** Removes a custom-property binding. */
    public void removeVar(String name) {
        vars.remove(name);
    }

    // endregion

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
        changeListeners.computeIfAbsent(key, k -> Lists.mutable.empty()).add(listener);
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
     * value as {@code key-id = formatted-value}, then every bound custom
     * property under its {@code --name} as css text.
     */
    public void collectStyleInspection(InspectionInfoCollector collector) {
        for (StyleValue<?> value : applied) {
            collectFormatted(collector, value);
        }
        vars.forEach((name, tokens) -> collector.add(name, tokens.text(), InspectionProperty.categoryVars));
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
        vars.clear();
        layoutStyle = new TaffyStyle();
        visualContext.reset();
    }
}
