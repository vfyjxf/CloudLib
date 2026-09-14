package dev.vfyjxf.cloudlib.api.text.layout;

import java.util.List;

/**
 * One horizontal line of laid-out rich text.
 *
 * @param y         top edge relative to the laid-out text origin
 * @param width     total advance width of the fragments
 * @param height    line box height (max of text line height and inline object heights)
 * @param fragments fragments in reading order
 */
public record TextLine(float y, float width, float height, List<TextFragment> fragments) {

    public TextLine {
        fragments = List.copyOf(fragments);
    }
}
