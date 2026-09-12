package dev.vfyjxf.nimbusprojection.api.sync;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side receiver for payloads clients push up a shared panel's
 * channel. Registered on {@link SharedPanelSpec#channel}.
 * <p>
 * This is where server authority lives: the handler must re-validate
 * everything the client claims (reach, permissions, current state) — a
 * channel payload is a <em>request</em>, never a fact.
 */
@FunctionalInterface
public interface ServerPanelMessageHandler {

    void receive(SharedPanel panel, ServerPlayer sender, CustomPacketPayload payload);
}
