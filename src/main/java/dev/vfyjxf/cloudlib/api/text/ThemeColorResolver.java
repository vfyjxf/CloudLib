package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleVar;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.Themes;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParseContext;
import dev.vfyjxf.cloudlib.internal.ui.style.Cascade;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a color slot ({@link StyleVar}) to the value the active theme binds
 * it to.
 * <p>
 * A rich text document is pure data: a pen color may name a theme slot instead
 * of a literal, and the resolution happens while rendering, once per frame. That
 * keeps documents independent of the theme they happen to render under — a theme
 * hot reload is picked up on the next frame without rebuilding a single node,
 * and the same document renders correctly in a widget tree themed differently
 * from the global selection.
 * <p>
 * Prefer {@link #of(StyleContext)} inside the widget tree: a widget's own style
 * context holds the theme values it actually resolved (an inline {@code --x}
 * written on the widget, or a scene-level theme override, is honored there and
 * would be invisible to the global selection).
 *
 * @see RichTextStyle#textColor(ThemeColorResolver, int)
 */
@FunctionalInterface
public interface ThemeColorResolver {

    /**
     * @param slot the color slot to read
     * @return the ARGB value the active theme binds to {@code slot}, or
     *         {@code null} when the active theme binds none (an unset slot, or a
     *         value that is not a color)
     */
    @Nullable
    Integer resolve(StyleVar<Integer> slot);

    /**
     * Resolves through a widget's computed style context — the same table inline
     * {@code setVar} writes and theme resolution populate.
     */
    static ThemeColorResolver of(StyleContext context) {
        return context::var;
    }

    /**
     * Resolves {@code :root} slots of the composed active theme, for rendering
     * that happens outside the widget tree (tooltips). Reads the selection at
     * resolve time, so a reload is visible immediately.
     */
    static ThemeColorResolver ofActiveTheme() {
        return new ThemeColorResolver() {

            private @Nullable Theme cached;
            private Map<String, Tokens> env = Map.of();

            @Override
            public @Nullable Integer resolve(StyleVar<Integer> slot) {
                Theme theme = Themes.active();
                if (theme == null) {
                    return null;
                }
                if (theme != cached) {
                    cached = theme;
                    env = environmentOf(theme);
                }
                Tokens tokens = env.get(slot.name());
                if (tokens == null) {
                    return null;
                }
                // Slots may reference other slots (`--accent: var(--brand)`), which
                // resolves against the same root environment.
                Tokens resolved = Cascade.substituteVars(tokens, env);
                return slot.parse(resolved.values(), StyleParseContext.plain());
            }
        };
    }

    private static Map<String, Tokens> environmentOf(Theme theme) {
        Map<String, Tokens> env = new LinkedHashMap<>();
        for (Map.Entry<String, List<ComponentValue>> e : theme.rootVars().entrySet()) {
            env.put(e.getKey(), Tokens.of(e.getValue()));
        }
        return env;
    }
}
