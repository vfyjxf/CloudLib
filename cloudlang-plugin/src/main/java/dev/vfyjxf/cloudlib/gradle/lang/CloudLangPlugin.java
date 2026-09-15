package dev.vfyjxf.cloudlib.gradle.lang;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.nio.file.Path;

/**
 * Implements the distributed language system:
 *
 * <ul>
 *     <li>{@code generateLangKeys} - reads the yaml files of the default locale and generates
 *     the "R-like" key class (configurable name and package) with {@code LangEntry} constants
 *     grouped into nested classes mirroring the key structure.</li>
 *     <li>{@code generateLangResources} - merges the distributed yaml files of every locale and
 *     packs them into vanilla {@code assets/<modId>/lang/<locale>.json} files, wired into
 *     {@code processResources} so they end up in the jar (mirroring how the NeoForge MDK
 *     generates {@code neoforge.mods.toml} at build time).</li>
 * </ul>
 *
 * <p>Sources are {@code src/main/lang/<locale>/**.yaml} (hand-written) and
 * {@code build/generated/datagen/lang/<locale>/**.yaml} (datagen output). Both are merged,
 * with the datagen files taking precedence on conflicts.
 */
public class CloudLangPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var extension = project.getExtensions().create("cloudLang", CloudLangExtension.class);
        extension.getClassName().convention("LangKeys");
        extension.getConstantNameStyle().convention("upper_snake");
        extension.getGeneratedAnnotation().convention("javax.annotation.processing.Generated");
        extension.getEntryClass().convention("dev.vfyjxf.cloudlib.data.lang.LangEntry");
        extension.getDefaultLocale().convention("en_us");
        extension.getPackageName().convention(project.provider(() -> {
            String group = String.valueOf(project.getGroup());
            return group.isEmpty() ? "lang" : group + ".lang";
        }));
        extension.getSourceDirectory().convention(project.getLayout().getProjectDirectory().dir("src/main/lang"));
        extension.getGeneratedSourceDirectory().convention(project.getLayout().getBuildDirectory().dir("generated/datagen/lang"));

        var generateLangKeys = project.getTasks().register("generateLangKeys", GenerateLangKeysTask.class, task -> {
            task.getModId().set(extension.getModId());
            task.getPackageName().set(extension.getPackageName());
            task.getClassName().set(extension.getClassName());
            task.getConstantNameStyle().set(extension.getConstantNameStyle());
            task.getGeneratedAnnotation().set(extension.getGeneratedAnnotation());
            task.getEntryClass().set(extension.getEntryClass());
            task.getDefaultLocale().set(extension.getDefaultLocale());
            task.getSourceDirectories().from(extension.getSourceDirectory(), extension.getGeneratedSourceDirectory());
            wireLabels(task.getSourceDirectoryLabels(), extension, project);
            task.getOutputDirectory().convention(project.getLayout().getBuildDirectory().dir("generated/sources/langKeys"));
        });

        var generateLangResources = project.getTasks().register("generateLangResources", GenerateLangResourcesTask.class, task -> {
            task.getModId().set(extension.getModId());
            task.getSourceDirectories().from(extension.getSourceDirectory(), extension.getGeneratedSourceDirectory());
            wireLabels(task.getSourceDirectoryLabels(), extension, project);
            task.getOutputDirectory().convention(project.getLayout().getBuildDirectory().dir("generated/langResources"));
        });

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            //carries the task dependency, compileJava picks it up automatically
            main.getJava().srcDir(generateLangKeys.flatMap(GenerateLangKeysTask::getOutputDirectory));
            project.getTasks().named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, ProcessResources.class, processResources ->
                    processResources.from(generateLangResources)
            );
        });
    }

    /**
     * Wires project-relative display labels of the source roots, used in generated file
     * comments.
     */
    private static void wireLabels(ListProperty<String> labels, CloudLangExtension extension, Project project) {
        var projectDir = project.getProjectDir().toPath();
        labels.add(extension.getSourceDirectory().map(dir -> relativize(projectDir, dir.getAsFile().toPath())));
        labels.add(extension.getGeneratedSourceDirectory().map(dir -> relativize(projectDir, dir.getAsFile().toPath())));
    }

    private static String relativize(Path projectDir, Path directory) {
        return directory.startsWith(projectDir)
                ? projectDir.relativize(directory).toString()
                : directory.toString();
    }

}
