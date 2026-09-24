package dev.vfyjxf.cloudlib.gradle.lang;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
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
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Generates the "R-like" key class from the yaml files of the default locale.
 */
@CacheableTask
public abstract class GenerateLangKeysTask extends DefaultTask {

    @Input
    public abstract Property<String> getModId();

    @Input
    public abstract Property<String> getPackageName();

    @Input
    public abstract Property<String> getClassName();

    @Input
    public abstract Property<String> getConstantNameStyle();

    @Input
    public abstract Property<String> getGeneratedAnnotation();

    @Input
    public abstract Property<String> getEntryClass();

    @Input
    public abstract Property<String> getDefaultLocale();

    @InputFiles
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceDirectories();

    /**
     * Directory the source roots are labelled against in generated comments.
     */
    @Input
    public abstract Property<String> getProjectDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void generate() {
        var locales = LangMerger.merge(sourceRoots(), warning -> getLogger().warn(warning));
        String defaultLocale = getDefaultLocale().get();
        var content = locales.getOrDefault(defaultLocale, LangMerger.LocaleContent.empty());
        if (!locales.isEmpty() && !locales.containsKey(defaultLocale)) {
            getLogger().warn("No lang sources for the default locale '{}'; the generated key class will be empty. Available locales: {}",
                    defaultLocale, String.join(", ", locales.keySet()));
        }

        String source = LangKeysClassWriter.write(
                getPackageName().get(),
                getClassName().get(),
                getEntryClass().get(),
                getGeneratedAnnotation().get(),
                getModId().get(),
                content.mergedEntries(),
                content.mergedFieldNames(),
                LangNameStyle.parse(getConstantNameStyle().get())
        );

        Path outputRoot = getOutputDirectory().get().getAsFile().toPath();
        clean(outputRoot);
        Path classFile = outputRoot.resolve(getPackageName().get().replace('.', '/') + "/" + getClassName().get() + ".java");
        try {
            Files.createDirectories(classFile.getParent());
            Files.writeString(classFile, source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + classFile, e);
        }
        getLogger().lifecycle("Generated {}.{} ({} keys)", getPackageName().get(), getClassName().get(), content.entryCount());
    }

    protected final java.util.List<LangMerger.SourceRoot> sourceRoots() {
        Path projectDirectory = Path.of(getProjectDirectory().get());
        var roots = new ArrayList<LangMerger.SourceRoot>();
        for (java.io.File file : getSourceDirectories().getFiles()) {
            roots.add(LangMerger.SourceRoot.of(projectDirectory, file.toPath()));
        }
        return roots;
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
