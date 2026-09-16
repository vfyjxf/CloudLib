package dev.vfyjxf.cloudlib.api.ui.inworld.layout.inscreen;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutOutput.ScreenRect;

/**
 * The authority for inscreen placement: takes one inscreen layout intent plus
 * the panel's {@link ScreenHints} and produces its screen rect.
 * <p>
 * The split of control is deliberate — the API side steers through hints
 * (corner preference, grouping, collapse, indicators), while stacking,
 * conflict resolution, zoning and every other placement decision stay
 * entirely with the coordinator implementation.
 *
 * @param <I> the inscreen intent type this coordinator understands
 */
public interface ScreenLayoutCoordinator<I> {

    /**
     * Resolves the screen rect for one inscreen intent, honoring
     * {@code hints} as preferences.
     */
    ScreenRect coordinate(I intent, ScreenHints hints, LayoutContext context);
}
