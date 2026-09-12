package dev.vfyjxf.nimbusprojection.api.sync;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side handle of a shared panel's message channel — the counterpart
 * of the client-side
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.PanelChannel}.
 */
public interface SharedPanelChannel {

    /** Sends a payload to one specific watching player. */
    void sendTo(ServerPlayer player, CustomPacketPayload payload);

    /** Sends a payload to every player currently watching this panel. */
    void broadcast(CustomPacketPayload payload);
}
