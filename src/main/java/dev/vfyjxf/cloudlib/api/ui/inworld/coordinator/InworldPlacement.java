package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A granted placement — the arbitration result for one element. Per §3.0 iron
 * rule 1 the rect is stored as an {@link #offsetRect} relative to the
 * {@link #anchor} projection, never as an absolute screen coordinate: the
 * anchor drifts with the camera every frame, and the element's target
 * position is the pair (anchor, offset), from which
 * {@link #screenRect()} derives the absolute rect on demand.
 *
 * @param elementId the placed element's id
 * @param variant the granted rung of the element's ladder
 * @param anchor the anchor's screen position at commit time
 * @param offsetRect the granted rect relative to {@code anchor}: its x/y are
 *        offsets, its size the granted footprint
 * @param world the world half of the granted candidate's dual representation;
 *        {@code null} for screen-space placements
 * @param arbitrationIndex the element's position in this frame's total
 *        arbitration order
 * @param epoch the decision epoch the placement was granted in
 * <p>
 * A world-only element's grant carries only the world half: the anchor is the
 * origin and the offset rect is empty, so {@link #screenRect()} is
 * meaningless — a consumer reads {@link #world()} and ignores the rest.
 */
public record InworldPlacement(
    String elementId,
    InworldVariant variant,
    FloatPos anchor,
    FloatRect offsetRect,
    @Nullable WorldAabb world,
    int arbitrationIndex,
    long epoch
) {

    public InworldPlacement {
        Objects.requireNonNull(elementId, "elementId");
        Objects.requireNonNull(variant, "variant");
        anchor = new FloatPos(anchor.x(), anchor.y());
        Objects.requireNonNull(offsetRect, "offsetRect");
        if (arbitrationIndex < 0) {
            throw new IllegalArgumentException("arbitrationIndex must not be negative: " + arbitrationIndex);
        }
    }

    /** The absolute target rect: the offset translated onto the anchor. */
    public FloatRect screenRect() {
        return offsetRect.translate(anchor.x(), anchor.y());
    }
}
