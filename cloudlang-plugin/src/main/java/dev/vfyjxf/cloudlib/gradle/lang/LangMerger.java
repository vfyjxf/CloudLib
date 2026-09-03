package dev.vfyjxf.cloudlib.gradle.lang;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Reads the distributed yaml lang files ({@code <root>/<locale>/**.yaml}) and merges them
 * into a chunked {@code locale -> LocaleContent} structure: every source yaml file is one
 * <em>chunk</em> (identified by its file name), keeping its entries, explicit field names
 * and source description separate. Nested maps are flattened into dotted keys, so both of
 * these produce the same entry:
 *
 * <pre>{@code
 * cloudlib:
 *   keys:
 *     debug: Debug
 * }</pre>
 *
 * <pre>{@code
 * cloudlib.keys.debug: "Debug"
 * }</pre>
 *
 * <p>An entry may also declare an explicit constant name for the generated key class by
 * using the annotated leaf form: a mapping containing the magic token {@code $field}
 * (reserved and never a translation key) plus a {@code value} scalar. Otherwise the
 * constant name is derived from the key with the configured {@link LangNameStyle}:
 *
 * <pre>{@code
 * cloudlib:
 *   keys:
 *     rebuild_ui:
 *       value: Rebuild UI
 *       $field: rebuildUIButton
 * }</pre>
 */
final class LangMerger {

    /**
     * Magic token marking an annotated leaf. Reserved: a yaml key of this name is always
     * interpreted as the explicit-name directive, never as a translation key.
     */
    static final String FIELD_NAME_TOKEN = "$field";

    private LangMerger() {
    }

    /**
     * The content of one source yaml file.
     */
    record Chunk(Map<String, String> entries, Map<String, String> fieldNames, List<String> sources) {

        static Chunk empty() {
            return new Chunk(new TreeMap<>(), new TreeMap<>(), new ArrayList<>());
        }
    }

    /**
     * The merged content of one locale, grouped by chunk (source file) name.
     */
    record LocaleContent(Map<String, Chunk> chunks) {

        static LocaleContent empty() {
            return new LocaleContent(new TreeMap<>());
        }

        Map<String, String> mergedEntries() {
            Map<String, String> merged = new TreeMap<>();
            chunks.values().forEach(chunk -> merged.putAll(chunk.entries()));
            return merged;
        }

        Map<String, String> mergedFieldNames() {
            Map<String, String> merged = new TreeMap<>();
            chunks.values().forEach(chunk -> merged.putAll(chunk.fieldNames()));
            return merged;
        }

        int entryCount() {
            return chunks.values().stream().mapToInt(chunk -> chunk.entries().size()).sum();
        }
    }

    /**
     * A source root together with a display label used in generated file comments.
     */
    record SourceRoot(Path directory, String label) {
    }

    /**
     * Merges every locale found in the given source roots. Roots are processed in order;
     * within the same chunk a later root wins on a key conflict and a warning is emitted.
     *
     * @return locale -> chunked content
     */
    static Map<String, LocaleContent> merge(List<SourceRoot> roots, Consumer<String> warnings) {
        Map<String, LocaleContent> locales = new TreeMap<>();
        for (SourceRoot root : roots) {
            if (!Files.isDirectory(root.directory())) {
                continue;
            }
            try (Stream<Path> children = Files.list(root.directory())) {
                for (Path child : children.sorted().toList()) {
                    if (Files.isDirectory(child)) {
                        mergeLocale(root, child, locales.computeIfAbsent(child.getFileName().toString(), locale -> LocaleContent.empty()), warnings);
                    } else if (isYaml(child)) {
                        warnings.accept("Ignoring yaml file '" + child + "': lang yaml files must live inside a locale directory (e.g. en_us/)");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to scan lang directory " + root.directory(), e);
            }
        }
        return locales;
    }

    private static void mergeLocale(SourceRoot root, Path localeDir, LocaleContent content, Consumer<String> warnings) {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(localeDir)) {
            files = walk.filter(Files::isRegularFile).filter(LangMerger::isYaml).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan lang directory " + localeDir, e);
        }
        String locale = localeDir.getFileName().toString();
        for (Path file : files) {
            String relative = localeDir.relativize(file).toString().replaceAll("\\.ya?ml$", "").replace('/', '.');
            String chunkName = relative.isEmpty() ? locale : relative;
            Chunk chunk = content.chunks().computeIfAbsent(chunkName, name -> Chunk.empty());
            Chunk flattened;
            try (var input = Files.newInputStream(file)) {
                flattened = flatten(new Yaml().load(input), warnings);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read lang file " + file, e);
            }
            for (var entry : flattened.entries().entrySet()) {
                String previous = chunk.entries().put(entry.getKey(), entry.getValue());
                if (previous != null && !previous.equals(entry.getValue())) {
                    warnings.accept("Duplicate lang key '" + entry.getKey() + "' in " + file + " ('" + previous + "' -> '" + entry.getValue() + "')");
                }
            }
            for (var fieldName : flattened.fieldNames().entrySet()) {
                String previous = chunk.fieldNames().put(fieldName.getKey(), fieldName.getValue());
                if (previous != null && !previous.equals(fieldName.getValue())) {
                    warnings.accept("Duplicate field name for '" + fieldName.getKey() + "' in " + file + " ('" + previous + "' -> '" + fieldName.getValue() + "')");
                }
            }
            chunk.sources().add(root.label() + "/" + localeDir.getParent().relativize(file));
        }
    }

    /**
     * Flattens a parsed yaml document into dotted keys. Scalars are coerced to strings;
     * sequences are skipped with a warning.
     */
    static Chunk flatten(Object document, Consumer<String> warnings) {
        Map<String, String> entries = new TreeMap<>();
        Map<String, String> fieldNames = new TreeMap<>();
        if (document instanceof Map<?, ?> map) {
            flattenInto(map, "", entries, fieldNames, warnings);
        } else if (document != null) {
            warnings.accept("Ignoring lang yaml document: root must be a mapping");
        }
        return new Chunk(entries, fieldNames, new ArrayList<>());
    }

    private static void flattenInto(Map<?, ?> map, String prefix, Map<String, String> entries, Map<String, String> fieldNames, Consumer<String> warnings) {
        for (var entry : map.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> child) {
                if (containsKey(child, FIELD_NAME_TOKEN)) {
                    Object text = valueOf(child, "value");
                    Object field = valueOf(child, FIELD_NAME_TOKEN);
                    if (text == null || field == null || !isScalar(text) || !isScalar(field)) {
                        warnings.accept("Ignoring annotated entry '" + key + "': it must contain plain scalar 'value' and '" + FIELD_NAME_TOKEN + "'");
                    } else if (child.size() != 2) {
                        warnings.accept("Ignoring annotated entry '" + key + "': it may only contain 'value' and '" + FIELD_NAME_TOKEN + "', found " + child.size() + " keys");
                    } else {
                        entries.put(key, String.valueOf(text));
                        fieldNames.put(key, String.valueOf(field));
                    }
                } else {
                    flattenInto(child, key, entries, fieldNames, warnings);
                }
            } else if (value == null) {
                warnings.accept("Ignoring lang key '" + key + "': value is null");
            } else if (value instanceof Iterable<?>) {
                warnings.accept("Ignoring lang key '" + key + "': sequences are not supported, use a plain string value");
            } else {
                entries.put(key, String.valueOf(value));
            }
        }
    }

    private static boolean containsKey(Map<?, ?> map, String key) {
        for (var k : map.keySet()) {
            if (key.equals(k)) {
                return true;
            }
        }
        return false;
    }

    /** Returns the value for the given string key, or {@code null} when absent. */
    private static Object valueOf(Map<?, ?> map, String key) {
        for (var entry : map.entrySet()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean isScalar(Object value) {
        return !(value instanceof Map) && !(value instanceof Iterable);
    }

    private static boolean isYaml(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

}
