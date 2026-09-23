package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.WorldAabb;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The resolved anchor for one frame: the anchor's projected screen position
 * (the motion reference every granted offset hangs off — §3.0 iron rule 1)
 * plus, for world anchors, the world half of the dual representation.
 * Adapters produce these from entity render positions, block-face poses or
 * world→screen projection; tests build them directly.
 *
 * @param screen the anchor's projected screen position, gui pixels — always
 *        present (an anchor that does not project produces no frame)
 * @param world the anchor's world bounds; {@code null} for screen-space
 *        anchors
 * @param projectedSize the anchor's on-screen extent when meaningful (the
 *        projected size of an entity's nameplate anchor point or a block
 *        face); {@code null} when the anchor is a point
 */
public record AnchorFrame(FloatPos screen, @Nullable WorldAabb world, @Nullable Size projectedSize) {

    public AnchorFrame {
        Objects.requireNonNull(screen, "screen");
        screen = new FloatPos(screen.x(), screen.y());
    }

    /** A screen-only anchor frame. */
    public static AnchorFrame screen(FloatPos screen) {
        return new AnchorFrame(screen, null, null);
    }

    /** A world anchor frame carrying both representations. */
    public static AnchorFrame dual(FloatPos screen, WorldAabb world) {
        return new AnchorFrame(screen, Objects.requireNonNull(world, "world"), null);
    }

    /** A world anchor frame with an on-screen extent. */
    public static AnchorFrame dual(FloatPos screen, WorldAabb world, Size projectedSize) {
        return new AnchorFrame(
            screen,
            Objects.requireNonNull(world, "world"),
            Objects.requireNonNull(projectedSize, "projectedSize")
        );
    }
}
