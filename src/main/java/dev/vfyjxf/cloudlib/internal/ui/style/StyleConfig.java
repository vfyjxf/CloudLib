package dev.vfyjxf.cloudlib.internal.ui.style;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Client config for the ui theme stack.
 * <p>
 * {@code uiThemes} is the user's authoritative theme selection — an ordered
 * list of theme ids ({@code "namespace:path"}), lowest priority first. When
 * empty, the {@code theme.json} {@code "default"} themes apply; the bundled
 * {@code cloudlib:standard} theme is the final fallback.
 */
public final class StyleConfig {

    private StyleConfig() {}

    private static final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> uiThemes;

    private static final ModConfigSpec.BooleanValue uiThemeWatch;

    private static final ModConfigSpec spec;

    static {
        builder.push("ui");
        {
            uiThemes = builder.comment(
                "Active ui themes, lowest priority first — e.g. [\"cloudlib:standard\", \"mypack:dark\"].",
                "Empty = follow the packs' recommended defaults (theme.json \"default\": true)."
            ).defineList("ui_themes", List.of(), () -> "", o -> o instanceof String);
            uiThemeWatch = builder.comment(
                "Watch development resource roots for theme changes and reload automatically.",
                "Only effective outside production."
            ).define("ui_theme_watch", true);
        }
        builder.pop();
        spec = builder.build();
    }

    /** The user-selected theme stack, or empty when unset. */
    public static List<String> uiThemes() {
        return List.copyOf(uiThemes.get());
    }

    /**
     * Persists the user-selected theme stack to {@code ui_themes} (written to
     * disk when the config is loaded — always the case in-game).
     */
    public static void setUiThemes(List<String> ids) {
        uiThemes.set(List.copyOf(ids));
        if (spec.isLoaded()) {
            spec.save();
        }
    }

    /** Whether the dev-time theme file watcher should run. */
    public static boolean uiThemeWatch() {
        return uiThemeWatch.get();
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, spec, "cloudlib-themes.toml");
    }
}
