package dev.vfyjxf.cloudlib.data.lang;

import com.google.common.hash.Hashing;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Data provider that writes <em>distributed</em> language files: one yaml file per domain
 * (see {@link LangFile}) under {@code <langRoot>/<locale>/}, instead of a single monolithic
 * json file. The cloudLang gradle plugin later merges these files (together with the
 * hand-written ones in {@code src/main/lang}) into the packed
 * {@code assets/<modId>/lang/<locale>.json} resources at build time.
 *
 * <p>The lang root defaults to the {@code lang} directory next to the datagen output
 * folder, which resolves to {@code build/generated/datagen/lang} when the data run's
 * {@code --output} points at {@code build/generated/datagen/resources}.
 *
 * <p>Usage:
 * <pre>{@code
 * public class EnUsLang extends DistributedLangProvider {
 *     public EnUsLang(PackOutput output) {
 *         super(output, "mymod", "en_us");
 *     }
 *
 *     @Override
 *     protected void addTranslations() {
 *         file("items").add(ModItems.TEST_ITEM.get(), "Test Item");
 *         file("blocks").add(ModBlocks.TEST_BLOCK.get(), "Test Block");
 *         add("mymod.ui.title", "My Title"); //goes to main.yaml
 *     }
 * }
 * }</pre>
 */
public abstract class DistributedLangProvider implements DataProvider {

    private final PackOutput output;
    private final String modId;
    private final String locale;
    private final Map<String, LangFile> files = new TreeMap<>();
    private boolean translated;

    protected DistributedLangProvider(PackOutput output, String modId, String locale) {
        this.output = output;
        this.modId = modId;
        this.locale = locale;
    }

    protected abstract void addTranslations();

    /**
     * Returns the file named {@code <name>.yaml} of this locale, creating it on first use.
     */
    protected LangFile file(String name) {
        return files.computeIfAbsent(name, LangFile::new);
    }

    /**
     * Adds an entry to {@code main.yaml}.
     */
    protected void add(String key, String value) {
        file("main").add(key, value);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        if (!translated) {
            addTranslations();
            translated = true;
        }
        Path localeRoot = output.getOutputFolder().resolveSibling("lang").resolve(locale);
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (LangFile file : files.values()) {
            if (file.entries().isEmpty()) {
                continue;
            }
            byte[] content = LangYamlWriter.write(file.entries());
            Path path = localeRoot.resolve(file.name() + ".yaml");
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    cache.writeIfNeeded(path, content, Hashing.sha256().hashBytes(content));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write lang file " + path, e);
                }
            }));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Distributed lang files (" + modId + ":" + locale + ")";
    }

}
