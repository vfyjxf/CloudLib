package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Rect;

/**
 * The frame context an {@link ExclusionProvider} is evaluated against, in
 * gui-scaled screen pixels.
 *
 * @param screenWidth  the gui-scaled screen width
 * @param screenHeight the gui-scaled screen height
 * @param partialTick  the frame's partial tick, for providers that animate
 */
public record ExclusionContext(int screenWidth, int screenHeight, float partialTick) {

    public ExclusionContext {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("screen size must be positive: " + screenWidth + "x" + screenHeight);
        }
    }

    /** The full-screen rectangle providers should clip against. */
    public Rect viewport() {
        return new Rect(0, 0, screenWidth, screenHeight);
    }
}
