package dev.vfyjxf.cloudlib.internal.ui.inworld.lines;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.render.TraceStyle;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Screen-space polyline stroker for trace links — emits joined,
 * antialiased ribbons in gui px through the canvas's colored batch as
 * plain {@code POSITION_COLOR} quads: no shaders, no extra vertex
 * formats, and the batch's own scissor/z conventions apply untouched.
 * <p>
 * Every station gets a four-point cross-section — an alpha-1 core band
 * inset {@link #edgeInset} px inside the nominal edge and an alpha-0
 * fringe {@link #feather} px past it — so the silhouette feathers over
 * ~1 px whatever the width. Interior vertices join with a miter while
 * {@code k = 1/cos(θ/2) ≤ miterLimit} (a 90° elbow needs only √2);
 * sharper turns fall back to a bevel — two stations on the outer side
 * plus a wedge — so folds can't spike.
 * <p>
 * {@link TraceStyle} changes the recipe, not the path: {@code HALO}
 * underdraws a wide dim ribbon, {@code DISSOLVE} ramps vertex alpha in
 * over the first {@link #dissolveLen} px of arc length, {@code ENERGY}
 * clips marching dash windows against each segment's arc range
 * (interval math, not subdivision), and {@code BEAM} renders as
 * {@code CRISP}.
 * <p>
 * Degenerate input is skipped piecewise: non-finite or duplicate points
 * are dropped, collapsed strips never reach the batch. {@link #emit} is
 * a plain batch producer like {@code SceneCanvas.line} — call it outside
 * {@code canvas.batch(...)} scopes; the caller owns
 * {@code canvas.flushBatch()}.
 */
public final class ScreenStroke {

    /** Miter join limit — k = 1/cos(halfTurn); past it the corner bevels. */
    private static final float miterLimit = 2f;
    /** Fringe reach past the nominal edge, px — the alpha-0 band offset. */
    private static final float feather = 0.75f;
    /** Core band inset, px — the alpha-1 edge sits this far inside the edge. */
    private static final float edgeInset = 0.35f;
    /** Per-vertex band alphas for offsets (−out, −in, +in, +out). */
    private static final float[] bandAlpha = {0f, 1f, 1f, 0f};
    /** DISSOLVE fade-in length in px along the arc length. */
    private static final float dissolveLen = 24f;
    /** Cut positions (arc px) subdividing the DISSOLVE ramp. */
    private static final float[] rampCuts = {6f, 12f, 18f, 24f};
    /** ENERGY dash period in px and the lit fraction of it. */
    private static final float dashPeriod = 14f;

    private static final float dashDuty = 0.55f;
    /** ENERGY march speed in px/s toward the target. */
    private static final float dashSpeed = 30f;
    /** Length/area epsilon in gui px. */
    private static final float eps = 1.0e-4f;

    private ScreenStroke() {}

    /**
     * Emits a joined, AA'd polyline in gui px through the canvas's
     * colored batch.
     *
     * @param canvas  the canvas whose colored batch receives the quads
     * @param pts     routed polyline points in gui px
     * @param widthPx line width in gui px (the nominal edge-to-edge size)
     * @param argb    stroke color
     * @param style   shading recipe; null → {@link TraceStyle.crisp}
     */
    public static void emit(SceneCanvas canvas, List<FloatPos> pts, float widthPx, int argb, TraceStyle style) {
        if (canvas == null || pts == null || pts.size() < 2) return;
        if (!Float.isFinite(widthPx) || widthPx <= 0f || (argb >>> 24) == 0) return;
        double[] xs = new double[pts.size()];
        double[] ys = new double[pts.size()];
        int n = clean(pts, xs, ys);
        if (n < 2) return;
        // arc length per vertex — style effects (dissolve ramp, dashes) key off it
        float[] s = new float[n];
        for (int i = 1; i < n; i++) {
            double ex = xs[i] - xs[i - 1];
            double ey = ys[i] - ys[i - 1];
            s[i] = s[i - 1] + (float) Math.sqrt(ex * ex + ey * ey);
        }
        float hw = widthPx * 0.5f;
        switch (style == null ? TraceStyle.crisp : style) {
            case halo -> {
                // a wide, dim ribbon under the crisp core
                joined(canvas, xs, ys, s, n, hw + 3f, argb, 0.15f, null);
                joined(canvas, xs, ys, s, n, hw, argb, 1f, null);
            }
            case dissolve -> joined(canvas, xs, ys, s, n, hw, argb, 1f, ScreenStroke::dissolveAlpha);
            case energy -> dashes(canvas, xs, ys, s, n, hw, argb);
            default -> joined(canvas, xs, ys, s, n, hw, argb, 1f, null);
        }
    }

    // region stroke passes

    /**
     * One joined pass over the polyline: per-vertex stations from the
     * segment normals — mitered while {@code k ≤ miterLimit}, a bevel
     * pair plus wedge past it — then a band strip per segment, subdivided
     * at {@link #rampCuts} wherever {@code alphaAt} bends the ramp.
     */
    private static void joined(
            SceneCanvas canvas,
            double[] xs,
            double[] ys,
            float[] s,
            int n,
            float hw,
            int argb,
            float scale,
            @Nullable AlphaScale alphaAt) {
        float oIn = Math.max(hw - edgeInset, 0f);
        float oOut = hw + feather;
        float[] off = {-oOut, -oIn, oIn, oOut};

        // per-segment unit directions and their left normals
        float[] dx = new float[n - 1];
        float[] dy = new float[n - 1];
        float[] nx = new float[n - 1];
        float[] ny = new float[n - 1];
        for (int i = 0; i + 1 < n; i++) {
            float ex = (float) (xs[i + 1] - xs[i]);
            float ey = (float) (ys[i + 1] - ys[i]);
            float len = (float) Math.sqrt(ex * ex + ey * ey);
            if (len < eps) continue;
            dx[i] = ex / len;
            dy[i] = ey / len;
            nx[i] = -dy[i];
            ny[i] = dx[i];
        }

        // stations per vertex: in[i] ends segment i-1, out[i] starts
        // segment i — identical unless the vertex bevels into two
        Station[] in = new Station[n];
        Station[] out = new Station[n];
        in[0] = out[0] = station(xs[0], ys[0], nx[0], ny[0], off, argb, alpha(scale, alphaAt, s[0]));
        in[n - 1] = out[n - 1] =
                station(xs[n - 1], ys[n - 1], nx[n - 2], ny[n - 2], off, argb, alpha(scale, alphaAt, s[n - 1]));
        for (int i = 1; i + 1 < n; i++) {
            float a = alpha(scale, alphaAt, s[i]);
            float mx = nx[i - 1] + nx[i];
            float my = ny[i - 1] + ny[i];
            float ml = (float) Math.sqrt(mx * mx + my * my);
            float k;
            if (ml < eps) {
                // a 180° fold — keep the incoming normal; the turn draws
                // as a clean butt and the reversal reads as one line
                mx = nx[i - 1];
                my = ny[i - 1];
                k = 1f;
            } else {
                mx /= ml;
                my /= ml;
                float dot = mx * nx[i - 1] + my * ny[i - 1];
                k = dot > eps ? 1f / dot : miterLimit;
            }
            if (k <= miterLimit) {
                in[i] = out[i] = station(xs[i], ys[i], mx * k, my * k, off, argb, a);
            } else {
                // sharp corner — bevel the outer side; the turn's sign
                // picks which side is outer (cross < 0 → the +n side)
                float cross = dx[i - 1] * dy[i] - dy[i - 1] * dx[i];
                float side = cross < 0f ? 1f : -1f;
                in[i] = bevelStation(xs[i], ys[i], nx[i - 1], ny[i - 1], mx, my, side, off, argb, a);
                out[i] = bevelStation(xs[i], ys[i], nx[i], ny[i], mx, my, side, off, argb, a);
            }
        }

        for (int i = 0; i + 1 < n; i++) {
            segment(
                    canvas, out[i], in[i + 1], xs[i], ys[i], dx[i], dy[i], nx[i], ny[i], s[i], s[i + 1], off, argb,
                    scale, alphaAt);
        }
        for (int i = 1; i + 1 < n; i++) {
            if (in[i] != out[i]) {
                strip(canvas, in[i], out[i]); // bevel wedge
            }
        }
    }

    /**
     * ENERGY: marching dashes flowing source→target — clip each lit
     * window {@code [k·P+φ, k·P+φ + duty·P]} against the segment's arc
     * range and emit one band strip per visible interval.
     */
    private static void dashes(SceneCanvas canvas, double[] xs, double[] ys, float[] s, int n, float hw, int argb) {
        float oIn = Math.max(hw - edgeInset, 0f);
        float oOut = hw + feather;
        float[] off = {-oOut, -oIn, oIn, oOut};
        float duty = dashPeriod * dashDuty;
        float phase = (float) (Util.getMillis() / 1000.0 * dashSpeed % dashPeriod);
        for (int i = 0; i + 1 < n; i++) {
            float ex = (float) (xs[i + 1] - xs[i]);
            float ey = (float) (ys[i + 1] - ys[i]);
            float len = (float) Math.sqrt(ex * ex + ey * ey);
            if (len < eps) continue;
            float ux = ex / len;
            float uy = ey / len;
            float nx = -uy;
            float ny = ux;
            float s0 = s[i];
            float s1 = s[i + 1];
            int kMin = (int) Math.floor((s0 - phase - duty) / dashPeriod) + 1;
            int kMax = (int) Math.floor((s1 - phase) / dashPeriod);
            for (int k = kMin; k <= kMax; k++) {
                float a = Math.max(s0, k * dashPeriod + phase);
                float b = Math.min(s1, k * dashPeriod + phase + duty);
                if (b - a < eps) continue;
                Station sa = station(xs[i] + ux * (a - s0), ys[i] + uy * (a - s0), nx, ny, off, argb, 1f);
                Station sb = station(xs[i] + ux * (b - s0), ys[i] + uy * (b - s0), nx, ny, off, argb, 1f);
                strip(canvas, sa, sb);
            }
        }
    }

    // endregion

    // region geometry emit

    /**
     * The strip between two stations of one segment — three band quads
     * (−out→−in→+in→+out), subdivided at {@link #rampCuts} when an alpha
     * ramp is live.
     */
    private static void segment(
            SceneCanvas canvas,
            Station from,
            Station to,
            double px,
            double py,
            float dx,
            float dy,
            float nx,
            float ny,
            float s0,
            float s1,
            float[] off,
            int argb,
            float scale,
            @Nullable AlphaScale alphaAt) {
        if (alphaAt == null) {
            strip(canvas, from, to);
            return;
        }
        Station prev = from;
        for (float sc : rampCuts) {
            if (sc <= s0 + eps || sc >= s1 - eps) continue;
            Station mid =
                    station(px + dx * (sc - s0), py + dy * (sc - s0), nx, ny, off, argb, alpha(scale, alphaAt, sc));
            strip(canvas, prev, mid);
            prev = mid;
        }
        strip(canvas, prev, to);
    }

    /**
     * The three adjacent band quads between two stations; zero-area or
     * non-finite pieces (collapsed joins, empty wedges) are skipped.
     */
    private static void strip(SceneCanvas canvas, Station a, Station b) {
        for (int j = 0; j < 3; j++) {
            float x0 = a.x[j];
            float y0 = a.y[j];
            float x1 = b.x[j];
            float y1 = b.y[j];
            float x2 = b.x[j + 1];
            float y2 = b.y[j + 1];
            float x3 = a.x[j + 1];
            float y3 = a.y[j + 1];
            float area2 = x0 * y1 - x1 * y0 + x1 * y2 - x2 * y1 + x2 * y3 - x3 * y2 + x3 * y0 - x0 * y3;
            if (!Float.isFinite(area2) || Math.abs(area2) < eps * eps) continue;
            canvas.coloredQuad(x0, y0, x1, y1, x2, y2, x3, y3, a.c[j], b.c[j], b.c[j + 1], a.c[j + 1]);
        }
    }

    // endregion

    // region stations

    /**
     * A stroke cross-section: four points at offsets
     * {@code (−oOut, −oIn, +oIn, +oOut)} with baked per-vertex colors.
     */
    private static final class Station {
        final float[] x = new float[4];
        final float[] y = new float[4];
        final int[] c = new int[4];
    }

    /** A station whose four points share one offset direction (butt or miter). */
    private static Station station(double px, double py, float ux, float uy, float[] off, int argb, float mul) {
        Station st = new Station();
        for (int j = 0; j < 4; j++) {
            st.x[j] = (float) (px + ux * off[j]);
            st.y[j] = (float) (py + uy * off[j]);
        }
        bakeColors(st, argb, mul);
        return st;
    }

    /**
     * Half of a bevel join: outer-side points ride the segment's own
     * normal (the two-station split), inner-side points sit on the miter
     * ray clamped to {@link #miterLimit}.
     */
    private static Station bevelStation(
            double px,
            double py,
            float nox,
            float noy,
            float mx,
            float my,
            float side,
            float[] off,
            int argb,
            float mul) {
        Station st = new Station();
        for (int j = 0; j < 4; j++) {
            float o = off[j];
            if (o * side > 0f) {
                st.x[j] = (float) (px + nox * o);
                st.y[j] = (float) (py + noy * o);
            } else {
                st.x[j] = (float) (px + mx * miterLimit * o);
                st.y[j] = (float) (py + my * miterLimit * o);
            }
        }
        bakeColors(st, argb, mul);
        return st;
    }

    private static void bakeColors(Station st, int argb, float mul) {
        int rgb = argb & 0xFFFFFF;
        int a = argb >>> 24;
        for (int j = 0; j < 4; j++) {
            int aj = Math.min(255, Math.max(0, Math.round(a * bandAlpha[j] * mul)));
            st.c[j] = rgb | (aj << 24);
        }
    }

    // endregion

    // region helpers

    /** Per-vertex alpha multiplier along the arc length — null → opaque. */
    private interface AlphaScale {
        float at(float s);
    }

    private static float alpha(float scale, @Nullable AlphaScale alphaAt, float s) {
        return scale * (alphaAt == null ? 1f : alphaAt.at(s));
    }

    /** smoothstep(0, dissolveLen, s) — the fade-in out of the source port. */
    private static float dissolveAlpha(float s) {
        float t = s / dissolveLen;
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return t * t * (3f - 2f * t);
    }

    /**
     * Copies {@code pts} into {@code xs}/{@code ys}, dropping non-finite
     * points and sub-epsilon duplicates; returns the kept count.
     */
    private static int clean(List<FloatPos> pts, double[] xs, double[] ys) {
        int n = 0;
        for (FloatPos p : pts) {
            if (p == null || !Double.isFinite(p.x) || !Double.isFinite(p.y)) continue;
            if (n > 0) {
                double ex = p.x - xs[n - 1];
                double ey = p.y - ys[n - 1];
                if (ex * ex + ey * ey < eps * eps) continue;
            }
            xs[n] = p.x;
            ys[n] = p.y;
            n++;
        }
        return n;
    }

    // endregion
}
