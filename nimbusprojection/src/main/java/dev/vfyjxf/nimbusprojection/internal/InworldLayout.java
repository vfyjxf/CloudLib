package dev.vfyjxf.nimbusprojection.internal;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    /**
     * How much of a candidate rect is covered by the occupied set:
     * {@code area} = total covered px, {@code depth} = the worst single
     * thin-graze intrusion (min of intersection w/h per blocker),
     * {@code blocker} = the contributor covering the most area.
     */
    record Occl(double area, double depth, @Nullable Rect2i blocker) {
        /** A slight graze is fine — tags render behind interactive panels anyway. */
        boolean tolerable(int w, int h) {
            return blocker == null || area <= w * h * 0.15 || depth <= 5;
        }

        /** Covered share of the rect, 0..1+. */
        double coverage(int w, int h) {
            return area / (w * (double) h);
        }

        /**
         * Still readable enough to leave in place — a tag renders behind the
         * foreground chrome, so up to half-covered (or a ≤10px graze) is
         * preferable to moving the tag away from its entity.
         */
        boolean acceptable(int w, int h) {
            return blocker == null || coverage(w, h) <= 0.5 || depth <= 10;
        }

        /** Mostly hidden — the rare case where a rail slot is actually better. */
        boolean buried(int w, int h) {
            return blocker != null && coverage(w, h) > 0.72;
        }
    }

    /**
     * Retarget deadband: a resolved position only replaces the running target
     * when it moved more than {@code eps} px. Kills the per-frame target churn
     * that makes panels glide forever chasing projection noise.
     */
    static boolean retarget(double oldX, double oldY, double newX, double newY, double eps) {
        return Math.abs(newX - oldX) > eps || Math.abs(newY - oldY) > eps;
    }

    /** Frame-rate independent exponential approach. */
    static float approach(float cur, float target, float dtSeconds, float rate) {
        float k = 1f - (float) Math.exp(-dtSeconds * rate);
        return cur + (target - cur) * k;
    }

    static Occl occlusion(int x, int y, int w, int h, List<Rect2i> rects) {
        double area = 0, depth = 0, dominantArea = 0;
        Rect2i dominant = null;
        for (Rect2i o : rects) {
            int iw = Math.min(x + w, o.getX() + o.getWidth()) - Math.max(x, o.getX());
            int ih = Math.min(y + h, o.getY() + o.getHeight()) - Math.max(y, o.getY());
            if (iw <= 0 || ih <= 0) continue;
            double a = iw * (double) ih;
            area += a;
            depth = Math.max(depth, Math.min(iw, ih));
            if (a > dominantArea) {
                dominantArea = a;
                dominant = o;
            }
        }
        return new Occl(area, depth, dominant);
    }

}
