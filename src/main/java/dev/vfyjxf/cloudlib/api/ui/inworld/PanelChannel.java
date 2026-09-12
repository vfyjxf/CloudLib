package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-side handle of a panel's message channel to the server.
 * <p>
 * Every panel may carry a keyed channel: payloads sent through it arrive at
 * the server-side handler registered for the panel's key, and payloads the
 * server sends back are delivered to the handler declared on the panel's
 * spec. Payloads are ordinary {@link CustomPacketPayload}s — any type
 * registered on the mod's network registrar can ride a panel channel.
 * <p>
 * Block-entity-backed panels usually don't need this: their live state
 * rides CloudLib's expose machinery ({@code SyncedBlockEntity} server →
 * client, {@code ReversedOnly} exposes client → server). Channels exist for
 * panels <em>without</em> a synced block entity — positional panels,
 * player-inventory panels, shared panels whose anchor is a plain block.
 */
public interface PanelChannel {

    /**
     * Sends a payload to the server-side handler bound to this panel's key.
     * No-op when the panel has no server counterpart or the channel is
     * closed.
     */
    void sendToServer(CustomPacketPayload payload);

}
