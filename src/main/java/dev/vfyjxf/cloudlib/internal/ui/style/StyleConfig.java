package dev.vfyjxf.cloudlib.internal.ui.theme;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Client config for the ui theme stack.
 * <p>
 * {@code uiThemes} is the user's authoritative theme selection — an ordered
 * list of theme ids ({@code "namespace:path"}), lowest priority first. When
 * empty, the manifests' recommended defaults apply; the bundled
 * {@code cloudlib:standard} theme is the final fallback.
 */
public final class ThemeConfig {

    private ThemeConfig() {}

    private static final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> uiThemes;

    private static final ModConfigSpec spec;

    static {
        builder.push("ui");
        {
            uiThemes = builder.comment(
                            "Active ui themes, lowest priority first — e.g. [\"cloudlib:standard\", \"mypack:dark\"].",
                            "Empty = follow the packs' recommended defaults (ui/themes/themes.json).")
                    .defineList("ui_themes", List.of(), () -> "", o -> o instanceof String);
        }
        builder.pop();
        spec = builder.build();
    }

    /** The user-selected theme stack, or empty when unset. */
    public static List<String> uiThemes() {
        return List.copyOf(uiThemes.get());
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, spec, "cloudlib-themes.toml");
    }
}
