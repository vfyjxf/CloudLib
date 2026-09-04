package dev.vfyjxf.cloudlib.gradle.lang;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Merges the distributed yaml files of every locale and packs them into vanilla
 * {@code assets/<modId>/lang/<locale>.json} files. Each source yaml file becomes one
 * annotated block inside the json, separated by reserved {@code #} comment keys.
 */
@CacheableTask
public abstract class GenerateLangResourcesTask extends DefaultTask {

    @Input
    public abstract Property<String> getModId();

    @InputFiles
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceDirectories();

    /**
     * Display labels of {@link #getSourceDirectories()}, used in generated comments.
     */
    @Input
    public abstract ListProperty<String> getSourceDirectoryLabels();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void generate() {
        var sourceRoots = getSourceDirectories().getFiles().stream().map(java.io.File::toPath).toList();
        var labels = getSourceDirectoryLabels().get();
        var roots = new java.util.ArrayList<LangMerger.SourceRoot>(sourceRoots.size());
        for (int i = 0; i < sourceRoots.size(); i++) {
            String label = i < labels.size() ? labels.get(i) : sourceRoots.get(i).toString();
            roots.add(new LangMerger.SourceRoot(sourceRoots.get(i), label));
        }
        var locales = LangMerger.merge(roots, warning -> getLogger().warn(warning));

        Path outputRoot = getOutputDirectory().get().getAsFile().toPath();
        clean(outputRoot);
        for (var locale : locales.entrySet()) {
            var content = locale.getValue();
            Path jsonFile = outputRoot.resolve("assets/" + getModId().get() + "/lang/" + locale.getKey() + ".json");
            try {
                Files.createDirectories(jsonFile.getParent());
                Files.writeString(jsonFile, LangJsonWriter.write(content), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write " + jsonFile, e);
            }
            getLogger().lifecycle("Packed {} ({} entries in {} files)", jsonFile.getFileName(), content.entryCount(), content.chunks().size());
        }
    }

    private static void clean(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to clean " + directory, e);
        }
    }

}
