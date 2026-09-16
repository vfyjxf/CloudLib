package dev.vfyjxf.cloudlib.api.ui.inworld.trace.cursor;

import dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceSource;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Optional helper SPI: resolves the geometric attachment point of a
 * {@link TraceSource} — the world point a trace visually binds to.
 * <p>
 * The trace contracts never prescribe attachment semantics; cursors are a
 * convenience for implementations that want the standard ones (block origin,
 * entity origin). Implementations may ignore cursors entirely and compute
 * their own attachment points.
 */
@FunctionalInterface
public interface SourceCursor {

    /**
     * Resolves the source's attachment point in world space, or empty when
     * this cursor doesn't recognize the source or the point can't be resolved
     * this frame. Implementations read what they need (level, partial tick, …)
     * from {@code context} — see {@link TraceContext}'s conventional keys.
     */
    Optional<Vec3> resolve(TraceSource source, TraceContext context);

    /** A cursor that tries this one first and falls back to {@code next} — first hit wins. */
    default SourceCursor orElse(SourceCursor next) {
        return (source, context) -> {
            Optional<Vec3> hit = resolve(source, context);
            return hit.isPresent() ? hit : next.resolve(source, context);
        };
    }
}
