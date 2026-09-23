package dev.vfyjxf.cloudlib.internal.ui.style;

import com.google.gson.JsonParser;
import dev.vfyjxf.cloudlib.api.css.CssError;
import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.css.Rule;
import dev.vfyjxf.cloudlib.api.css.Stylesheet;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.Themes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads themes from {@code assets/<ns>/ui/themes/} into the {@link Themes}
 * registry — the style layer's resource capability.
 * <p>
 * A theme is a <b>directory</b> described by {@code theme.json}:
 * <pre>{@code
 * assets/<ns>/ui/themes/<id>/theme.json
 * assets/<ns>/ui/themes/<id>/*.css
 * }</pre>
 * The theme id is the descriptor's directory ({@code ns:path}); the descriptor
 * lists its composing css files in order ({@code "css"} — default: every
 * direct-child {@code *.css} sorted by name), its theme dependencies
 * ({@code "extends"} — their rules sit before this theme's), display metadata
 * ({@code "name"}/{@code "description"}) and whether it belongs to the packs'
 * recommended defaults ({@code "default": true}).
 * <p>
 * A bare {@code *.css} outside every descriptor directory loads as an implicit
 * single-file theme ({@code ns:path} minus the extension).
 * <p>
 * Registered on the client reload bus ({@code RegisterClientReloadListenersEvent});
 * {@link #reload(ResourceManager, ProfilerFiller)} is the manual/dev entry.
 */
public final class StyleLoader extends SimplePreparableReloadListener<StyleLoader.Prepared> {

    public static final StyleLoader instance = new StyleLoader();

    private static final Logger logger = LoggerFactory.getLogger("cloudlib/themes");
    /** The directory inside each namespace that holds themes. */
    public static final String directory = "ui/themes";

    /** What a reload produces: theme definitions by id + recommended defaults in pack order. */
    record Prepared(Map<ResourceLocation, ThemeDef> themes, List<ResourceLocation> defaults) {}

    /** One theme's collected sources. */
    record ThemeDef(
        @Nullable ThemeDescriptor descriptor,
        int packRank,
        Map<String, String> cssFiles /* file name → source text */
    ) {}

    /** Parsed {@code theme.json}. {@code css == null} means "every direct-child css, sorted". */
    record ThemeDescriptor(
        @Nullable String name,
        @Nullable String description,
        @Nullable List<String> css,
        List<ResourceLocation> extendz,
        boolean isDefault
    ) {}

    /** Theme ids this loader installed last reload — stale ones get unregistered. */
    private final List<ResourceLocation> loadedIds = new ArrayList<>();

    private StyleLoader() {}

    /** Manual reload — runs the full prepare/apply inline. Call on the client thread. */
    public void reload(ResourceManager manager, ProfilerFiller profiler) {
        Prepared prepared = prepare(manager, profiler);
        apply(prepared, manager, profiler);
    }

    // region stage 1: read

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<String> packOrder = manager.listPacks().map(pack -> pack.packId()).toList();

        // pass 1 — descriptors: a dir containing theme.json is a theme dir
        Map<ResourceLocation, ThemeDef> themes = new LinkedHashMap<>();
        Map<String, ResourceLocation> descDirs = new HashMap<>(); // ns + '/' + dir → theme id

        Map<ResourceLocation, Resource> files = manager.listResources(
            directory,
            path -> path.getPath().endsWith(".css") || path.getPath().endsWith("theme.json")
        );

        List<Map.Entry<ResourceLocation, Resource>> cssFiles = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            ResourceLocation file = entry.getKey();
            String rel = file.getPath().substring(directory.length() + 1);
            int slash = rel.lastIndexOf('/');
            String dir = slash < 0 ? "" : rel.substring(0, slash);
            String name = slash < 0 ? rel : rel.substring(slash + 1);
            if (name.equals("theme.json")) {
                if (dir.isEmpty()) {
                    logger.warn("{}: theme.json must live inside a theme directory", file);
                    continue;
                }
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), dir);
                ThemeDescriptor descriptor = parseDescriptor(file, read(entry.getValue(), file));
                int rank = packOrder.indexOf(entry.getValue().sourcePackId());
                descDirs.put(file.getNamespace() + '/' + dir, id);
                themes.put(id, new ThemeDef(descriptor, Math.max(rank, 0), new TreeMap<>()));
            } else {
                cssFiles.add(entry);
            }
        }

        // pass 2 — css: direct children of a descriptor dir belong to it;
        // every other file is an implicit single-file theme
        List<ResourceLocation> defaults = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> entry : cssFiles) {
            ResourceLocation file = entry.getKey();
            String rel = file.getPath().substring(directory.length() + 1);
            int slash = rel.lastIndexOf('/');
            String dir = slash < 0 ? "" : rel.substring(0, slash);
            String name = slash < 0 ? rel : rel.substring(slash + 1);
            ResourceLocation owner = descDirs.get(file.getNamespace() + '/' + dir);
            if (owner != null) {
                themes.get(owner).cssFiles().put(name, read(entry.getValue(), file));
            } else {
                ResourceLocation id = ResourceLocation
                        .fromNamespaceAndPath(file.getNamespace(), rel.substring(0, rel.length() - ".css".length()));
                if (themes.containsKey(id)) {
                    logger.warn("{}: theme id {} already claimed by a theme directory — skipping", file, id);
                    continue;
                }
                int rank = packOrder.indexOf(entry.getValue().sourcePackId());
                themes.put(id, new ThemeDef(null, Math.max(rank, 0), Map.of(name, read(entry.getValue(), file))));
            }
        }

        // defaults: descriptor-flagged themes in pack-priority order
        themes.entrySet().stream()
                .filter(e -> e.getValue().descriptor() != null && e.getValue().descriptor().isDefault())
                .sorted(Comparator.comparingInt(e -> e.getValue().packRank())).forEach(e -> defaults.add(e.getKey()));

        return new Prepared(themes, defaults);
    }

    private static String read(Resource resource, ResourceLocation file) {
        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        } catch (IOException e) {
            logger.warn("Failed to read {}", file, e);
            return "";
        }
    }

    /**
     * {@code {"name","description","css":[..],"extends":["ns:id"],"default":bool}} —
     * every field optional; a malformed descriptor still loads as a plain theme.
     */
    private static ThemeDescriptor parseDescriptor(ResourceLocation file, String text) {
        try {
            var json = JsonParser.parseString(text).getAsJsonObject();
            String name = json.has("name") ? json.get("name").getAsString() : null;
            String description = json.has("description") ? json.get("description").getAsString() : null;
            List<String> css = null;
            if (json.has("css")) {
                css = new ArrayList<>();
                for (var el : json.getAsJsonArray("css")) {
                    css.add(el.getAsString());
                }
            }
            List<ResourceLocation> extendz = new ArrayList<>();
            if (json.has("extends")) {
                for (var el : json.getAsJsonArray("extends")) {
                    ResourceLocation id = ResourceLocation.tryParse(el.getAsString());
                    if (id != null) {
                        extendz.add(id);
                    } else {
                        logger.warn("{}: unparseable theme id '{}'", file, el.getAsString());
                    }
                }
            }
            boolean isDefault = json.has("default") && json.get("default").getAsBoolean();
            return new ThemeDescriptor(name, description, css, extendz, isDefault);
        } catch (RuntimeException e) {
            logger.warn("Failed to parse theme descriptor {}: {}", file, e.getMessage());
            return new ThemeDescriptor(null, null, null, List.of(), false);
        }
    }

    // endregion

    // region stage 2: parse & register

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ThemeDef> defs = prepared.themes();
        Map<ResourceLocation, Theme> resolved = new HashMap<>();
        for (ResourceLocation id : defs.keySet()) {
            resolveTheme(id, defs, new ArrayDeque<>(), resolved);
        }

        // swap the loaded set — stale ids from the previous pass get dropped
        for (ResourceLocation id : loadedIds) {
            if (!resolved.containsKey(id)) {
                Themes.unregister(id);
            }
        }
        loadedIds.clear();
        for (Theme theme : resolved.values()) {
            Themes.register(theme);
            loadedIds.add(theme.id());
        }

        // activation: user config > descriptor defaults > standard fallback
        List<ResourceLocation> wanted = new ArrayList<>();
        for (String id : StyleConfig.uiThemes()) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null) {
                wanted.add(loc);
            } else {
                logger.warn("ui_themes entry '{}' is not a resource location", id);
            }
        }
        if (wanted.isEmpty()) {
            for (ResourceLocation id : prepared.defaults()) {
                if (resolved.containsKey(id) && !wanted.contains(id)) {
                    wanted.add(id);
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
     * Builds a theme: the {@code extends} themes' rules are spliced in before
     * its own (source order), then its css files in descriptor order (or
     * filename order). Cycles are cut with a warning.
     */
    private static @Nullable Theme resolveTheme(
        ResourceLocation id,
        Map<ResourceLocation, ThemeDef> defs,
        Deque<ResourceLocation> chain,
        Map<ResourceLocation, Theme> done
    ) {
        if (done.containsKey(id)) {
            return done.get(id);
        }
        ThemeDef def = defs.get(id);
        if (def == null) {
            logger.warn("Theme {} extends missing theme {}", chain.peekLast(), id);
            return null;
        }
        if (chain.contains(id)) {
            logger.warn("Theme extends cycle: {} -> {}", chain, id);
            return null;
        }
        chain.addLast(id);

        List<Rule> rules = new ArrayList<>();
        if (def.descriptor() != null) {
            for (ResourceLocation dep : def.descriptor().extendz()) {
                Theme parent = resolveTheme(dep, defs, chain, done);
                if (parent != null) {
                    rules.addAll(parent.sheet().rules());
                }
            }
        }

        List<String> fileOrder = def.descriptor() != null && def.descriptor().css() != null
                ? def.descriptor().css()
                : new ArrayList<>(def.cssFiles().keySet());
        for (String fileName : fileOrder) {
            String source = def.cssFiles().get(fileName);
            if (source == null) {
                logger.warn("{}: descriptor lists missing css file '{}'", id, fileName);
                continue;
            }
            List<CssError> errors = new ArrayList<>();
            Stylesheet sheet = CssParser.parse(source, errors);
            for (CssError error : errors) {
                logger.warn("{}:{}:{}:{} {}", id, fileName, error.line(), error.column(), error.message());
            }
            rules.addAll(sheet.rules());
        }
        chain.removeLast();

        Theme theme = new Theme(
            id,
            new Stylesheet(rules),
            def.descriptor() != null ? new Theme.Meta(def.descriptor().name(), def.descriptor().description()) : null
        );
        done.put(id, theme);
        return theme;
    }

    // endregion
}
