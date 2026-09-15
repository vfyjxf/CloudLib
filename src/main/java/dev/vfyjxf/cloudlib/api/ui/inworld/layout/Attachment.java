package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One candidate anchor point on a source — where a leader line may
 * attach. {@code normal} is the outward direction for line routing when
 * known, and {@code preference} is a penalty added to routes that pick
 * this point.
 */
public record Attachment(String key, Vec3 point, @Nullable Vec3 normal, double preference) {

    public Attachment {
        Vecs.finite(point);
        if (key == null || key.isBlank() || !Double.isFinite(preference)) {
            throw new IllegalArgumentException();
        }
        if (normal != null) {
            normal = Vecs.unit(Vecs.finite(normal));
        }
    }
}
