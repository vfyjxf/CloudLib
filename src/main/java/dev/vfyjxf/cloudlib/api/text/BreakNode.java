package dev.vfyjxf.cloudlib.api.text;

/**
 * An explicit line break.
 */
public record BreakNode() implements RichNode {

    public static final BreakNode instance = new BreakNode();
}
