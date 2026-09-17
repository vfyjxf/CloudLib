package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Rect;

import java.util.List;

/**
 * Supplies the screen rectangles a mod's UI occupies so the inworld layout
 * system can avoid them — the JEI exclusion-area pattern, and the one public
 * avoidance API of this system. Providers are evaluated every frame via
 * {@link #exclusionAreas} and must be cheap, side-effect free and non-throwing.
 * <p>
 * Register through {@link InworldExclusions}; rectangles are gui-scaled screen
 * pixels (see the {@code space} package docs for the coordinate system).
 */
@FunctionalInterface
public interface ExclusionProvider {

    /**
     * The screen rectangles this provider currently occupies. The returned
     * list is clipped to the context viewport and may be empty.
     */
    List<Rect> exclusionAreas(ExclusionContext context);
}
