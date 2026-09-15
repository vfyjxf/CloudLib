package dev.vfyjxf.cloudlib.api.ui.border;

import dev.vfyjxf.cloudlib.api.math.FloatPos;

/**
 * A resolved attach point on a screen-space frame: the position in gui px
 * plus the frame's outward direction there (a unit vector, or the zero
 * vector when the point has no preferred exit — e.g. the center).
 * <p>
 * Routers read {@link #outward()} to leave the frame in a sensible
 * direction — lead segments follow it.
 */
public record ScreenPort(FloatPos pos, FloatPos outward) {

    /** Whether this port has a preferred exit direction. */
    public boolean hasOutward() {
        return outward.x != 0 || outward.y != 0;
    }

    /** The point {@code px} gui px out along the outward direction. */
    public FloatPos lead(double px) {
        return new FloatPos(pos.x + outward.x * px, pos.y + outward.y * px);
    }
}
