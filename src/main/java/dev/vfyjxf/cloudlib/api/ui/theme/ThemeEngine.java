package dev.vfyjxf.cloudlib.api.ui.theme;

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
 */
public final class ThemeEngine {

    private ThemeEngine() {}

    /**
     * Resolves the winning declarations for {@code node} into a {@code UIStyle}.
     *
     * @param theme the theme to resolve against
     * @param node  the widget-side selector surface
     * @param warn  sink for dropped/invalid declarations (may be null)
     */
    public static UIStyle resolve(Theme theme, Themeable node, @Nullable Consumer<String> warn) {
        Map<String, List<ComponentValue>> resolved = Cascade.resolve(theme, node);
        List<StyleProperty> props = new ArrayList<>(resolved.size());
        for (Map.Entry<String, List<ComponentValue>> e : resolved.entrySet()) {
            if (e.getKey().startsWith("--")) {
                continue; // custom properties are var() inputs, not style props
            }
            PropertyParsers.Context ctx =
                    new PropertyParsers.Context(theme.id().toString(), e.getKey(), warn != null ? warn : s -> {});
            StyleProperty prop = PropertyParsers.parse(ctx, e.getKey(), e.getValue());
            if (prop != null) {
                props.add(prop);
            }
        }
        return props.isEmpty() ? UIStyle.empty : UIStyle.of(props);
    }

    /** Convenience overload without a warning sink. */
    public static UIStyle resolve(Theme theme, Themeable node) {
        return resolve(theme, node, null);
    }

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
}
