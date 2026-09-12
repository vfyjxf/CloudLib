package dev.vfyjxf.nimbusprojection.api.sync;

import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side handle of a live shared panel, returned by
 * {@link dev.vfyjxf.nimbusprojection.api.NimbusServer#share}.
 */
public interface SharedPanel {

    /** The shared panel's identity key. */
    PanelKey key();

    /** The declaration this panel was shared with. */
    SharedPanelSpec spec();

    /**
     * Re-broadcasts the panel's data payload to every watcher — clients
     * feed it to their registered {@link SharedPanelView} state. Use for
     * coarse state changes; high-frequency values belong on expose
     * channels instead.
     */
    void update(@Nullable CustomPacketPayload payload);

    /** The server-side send half of this panel's channel. */
    SharedPanelChannel channel();

    /** Revokes the panel for all watchers. */
    void unshare();

}
