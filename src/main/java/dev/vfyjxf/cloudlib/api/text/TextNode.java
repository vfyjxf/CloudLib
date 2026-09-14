package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.RichNode;

/**
 * A run of literal text. May contain {@code \n}, which produces line breaks.
 */
public record TextNode(String text) implements RichNode {

    public TextNode {
        if (text == null) throw new NullPointerException("text");
    }
}
