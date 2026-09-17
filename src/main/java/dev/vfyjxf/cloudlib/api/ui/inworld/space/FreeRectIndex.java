package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A MaxRects-style index of free screen rectangles for in-screen block
 * allocation. Starts as one rectangle covering the whole area; every
 * {@link #occupy} splits each free rectangle the used one intersects into the
 * up-to-four leftover strips and drops rectangles contained in another, and
 * {@link #insert} picks the free rectangle with the best-short-side-fit and
 * occupies its top-left corner.
 * <p>
 * Designed for low-frequency rebuilds (an epoch), never per-frame recomputation
 * — per-frame MaxRects is the classic cause of layout jitter. Iteration order
 * is the deterministic insertion order and ties in the insert heuristic are
 * broken by that order, so equal inputs always produce equal snapshots.
 */
public final class FreeRectIndex {

    private final int width;
    private final int height;
    private final List<Rect> freeRects = new ArrayList<>();

    /**
     * @throws IllegalArgumentException if dimensions are not positive
     */
    public FreeRectIndex(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("index size must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        reset();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Restores the index to a single free rectangle covering everything. */
    public void reset() {
        freeRects.clear();
        freeRects.add(new Rect(0, 0, width, height));
    }

    /** An immutable snapshot of the free rectangles, in deterministic order. */
    public List<Rect> freeRects() {
        return List.copyOf(freeRects);
    }

    /** Whether a block of the given size fits in any free rectangle. */
    public boolean canFit(int blockWidth, int blockHeight) {
        for (Rect free : freeRects) {
            if (free.width() >= blockWidth && free.height() >= blockHeight) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places a {@code blockWidth × blockHeight} block at the top-left corner
     * of the best-fitting free rectangle (best short side fit; ties broken by
     * free-rect list order) and marks it used.
     *
     * @return the placement, or {@code null} if no free rectangle fits
     * @throws IllegalArgumentException if the block size is not positive
     */
    public @Nullable Rect insert(int blockWidth, int blockHeight) {
        if (blockWidth <= 0 || blockHeight <= 0) {
            throw new IllegalArgumentException("block size must be positive: " + blockWidth + "x" + blockHeight);
        }
        Rect best = null;
        long bestScore = Long.MAX_VALUE;
        for (Rect free : freeRects) {
            if (free.width() < blockWidth || free.height() < blockHeight) {
                continue;
            }
            int leftoverHorizontal = free.width() - blockWidth;
            int leftoverVertical = free.height() - blockHeight;
            int shortSide = Math.min(leftoverHorizontal, leftoverVertical);
            int longSide = Math.max(leftoverHorizontal, leftoverVertical);
            long score = ((long) shortSide << 32) | longSide;
            if (score < bestScore) {
                bestScore = score;
                best = free;
            }
        }
        if (best == null) {
            return null;
        }
        Rect placement = new Rect(best.x(), best.y(), blockWidth, blockHeight);
        occupy(placement);
        return placement;
    }

    /**
     * Marks {@code used} as no longer free, splitting every free rectangle it
     * intersects. The rectangle is clipped to the index bounds first;
     * out-of-bounds or empty rectangles are ignored.
     */
    public void occupy(Rect used) {
        Rect clipped = new Rect(0, 0, width, height).intersection(used);
        if (clipped.width() <= 0 || clipped.height() <= 0) {
            return;
        }
        List<Rect> next = new ArrayList<>();
        for (Rect free : freeRects) {
            if (!free.intersects(clipped)) {
                next.add(free);
                continue;
            }
            splitAround(free, clipped, next);
        }
        freeRects.clear();
        appendPruned(next);
    }

    /** Splits {@code free} into the leftover strips around {@code used}. */
    private static void splitAround(Rect free, Rect used, List<Rect> out) {
        if (used.x() > free.x()) {
            out.add(new Rect(free.x(), free.y(), used.x() - free.x(), free.height()));
        }
        if (used.right() < free.right()) {
            out.add(new Rect(used.right(), free.y(), free.right() - used.right(), free.height()));
        }
        if (used.y() > free.y()) {
            out.add(new Rect(free.x(), free.y(), free.width(), used.y() - free.y()));
        }
        if (used.bottom() < free.bottom()) {
            out.add(new Rect(free.x(), used.bottom(), free.width(), free.bottom() - used.bottom()));
        }
    }

    /**
     * Appends {@code candidates}, skipping empty rectangles and ones contained
     * in another candidate (equal rectangles keep the earlier copy).
     */
    private void appendPruned(List<Rect> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            Rect candidate = candidates.get(i);
            if (candidate.width() <= 0 || candidate.height() <= 0) {
                continue;
            }
            boolean contained = false;
            for (int j = 0; j < candidates.size() && !contained; j++) {
                if (i == j) {
                    continue;
                }
                Rect other = candidates.get(j);
                boolean strictlyContained = other.contains(candidate) && !candidate.contains(other);
                boolean duplicate = j < i && candidate.equals(other);
                if (strictlyContained || duplicate) {
                    contained = true;
                }
            }
            if (!contained) {
                freeRects.add(candidate);
            }
        }
    }
}
