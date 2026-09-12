package dev.vfyjxf.cloudlib.api.ui.style.key;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;

/**
 * A {@link StyleKey} bound to a concrete value — the unit every style front-end
 * (java dsl, css declaration) produces.
 *
 * @param <T> the value type
 */
public record StyleValue<T>(StyleKey<T> key, T value) implements StyleEntry {

    /** Applies this value through the key's applier. */
    public void apply(StyleContext context) {
        key.applier().apply(context, value);
    }

    @Override
    public void collectInto(java.util.function.Consumer<StyleValue<?>> out) {
        out.accept(this);
    }
}
