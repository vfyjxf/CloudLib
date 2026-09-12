package dev.vfyjxf.nimbusprojection.api.sync;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-side receiver for payloads the server pushes down a panel's
 * channel. Registered on
 * {@link dev.vfyjxf.nimbusprojection.api.panel.PanelSpec#channel}; the
 * matching send handle for the reverse direction is
 * {@link InworldPanelContext#channel()}.
 */
@FunctionalInterface
public interface PanelChannelHandler {

    void receive(InworldPanelContext context, CustomPacketPayload payload);

}
