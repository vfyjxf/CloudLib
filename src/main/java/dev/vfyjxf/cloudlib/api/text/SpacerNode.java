package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.RichNode;

/**
 * An unbreakable horizontal gap of the given width, at text line height.
 */
public record SpacerNode(int width) implements RichNode {
}
