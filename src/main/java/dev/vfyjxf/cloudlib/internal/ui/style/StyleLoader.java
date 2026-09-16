package dev.vfyjxf.cloudlib.internal.ui.theme;

import dev.vfyjxf.cloudlib.api.css.AtRule;
import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.CssError;
import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.css.Rule;
import dev.vfyjxf.cloudlib.api.css.Stylesheet;
import dev.vfyjxf.cloudlib.api.ui.theme.Theme;
import dev.vfyjxf.cloudlib.api.ui.theme.Themes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code assets/<ns>/ui/themes/**.css} from the resource manager into the
 * {@link Themes} registry, resolving {@code @import} references between
 * theme files.
 * <p>
 * Registered on the client reload bus ({@code RegisterClientReloadListenersEvent}).
 * Resource packs can override or extend any theme by id; the import chain is
 * flattened into each theme's rule list with source order preserved.
 */
public final class ThemeLoader extends SimplePreparableReloadListener<ThemeLoader.Prepared> {

    /** What a pack contributes: theme sources plus its manifest text. */
    record Prepared(Map<ResourceLocation, String> sources, List<ThemeManifest> manifests) {}

    /** Parsed {@code ui/themes/themes.json} — recommended defaults for the pack. */
    record ThemeManifest(List<ResourceLocation> defaultStack) {}

    public static final ThemeLoader instance = new ThemeLoader();

    private static final Logger logger = LoggerFactory.getLogger("cloudlib/themes");
    /** The directory inside each namespace that holds themes. */
    public static final String directory = "ui/themes";

    private ThemeLoader() {}

    // region stage 1: read
    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, String> sources = new HashMap<>();
        List<ThemeManifest> manifests = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(
                        directory,
                        path -> path.getPath().endsWith(".css")
                                || path.getPath().endsWith("themes.json"))
                .entrySet()) {
            ResourceLocation file = entry.getKey();
            try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) >= 0) {
                    sb.append(buf, 0, n);
                }
                String text = sb.toString();
                if (file.getPath().endsWith("themes.json")) {
                    ThemeManifest manifest = parseManifest(file, text);
                    if (manifest != null) {
                        manifests.add(manifest);
                    }
                } else {
                    // assets/<ns>/ui/themes/<path>.css → <ns>:<path without .css>
                    String path = file.getPath();
                    String relative = path.substring(directory.length() + 1, path.length() - ".css".length());
                    sources.put(ResourceLocation.fromNamespaceAndPath(file.getNamespace(), relative), text);
                }
            } catch (IOException e) {
                logger.warn("Failed to read theme {}", file, e);
            }
        }
        return new Prepared(sources, manifests);
    }

    /**
     * {@code {"default": ["ns:path", ...]}} — the pack's recommended activation
     * stack, lowest priority first.
     */
    private static @Nullable ThemeManifest parseManifest(ResourceLocation file, String text) {
        try {
            var json = com.google.gson.JsonParser.parseString(text).getAsJsonObject();
            List<ResourceLocation> stack = new ArrayList<>();
            if (json.has("default")) {
                for (var el : json.getAsJsonArray("default")) {
                    ResourceLocation id = ResourceLocation.tryParse(el.getAsString());
                    if (id != null) {
                        stack.add(id);
                    } else {
                        logger.warn("{}: unparseable theme id '{}'", file, el.getAsString());
                    }
                }
            }
            return new ThemeManifest(stack);
        } catch (RuntimeException e) {
            logger.warn("Failed to parse theme manifest {}: {}", file, e.getMessage());
            return null;
        }
    }

    // endregion

    // region stage 2: parse & register
    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, String> sources = prepared.sources();
        Map<ResourceLocation, Theme> resolved = new HashMap<>();
        for (ResourceLocation id : sources.keySet()) {
            Theme theme = resolveTheme(id, sources, new ArrayDeque<>(), resolved);
            if (theme != null) {
                resolved.putIfAbsent(id, theme);
            }
        }
        for (Theme theme : resolved.values()) {
            Themes.register(theme);
        }

        // activation: user config > manifest defaults > standard fallback
        List<ResourceLocation> wanted = new ArrayList<>();
        for (String id : ThemeConfig.uiThemes()) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null) {
                wanted.add(loc);
            } else {
                logger.warn("ui_themes entry '{}' is not a resource location", id);
            }
        }
        if (wanted.isEmpty()) {
            for (ThemeManifest manifest : prepared.manifests()) {
                for (ResourceLocation id : manifest.defaultStack()) {
                    if (resolved.containsKey(id) && !wanted.contains(id)) {
                        wanted.add(id);
                    }
                }
            }
        }
        if (wanted.isEmpty()) {
            ResourceLocation standard = ResourceLocation.fromNamespaceAndPath("cloudlib", "standard");
            if (resolved.containsKey(standard)) {
                wanted.add(standard);
            }
        }
        Themes.setActive(wanted);
        logger.info("Loaded {} theme(s): {}, active: {}", resolved.size(), resolved.keySet(), wanted);
    }

    /**
     * Parses a theme and inlines its {@code @import} chain — the imported rules sit
     * at the import point so equal-specificity ties break by final source order.
     * Cycles are cut with a warning.
     */
    private static Theme resolveTheme(
            ResourceLocation id,
            Map<ResourceLocation, String> sources,
            Deque<ResourceLocation> chain,
            Map<ResourceLocation, Theme> done) {
        if (done.containsKey(id)) {
            return done.get(id);
        }
        if (chain.contains(id)) {
            logger.warn("Theme import cycle: {} -> {}", chain, id);
            return null;
        }
        String source = sources.get(id);
        if (source == null) {
            logger.warn("Theme {} imports missing theme {}", chain.peekLast(), id);
            return null;
        }
        chain.addLast(id);
        List<CssError> errors = new ArrayList<>();
        Stylesheet sheet = CssParser.parse(source, errors);
        for (CssError error : errors) {
            logger.warn("{}:{}:{} {}", id, error.line(), error.column(), error.message());
        }
        // flatten: replace each @import with the imported sheet's rules
        List<Rule> flat = new ArrayList<>();
        for (Rule rule : sheet.rules()) {
            if (rule instanceof AtRule at && at.name().equals("import")) {
                ResourceLocation target = importTarget(at);
                if (target == null) {
                    logger.warn("{}: malformed @import", id);
                    continue;
                }
                Theme imported = resolveTheme(target, sources, chain, done);
                if (imported != null) {
                    flat.addAll(imported.sheet().rules());
                }
                continue;
            }
            flat.add(rule);
        }
        chain.removeLast();
        Theme theme = new Theme(id, new Stylesheet(flat));
        done.put(id, theme);
        return theme;
    }

    /** {@code @import "ns:path"} / {@code url(ns:path)} / bare ident. */
    private static ResourceLocation importTarget(AtRule at) {
        for (ComponentValue v : at.prelude()) {
            if (v instanceof ComponentValue.StringValue s) {
                return ResourceLocation.tryParse(s.value());
            }
            if (v instanceof ComponentValue.UrlValue u) {
                return ResourceLocation.tryParse(u.value());
            }
            if (v instanceof ComponentValue.Function fn
                    && fn.name().equalsIgnoreCase("url")
                    && !fn.args().isEmpty()
                    && fn.args().get(0) instanceof ComponentValue.StringValue s) {
                return ResourceLocation.tryParse(s.value());
            }
            if (v instanceof ComponentValue.Ident id) {
                return ResourceLocation.tryParse(id.value());
            }
        }
        return null;
    }
    // endregion
}
