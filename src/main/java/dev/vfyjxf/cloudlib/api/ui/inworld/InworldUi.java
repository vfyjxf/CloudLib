package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.ui.inworld.InworldManager;
import dev.vfyjxf.cloudlib.util.Checks;

/**
 * Static entry point for the in-world UI layer.
 */
public final class InworldUi {

    private InworldUi() {
    }

    public static InworldUiApi instance() {
        return Checks.checkNotNull(InworldManager.instance(), "inworld ui is not initialized on the client");
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
