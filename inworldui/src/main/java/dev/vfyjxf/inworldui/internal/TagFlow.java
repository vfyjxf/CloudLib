package dev.vfyjxf.inworldui.internal;

import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pure conflict-resolution solver for non-interactive flat panels (entity
 * tags, passive floats): decides where a tag whose home rect collides with
 * committed chrome lands. No Minecraft client state — inputs are rects and
 * numbers, so every decision is deterministic and unit-testable.
 *
 * <h3>Policy</h3>
 * <ol>
 *   <li><b>Tolerate</b>: a tag renders behind the foreground chrome, so a
 *       partly covered tag stays put — see {@link InworldLayout.Occl#acceptable}.</li>
 *   <li><b>Slide</b>: past that, pick the cheapest of the four sides of the
 *       dominant blocker within a {@value #MAX_SLIDE}px budget; an escape must
 *       land readable or at least halve the coverage.</li>
 *   <li><b>Rail</b>: only when even that fails <em>and</em> the tag is nearly
 *       buried ({@link InworldLayout.Occl#buried}) does it join a side rail.</li>
 *   <li><b>Stickiness</b>: the previous slide direction gets a cost discount so
 *       near-tied sides don't flip-flop frame to frame.</li>
 * </ol>
 */
final class TagFlow {

    /** Maximum px a tag may glide to escape a blocker before it is left alone. */
    static final int MAX_SLIDE = 48;
    /** Cost discount for keeping the previous slide direction (anti flip-flop). */
    static final int DIR_STICK = 14;
    /** Graze px an escape position may keep into its blocker. */
    static final int ESCAPE_TOLERANCE = 10;
    static final int EDGE_PAD = 2;
    static final int RAIL_MARGIN = 8;
    static final int RAIL_GAP = 4;

    private TagFlow() {
    }

    enum Outcome {
        /** tag stays on its anchor (possibly behind chrome) */
        STAY,
        /** tag slid to a nearby clear spot */
        SLIDED,
        /** tag is queued for a side rail */
        RAIL
    }

    record Result(int x, int y, int slideDir, Outcome outcome) {
    }

    /**
     * Resolves one tag's position against committed obstacles.
     *
     * @param hx, hy   home position (already clamped to the viewport)
     * @param lastDir  the direction index this tag slid last frame, −1 when none
     */
    static Result resolve(int hx, int hy, int w, int h,
                          List<Rect2i> obstacles, int lastDir, int W, int H) {
        var oc = InworldLayout.occlusion(hx, hy, w, h, obstacles);
        if (oc.acceptable(w, h)) {
            return new Result(hx, hy, lastDir, Outcome.STAY);
        }
        Rect2i blocker = oc.blocker();
        if (blocker != null) {
            int tol = ESCAPE_TOLERANCE;
            int[][] candidates = {
                    {hx, blocker.getY() - h + tol},
                    {hx, blocker.getY() + blocker.getHeight() - tol},
                    {blocker.getX() - w + tol, hy},
                    {blocker.getX() + blocker.getWidth() - tol, hy}};
            int bx = 0, by = 0, bestCost = Integer.MAX_VALUE, bestDir = -1;
            for (int i = 0; i < candidates.length; i++) {
                int[] c = candidates[i];
                int cx = Math.max(EDGE_PAD, Math.min(W - w - EDGE_PAD, c[0]));
                int cy = Math.max(EDGE_PAD, Math.min(H - h - EDGE_PAD, c[1]));
                var co = InworldLayout.occlusion(cx, cy, w, h, obstacles);
                //an escape must land readable — or at least halve the cover
                if (!co.acceptable(w, h) && co.area() >= oc.area() * 0.55) continue;
                int cost = Math.abs(cx - hx) + Math.abs(cy - hy)
                        - (i == lastDir ? DIR_STICK : 0);
                if (cost < bestCost) {
                    bestCost = cost;
                    bx = cx;
                    by = cy;
                    bestDir = i;
                }
            }
            if (bestDir >= 0 && bestCost <= MAX_SLIDE) {
                return new Result(bx, by, bestDir, Outcome.SLIDED);
            }
        }
        if (oc.buried(w, h)) {
            return new Result(hx, hy, -1, Outcome.RAIL);
        }
        //not worth the trip — stay behind the chrome
        return new Result(hx, hy, -1, Outcome.STAY);
    }

    /**
     * Per-frame side-rail packer: tags that lost their home entirely stack
     * into edge columns on the half of the screen their anchor projects into,
     * below that side's top dock stack. Created once per frame.
     */
    static final class Rails {
        private final int[] y;
        private final int H;
        private final int W;
        private final List<Rect2i> occupied;

        Rails(int W, int H, int topExtentLeft, int topExtentRight, List<Rect2i> occupied) {
            this.W = W;
            this.H = H;
            this.occupied = occupied;
            this.y = new int[]{
                    topExtentLeft > 0 ? topExtentLeft + 6 : RAIL_MARGIN + 16,
                    topExtentRight > 0 ? topExtentRight + 6 : RAIL_MARGIN + 16};
        }

        /** Anchor's screen x decides the rail side; {@link Double#NaN} anchors go right. */
        int side(double anchorX) {
            return Double.isNaN(anchorX) || anchorX >= W * 0.5 ? 1 : 0;
        }

        /**
         * Finds the next free rail slot for a tag on {@code side}, committing
         * its rect to the occupied set. Returns the slot as {@code {x, y}}, or
         * {@code null} when the rail is full (the caller hides the tag).
         */
        @Nullable int[] claim(int side, int w, int h) {
            int x = side == 0 ? RAIL_MARGIN : W - RAIL_MARGIN - w;
            int yy = y[side];
            while (firstOverlap(x, yy, w, h, occupied) != null && yy + h <= H - RAIL_MARGIN) {
                yy += RAIL_GAP;
            }
            if (yy + h > H - RAIL_MARGIN) return null;
            occupied.add(new Rect2i(x, yy, w, h));
            y[side] = yy + h + RAIL_GAP;
            return new int[]{x, yy};
        }
    }

    static @Nullable Rect2i firstOverlap(int x, int y, int w, int h, List<Rect2i> rects) {
        for (Rect2i o : rects) {
            if (x < o.getX() + o.getWidth() && x + w > o.getX()
                    && y < o.getY() + o.getHeight() && y + h > o.getY()) {
                return o;
            }
        }
        return null;
    }

}
