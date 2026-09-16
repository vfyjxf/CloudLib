package dev.vfyjxf.cloudlib.api.ui.inworld.trace.cursor;

import dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The entity-source origin cursor: attaches at the top of the entity's
 * bounding box, interpolated with the frame's partial tick — the point a
 * hovering source mark sits over.
 * <p>
 * Reads {@link TraceContext#level} (required — entities resolve against the
 * client level) and {@link TraceContext#partialTick} (defaults to 0 when
 * absent) from the context.
 */
public final class EntityOriginCursor implements SourceCursor {

    @Override
    public Optional<Vec3> resolve(TraceSource source, TraceContext context) {
        ClientLevel level = context.get(TraceContext.level);
        if (level == null) return Optional.empty();
        Entity entity = source.entity(level);
        if (entity == null) return Optional.empty();
        Float partialTick = context.get(TraceContext.partialTick);
        Vec3 p = entity.getPosition(partialTick == null ? 0f : partialTick);
        return Optional.of(p.add(0, entity.getDimensions(entity.getPose()).height(), 0));
    }
}
