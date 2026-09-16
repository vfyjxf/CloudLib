package dev.vfyjxf.cloudlib.api.ui.style.key;

import dev.vfyjxf.cloudlib.api.css.Tokens;

/**
 * The sink {@link StyleEntry}s emit into — accepts both builtin
 * {@link StyleValue}s and custom-property bindings ({@code --name → tokens}).
 */
public interface StyleCollector {

    /** Collects a builtin property value. */
    void accept(StyleValue<?> value);

    /** Collects a css custom-property binding ({@code --name → token stream}). */
    void var(String name, Tokens value);
}
