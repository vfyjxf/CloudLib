package dev.vfyjxf.cloudlib.api.ui.style.key;

import java.util.function.Consumer;

/**
 * Anything that can contribute {@link StyleValue}s to a style — either a single
 * value ({@link StyleValue}) or a group produced by a shorthand factory
 * ({@link StyleValues}).
 * <p>
 * {@code UIStyle.of(...)} and {@code Widget.useStyle(...)} accept
 * {@code StyleEntry} varargs so {@code padding(4)}-style factories that expand
 * into several longhands flatten transparently.
 */
public sealed interface StyleEntry permits StyleValue, StyleValues {

    /** Emits every style value this entry stands for, in order. */
    void collectInto(Consumer<StyleValue<?>> out);
}
