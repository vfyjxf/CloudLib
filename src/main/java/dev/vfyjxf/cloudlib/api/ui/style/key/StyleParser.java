package dev.vfyjxf.cloudlib.api.ui.style.key;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The css front-end of a {@link StyleKey}: turns the component-value list of a
 * declaration into the key's typed value.
 *
 * @param <T> the key's value type
 */
@FunctionalInterface
public interface StyleParser<T> {

    /**
     * Parses the declaration value.
     *
     * @param values the component values between {@code name:} and {@code ;}
     * @param ctx    shared parse context (warnings, source info)
     * @return the parsed value, or {@code null} when the declaration is invalid
     *         and must be dropped
     */
    @Nullable
    T parse(List<ComponentValue> values, StyleParseContext ctx);
}
