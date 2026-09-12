package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Nullable;

/**
 * Static entry point for the in-world UI layer.
 * <p>
 * The API ships with CloudLib; the runtime implementation is provided by a
 * separate mod (the reference implementation is {@code nimbusprojection}) which
 * installs itself via {@link #install(InworldUiApi)} during client init.
 * Code offering panels should either run after installation or guard with
 * {@link #available()}.
 */
public final class InworldUi {

    private static @Nullable InworldUiApi impl;

    private InworldUi() {
    }

    /**
     * Installs the runtime implementation. Called once by the implementing
     * mod during client init; not API for ordinary consumers.
     */
    public static void install(InworldUiApi api) {
        impl = api;
    }

    /** Drops the installed implementation (client shutdown). */
    public static void uninstall() {
        impl = null;
    }

    /** Whether an in-world UI implementation is currently installed. */
    public static boolean available() {
        return impl != null;
    }

    public static InworldUiApi instance() {
        return Checks.checkNotNull(impl, "inworld ui is not initialized on the client");
    }

    public static InworldPanel show(InworldPanelSpec spec) {
        return instance().show(spec);
    }

    public static void registerProvider(InworldProvider provider) {
        instance().registerProvider(provider);
    }

    public static void registerProvider(InworldProvider provider, int intervalTicks) {
        instance().registerProvider(provider, intervalTicks);
    }
}
