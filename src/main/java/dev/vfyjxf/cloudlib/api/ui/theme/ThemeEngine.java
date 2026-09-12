package dev.vfyjxf.cloudlib.api.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import dev.vfyjxf.cloudlib.internal.css.ComponentValue;
import dev.vfyjxf.cloudlib.internal.css.Declaration;
import dev.vfyjxf.cloudlib.internal.ui.theme.Cascade;
import dev.vfyjxf.cloudlib.internal.ui.theme.PropertyParsers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Resolves a {@link Theme} against a {@link Themeable} node into a {@link UIStyle}.
 * <p>
 * Pipeline: cascade (match → specificity/order → var() → inherit) → per-property
 * value conversion through {@link PropertyParsers} → {@code UIStyle}.
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
     * @param node  the widget-side selector surface
     * @param warn  sink for dropped/invalid declarations (may be null)
     */
    public static UIStyle resolve(Theme theme, Themeable node, @Nullable Consumer<String> warn) {
        return resolve(new Cascade.ResolveContext(theme), theme, node, warn);
    }

    /** Convenience overload without a warning sink. */
    public static UIStyle resolve(Theme theme, Themeable node) {
        return resolve(theme, node, null);
    }

    private static UIStyle resolve(
            Cascade.ResolveContext ctx, Theme theme, Themeable node, @Nullable Consumer<String> warn) {
        Map<String, List<ComponentValue>> resolved = ctx.resolve(node);
        List<StyleProperty> props = new ArrayList<>(resolved.size());
        for (Map.Entry<String, List<ComponentValue>> e : resolved.entrySet()) {
            if (e.getKey().startsWith("--")) {
                continue; // custom properties are var() inputs, not style props
            }
            PropertyParsers.Context c =
                    new PropertyParsers.Context(theme.id().toString(), e.getKey(), warn != null ? warn : s -> {});
            StyleProperty prop = PropertyParsers.parse(c, e.getKey(), e.getValue());
            if (prop != null) {
                props.add(prop);
            }
        }
        return props.isEmpty() ? UIStyle.empty : UIStyle.of(props);
    }

    // endregion

    // region tree application

    /**
     * Re-resolves the theme for {@code root} and its whole subtree, sharing one
     * cascade context so ancestors resolve once per pass. Each widget applies the
     * resolved style and replays its code styles — see {@link Widget#refreshTheme}.
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
    public static List<Declaration> customProperties(Theme theme, Themeable node) {
        Map<String, List<ComponentValue>> resolved = Cascade.resolve(theme, node);
        List<Declaration> out = new ArrayList<>();
        for (Map.Entry<String, List<ComponentValue>> e : resolved.entrySet()) {
            if (e.getKey().startsWith("--")) {
                out.add(new Declaration(e.getKey(), e.getValue(), false));
            }
        }
        return out;
    }

    // endregion
}
