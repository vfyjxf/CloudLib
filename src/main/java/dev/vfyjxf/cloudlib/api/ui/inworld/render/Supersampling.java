package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.List;

/**
 * The pure math behind adaptive surface supersampling: a world panel's FBO is
 * rasterized at {@code logicalSize × ss} texels, then magnified onto its quad
 * by the world pass — a fixed {@code ss} is either wasteful (panel minified at
 * distance) or blurry (panel magnified up close, one texel spanning many
 * framebuffer pixels). The helpers here size the surface so one texel lands on
 * roughly one framebuffer pixel:
 * <ul>
 *   <li>{@link #projectQuad} — the quad's on-screen bounding box in
 *       framebuffer pixels;</li>
 *   <li>{@link #desired} — the supersample factor that matches that projected
 *       size, quantized to integers and clamped to {@code [floor, maxSupersample]}
 *       (the profile-configured {@code supersample} acts as the floor, so an
 *       explicit user value is a minimum, not a target); a degenerate
 *       projection answers 0 — no observation, the caller holds;</li>
 *   <li>{@link #allocate} — a global texel budget across all visible panels:
 *       when the frame overspends, panels are granted in descending screen
 *       area (big panels stay sharp, small ones give way) and never drop
 *       below their floor;</li>
 *   <li>{@link #needsMipmap} — mipmaps only pay off while the quad minifies
 *       the surface; a magnified surface samples level 0 exclusively, so
 *       regenerating the chain is skipped.</li>
 * </ul>
 * All functions are headless-pure (no GL, no game state); the quantization +
 * multi-frame-stability that keeps the FBO from resizing every frame lives in
 * {@link SupersampleController}.
 */
public final class Supersampling {

    /** The upper bound for adaptive supersampling — beyond this the surface costs more than it returns. */
    public static final int maxSupersample = 8;

    /**
     * The default global texel budget across all visible panel surfaces
     * (8M texels ≈ a 2880×2880 RGBA8 color+depth pair on the table at once).
     */
    public static final long defaultTexelBudget = 8L * 1024 * 1024;

    private Supersampling() {}

    /** The quad's projected bounding-box size in framebuffer pixels. */
    public record ProjectedSize(double widthPx, double heightPx) {
        /** The projected bounding-box area in framebuffer pixels. */
        public double areaPx() {
            return widthPx * heightPx;
        }
    }

    /**
     * Projects a quad's four corners through {@code viewToClip · worldToView}
     * into a {@code viewportW × viewportH} framebuffer and returns the
     * bounding-box size — the panel's actual on-screen extent, perspective and
     * orientation included.
     *
     * @return null when any corner sits behind the camera (clip.w ≤ 0 — the
     *     projected extent would flip); callers should hold their previous
     *     supersample decision instead
     */
    public static @Nullable ProjectedSize projectQuad(
            Matrix4f worldToView,
            Matrix4f viewToClip,
            QuadBasis basis,
            int wPx,
            int hPx,
            int viewportW,
            int viewportH) {
        Matrix4f worldToClip = new Matrix4f(viewToClip).mul(worldToView);
        Vec3[] corners = quadCorners(basis, wPx, hPx);
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            Vector4f clip =
                    worldToClip.transform(new Vector4f((float) corner.x, (float) corner.y, (float) corner.z, 1f));
            if (clip.w <= 1.0e-6f) return null;
            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            double x = (ndcX + 1f) * 0.5f * viewportW;
            double y = (1f - ndcY) * 0.5f * viewportH;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return new ProjectedSize(maxX - minX, maxY - minY);
    }

    private static Vec3[] quadCorners(QuadBasis basis, int wPx, int hPx) {
        Vec3 o = basis.origin();
        return new Vec3[] {
            o,
            o.add(basis.u().scale(wPx)),
            o.add(basis.v().scale(hPx)),
            o.add(basis.u().scale(wPx)).add(basis.v().scale(hPx))
        };
    }

    /**
     * The supersample factor that keeps one surface texel ≈ one framebuffer
     * pixel: {@code clamp(round(projectedHeightPx / logicalHeight), floor, maxSupersample)}.
     * {@code projectedHeightPx} is in framebuffer pixels — a gui-logical
     * projected height times the gui scale.
     *
     * @param floor the minimum factor (the profile's supersample setting — an
     *     explicit user value is a lower bound, never a target); clamped to
     *     {@code [1, maxSupersample]} itself
     * @return 0 when there is no observation this frame (non-positive or NaN
     *     projected height, non-positive logical height) — a degenerate
     *     projection (quad edge-on, a corner behind the camera) is "no data",
     *     not "no magnification", so the caller's
     *     {@link SupersampleController} holds its current factor instead of
     *     reading it as the floor and resizing
     */
    public static int desired(double projectedHeightPx, int logicalHeight, int floor) {
        if (logicalHeight <= 0 || !(projectedHeightPx > 0)) return 0;
        int min = clampFloor(floor);
        long ss = Math.round(projectedHeightPx / logicalHeight);
        return (int) Math.max(min, Math.min((long) maxSupersample, ss));
    }

    /** One panel's ask for {@link #allocate}. */
    public record Request(int logicalWidth, int logicalHeight, int desired, int floor, double screenArea) {}

    /**
     * Grants every request a supersample factor under the global texel budget
     * ({@code logicalWidth·ss × logicalHeight·ss} texels per panel):
     * requests are served in descending screen area so large panels keep
     * their desired sharpness while smaller ones step down to fit; a panel is
     * never granted below its floor — floors outrank the budget, so an
     * all-floors frame may still overshoot. Ties break by list order, making
     * the allocation deterministic.
     *
     * @return the granted factors, aligned with {@code requests}
     */
    public static int[] allocate(List<Request> requests, long budgetTexels) {
        int n = requests.size();
        int[] granted = new int[n];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> {
            int byArea =
                    Double.compare(requests.get(b).screenArea(), requests.get(a).screenArea());
            return byArea != 0 ? byArea : Integer.compare(a, b);
        });
        long used = 0;
        for (int idx : order) {
            Request r = requests.get(idx);
            int floor = clampFloor(r.floor());
            int ss = Math.max(floor, Math.min(maxSupersample, Math.max(1, r.desired())));
            long base = (long) r.logicalWidth() * r.logicalHeight();
            while (ss > floor && used + texels(base, ss) > budgetTexels) ss--;
            granted[idx] = ss;
            used += texels(base, ss);
        }
        return granted;
    }

    private static long texels(long logicalArea, int ss) {
        return logicalArea * (long) ss * ss;
    }

    /**
     * Whether the surface's mip chain needs regenerating: mipmaps only filter
     * the world quad's minification of the surface, so a quad that magnifies
     * (or matches) the surface skips {@code glGenerateMipmap} entirely.
     * Unknown projected sizes (non-positive or NaN) conservatively generate.
     */
    public static boolean needsMipmap(int surfaceW, int surfaceH, double projectedW, double projectedH) {
        if (!(projectedW > 0) || !(projectedH > 0)) return true;
        return projectedW < surfaceW || projectedH < surfaceH;
    }

    private static int clampFloor(int floor) {
        return Math.max(1, Math.min(maxSupersample, floor));
    }
}
