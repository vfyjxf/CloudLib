package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * One candidate center position a {@link PositionPolicy} offers, with a
 * preference cost (higher = less preferred).
 */
public record PositionSlot(String key, Vec3 center, double preference) {

    public PositionSlot {
        Objects.requireNonNull(key);
        Vecs.finite(center);
        if (!Double.isFinite(preference)) {
            throw new IllegalArgumentException();
        }
    }
}
