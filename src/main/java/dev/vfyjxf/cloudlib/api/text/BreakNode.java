package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.RichNode;

/**
 * An explicit line break.
 */
public record BreakNode() implements RichNode {

    public static final BreakNode INSTANCE = new BreakNode();
}
