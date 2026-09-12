package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.style.key.StyleEntry;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import org.jetbrains.annotations.Nullable;

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
 * import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
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
    public static final UIStyle empty = new UIStyle(Collections.emptyList());

    private final List<StyleValue<?>> values;

    @Nullable
    private Map<StyleKey<?>, StyleValue<?>> valueMap;

    private UIStyle(List<StyleValue<?>> values) {
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    // region factories

    /**
     * Creates a style from entries — {@link StyleValue}s and {@link
     * dev.vfyjxf.cloudlib.api.ui.style.key.StyleValues} groups both accepted
     * (groups flatten).
     */
    public static UIStyle of(StyleEntry... entries) {
        if (entries.length == 0) {
            return empty;
        }
        return new UIStyle(deduplicate(flatten(Arrays.asList(entries))));
    }

    /**
     * Creates a style from a list of style values.
     */
    public static UIStyle ofValues(List<StyleValue<?>> values) {
        if (values.isEmpty()) {
            return empty;
        }
        return new UIStyle(deduplicate(values));
    }

    /**
     * Creates a style from values already unique per key — skips the dedup pass.
     * Contract: at most one value per {@link StyleKey}; the list is copied.
     */
    public static UIStyle ofDistinctValues(List<StyleValue<?>> values) {
        if (values.isEmpty()) {
            return empty;
        }
        return new UIStyle(values);
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
     * same key.
     */
    public UIStyle with(StyleEntry... entries) {
        if (entries.length == 0) {
            return this;
        }
        List<StyleValue<?>> combined = new ArrayList<>(this.values);
        combined.addAll(flatten(Arrays.asList(entries)));
        return new UIStyle(deduplicate(combined));
    }

    /**
     * Merges another style into this style — the other style's values win for
     * the same key.
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
        return new UIStyle(deduplicate(combined));
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
        return new UIStyle(filtered);
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

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }

    // endregion

    // region apply

    /**
     * Applies every value to the context, in order.
     */
    public void apply(StyleContext context) {
        for (StyleValue<?> value : values) {
            context.apply(value);
        }
    }

    /**
     * Converts this style to a builder for modification.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.values.addAll(this.values);
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

    private static List<StyleValue<?>> flatten(List<StyleEntry> entries) {
        List<StyleValue<?>> out = new ArrayList<>(entries.size());
        for (StyleEntry entry : entries) {
            entry.collectInto(out::add);
        }
        return out;
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
        return Objects.equals(values, style.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
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
            if (i < values.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    // endregion

    /**
     * Mutable builder for UIStyle.
     */
    public static final class Builder {

        private final List<StyleValue<?>> values = new ArrayList<>();

        private Builder() {}

        /**
         * Adds an entry (value or value group) to the builder.
         */
        public Builder add(StyleEntry entry) {
            entry.collectInto(values::add);
            return this;
        }

        /**
         * Adds multiple entries to the builder.
         */
        public Builder add(StyleEntry... entries) {
            for (StyleEntry entry : entries) {
                entry.collectInto(values::add);
            }
            return this;
        }

        /**
         * Adds all values from another style.
         */
        public Builder add(UIStyle style) {
            this.values.addAll(style.values());
            return this;
        }

        /**
         * Removes a key's value.
         */
        public Builder remove(StyleKey<?> key) {
            values.removeIf(v -> v.key() == key);
            return this;
        }

        public Builder clear() {
            values.clear();
            return this;
        }

        public UIStyle build() {
            if (values.isEmpty()) {
                return empty;
            }
            return new UIStyle(deduplicate(values));
        }
    }
}
