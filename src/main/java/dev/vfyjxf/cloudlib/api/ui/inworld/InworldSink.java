package dev.vfyjxf.cloudlib.api.ui.inworld;

/**
 * Collects the panel specs a {@link InworldProvider} wants alive this pass.
 */
@FunctionalInterface
public interface InworldSink {

    /**
     * Keeps alive (or creates) the panel described by {@code spec}.
     * Re-offering an existing key preserves the panel's widget and state.
     */
    void offer(InworldPanelSpec spec);

}
