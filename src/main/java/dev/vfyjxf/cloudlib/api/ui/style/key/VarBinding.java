package dev.vfyjxf.cloudlib.api.ui.style.key;

import dev.vfyjxf.cloudlib.api.css.Tokens;

import java.util.Objects;

/**
 * A css custom-property binding — {@code --name → raw token stream}.
 * <p>
 * This is the style-entry counterpart of a {@code --x: …} declaration: it can
 * sit inside {@code UIStyle.of(...)} / {@code Widget.useStyle(...)} next to
 * builtin {@link StyleValue}s. The tokens are the <em>specified</em> value —
 * {@code var()} references inside them are resolved against the node's
 * computed custom-property environment (inline vars win over theme vars).
 */
public record VarBinding(String name, Tokens value) implements StyleEntry {

    public VarBinding {
        if (name == null || !name.startsWith("--")) {
            throw new IllegalArgumentException("custom property names start with '--': " + name);
        }
        Objects.requireNonNull(value, "value");
    }

    @Override
    public void collectInto(StyleCollector out) {
        out.var(name, value);
    }
}
