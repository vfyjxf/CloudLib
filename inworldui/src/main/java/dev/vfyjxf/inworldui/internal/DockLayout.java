package dev.vfyjxf.inworldui.internal;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pure screen-edge dock solver: packs panels into the four screen corners.
 * No Minecraft types — every input is a plain number so the whole pass is
 * deterministic and unit-testable.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>One {@link Item} per panel, in stable offer order — output order never
 *       changes, so slots don't churn frame to frame.</li>
 *   <li>{@link InworldPlacement.DockCorner#AUTO} resolves to the quadrant the
 *       anchor projects into, with a deadband around the screen centre lines:
 *       an anchor has to push {@value #CORNER_DEADBAND}px past centre before the
 *       panel flips sides.</li>
 *   <li>Each screen <em>side</em> (left/right) is one shared vertical budget
 *       {@code H − 2·margin} — top and bottom columns grow toward each other
 *       and draw from the same purse.</li>
 *   <li>When a panel no longer fits it folds to {@link Item#foldH}; when even
 *       folded strips overflow it is hidden for the frame and counted into the
 *       corner's {@code +N} overflow chip.</li>
 * </ul>
 */
final class DockLayout {

    /** Centre-line deadband for AUTO corner flips (gui px). */
    static final int CORNER_DEADBAND = 72;
    static final int MARGIN_X = 8;
    static final int MARGIN_Y = 8;
    static final int GAP = 6;

    private DockLayout() {
    }

    /** One docked panel: inputs on construction, outputs written by {@link #solve}. */
    static final class Item {
        /** Requested corner (may be AUTO) — resolved corner written back to {@link #resolved}. */
        final InworldPlacement.DockCorner corner;
        final int w;
        final int h;
        /** Folded height (chrome strip only) used when the column runs out of room. */
        final int foldH;
        /** Anchor's projected screen position, {@link Double#NaN} when off-screen. */
        final double anchorX;
        final double anchorY;
        /** The corner this panel sat in last frame — AUTO hysteresis input. */
        final @Nullable InworldPlacement.DockCorner prevCorner;

        //outputs
        InworldPlacement.DockCorner resolved;
        int x;
        int y;
        boolean folded;
        boolean hidden;

        Item(InworldPlacement.DockCorner corner, int w, int h, int foldH,
             double anchorX, double anchorY, @Nullable InworldPlacement.DockCorner prevCorner) {
            this.corner = corner;
            this.w = w;
            this.h = h;
            this.foldH = foldH;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.prevCorner = prevCorner;
        }
    }

    /** Aggregate results a renderer needs to draw overflow chips. */
    static final class Result {
        /** per-corner count of panels hidden because even folded they didn't fit */
        final int[] overflow = new int[InworldPlacement.DockCorner.values().length];
        /** bottom edge of the TOP_LEFT/TOP_RIGHT stacks — rail starts hang below them */
        final int[] topExtent = new int[2];
        /** per-corner final stack extent — where the overflow chip hangs */
        final int[] cursorEnd = new int[InworldPlacement.DockCorner.values().length];
    }

    static Result solve(List<Item> items, int W, int H) {
        Result out = new Result();
        int[] cursors = new int[InworldPlacement.DockCorner.values().length];
        int[] sideUsed = new int[2];
        int budget = H - MARGIN_Y * 2;

        for (Item item : items) {
            InworldPlacement.DockCorner corner = item.corner;
            if (corner == InworldPlacement.DockCorner.AUTO) {
                corner = autoCorner(item.anchorX, item.anchorY, W, H, item.prevCorner);
            }
            item.resolved = corner;
            int side = isLeft(corner) ? 0 : 1;
            boolean top = isTop(corner);
            int h = item.h;

            item.folded = false;
            if (sideUsed[side] + h + GAP > budget) {
                //the home column is full — an empty column on the other side
                //beats a folded strip: cross over at full size first, fold at
                //home next, fold across after that, overflow only when even a
                //folded strip fits nowhere
                int other = side ^ 1;
                if (sideUsed[other] + h + GAP <= budget) {
                    corner = flipSide(corner);
                    item.resolved = corner;
                    side = other;
                } else if (sideUsed[side] + item.foldH + GAP <= budget) {
                    item.folded = true;
                    h = item.foldH;
                } else if (sideUsed[other] + item.foldH + GAP <= budget) {
                    corner = flipSide(corner);
                    item.resolved = corner;
                    side = other;
                    item.folded = true;
                    h = item.foldH;
                } else {
                    item.hidden = true;
                    out.overflow[corner.ordinal()]++;
                    continue;
                }
            }

            int slot = cursors[corner.ordinal()];
            item.x = switch (corner) {
                case TOP_LEFT, BOTTOM_LEFT -> MARGIN_X;
                default -> W - MARGIN_X - item.w;
            };
            //a panel wider than the screen still pins its left edge inside
            //the viewport rather than leaking off the side
            item.x = Math.max(2, Math.min(item.x, W - item.w - 2));
            item.y = top ? MARGIN_Y + slot : H - MARGIN_Y - h - slot;
            cursors[corner.ordinal()] = slot + h + GAP;
            sideUsed[side] += h + GAP;
            if (corner == InworldPlacement.DockCorner.TOP_LEFT) {
                out.topExtent[0] = Math.max(out.topExtent[0], item.y + h);
            } else if (corner == InworldPlacement.DockCorner.TOP_RIGHT) {
                out.topExtent[1] = Math.max(out.topExtent[1], item.y + h);
            }
        }
        System.arraycopy(cursors, 0, out.cursorEnd, 0, cursors.length);
        return out;
    }

    /**
     * AUTO-corner pick with hysteresis: the anchor has to push a deadband past
     * the screen's centre lines before the panel switches sides, so crossing
     * the centre doesn't slam the panel to the opposite corner.
     */
    static InworldPlacement.DockCorner autoCorner(
            double anchorX, double anchorY, int W, int H,
            @Nullable InworldPlacement.DockCorner prev) {
        boolean left, top;
        boolean known = !Double.isNaN(anchorX) && !Double.isNaN(anchorY);
        if (!known || prev == null) {
            left = !known || anchorX < W * 0.5;
            top = !known || anchorY < H * 0.5;
        } else {
            int db = CORNER_DEADBAND;
            left = anchorX < W * 0.5 + (isLeft(prev) ? db : -db);
            top = anchorY < H * 0.5 + (isTop(prev) ? db : -db);
        }
        return top
                ? (left ? InworldPlacement.DockCorner.TOP_LEFT : InworldPlacement.DockCorner.TOP_RIGHT)
                : (left ? InworldPlacement.DockCorner.BOTTOM_LEFT : InworldPlacement.DockCorner.BOTTOM_RIGHT);
    }

    /** Mirror to the same top/bottom slot on the opposite screen side. */
    static InworldPlacement.DockCorner flipSide(InworldPlacement.DockCorner c) {
        return switch (c) {
            case TOP_LEFT -> InworldPlacement.DockCorner.TOP_RIGHT;
            case TOP_RIGHT -> InworldPlacement.DockCorner.TOP_LEFT;
            case BOTTOM_LEFT -> InworldPlacement.DockCorner.BOTTOM_RIGHT;
            case BOTTOM_RIGHT -> InworldPlacement.DockCorner.BOTTOM_LEFT;
            case AUTO -> InworldPlacement.DockCorner.AUTO; //unreachable — resolved before packing
        };
    }

    static boolean isLeft(InworldPlacement.DockCorner c) {
        return c == InworldPlacement.DockCorner.TOP_LEFT || c == InworldPlacement.DockCorner.BOTTOM_LEFT;
    }

    static boolean isTop(InworldPlacement.DockCorner c) {
        return c == InworldPlacement.DockCorner.TOP_LEFT || c == InworldPlacement.DockCorner.TOP_RIGHT;
    }

}
