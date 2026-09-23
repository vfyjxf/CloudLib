package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleCollector;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleEntry;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParseContext;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValues;
import dev.vfyjxf.cloudlib.api.ui.style.key.VarBinding;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A composable style definition — an ordered set of {@link StyleValue}s keyed
 * by {@link StyleKey}.
 * <p>
 * Later writers win for the same key. Built via {@link #of(StyleEntry...)} —
 * shorthand factories that expand to several longhands flatten transparently
 * through {@link StyleEntry}.
 * <p>
 * Example usage:
 * <pre>{@code
 * import static UIStyles.*;
 *
 * var cardStyle = UIStyle.of(
 *     padding(12),
 *     background(0xFFFFFFFF),
 *     border(1),
 *     display(TaffyDisplay.flex)
 * );
 *
 * var combined = cardStyle.merge(buttonStyle);   // button's values win per key
 * style.apply(widget.style());                   // writes into the context
 * }</pre>
 *
 * @see StyleKey
 * @see StyleValue
 * @see UIStyles
 */
public final class UIStyle {

    /**
     * Empty style with no values.
     */
    public static final UIStyle empty = new UIStyle(Collections.emptyList(), Map.of());

    private final List<StyleValue<?>> values;
    private final Map<String, Tokens> vars;

    @Nullable
    private Map<StyleKey<?>, StyleValue<?>> valueMap;

    private UIStyle(List<StyleValue<?>> values, Map<String, Tokens> vars) {
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
        this.vars = vars.isEmpty() ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(vars));
    }

    // region factories

    /**
     * Creates a style from entries — {@link StyleValue}s, {@link
     * StyleValues} groups and {@link
     * VarBinding}s all accepted (groups
     * and var bindings flatten).
     */
    public static UIStyle of(StyleEntry... entries) {
        if (entries.length == 0) {
            return empty;
        }
        List<StyleValue<?>> values = new ArrayList<>();
        Map<String, Tokens> vars = new LinkedHashMap<>();
        collect(Arrays.asList(entries), values, vars);
        return new UIStyle(deduplicate(values), vars);
    }

    /**
     * Creates a style from a list of style values.
     */
    public static UIStyle ofValues(List<StyleValue<?>> values) {
        if (values.isEmpty()) {
            return empty;
        }
        return new UIStyle(deduplicate(values), Map.of());
    }

    /**
     * Creates a style from values already unique per key — skips the dedup pass.
     * Contract: at most one value per {@link StyleKey}; the list is copied.
     */
    public static UIStyle ofDistinctValues(List<StyleValue<?>> values) {
        return ofDistinctValues(values, Map.of());
    }

    /**
     * Resolved-style factory — distinct builtin values plus the computed
     * custom-property bindings ({@code --name → resolved tokens}). Used by the
     * theme engine.
     */
    public static UIStyle ofDistinctValues(List<StyleValue<?>> values, Map<String, Tokens> vars) {
        if (values.isEmpty() && vars.isEmpty()) {
            return empty;
        }
        return new UIStyle(values, vars);
    }

    /**
     * Creates an empty style builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    // endregion

    // region composition

    /**
     * Creates a new style with the entries appended — later entries win for the
     * same key / var name.
     */
    public UIStyle with(StyleEntry... entries) {
        if (entries.length == 0) {
            return this;
        }
        List<StyleValue<?>> combined = new ArrayList<>(this.values);
        Map<String, Tokens> combinedVars = new LinkedHashMap<>(this.vars);
        collect(Arrays.asList(entries), combined, combinedVars);
        return new UIStyle(deduplicate(combined), combinedVars);
    }

    /**
     * Merges another style into this style — the other style's values and vars
     * win for the same key / var name.
     */
    public UIStyle merge(UIStyle other) {
        if (other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }
        List<StyleValue<?>> combined = new ArrayList<>(this.values);
        combined.addAll(other.values);
        Map<String, Tokens> combinedVars = new LinkedHashMap<>(this.vars);
        combinedVars.putAll(other.vars);
        return new UIStyle(deduplicate(combined), combinedVars);
    }

    /**
     * Creates a new style by transforming this style.
     */
    public UIStyle transform(UnaryOperator<Builder> transformer) {
        Builder builder = toBuilder();
        return transformer.apply(builder).build();
    }

    /**
     * Creates a new style without the given key.
     */
    public UIStyle without(StyleKey<?> key) {
        List<StyleValue<?>> filtered = new ArrayList<>(values.size());
        for (StyleValue<?> v : values) {
            if (v.key() != key) {
                filtered.add(v);
            }
        }
        return new UIStyle(filtered, vars);
    }

    /**
     * Creates a new style without the given custom property.
     */
    public UIStyle withoutVar(String name) {
        if (!vars.containsKey(name)) {
            return this;
        }
        Map<String, Tokens> filtered = new LinkedHashMap<>(vars);
        filtered.remove(name);
        return new UIStyle(values, filtered);
    }

    // endregion

    // region access

    /**
     * Gets the value bound to a key, or null if absent.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable StyleValue<T> get(StyleKey<T> key) {
        return (StyleValue<T>) getValueMap().get(key);
    }

    /**
     * Checks if a value exists under the key.
     */
    public boolean has(StyleKey<?> key) {
        return getValueMap().containsKey(key);
    }

    /**
     * All style values, in order.
     */
    public List<StyleValue<?>> values() {
        return values;
    }

    /**
     * The custom-property bindings — {@code --name → token stream}. Resolved
     * styles (from {@link Theme#resolve}) carry the computed values with
     * {@code var()} already substituted.
     */
    public Map<String, Tokens> vars() {
        return vars;
    }

    /** The raw token stream bound to {@code --name}, or {@code null}. */
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

    public boolean isEmpty() {
        return values.isEmpty() && vars.isEmpty();
    }

    public int size() {
        return values.size();
    }

    // endregion

    // region apply

    /**
     * Applies every value to the context, in order — builtin values through
     * their appliers, then the var bindings into the context's var table.
     */
    public void apply(StyleContext context) {
        for (StyleValue<?> value : values) {
            context.apply(value);
        }
        vars.forEach(context::setVar);
    }

    /**
     * Converts this style to a builder for modification.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.values.addAll(this.values);
        builder.vars.putAll(this.vars);
        return builder;
    }

    // endregion

    // region internal

    private Map<StyleKey<?>, StyleValue<?>> getValueMap() {
        if (valueMap == null) {
            valueMap = new LinkedHashMap<>();
            for (StyleValue<?> value : values) {
                valueMap.put(value.key(), value);
            }
        }
        return valueMap;
    }

    private static void collect(List<StyleEntry> entries, List<StyleValue<?>> values, Map<String, Tokens> vars) {
        StyleCollector out = new StyleCollector() {
            @Override
            public void accept(StyleValue<?> value) {
                values.add(value);
            }

            @Override
            public void var(String name, Tokens value) {
                vars.put(name, value);
            }
        };
        for (StyleEntry entry : entries) {
            entry.collectInto(out);
        }
    }

    /**
     * Removes duplicate keys, keeping the last occurrence (stable order).
     */
    private static List<StyleValue<?>> deduplicate(List<StyleValue<?>> values) {
        Map<StyleKey<?>, StyleValue<?>> map = new LinkedHashMap<>();
        for (StyleValue<?> value : values) {
            map.put(value.key(), value);
        }
        return new ArrayList<>(map.values());
    }

    // endregion

    // region object

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UIStyle style)) return false;
        return Objects.equals(values, style.values) && Objects.equals(vars, style.vars);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values, vars);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "UIStyle.EMPTY";
        }
        StringBuilder sb = new StringBuilder("UIStyle.of(\n");
        for (int i = 0; i < values.size(); i++) {
            StyleValue<?> value = values.get(i);
            sb.append("    ").append(value.key().id()).append(": ").append(value.value());
            sb.append(",\n");
        }
        vars.forEach((name, tokens) -> sb.append("    ").append(name).append(": ").append(tokens).append(",\n"));
        sb.setLength(sb.length() - 2);
        sb.append("\n)");
        return sb.toString();
    }

    // endregion

    /**
     * Mutable builder for UIStyle.
     */
    public static final class Builder {

        private final List<StyleValue<?>> values = new ArrayList<>();
        private final Map<String, Tokens> vars = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Adds an entry (value, value group or var binding) to the builder.
         */
        public Builder add(StyleEntry entry) {
            entry.collectInto(sink);
            return this;
        }

        /**
         * Adds multiple entries to the builder.
         */
        public Builder add(StyleEntry... entries) {
            for (StyleEntry entry : entries) {
                entry.collectInto(sink);
            }
            return this;
        }

        /**
         * Adds all values and vars from another style.
         */
        public Builder add(UIStyle style) {
            this.values.addAll(style.values());
            this.vars.putAll(style.vars());
            return this;
        }

        /** Binds a custom property to a raw token stream. */
        public Builder var(String name, Tokens value) {
            vars.put(name, value);
            return this;
        }

        /** Binds a custom property to tokenized css source. */
        public Builder var(String name, String cssValue) {
            return var(name, Tokens.of(cssValue));
        }

        /** Binds a custom property through a {@link StyleVar} lens. */
        public <T> Builder var(StyleVar<T> var, T value) {
            return var(var.name(), var.writeTokens(value));
        }

        /**
         * Removes a key's value.
         */
        public Builder remove(StyleKey<?> key) {
            values.removeIf(v -> v.key() == key);
            return this;
        }

        /** Removes a custom-property binding. */
        public Builder removeVar(String name) {
            vars.remove(name);
            return this;
        }

        public Builder clear() {
            values.clear();
            vars.clear();
            return this;
        }

        public UIStyle build() {
            if (values.isEmpty() && vars.isEmpty()) {
                return empty;
            }
            return new UIStyle(deduplicate(values), vars);
        }

        private final StyleCollector sink = new StyleCollector() {
            @Override
            public void accept(StyleValue<?> value) {
                values.add(value);
            }

            @Override
            public void var(String name, Tokens value) {
                vars.put(name, value);
            }
        };
    }
}
