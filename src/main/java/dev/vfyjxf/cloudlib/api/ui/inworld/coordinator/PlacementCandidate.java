package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One placement an element offers the coordinator, in the mandated
 * <em>dual representation</em>: the world-space {@link WorldAabb} and the
 * gui-pixel {@link FloatRect screen rect} it projects to this frame, carried
 * as a pair (§3.2). Screen-space overlap arbitration looks at the screen rect;
 * occlusion sampling and leader routing look at the world box — never one
 * alone. Screen-space candidates (tracked/panel elements) have no world box
 * and use {@link #screen}.
 * <p>
 * A {@code world-only} element may offer pure world geometry with no
 * projection at all ({@link #world}) — such a candidate never enters screen
 * arbitration, so it carries no screen rect.
 *
 * @param world the candidate's world bounds; {@code null} for screen-space
 *        candidates
 * @param screenRect the candidate's projected rect, gui-scaled pixels, this
 *        frame; {@code null} only for a world-only candidate — positive size
 *        when present
 */
public record PlacementCandidate(@Nullable WorldAabb world, @Nullable FloatRect screenRect) {

    public PlacementCandidate {
        if (screenRect != null && (screenRect.width() <= 0 || screenRect.height() <= 0)) {
            throw new IllegalArgumentException("screenRect must have positive size: " + screenRect);
        }
    }

    /** A screen-space candidate with no world representation. */
    public static PlacementCandidate screen(FloatRect screenRect) {
        return new PlacementCandidate(null, Objects.requireNonNull(screenRect, "screenRect"));
    }

    /** A world candidate carrying both representations. */
    public static PlacementCandidate dual(WorldAabb world, FloatRect screenRect) {
        return new PlacementCandidate(
            Objects.requireNonNull(world, "world"),
            Objects.requireNonNull(screenRect, "screenRect")
        );
    }

    /**
     * A world-only candidate: pure world geometry that is never projected and
     * never enters screen arbitration — the coordinator grants it to a
     * world-only element as-is.
     */
    public static PlacementCandidate world(WorldAabb world) {
        return new PlacementCandidate(Objects.requireNonNull(world, "world"), null);
    }
}
