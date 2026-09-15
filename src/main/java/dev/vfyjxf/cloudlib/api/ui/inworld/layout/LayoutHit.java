package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * A hit-test result: which visual (and which of its member requests) the
 * point landed on, plus the panel-local uv in {@code [0,1]}.
 * {@code overflow} marks hits on the dock/drawer chrome rather than a panel.
 */
public record LayoutHit(String visualId, String activeRequestId, double u, double v, boolean overflow) {}
