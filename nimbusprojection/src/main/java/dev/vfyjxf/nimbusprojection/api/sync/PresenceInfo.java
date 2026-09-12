package dev.vfyjxf.nimbusprojection.api.sync;

import java.util.UUID;

/**
 * One remote player's current interaction with a panel — relayed by the
 * server to every other watcher so clients can render ghost affordances
 * ("player X is operating this").
 *
 * @param playerId the interacting player's UUID
 * @param panelKey the panel's key — for shared panels this is the shared
 *                 spec key; for local panels presence propagates only when
 *                 the key is network-meaningful (e.g. a String)
 * @param kind     what the player is doing
 * @param sinceTick server tick the state began — for ordering/freshness
 */
public record PresenceInfo(
        UUID playerId,
        Object panelKey,
        PresenceKind kind,
        long sinceTick
) {
}
