package dev.vfyjxf.nimbusprojection.api.sync;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

/**
 * The context handed to a {@link SharedPanelView} when a shared panel
 * materializes on a client.
 *
 * @param key          the shared panel's key — the returned {@code PanelSpec}
 *                     MUST be built with this key, or the spawn is rejected
 *                     (channel routing addresses panels by it)
 * @param anchor       the anchor from the shared spec
 * @param presentation the presentation hint from the shared spec — the view
 *                     may override it on the returned {@code PanelSpec}
 * @param payload      the server's data payload, decoded with the codec
 *                     given at view registration; null when the spec
 *                     carried none
 */
public record SharedViewContext<P extends CustomPacketPayload>(
        ClientLevel level,
        LocalPlayer player,
        PanelKey key,
        InworldAnchor anchor,
        Presentation presentation,
        @Nullable P payload
) {
}
