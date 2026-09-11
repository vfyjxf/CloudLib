package dev.vfyjxf.cloudlib.ui.inworld;

import net.minecraft.world.phys.Vec3;

/**
 * Pure, allocation-free layout math for the in-world manager — extracted so
 * the selection/spreading rules can be unit-tested without a running client.
 */
final class InworldLayout {

    private InworldLayout() {
    }

    /** Soft-focus cone half-angle — anchors within ~30° of the look vector are selectable. */
    static final double SOFT_FOCUS_COS = Math.cos(Math.toRadians(30));
    /** Soft-focus distance cap — roughly the usual block-interaction reach. */
    static final double SOFT_FOCUS_RANGE = 12.0;

    /**
     * Angular score of a soft-focus candidate: {@code 1 - cos(angle)} between
     * the look vector and eye→target, so 0 = dead ahead. Returns a negative
     * value when the target is outside the soft-focus cone.
     * {@code look} must be normalized.
     */
    static double softFocusScore(Vec3 eye, Vec3 look, Vec3 target) {
        Vec3 to = target.subtract(eye);
        double len = to.length();
        if (len < 1e-4) return 0;
        double cos = to.dot(look) / len;
        if (cos < SOFT_FOCUS_COS) return -1;
        return 1 - cos;
    }

    /**
     * Spreads sorted slot positions along an edge so neighbours keep
     * {@code gap}; when the run overflows {@code [lo, hi]} the gap shrinks to
     * fit, and the whole run is then shifted back inside. Operates in place;
     * input must be sorted ascending — order is never changed, so callers get
     * deterministic output frame to frame.
     */
    static void spreadEdgeSlots(double[] tan, double lo, double hi, double gap) {
        if (tan.length < 2) return;
        if (tan.length > 1) {
            gap = Math.min(gap, (hi - lo) / (tan.length - 1));
        }
        for (int i = 1; i < tan.length; i++) {
            if (tan[i] < tan[i - 1] + gap) tan[i] = tan[i - 1] + gap;
        }
        double shift = Math.min(0, hi - tan[tan.length - 1]);
        if (tan[0] + shift < lo) shift = lo - tan[0];
        for (int i = 0; i < tan.length; i++) tan[i] += shift;
    }

}
