package dev.vfyjxf.nimbusprojection.api.provider;

import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;

/**
 * Collects the panel specs a {@link PanelProvider} wants alive this pass.
 */
@FunctionalInterface
public interface PanelSink {

    /**
     * Keeps alive (or creates) the panel described by {@code spec}.
     * Re-offering an existing key preserves the panel's widget and state.
     */
    void offer(PanelSpec spec);

}
