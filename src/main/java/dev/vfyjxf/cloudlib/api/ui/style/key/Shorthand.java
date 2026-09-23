package dev.vfyjxf.cloudlib.api.ui.style.key;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A css shorthand — a declaration name that expands into several longhand
 * {@link StyleValue}s at declaration-collection time.
 * <p>
 * The {@link BuiltinKeys} vocabulary is longhand-only; shorthand names live
 * in a separate fixed table there and expand to longhand values that then
 * compete in the cascade under their own key ids.
 */
@FunctionalInterface
public interface Shorthand {

    /**
     * Expands the declaration value into longhand style values, in css order.
     *
     * @return the expanded values, or {@code null} when the declaration is
     *         invalid and must be dropped
     */
    @Nullable
    List<StyleValue<?>> expand(List<ComponentValue> values, StyleParseContext ctx);
}
