package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.Declaration;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.key.Shorthand;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParseContext;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.internal.ui.style.Cascade;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The {@link Theme#resolve} implementation: the cascade produces the winning
 * declarations in order; each then goes through the builtin vocabulary —
 * longhand names parse through their key's {@code parser}, shorthand names
 * expand into longhand {@link StyleValue}s — and the resulting values land in
 * a {@code Map<StyleKey, StyleValue>} where later declarations win per key.
 */
final class ThemeEngine {

    private ThemeEngine() {}

    static UIStyle resolve(Cascade.ResolveContext ctx, Theme theme, Widget node, @Nullable Consumer<String> warn) {
        Map<String, Cascade.ResolvedDecl> resolved = ctx.resolve(node);
        var parseCtx = new StyleParseContext(theme.id().toString());
        Consumer<String> warnSink = warn != null ? warn : s -> {};

        // winners emit in declaration order; each expands/parses into longhand
        // StyleValues — per-key, last write wins. var-free declarations are
        // node-independent → memoize their parsed values on declaration identity.
        Map<StyleKey<?>, StyleValue<?>> out = new LinkedHashMap<>();
        Map<String, Tokens> vars = new LinkedHashMap<>();
        Map<Declaration, List<StyleValue<?>>> valueCache = ctx.valueCache();
        for (Map.Entry<String, Cascade.ResolvedDecl> e : resolved.entrySet()) {
            String name = e.getKey();
            Cascade.ResolvedDecl decl = e.getValue();
            List<ComponentValue> tokens = decl.value();
            if (name.startsWith("--")) {
                // custom properties are style values — the resolved (var()-
                // substituted) token stream lands on the node's var table
                if (!tokens.isEmpty()) {
                    vars.put(name, Tokens.of(tokens));
                }
                continue;
            }
            if (tokens.isEmpty()) {
                continue; // poisoned by an unresolved var()
            }
            List<StyleValue<?>> values;
            if (!decl.hadVar() && (values = valueCache.get(decl.declaration())) != null) {
                for (StyleValue<?> sv : values) {
                    out.put(sv.key(), sv);
                }
                continue;
            }
            values = parseDecl(name, tokens, parseCtx, warnSink);
            if (values == null) {
                continue;
            }
            if (!decl.hadVar()) {
                valueCache.put(decl.declaration(), values);
            }
            for (StyleValue<?> sv : values) {
                out.put(sv.key(), sv);
            }
        }
        return out.isEmpty() && vars.isEmpty()
                ? UIStyle.empty
                : UIStyle.ofDistinctValues(new ArrayList<>(out.values()), vars);
    }

    /** Parses one resolved declaration into longhand values (key or shorthand). */
    private static @Nullable List<StyleValue<?>> parseDecl(
            String name, List<ComponentValue> tokens, StyleParseContext parseCtx, Consumer<String> warn) {
        // css-wide keywords resolve upstream in the cascade; a lone `inherit`
        // token reaching here means "inherit" — drop it (parent fill already ran)
        if (tokens.size() == 1
                && tokens.get(0) instanceof ComponentValue.Ident id
                && id.value().equalsIgnoreCase("inherit")) {
            return null;
        }
        StyleKey<?> key = Styles.byId(name);
        if (key != null) {
            StyleValue<?> value = parseValue(key, tokens, parseCtx, warn, name);
            return value == null ? null : List.of(value);
        }
        Shorthand shorthand = Styles.shorthand(name);
        if (shorthand != null) {
            return expand(shorthand, tokens, parseCtx, warn, name);
        }
        warn.accept("unknown property '" + name + "'");
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable StyleValue<?> parseValue(
            StyleKey key, List<ComponentValue> tokens, StyleParseContext parseCtx, Consumer<String> warn, String name) {
        try {
            Object parsed = key.parser().parse(tokens, parseCtx);
            if (parsed == null) {
                warn.accept("invalid value for '" + name + "'");
                return null;
            }
            return key.of(parsed);
        } catch (RuntimeException e) {
            warn.accept("invalid value for '" + name + "': " + e.getMessage());
            return null;
        }
    }

    private static @Nullable List<StyleValue<?>> expand(
            Shorthand shorthand,
            List<ComponentValue> tokens,
            StyleParseContext parseCtx,
            Consumer<String> warn,
            String name) {
        try {
            List<StyleValue<?>> expanded = shorthand.expand(tokens, parseCtx);
            if (expanded == null) {
                warn.accept("invalid value for '" + name + "'");
            }
            return expanded;
        } catch (RuntimeException e) {
            warn.accept("invalid value for '" + name + "': " + e.getMessage());
            return null;
        }
    }
}
