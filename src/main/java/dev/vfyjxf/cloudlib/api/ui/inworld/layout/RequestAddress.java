package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * Where a request ended up this frame — the tier it was assigned, the
 * container ({@code "panel"}, {@code "dock"}, {@code "drawer"}...) and a
 * human-readable reason. Keyed by request id in
 * {@link LayoutResult#addresses}.
 */
public record RequestAddress(String requestId, Tier tier, String container, String reason) {}
