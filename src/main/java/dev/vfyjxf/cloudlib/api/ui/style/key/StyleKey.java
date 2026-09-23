package dev.vfyjxf.cloudlib.api.ui.style.key;

import dev.vfyjxf.cloudlib.api.ui.style.StyleVar;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The definition of one style property — the vocabulary shared by the java dsl
 * and the css front-end.
 * <p>
 * A key is a plain data carrier: identity ({@link #id}, css name), the value
 * type it carries, which {@link StyleScope} it feeds, whether it inherits, its
 * initial value, and the three behaviours — {@link #parser} (css → T),
 * {@link #applier} (T → context effect) and {@link #formatter} (inspection).
 * <p>
 * The vocabulary is <b>closed</b>: the constructor is package-private and every
 * key is a constant on {@link Styles}. Keys are compared by identity. To expose
 * a custom, typed css property use a css custom property ({@code --name}) plus
 * a {@link StyleVar} lens instead.
 *
 * @param <T> the value type this key carries
 */
public final class StyleKey<T> {

    private final String id;
    private final Class<T> type;
    private final StyleScope scope;
    private final boolean inherited;
    private final @Nullable Supplier<T> initial;
    private final StyleParser<T> parser;
    private final StyleApply<T> applier;
    private final Function<T, String> formatter;

    StyleKey(
        String id,
        Class<T> type,
        StyleScope scope,
        boolean inherited,
        @Nullable Supplier<T> initial,
        StyleParser<T> parser,
        StyleApply<T> applier,
        Function<T, String> formatter
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.inherited = inherited;
        this.initial = initial;
        this.parser = Objects.requireNonNull(parser, "parser");
        this.applier = Objects.requireNonNull(applier, "applier");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    /** Convenience constructor for non-inherited keys with no initial value. */
    StyleKey(String id, Class<T> type, StyleScope scope, StyleParser<T> parser, StyleApply<T> applier) {
        this(id, type, scope, false, null, parser, applier, Objects::toString);
    }

    public String id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    public StyleScope scope() {
        return scope;
    }

    public boolean inherited() {
        return inherited;
    }

    public @Nullable Supplier<T> initial() {
        return initial;
    }

    public StyleParser<T> parser() {
        return parser;
    }

    public StyleApply<T> applier() {
        return applier;
    }

    public Function<T, String> formatter() {
        return formatter;
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

    @Override
    public String toString() {
        return "StyleKey[" + id + "]";
    }
}
