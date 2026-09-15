package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * Whether a panel wants a leader line back to its source. {@link #auto}
 * draws one when it helps readability, {@link #required} treats a missing
 * route as a hard failure and {@link #none} never routes.
 */
public enum LeaderMode {
    none,
    auto,
    required
}
