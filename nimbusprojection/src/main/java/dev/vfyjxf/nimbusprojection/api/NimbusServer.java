package dev.vfyjxf.nimbusprojection.api;

import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanel;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanelSpec;

import java.util.Collection;

/**
 * The server-side shared-panel registry.
 * <p>
 * A shared panel is declared once on the server — an anchor, a
 * client-registered view id and a payload — and the runtime broadcasts it to
 * every client in range, so all watching players see the same UI at the same
 * anchor. Clients materialize the actual widget tree through the view
 * registered under the spec's view id; the server never touches widgets.
 * <p>
 * Interactions on a shared panel travel back over the panel's
 * {@link dev.vfyjxf.nimbusprojection.api.sync.PanelChannel} to the handler
 * declared on the spec, and each player's focus/engagement state is relayed
 * to the other watchers as {@link dev.vfyjxf.nimbusprojection.api.sync.PresenceInfo}.
 */
public interface NimbusServer {

    /**
     * Declares a shared panel. Returns a handle for pushing payload updates
     * or revoking the panel; the panel persists until unshared or its anchor
     * leaves the world.
     */
    SharedPanel share(SharedPanelSpec spec);

    /** Revokes the shared panel with the given key, if present. */
    void unshare(PanelKey key);

    /** All currently shared panels. */
    Collection<? extends SharedPanel> shared();

}
