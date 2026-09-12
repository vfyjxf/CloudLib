package dev.vfyjxf.nimbusprojection.api.sync;

/**
 * What a remote player is currently doing with a panel — the granularity
 * of "operations are visible to others".
 */
public enum PresenceKind {

    /** The panel is inside the player's focus cone — the weakest signal. */
    watching,

    /** The player has engaged the panel (opened an on-demand panel). */
    engaged,

    /** The player is dragging content between the panel and the world. */
    dragging,

    /** The player is tracing a stroke on the panel surface. */
    tracing,
}
