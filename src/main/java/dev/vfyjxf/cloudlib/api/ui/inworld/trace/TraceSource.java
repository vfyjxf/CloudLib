package dev.vfyjxf.cloudlib.api.ui.inworld.trace;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The minimal description of where a trace comes from: a world position, plus
 * optional typed payloads ({@link #blockPos()}, {@link #entity(ClientLevel)})
 * letting implementations recognize what backs the source.
 * <p>
 * The contract deliberately carries no attachment semantics — which point of a
 * block or entity a trace visually binds to is the implementation's call (the
 * {@code cursor} package offers an optional SPI for resolving such points).
 */
public interface TraceSource {

    /**
     * Resolves the source's world position for the rendered frame, or
     * {@code null} when the source is currently unavailable (unloaded chunk,
     * dead entity, …). Implementations backed by moving targets should
     * interpolate with {@code partialTick} to avoid step-jitter.
     */
    @Nullable
    Vec3 position(ClientLevel level, float partialTick);

    /** The backing block position when this source is block-bound, else {@code null}. */
    default @Nullable BlockPos blockPos() {
        return null;
    }

    /** The backing entity when this source is entity-bound, else {@code null}. */
    default @Nullable Entity entity(ClientLevel level) {
        return null;
    }
}
