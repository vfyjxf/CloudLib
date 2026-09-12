package dev.vfyjxf.cloudlib.internal.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.theme.Theme;
import dev.vfyjxf.cloudlib.api.ui.theme.ThemeManager;
import dev.vfyjxf.cloudlib.internal.css.AtRule;
import dev.vfyjxf.cloudlib.internal.css.ComponentValue;
import dev.vfyjxf.cloudlib.internal.css.CssError;
import dev.vfyjxf.cloudlib.internal.css.CssParser;
import dev.vfyjxf.cloudlib.internal.css.Rule;
import dev.vfyjxf.cloudlib.internal.css.Stylesheet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
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
 * {@link ThemeManager} registry, resolving {@code @import} references between
 * theme files.
 * <p>
 * Registered on the client reload bus ({@code RegisterClientReloadListenersEvent}).
 * Resource packs can override or extend any theme by id; the import chain is
 * flattened into each theme's rule list with source order preserved.
 */
public final class ThemeLoader extends SimplePreparableReloadListener<Map<ResourceLocation, String>> {

    public static final ThemeLoader instance = new ThemeLoader();

    private static final Logger logger = LoggerFactory.getLogger("cloudlib/themes");
    /** The directory inside each namespace that holds themes. */
    public static final String directory = "ui/themes";

    private ThemeLoader() {}

    // region stage 1: read
    @Override
    protected Map<ResourceLocation, String> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, String> sources = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(
                        directory, path -> path.getPath().endsWith(".css"))
                .entrySet()) {
            ResourceLocation file = entry.getKey();
            // assets/<ns>/ui/themes/<path>.css → <ns>:<path without .css>
            String path = file.getPath();
            String relative = path.substring(directory.length() + 1, path.length() - ".css".length());
            ResourceLocation themeId = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), relative);
            try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) >= 0) {
                    sb.append(buf, 0, n);
                }
                sources.put(themeId, sb.toString());
            } catch (IOException e) {
                logger.warn("Failed to read theme {}", file, e);
            }
        }
        return sources;
    }

    // endregion

    // region stage 2: parse & register
    @Override
    protected void apply(Map<ResourceLocation, String> sources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Theme> resolved = new HashMap<>();
        for (ResourceLocation id : sources.keySet()) {
            Theme theme = resolveTheme(id, sources, new ArrayDeque<>(), resolved);
            if (theme != null) {
                resolved.putIfAbsent(id, theme);
            }
        }
        for (Theme theme : resolved.values()) {
            ThemeManager.register(theme);
        }
        // the bundled standard theme is the default look — packs override by id
        ResourceLocation standard = ResourceLocation.fromNamespaceAndPath("cloudlib", "standard");
        if (resolved.containsKey(standard) && ThemeManager.active() == null) {
            ThemeManager.activate(standard);
        }
        if (!resolved.isEmpty()) {
            ThemeManager.notifyChanged();
        }
        logger.info("Loaded {} theme(s): {}", resolved.size(), resolved.keySet());
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
