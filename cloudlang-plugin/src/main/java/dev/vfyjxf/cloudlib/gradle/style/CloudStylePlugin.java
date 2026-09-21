package dev.vfyjxf.cloudlib.gradle.style;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.spotless.FormatterFunc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Applies the shared CloudLib style rules through Spotless:
 * <ul>
 *   <li>{@code eclipse} — the Eclipse JDT formatter driven by the bundled
 *       {@code cloud-jdt-formatter.xml} profile: 120 columns, four-space
 *       indentation, and parameter/argument lists that break one element per
 *       line with the parentheses on their own lines once a list no longer
 *       fits on a single line</li>
 *   <li>import order: everything else first (alphabetical), then
 *       {@code java.*}/{@code javax.*} last; unused imports removed</li>
 *   <li>trailing whitespace trimmed, files end with a newline</li>
 *   <li>{@code lowerCamelConstants} — a lint step that fails the check when
 *       a {@code static final} field or enum constant isn't lowerCamelCase
 *       (report-only; renames are never auto-applied)</li>
 *   <li>{@code noFullyQualifiedNames} — a lint step that fails the check when
 *       code uses a fully qualified class reference that could be an import
 *       (collision-forced qualifications and {@code // fqn-ok} suppressions
 *       pass; literals and comments are never scanned)</li>
 * </ul>
 */
public class CloudStylePlugin implements Plugin<Project> {

    /** The Eclipse release whose JDT formatter the bundled profile targets. */
    public static final String JDT_FORMATTER_VERSION = "4.35";

    private static final String PROFILE_RESOURCE = "cloud-jdt-formatter.xml";

    private static final String PROFILE_PACKAGE = "dev/vfyjxf/cloudlib/gradle/style/";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("com.diffplug.spotless");
        SpotlessExtension spotless = project.getExtensions().getByType(SpotlessExtension.class);
        String projectName = project.getName();
        File profile = unpackFormatterProfile(project);
        spotless.java(java -> {
            java.eclipse(JDT_FORMATTER_VERSION).configFile(profile);
            java.importOrder("", "java", "javax", "\\#");
            java.removeUnusedImports();
            java.trimTrailingWhitespace();
            java.endWithNewline();
            java.custom("lowerCamelConstants",
                    (FormatterFunc) raw -> ConstantNamingLint.check(raw, projectName));
            java.custom("noFullyQualifiedNames", (FormatterFunc) FqnLint::check);
        });
    }

    /**
     * Spotless reads the JDT profile from a file, so the profile packaged in
     * this plugin's jar is unpacked under the project's build directory. The
     * copy is only rewritten when its bytes differ, which keeps the profile a
     * stable input for the formatting tasks.
     */
    private static File unpackFormatterProfile(Project project) {
        byte[] profile;
        try (InputStream in = CloudStylePlugin.class.getResourceAsStream("/" + PROFILE_PACKAGE + PROFILE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing the bundled formatter profile " + PROFILE_RESOURCE);
            }
            profile = in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read the bundled formatter profile " + PROFILE_RESOURCE, e);
        }
        Path target = project.getLayout()
                .getBuildDirectory()
                .get()
                .getAsFile()
                .toPath()
                .resolve("cloudstyle")
                .resolve(PROFILE_RESOURCE);
        try {
            if (!Files.isRegularFile(target) || !Arrays.equals(Files.readAllBytes(target), profile)) {
                Files.createDirectories(target.getParent());
                Files.write(target, profile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to unpack the formatter profile to " + target, e);
        }
        return target.toFile();
    }

}
