package dev.vfyjxf.cloudlib.api.ui.style.key;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The definition of one style property — the single vocabulary shared by the
 * java dsl and the css front-end.
 * <p>
 * A key is a plain data record: identity ({@link #id}, css name), the value
 * type it carries, which {@link StyleScope} it feeds, whether it inherits,
 * its initial value, and the three behaviours — {@link #parser} (css → T),
 * {@link #applier} (T → context effect) and {@link #formatter} (inspection).
 * <p>
 * Keys are predefined constants on {@link Styles}; mods register their own via
 * {@link StyleRegistry#register(StyleKey)} from the plugin hook.
 *
 * @param <T> the value type this key carries
 */
public record StyleKey<T>(
        String id,
        Class<T> type,
        StyleScope scope,
        boolean inherited,
        @Nullable Supplier<T> initial,
        StyleParser<T> parser,
        StyleApply<T> applier,
        Function<T, String> formatter) {

    public StyleKey {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(applier, "applier");
        Objects.requireNonNull(formatter, "formatter");
    }

    /**
     * Convenience constructor for non-inherited keys with no initial value.
     */
    public StyleKey(String id, Class<T> type, StyleScope scope, StyleParser<T> parser, StyleApply<T> applier) {
        this(id, type, scope, false, null, parser, applier, Objects::toString);
    }

    /**
     * Binds a value to this key — the unit {@code useStyle}/{@code UIStyle.of}
     * consume.
     */
    public StyleValue<T> of(T value) {
        return new StyleValue<>(this, value);
    }

    /**
     * The initial value for this key, or {@code null} when unset.
     * A fresh value is produced per call so mutable value types stay safe.
     */
    public @Nullable T initialValue() {
        return initial == null ? null : initial.get();
    }

    /** Formats a value for the inspector. */
    public String format(@Nullable T value) {
        return value == null ? "null" : formatter.apply(value);
    }
}
