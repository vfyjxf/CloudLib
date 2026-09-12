package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
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
 *   <li>{@link Presentation.DockCorner#auto} resolves to the quadrant the
 *       anchor projects into, with a deadband around the screen centre lines:
 *       an anchor has to push {@value #cornerDeadband}px past centre before the
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

    /** Centre-line deadband for auto corner flips (gui px). */
    static final int cornerDeadband = 72;

    static final int marginX = 8;
    static final int marginY = 8;
    static final int gap = 6;

    private DockLayout() {}

    /** One docked panel: inputs on construction, outputs written by {@link #solve}. */
    static final class Item {
        /** Requested corner (may be auto) — resolved corner written back to {@link #resolved}. */
        final Presentation.DockCorner corner;

        final int w;
        final int h;
        /** Folded height (chrome strip only) used when the column runs out of room. */
        final int foldH;
        /** Anchor's projected screen position, {@link Double#NaN} when off-screen. */
        final double anchorX;

        final double anchorY;
        /** The corner this panel sat in last frame — auto hysteresis input. */
        final @Nullable Presentation.DockCorner prevCorner;

        // outputs
        Presentation.DockCorner resolved;
        int x;
        int y;
        boolean folded;
        boolean hidden;

        Item(
                Presentation.DockCorner corner,
                int w,
                int h,
                int foldH,
                double anchorX,
                double anchorY,
                @Nullable Presentation.DockCorner prevCorner) {
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
        final int[] overflow = new int[Presentation.DockCorner.values().length];
        /** bottom edge of the topLeft/topRight stacks — rail starts hang below them */
        final int[] topExtent = new int[2];
        /** per-corner final stack extent — where the overflow chip hangs */
        final int[] cursorEnd = new int[Presentation.DockCorner.values().length];
    }

    static Result solve(List<Item> items, int W, int H) {
        Result out = new Result();
        int[] cursors = new int[Presentation.DockCorner.values().length];
        int[] sideUsed = new int[2];
        int budget = H - marginY * 2;

        for (Item item : items) {
            Presentation.DockCorner corner = item.corner;
            if (corner == Presentation.DockCorner.auto) {
                corner = autoCorner(item.anchorX, item.anchorY, W, H, item.prevCorner);
            }
            item.resolved = corner;
            int side = isLeft(corner) ? 0 : 1;
            boolean top = isTop(corner);
            int h = item.h;

            item.folded = false;
            if (sideUsed[side] + h + gap > budget) {
                // the home column is full — an empty column on the other side
                // beats a folded strip: cross over at full size first, fold at
                // home next, fold across after that, overflow only when even a
                // folded strip fits nowhere
                int other = side ^ 1;
                if (sideUsed[other] + h + gap <= budget) {
                    corner = flipSide(corner);
                    item.resolved = corner;
                    side = other;
                } else if (sideUsed[side] + item.foldH + gap <= budget) {
                    item.folded = true;
                    h = item.foldH;
                } else if (sideUsed[other] + item.foldH + gap <= budget) {
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
                case topLeft, bottomLeft -> marginX;
                default -> W - marginX - item.w;
            };
            // a panel wider than the screen still pins its left edge inside
            // the viewport rather than leaking off the side
            item.x = Math.max(2, Math.min(item.x, W - item.w - 2));
            item.y = top ? marginY + slot : H - marginY - h - slot;
            cursors[corner.ordinal()] = slot + h + gap;
            sideUsed[side] += h + gap;
            if (corner == Presentation.DockCorner.topLeft) {
                out.topExtent[0] = Math.max(out.topExtent[0], item.y + h);
            } else if (corner == Presentation.DockCorner.topRight) {
                out.topExtent[1] = Math.max(out.topExtent[1], item.y + h);
            }
        }
        System.arraycopy(cursors, 0, out.cursorEnd, 0, cursors.length);
        return out;
    }

    /**
     * auto-corner pick with hysteresis: the anchor has to push a deadband past
     * the screen's centre lines before the panel switches sides, so crossing
     * the centre doesn't slam the panel to the opposite corner.
     */
    static Presentation.DockCorner autoCorner(
            double anchorX, double anchorY, int W, int H, @Nullable Presentation.DockCorner prev) {
        boolean left, top;
        boolean known = !Double.isNaN(anchorX) && !Double.isNaN(anchorY);
        if (!known || prev == null) {
            left = !known || anchorX < W * 0.5;
            top = !known || anchorY < H * 0.5;
        } else {
            int db = cornerDeadband;
            left = anchorX < W * 0.5 + (isLeft(prev) ? db : -db);
            top = anchorY < H * 0.5 + (isTop(prev) ? db : -db);
        }
        return top
                ? (left ? Presentation.DockCorner.topLeft : Presentation.DockCorner.topRight)
                : (left ? Presentation.DockCorner.bottomLeft : Presentation.DockCorner.bottomRight);
    }

    /** Mirror to the same top/bottom slot on the opposite screen side. */
    static Presentation.DockCorner flipSide(Presentation.DockCorner c) {
        return switch (c) {
            case topLeft -> Presentation.DockCorner.topRight;
            case topRight -> Presentation.DockCorner.topLeft;
            case bottomLeft -> Presentation.DockCorner.bottomRight;
            case bottomRight -> Presentation.DockCorner.bottomLeft;
            case auto -> Presentation.DockCorner.auto; // unreachable — resolved before packing
        };
    }

    static boolean isLeft(Presentation.DockCorner c) {
        return c == Presentation.DockCorner.topLeft || c == Presentation.DockCorner.bottomLeft;
    }

    static boolean isTop(Presentation.DockCorner c) {
        return c == Presentation.DockCorner.topLeft || c == Presentation.DockCorner.topRight;
    }
}
