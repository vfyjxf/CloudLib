package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Rect;

import java.util.List;

/**
 * The three-layer rectangle hierarchy every frame resolves against:
 * {@code viewport} (the whole gui-scaled screen), {@code safeArea} (viewport
 * minus platform safe-area insets — notches, overscan) and {@code workArea}
 * (safe area minus <em>struts</em>: edge-hugging exclusion rectangles such as
 * the vanilla HUD columns, which push the corresponding edge inward).
 * <p>
 * Struts only move an edge when they hug it — their span overlaps the edge's
 * extent and they touch that side of the safe area — and each strut claims the
 * single edge it hugs thinnest (a full-width top bar claims the top edge, not
 * the left and right ones). Multiple struts on one edge contribute their
 * maximum thickness, never a sum. Struts floating in the interior are ignored
 * here; interior avoidance is the occupancy bitmap's job, not a strut's.
 * <p>
 * Instances are immutable; the {@code with*} methods return derived copies.
 */
public final class LayoutSpace {

    private final Rect viewport;
    private final Insets safeInsets;
    private final List<Rect> struts;

    private LayoutSpace(Rect viewport, Insets safeInsets, List<Rect> struts) {
        this.viewport = viewport;
        this.safeInsets = safeInsets;
        this.struts = List.copyOf(struts);
    }

    /** A space covering the whole screen with no safe insets and no struts. */
    public static LayoutSpace of(int width, int height) {
        return new LayoutSpace(new Rect(0, 0, width, height), Insets.zero, List.of());
    }

    public Rect viewport() {
        return viewport;
    }

    public Insets safeInsets() {
        return safeInsets;
    }

    /** The strut rectangles this space was built with, in the order given. */
    public List<Rect> struts() {
        return struts;
    }

    /** {@code viewport} inset by {@link #safeInsets()}, clamped to non-negative size. */
    public Rect safeArea() {
        return inset(viewport, safeInsets);
    }

    /**
     * The per-edge thickness the struts push each safe-area edge inward by.
     * Each strut claims the one edge it hugs thinnest (ties resolved in
     * left/top/right/bottom order) — a full-width top bar claims the top, not
     * the left and right — and each edge takes the maximum thickness claimed
     * against it, never a sum.
     */
    public Insets strutInsets() {
        Rect safe = safeArea();
        int left = 0;
        int right = 0;
        int top = 0;
        int bottom = 0;
        for (Rect strut : struts) {
            if (strut.width() <= 0 || strut.height() <= 0) {
                continue;
            }
            boolean verticalOverlap = strut.y() < safe.bottom() && strut.bottom() > safe.y();
            boolean horizontalOverlap = strut.x() < safe.right() && strut.right() > safe.x();
            int claimedEdge = -1;
            int claimedDepth = Integer.MAX_VALUE;
            if (verticalOverlap && strut.x() <= safe.x() && strut.right() > safe.x()) {
                claimedEdge = 0;
                claimedDepth = strut.right() - safe.x();
            }
            if (horizontalOverlap && strut.y() <= safe.y() && strut.bottom() > safe.y()) {
                int depth = strut.bottom() - safe.y();
                if (depth < claimedDepth) {
                    claimedEdge = 1;
                    claimedDepth = depth;
                }
            }
            if (verticalOverlap && strut.right() >= safe.right() && strut.x() < safe.right()) {
                int depth = safe.right() - strut.x();
                if (depth < claimedDepth) {
                    claimedEdge = 2;
                    claimedDepth = depth;
                }
            }
            if (horizontalOverlap && strut.bottom() >= safe.bottom() && strut.y() < safe.bottom()) {
                int depth = safe.bottom() - strut.y();
                if (depth < claimedDepth) {
                    claimedEdge = 3;
                    claimedDepth = depth;
                }
            }
            switch (claimedEdge) {
                case 0 -> left = Math.max(left, claimedDepth);
                case 1 -> top = Math.max(top, claimedDepth);
                case 2 -> right = Math.max(right, claimedDepth);
                case 3 -> bottom = Math.max(bottom, claimedDepth);
                default -> {}
            }
        }
        return new Insets(top, right, bottom, left);
    }

    /** {@code safeArea} inset by {@link #strutInsets()}, clamped to non-negative size. */
    public Rect workArea() {
        return inset(safeArea(), strutInsets());
    }

    public LayoutSpace withViewport(int width, int height) {
        return new LayoutSpace(new Rect(0, 0, width, height), safeInsets, struts);
    }

    public LayoutSpace withSafeInsets(Insets insets) {
        return new LayoutSpace(viewport, insets, struts);
    }

    public LayoutSpace withStruts(List<Rect> newStruts) {
        return new LayoutSpace(viewport, safeInsets, newStruts);
    }

    private static Rect inset(Rect rect, Insets insets) {
        int x = rect.x() + insets.left();
        int y = rect.y() + insets.top();
        int width = Math.max(0, rect.width() - insets.left() - insets.right());
        int height = Math.max(0, rect.height() - insets.top() - insets.bottom());
        return new Rect(x, y, width, height);
    }
}
