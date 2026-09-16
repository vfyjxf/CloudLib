package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.ui.style.Themes;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Dev-time theme hot reload: watches every real directory that can contribute
 * {@code assets/<ns>/ui/themes} — exploded mod file roots, classpath resource
 * dirs and directory-type resource packs — and {@linkplain Themes#reload()
 * reloads} on the client thread after a short debounce. Jar-contained themes
 * can't change at runtime, so they're never watched.
 * <p>
 * Started from client load-complete when {@link StyleConfig#uiThemeWatch()} is
 * on and the game is not running production.
 */
public final class StyleWatcher {

    private static final Logger logger = LoggerFactory.getLogger("cloudlib/style-watcher");
    private static final long debounceMs = 300;

    private static volatile boolean running;

    private StyleWatcher() {}

    /** Idempotent — starts the daemon watch thread once. */
    public static void start() {
        if (running || FMLEnvironment.production || !StyleConfig.uiThemeWatch()) {
            return;
        }
        running = true;
        Thread thread = new Thread(StyleWatcher::loop, "cloudlib-style-watcher");
        thread.setDaemon(true);
        thread.start();
        logger.info("Theme file watcher started");
    }

    private static void loop() {
        try (WatchService service = FileSystems.getDefault().newWatchService()) {
            Map<WatchKey, Path> keys = new HashMap<>();
            rebuild(service, keys);

            boolean dirty = false;
            long lastEvent = 0;
            while (running) {
                WatchKey key = service.poll(100, TimeUnit.MILLISECONDS);
                if (key != null) {
                    Path dir = keys.get(key);
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == OVERFLOW) {
                            dirty = true;
                            continue;
                        }
                        if (dir == null) {
                            continue;
                        }
                        Path path = dir.resolve((Path) event.context());
                        // pick up directories created mid-burst (new theme dirs)
                        if (event.kind() == ENTRY_CREATE && Files.isDirectory(path)) {
                            registerAll(path, service, keys);
                            dirty = true;
                            continue;
                        }
                        if (isThemeFile(path.getFileName().toString())) {
                            dirty = true;
                        }
                    }
                    lastEvent = System.currentTimeMillis();
                    if (!key.reset()) {
                        keys.remove(key);
                    }
                }
                if (dirty && System.currentTimeMillis() - lastEvent >= debounceMs) {
                    dirty = false;
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null) {
                        mc.execute(Themes::reload);
                    }
                    rebuild(service, keys);
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Theme file watcher stopped: {}", e.toString());
        }
    }

    /** Rebuilds the whole watch set — cheap, and picks up new roots/dirs. */
    private static void rebuild(WatchService service, Map<WatchKey, Path> keys) {
        for (WatchKey key : keys.keySet()) {
            key.cancel();
        }
        keys.clear();
        for (Path root : roots()) {
            registerAll(root, service, keys);
        }
    }

    private static void registerAll(Path dir, WatchService service, Map<WatchKey, Path> keys) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path sub, BasicFileAttributes attrs) throws IOException {
                    keys.put(sub.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE), sub);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to watch {}: {}", dir, e.toString());
        }
    }

    /**
     * Every real-filesystem directory that can hold {@code ui/themes} content:
     * exploded mod roots (dev), classpath resource dirs (dev) and directory
     * resource packs.
     */
    private static List<Path> roots() {
        Set<Path> assets = new LinkedHashSet<>();
        for (var info : ModList.get().getModFiles()) {
            Path root = info.getFile().getFilePath();
            if (Files.isDirectory(root)) {
                assets.add(normalize(root.resolve("assets")));
            }
        }
        for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                assets.add(normalize(Path.of(entry).resolve("assets")));
            }
        }
        Path packsDir = FMLPaths.GAMEDIR.get().resolve("resourcepacks");
        if (Files.isDirectory(packsDir)) {
            try (var stream = Files.list(packsDir)) {
                for (Path pack : stream.filter(Files::isDirectory).toList()) {
                    if (Files.exists(pack.resolve("pack.mcmeta"))) {
                        assets.add(normalize(pack.resolve("assets")));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        List<Path> roots = new ArrayList<>();
        for (Path assetsDir : assets) {
            if (!Files.isDirectory(assetsDir)) {
                continue;
            }
            try (var namespaces = Files.list(assetsDir)) {
                for (Path ns : namespaces.filter(Files::isDirectory).toList()) {
                    Path themes = ns.resolve("ui").resolve("themes");
                    if (Files.isDirectory(themes)) {
                        roots.add(themes);
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return roots;
    }

    /** Theme-relevant file names — css sources and theme descriptors. */
    static boolean isThemeFile(String fileName) {
        return fileName.endsWith(".css") || fileName.equals("theme.json");
    }

    private static Path normalize(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }
}
