package dev.vfyjxf.cloudlib.gradle.style;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.spotless.FormatterFunc;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Applies the shared CloudLib style rules through Spotless:
 * <ul>
 *   <li>{@code palantirJavaFormat} — the 4-space formatting the codebase
 *       already follows</li>
 *   <li>import order: everything else first (alphabetical), then
 *       {@code java.*}/{@code javax.*} last; unused imports removed</li>
 *   <li>trailing whitespace trimmed, files end with a newline</li>
 *   <li>{@code lowerCamelConstants} — a lint step that fails the check when
 *       a {@code static final} field or enum constant isn't lowerCamelCase
 *       (report-only; renames are never auto-applied)</li>
 * </ul>
 */
public class CloudStylePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("com.diffplug.spotless");
        SpotlessExtension spotless = project.getExtensions().getByType(SpotlessExtension.class);
        String projectName = project.getName();
        spotless.java(java -> {
            java.palantirJavaFormat("2.73.0");
            java.importOrder("", "java", "javax", "\\#");
            java.removeUnusedImports();
            java.trimTrailingWhitespace();
            java.endWithNewline();
            java.custom("lowerCamelConstants",
                    (FormatterFunc) raw -> ConstantNamingLint.check(raw, projectName));
        });
    }

}
