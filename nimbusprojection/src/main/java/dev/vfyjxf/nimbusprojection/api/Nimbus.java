package dev.vfyjxf.nimbusprojection.api;

import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

/**
 * Static entry point of the Nimbus in-world UI layer.
 * <p>
 * Nimbus is a two-sided service:
 * <ul>
 *   <li>{@link #client()} — the client-side panel runtime: providers,
 *       imperative panels, focus/inspect state. Present only on the client
 *       distribution.</li>
 *   <li>{@link #server()} — the server-side shared-panel registry: panels
 *       the server declares once and every watching client sees identically.
 *       Present whenever a logical server exists (integrated or dedicated).</li>
 * </ul>
 * Code offering panels should either run after installation or guard with
 * {@link #available()}.
 */
public final class Nimbus {

    private static @Nullable NimbusClient client;
    private static @Nullable NimbusServer server;

    private Nimbus() {}

    /**
     * Installs the runtime services. Called by the implementing mod during
     * init; not API for ordinary consumers.
     */
    public static void install(@Nullable NimbusClient client, @Nullable NimbusServer server) {
        Nimbus.client = client;
        Nimbus.server = server;
    }

    /** Drops the installed services (shutdown / server stop). */
    public static void uninstall() {
        client = null;
        server = null;
    }

    /** Whether the client-side runtime is installed. */
    public static boolean available() {
        return client != null;
    }

    /** The client-side panel runtime. Only valid on the client distribution. */
    public static NimbusClient client() {
        return Checks.checkNotNull(client, "nimbus client runtime is not installed");
    }

    /**
     * The server-side shared-panel registry, or null when no logical server
     * is running (or the server half is not installed).
     */
    public static @Nullable NimbusServer server() {
        return server;
    }
}
