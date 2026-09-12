package dev.vfyjxf.nimbusprojection.api.provider;

import dev.vfyjxf.nimbusprojection.api.panel.PanelGroup;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;

/**
 * Collects what a {@link PanelProvider} wants alive this pass — standalone
 * panels and explicit groups.
 */
public interface PanelSink {

    /**
     * Keeps alive (or creates) the panel described by {@code spec}.
     * Re-offering an existing key preserves the panel's widget and state.
     * Standalone specs at the same anchor merge into an implicit container
     * unless the spec declared {@code standalone()}.
     */
    void offer(PanelSpec spec);

    /**
     * Keeps alive (or creates) an explicit panel container — see
     * {@link PanelGroup} for the membership rules.
     */
    void offerGroup(PanelGroup group);

}
