package dev.vfyjxf.nimbusprojection.internal;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side fly animation: a cosmetic stack copy travelling from the drag
 * release point to a world target. Rendered as a billboarded item entity-style
 * sprite; when it lands the target block flashes briefly.
 * <p>
 * Timing is tick-based (advanced once per client tick) so the animation speed
 * is framerate-independent and deterministic for tests.
 */
public final class FlyingStack {

    /** Total flight time in client ticks. */
    public static final int DURATION = 9;
    /** How long the landing flash lingers on the target block. */
    public static final int FLASH_TICKS = 14;

    public final ItemStack stack;
    public final Vec3 from;
    public final Vec3 to;
    public int age;

    public FlyingStack(ItemStack stack, Vec3 from, Vec3 to) {
        this.stack = stack;
        this.from = from;
        this.to = to;
    }

    /** Advances one tick; returns false once the flight is over. */
    public boolean tick() {
        return ++age < DURATION;
    }

    public boolean landed() {
        return age >= DURATION;
    }

    /**
     * Interpolated position — eases out (fast launch, soft landing) and arcs
     * slightly upward at mid-flight so the transfer reads as a toss.
     */
    public Vec3 pos() {
        float t = Math.min(1.0f, age / (float) DURATION);
        float eased = 1.0f - (1.0f - t) * (1.0f - t);
        double arc = Math.sin(t * Math.PI) * 0.35;
        return from.lerp(to, eased).add(0, arc, 0);
    }

    /** Item sprite scale — starts large and settles to drop-size. */
    public float scale() {
        float t = Math.min(1.0f, age / (float) DURATION);
        return 0.4f - t * 0.15f;
    }
}
