package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * A panel rect change worth animating — {@code kind} is
 * {@code "appear"}, {@code "move"}, {@code "resize"} or
 * {@code "disappear"}; {@code from} may be null on appear and
 * {@code to} on disappear.
 */
public record PanelTransition(String visualId, String kind, GuiRect from, GuiRect to) {}
