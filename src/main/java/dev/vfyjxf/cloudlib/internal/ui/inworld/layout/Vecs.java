package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;

import java.util.Objects;

/** Small {@link Vec3} helpers with the solver's strictness conventions. */
public final class Vecs {

    private Vecs() {}

    /**
     * The unit vector. Unlike {@link Vec3#normalize()} this refuses
     * degenerate input — a zero axis is a caller bug, not a direction.
     */
    public static Vec3 unit(Vec3 v) {
        double len = v.length();
        if (len < 1.0E-10) {
            throw new IllegalArgumentException("zero vector");
        }
        return v.scale(1.0 / len);
    }

    @Contract("_ -> param1")
    public static Vec3 finite(Vec3 v) {
        Objects.requireNonNull(v);
        if (Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z)) {
            return v;
        }
        throw new IllegalArgumentException("finite vector required");
    }
}
