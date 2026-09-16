package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParseContext;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParser;
import dev.vfyjxf.cloudlib.internal.ui.style.CssEnums;
import dev.vfyjxf.cloudlib.internal.ui.style.CssValues;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A typed lens over a css custom property ({@code --name}).
 * <p>
 * Custom properties need no registration — a {@code StyleVar} is just the
 * java-side declaration of how one {@code --name} converts to a type: a
 * {@link StyleParser} for reads (resolved token stream → T), an optional
 * writer (T → css text) for {@code setVar}/{@code var(...)} writes, and an
 * optional fallback returned when the property is unset or fails to parse.
 * Mods simply keep their own constants:
 *
 * <pre>{@code
 * public static final StyleVar<Integer> bg = StyleVar.color("--bg");
 * public static final StyleVar<Float> pad = StyleVar.number("--pad")
 *         .orElse(4f);
 * }</pre>
 *
 * <pre>{@code
 * Float pad = widget.style().var(InworldVars.pad);
 * widget.setVar(InworldVars.accent, 0xFFFF6FA5);
 * }</pre>
 *
 * @param <T> the java type this variable carries
 */
public final class StyleVar<T> {

    private final String name;
    private final StyleParser<T> parser;
    private final @Nullable Function<T, String> writer;
    private final @Nullable T fallback;

    private StyleVar(String name, StyleParser<T> parser, @Nullable Function<T, String> writer, @Nullable T fallback) {
        if (name == null || !name.startsWith("--")) {
            throw new IllegalArgumentException("custom property names start with '--': " + name);
        }
        this.name = name;
        this.parser = Objects.requireNonNull(parser, "parser");
        this.writer = writer;
        this.fallback = fallback;
    }

    /** A read-only lens — writes through it throw {@link IllegalStateException}. */
    public static <T> StyleVar<T> of(String name, StyleParser<T> parser) {
        return new StyleVar<>(name, parser, null, null);
    }

    public static <T> StyleVar<T> of(String name, StyleParser<T> parser, Function<T, String> writer) {
        return new StyleVar<>(name, parser, writer, null);
    }

    public static <T> StyleVar<T> of(
            String name, StyleParser<T> parser, @Nullable Function<T, String> writer, @Nullable T fallback) {
        return new StyleVar<>(name, parser, writer, fallback);
    }

    // region builtin-grammar codecs

    /** {@code <color>} — a Minecraft ARGB int; writer emits {@code #RRGGBB(AA)}. */
    public static StyleVar<Integer> color(String name) {
        return of(
                name,
                (values, ctx) -> {
                    ComponentValue v = CssEnums.single(values);
                    Integer c = v == null ? null : CssValues.color(v);
                    if (c == null) ctx.warn("invalid <color> for " + name);
                    return c;
                },
                StyleVar::colorText);
    }

    /** {@code <number>} — a float; writer emits a plain number. */
    public static StyleVar<Float> number(String name) {
        return of(name, CssEnums.floatParser(name), StyleVar::numberText);
    }

    /** {@code <integer>} — writer emits a plain integer. */
    public static StyleVar<Integer> integer(String name) {
        return of(name, CssEnums.intParser(name), Object::toString);
    }

    /** {@code true|false}. */
    public static StyleVar<Boolean> bool(String name) {
        return of(name, CssEnums.boolParser(name), Object::toString);
    }

    /** A single {@code <ident-token>} — the ident text is the value. */
    public static StyleVar<String> ident(String name) {
        return of(
                name,
                (values, ctx) -> {
                    ComponentValue v = CssEnums.single(values);
                    if (v instanceof ComponentValue.Ident id) return id.value();
                    ctx.warn("expected a single ident for " + name);
                    return null;
                },
                Function.identity());
    }

    /** Raw access — the resolved token stream itself. */
    public static StyleVar<Tokens> tokens(String name) {
        return of(name, (values, ctx) -> Tokens.of(values), Tokens::text);
    }

    private static String colorText(int argb) {
        int alpha = argb >>> 24;
        return alpha == 0xFF ? "#%06X".formatted(argb & 0xFFFFFF) : "#%06X%02X".formatted(argb & 0xFFFFFF, alpha);
    }

    private static String numberText(float value) {
        return value == Math.floor(value) && !Float.isInfinite(value)
                ? Long.toString((long) value)
                : Float.toString(value);
    }

    // endregion

    public String name() {
        return name;
    }

    public StyleParser<T> parser() {
        return parser;
    }

    /** The value used when the property is unset or fails to parse. */
    public @Nullable T fallback() {
        return fallback;
    }

    /** A copy of this lens with a fallback value. */
    public StyleVar<T> orElse(@Nullable T fallback) {
        return new StyleVar<>(name, parser, writer, fallback);
    }

    /** A copy of this lens with a writer — makes it writable through {@code setVar}. */
    public StyleVar<T> withWriter(Function<T, String> writer) {
        return new StyleVar<>(name, parser, writer, fallback);
    }

    /** Parses a resolved token stream; {@code null} = invalid, callers use {@link #fallback()}. */
    public @Nullable T parse(List<ComponentValue> values, StyleParseContext ctx) {
        return parser.parse(values, ctx);
    }

    /** Serializes a value to css text — requires a writer. */
    public String write(T value) {
        if (writer == null) {
            throw new IllegalStateException("var " + name + " has no writer — it is read-only");
        }
        return writer.apply(value);
    }

    /** Serializes a value and tokenizes it — the {@code setVar} payload. */
    public Tokens writeTokens(T value) {
        return Tokens.of(write(value));
    }
}
