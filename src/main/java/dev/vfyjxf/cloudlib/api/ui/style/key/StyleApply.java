package dev.vfyjxf.cloudlib.api.ui.style.key;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;

/**
 * The apply half of a {@link StyleKey}: writes a typed value into the widget's
 * style state (taffy layout style, visual context, or custom storage).
 *
 * @param <T> the key's value type
 */
@FunctionalInterface
public interface StyleApply<T> {

    void apply(StyleContext context, T value);
}
