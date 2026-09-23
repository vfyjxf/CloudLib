package dev.vfyjxf.cloudlib.api.text;

import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

/**
 * An inline entity render. Living entities may follow the mouse
 * (like the vanilla inventory player preview); other entities render with a fixed
 * orientation.
 *
 * @param entity      supplier of the entity to render (queried per frame; may supply
 *                    a cached dummy entity)
 * @param width       reserved box width in pixels
 * @param height      reserved box height in pixels
 * @param scale       render scale in pixels per block (roughly the entity height target)
 * @param followMouse whether the entity head/body tracks the mouse position
 */
public record EntityNode(Supplier<? extends Entity> entity, int width, int height, int scale, boolean followMouse)
        implements
            RichNode {

    public EntityNode {
        if (entity == null) throw new NullPointerException("entity");
    }

    public EntityNode(Supplier<? extends Entity> entity, int width, int height) {
        this(entity, width, height, Math.min(width, height) / 2, false);
    }
}
