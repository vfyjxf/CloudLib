package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.internal.ui.inworld.LevelOcclusionProbe;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/**
 * Samples whether world geometry blocks the line of sight to a UI surface —
 * the pure-geometry side of occlusion fading. Implementations only need the
 * one-segment primitive {@link #segmentClear}; the multi-sample visibility
 * heuristic (center plus the four corners of the quad, five equally weighted
 * segments) is a default method so every probe agrees on the sampling
 * contract. Partial occlusion of the corner segments is what makes
 * half-hidden panels fade gradually instead of popping (edge case ③).
 * <p>
 * The production implementation lives in the Minecraft-coupled adapter layer
 * (block clipping) behind {@link #ofLevel}; tests and headless layout logic
 * inject {@link #alwaysClear()} or scripted fakes.
 */
public interface OcclusionProbe {

    /**
     * @return true when the open segment from {@code from} to {@code to} is
     * unobstructed by world geometry
     */
    boolean segmentClear(Vec3 from, Vec3 to);

    /**
     * The fraction of a w×h pixel quad's five sight lines (center + corners,
     * {@code uAxis}/{@code vAxis} mapping gui pixels to world offsets — the
     * same basis a {@code QuadBasis} carries) that are clear: 1 fully
     * visible, 0 fully hidden, steps of 0.2.
     */
    default double visibility(Vec3 eye, Vec3 center, Vec3 uAxis, Vec3 vAxis, int wPx, int hPx) {
        Vec3 u = uAxis.scale(wPx * 0.5);
        Vec3 v = vAxis.scale(hPx * 0.5);
        int clear = 0;
        clear += segmentClear(eye, center) ? 1 : 0;
        clear += segmentClear(eye, center.add(u).add(v)) ? 1 : 0;
        clear += segmentClear(eye, center.add(u).subtract(v)) ? 1 : 0;
        clear += segmentClear(eye, center.subtract(u).add(v)) ? 1 : 0;
        clear += segmentClear(eye, center.subtract(u).subtract(v)) ? 1 : 0;
        return clear / 5.0;
    }

    /** The no-op probe: every segment is clear, everything fully visible. */
    static OcclusionProbe alwaysClear() {
        return (from, to) -> true;
    }

    /** The inverse no-op probe for tests: every segment blocked. */
    static OcclusionProbe alwaysOccluded() {
        return (from, to) -> false;
    }

    /**
     * The probe over live level geometry — segments clipped through the
     * visual block shapes with no fluids. Callers pass the current
     * {@link BlockGetter} (usually the client level) and rebuild when it
     * changes.
     */
    static OcclusionProbe ofLevel(BlockGetter level) {
        return new LevelOcclusionProbe(level);
    }
}
