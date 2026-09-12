package dev.vfyjxf.nimbusprojection.api.sync;

import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;

import java.util.UUID;

/**
 * One remote player's current interaction with a panel — relayed by the
 * server to every other watcher so clients can render ghost affordances
 * ("player X is operating this").
 *
 * @param playerId the interacting player's UUID
 * @param panelKey the panel's key — for shared panels this is the shared
 *                 spec key; for provider panels presence propagates only
 *                 for keys inside a declared shared domain
 *                 ({@code ProviderOptions.sharedDomain})
 * @param kind     what the player is doing
 * @param sinceTick server tick the state began — for ordering/freshness
 */
public record PresenceInfo(UUID playerId, PanelKey panelKey, PresenceKind kind, long sinceTick) {}
