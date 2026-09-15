package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.eclipse.collections.api.list.MutableList;

/**
 * Thrown when one or more plugins fail during event dispatch.
 * Individual failures are also added as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
 */
public class PluginLoadingException extends RuntimeException {

    private final MutableList<Failure> failures;

    public PluginLoadingException(MutableList<Failure> failures) {
        super(formatMessage(failures));
        this.failures = failures;
        for (Failure failure : failures) {
            addSuppressed(failure.cause());
        }
    }

    public MutableList<Failure> failures() {
        return failures;
    }

    private static String formatMessage(MutableList<Failure> failures) {
        return "Plugin loading failed: " +
                failures.collect(f -> f.pluginId() + ": " + f.cause().getMessage())
                        .makeString(", ");
    }

    public record Failure(Namespace pluginId, Throwable cause) {
    }

}
