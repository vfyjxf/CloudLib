package dev.vfyjxf.cloudlib.api.ui.style.key;

/**
 * Anything that can contribute to a style — a builtin value ({@link
 * StyleValue}), a group produced by a shorthand factory ({@link StyleValues}),
 * or a css custom-property binding ({@link VarBinding}).
 * <p>
 * {@code UIStyle.of(...)} and {@code Widget.useStyle(...)} accept
 * {@code StyleEntry} varargs so {@code padding(4)}-style factories and
 * {@code var("--x", "4px")} bindings flatten transparently.
 */
public sealed interface StyleEntry permits StyleValue, StyleValues, VarBinding {

    /** Emits every contribution this entry stands for, in order. */
    void collectInto(StyleCollector out);
}
