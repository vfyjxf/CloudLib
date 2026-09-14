package dev.vfyjxf.cloudlib.api.text.layout;

/**
 * Constraints handed to the layouter.
 *
 * @param maxWidth    the width to wrap at in pixels; {@link #UNCONSTRAINED} lays out
 *                    without wrapping (only explicit breaks produce new lines)
 * @param alignment   horizontal line alignment within {@code maxWidth}
 * @param lineSpacing extra pixels inserted between lines
 */
public record LayoutConstraints(int maxWidth, TextAlignment alignment, int lineSpacing) {

    public static final int UNCONSTRAINED = Integer.MAX_VALUE;

    public static LayoutConstraints wrap(int maxWidth) {
        return new LayoutConstraints(maxWidth, TextAlignment.LEFT, 0);
    }

    public static LayoutConstraints unconstrained() {
        return new LayoutConstraints(UNCONSTRAINED, TextAlignment.LEFT, 0);
    }

    public LayoutConstraints {
        if (maxWidth < 0) throw new IllegalArgumentException("maxWidth must be >= 0");
        if (alignment == null) throw new NullPointerException("alignment");
    }

    public boolean constrained() {
        return maxWidth != UNCONSTRAINED;
    }

    public LayoutConstraints withAlignment(TextAlignment alignment) {
        return new LayoutConstraints(maxWidth, alignment, lineSpacing);
    }

    public LayoutConstraints withLineSpacing(int lineSpacing) {
        return new LayoutConstraints(maxWidth, alignment, lineSpacing);
    }
}
