package dev.vfyjxf.cloudlib.api.text;

/**
 * An unbreakable horizontal gap of the given width, at text line height.
 */
public record SpacerNode(int width) implements RichNode {}
