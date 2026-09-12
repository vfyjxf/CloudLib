package dev.vfyjxf.cloudlib.api.ui.theme;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.Declaration;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.key.Shorthand;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParseContext;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleRegistry;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.internal.ui.theme.Cascade;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Resolves a {@link Theme} against a {@link Widget} into a {@link UIStyle}.
 * <p>
 * Pipeline: cascade (match → specificity/order → var() → inherit) produces the
 * winning declarations in order; each declaration then goes through the
 * {@link StyleRegistry} — longhand names parse through their key's
 * {@code parser}, shorthand names expand into longhand {@link StyleValue}s —
 * and the resulting values land in a {@code Map<StyleKey, StyleValue>} where
 * later declarations win per key.
 * <p>
 * For whole-tree work, {@link #applyTree(Theme, Widget, Consumer)} shares one
 * {@link Cascade.ResolveContext} across the subtree — ancestors resolve once.
 */
public final class ThemeEngine {

    private ThemeEngine() {}

    // region resolve

    /**
     * Resolves the winning declarations for {@code node} into a {@code UIStyle}.
     *
     * @param theme the theme to resolve against
     * @param node  the widget
     * @param warn  sink for dropped/invalid declarations (may be null)
     */
    public static UIStyle resolve(Theme theme, Widget node, @Nullable Consumer<String> warn) {
        return resolve(new Cascade.ResolveContext(theme), theme, node, warn);
    }

    /** Convenience overload without a warning sink. */
    public static UIStyle resolve(Theme theme, Widget node) {
        return resolve(theme, node, null);
    }

    /** Resolves through a shared cascade context — used by scene dirty flushes. */
    public static UIStyle resolveShared(Theme theme, Widget node, Cascade.ResolveContext ctx) {
        return resolve(ctx, theme, node, null);
    }

    private static UIStyle resolve(
            Cascade.ResolveContext ctx, Theme theme, Widget node, @Nullable Consumer<String> warn) {
        Map<String, Cascade.ResolvedDecl> resolved = ctx.resolve(node);
        var parseCtx = new StyleParseContext(theme.id().toString());
        Consumer<String> warnSink = warn != null ? warn : s -> {};

        // winners emit in declaration order; each expands/parses into longhand
        // StyleValues — per-key, last write wins. var-free declarations are
        // node-independent → memoize their parsed values on declaration identity.
        Map<StyleKey<?>, StyleValue<?>> out = new LinkedHashMap<>();
        Map<Declaration, List<StyleValue<?>>> valueCache = ctx.valueCache();
        for (Map.Entry<String, Cascade.ResolvedDecl> e : resolved.entrySet()) {
            String name = e.getKey();
            Cascade.ResolvedDecl decl = e.getValue();
            if (name.startsWith("--")) {
                continue; // custom properties are var() inputs, not style props
            }
            List<ComponentValue> tokens = decl.value();
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
        return out.isEmpty() ? UIStyle.empty : UIStyle.ofDistinctValues(new ArrayList<>(out.values()));
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
        Shorthand shorthand = StyleRegistry.get().shorthand(name);
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

    // endregion

    // region tree application

    /**
     * Re-resolves the theme for {@code root} and its whole subtree, sharing one
     * cascade context so ancestors resolve once per pass. Each widget applies the
     * resolved style into its theme segment — see {@link Widget#applyThemeStyle}.
     */
    public static void applyTree(Theme theme, Widget root, @Nullable Consumer<String> warn) {
        Cascade.ResolveContext ctx = new Cascade.ResolveContext(theme);
        applyTree(ctx, theme, root, warn);
    }

    private static void applyTree(
            Cascade.ResolveContext ctx, Theme theme, Widget widget, @Nullable Consumer<String> warn) {
        widget.applyThemeStyle(resolve(ctx, theme, widget, warn));
        if (widget instanceof CompositeWidget<?> composite) {
            composite.children().forEach(c -> applyTree(ctx, theme, c, warn));
        }
    }

    // endregion

    // region custom properties

    /**
     * Collects the {@code --*} custom properties that resolve for {@code node}
     * (from {@code :root} and inherited cascades), for free-form consumer lookup.
     */
    public static List<Declaration> customProperties(Theme theme, Widget node) {
        Map<String, Cascade.ResolvedDecl> resolved = Cascade.resolve(theme, node);
        List<Declaration> out = new ArrayList<>();
        for (Map.Entry<String, Cascade.ResolvedDecl> e : resolved.entrySet()) {
            if (e.getKey().startsWith("--")) {
                out.add(new Declaration(e.getKey(), e.getValue().value(), false));
            }
        }
        return out;
    }

    // endregion
}
