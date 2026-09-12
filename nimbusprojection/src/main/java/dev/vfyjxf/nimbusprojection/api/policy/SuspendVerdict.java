package dev.vfyjxf.nimbusprojection.api.policy;

/**
 * A {@link SuspendPolicy}'s per-tick verdict for a panel.
 */
public enum SuspendVerdict {

    /** Panel lives and renders normally. */
    live,

    /** Panel suspends: hidden and non-interactive this tick, but kept. */
    suspend,

    /** Panel closes permanently (its anchor is gone, the world changed). */
    close,
}
