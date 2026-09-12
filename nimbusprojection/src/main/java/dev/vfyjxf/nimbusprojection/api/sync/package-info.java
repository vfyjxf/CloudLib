/**
 * The dual-end model — how in-world UI stays consistent in multiplayer.
 * <ul>
 *   <li><b>Shared panels</b> — the server declares a panel once
 *       ({@link SharedPanelSpec}: anchor + client-registered view id +
 *       payload) and every watching client materializes the same UI
 *       through {@link SharedPanelView}.</li>
 *   <li><b>Channels</b> — per-panel keyed messaging:
 *       {@code PanelChannel} (client → server, on the panel context),
 *       {@link PanelChannelHandler} (server → client receive),
 *       {@link ServerPanelMessageHandler} /
 *       {@link SharedPanelChannel} (the server halves).</li>
 *   <li><b>Presence</b> — each client's focus/engage/drag/trace state is
 *       relayed to the other watchers as {@link PresenceInfo}, so other
 *       players' operations are visible.</li>
 *   <li><b>Block-entity state</b> — for BE-backed panels, live state rides
 *       CloudLib's expose machinery ({@code SyncedBlockEntity} server →
 *       client, {@code ReversedOnly} exposes client → server); channels are
 *       for panels without a synced BE.</li>
 * </ul>
 */
package dev.vfyjxf.nimbusprojection.api.sync;
