package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Coarse boolean occupancy grid over the screen, built from an 8-pixel cell
 * size by default. Rectangles are {@link #mark marked} into the bitmap (a cell
 * counts as occupied when a marked rectangle so much as touches it, so free
 * queries are conservative — they never report space a marked rectangle
 * overlaps) and queried with {@link #isFree}/{@link #isOccupied}. Queries cost
 * O(covered cells) and merging more rectangles into the bitmap is free, which
 * is why the coordinator prefers this over per-frame N² rectangle
 * intersection.
 * <p>
 * Coordinates are gui-scaled screen pixels; rectangles are clipped to the
 * bitmap bounds, so partially off-screen rectangles only occupy the visible
 * part.
 */
public final class OccupancyBitmap {

    /** The default grid resolution in screen pixels per cell. */
    public static final int defaultCellSize = 8;

    private final int cellSize;
    private final int cellsWide;
    private final int cellsHigh;
    private final long[] words;

    /**
     * Creates a bitmap covering {@code width × height} screen pixels with the
     * default cell size.
     */
    public OccupancyBitmap(int width, int height) {
        this(width, height, defaultCellSize);
    }

    /**
     * Creates a bitmap covering {@code width × height} screen pixels, one cell
     * per {@code cellSize} pixels.
     *
     * @throws IllegalArgumentException if dimensions or cell size are not positive
     */
    public OccupancyBitmap(int width, int height, int cellSize) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("bitmap size must be positive: " + width + "x" + height);
        }
        if (cellSize <= 0) {
            throw new IllegalArgumentException("cell size must be positive: " + cellSize);
        }
        this.cellSize = cellSize;
        this.cellsWide = divideRoundingUp(width, cellSize);
        this.cellsHigh = divideRoundingUp(height, cellSize);
        this.words = new long[divideRoundingUp(cellsWide * cellsHigh, Long.SIZE)];
    }

    private static int divideRoundingUp(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    public int cellSize() {
        return cellSize;
    }

    /** The number of cells along the x axis. */
    public int cellsWide() {
        return cellsWide;
    }

    /** The number of cells along the y axis. */
    public int cellsHigh() {
        return cellsHigh;
    }

    /** The screen-space rectangle this bitmap covers. */
    public Rect bounds() {
        return new Rect(0, 0, cellsWide * cellSize, cellsHigh * cellSize);
    }

    /** Marks every cell the rectangle touches as occupied. */
    public void mark(Rect rect) {
        forEachCoveredCell(rect, cell -> setCell(cell, true));
    }

    /** Marks every cell each rectangle touches as occupied. */
    public void markAll(Iterable<Rect> rects) {
        for (Rect rect : rects) {
            mark(rect);
        }
    }

    /** Clears every cell the rectangle touches. */
    public void clear(Rect rect) {
        forEachCoveredCell(rect, cell -> setCell(cell, false));
    }

    /** Clears the whole bitmap. */
    public void clearAll() {
        Arrays.fill(words, 0L);
    }

    /** Whether the screen point falls inside an occupied cell. */
    public boolean isOccupied(Pos point) {
        int cx = point.x() / cellSize;
        int cy = point.y() / cellSize;
        if (cx < 0 || cy < 0 || cx >= cellsWide || cy >= cellsHigh) {
            return false;
        }
        return cell(cx, cy);
    }

    /** Whether at least one cell covered by the rectangle is occupied. */
    public boolean isOccupied(Rect rect) {
        return !isFree(rect);
    }

    /** Whether no cell covered by the rectangle is occupied (conservative). */
    public boolean isFree(Rect rect) {
        CellRange range = coveredRange(rect);
        if (range == null) {
            return true;
        }
        for (int cy = range.y0(); cy <= range.y1(); cy++) {
            for (int cx = range.x0(); cx <= range.x1(); cx++) {
                if (cell(cx, cy)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The fraction of covered cells that are occupied, in {@code [0, 1]}. */
    public double occupiedFraction(Rect rect) {
        CellRange range = coveredRange(rect);
        if (range == null) {
            return 0.0;
        }
        int covered = 0;
        int occupied = 0;
        for (int cy = range.y0(); cy <= range.y1(); cy++) {
            for (int cx = range.x0(); cx <= range.x1(); cx++) {
                covered++;
                if (cell(cx, cy)) {
                    occupied++;
                }
            }
        }
        return (double) occupied / covered;
    }

    /** The total number of occupied cells. */
    public long occupiedCellCount() {
        long count = 0;
        for (long word : words) {
            count += Long.bitCount(word);
        }
        return count;
    }

    /** An immutable snapshot of the occupied cells as screen-space rectangles. */
    public List<Rect> occupiedCellRects() {
        List<Rect> cells = new ArrayList<>();
        for (int cy = 0; cy < cellsHigh; cy++) {
            for (int cx = 0; cx < cellsWide; cx++) {
                if (cell(cx, cy)) {
                    cells.add(new Rect(cx * cellSize, cy * cellSize, cellSize, cellSize));
                }
            }
        }
        return List.copyOf(cells);
    }

    private record Cell(int x, int y) {}

    private void forEachCoveredCell(Rect rect, CellConsumer consumer) {
        CellRange range = coveredRange(rect);
        if (range == null) {
            return;
        }
        for (int cy = range.y0(); cy <= range.y1(); cy++) {
            for (int cx = range.x0(); cx <= range.x1(); cx++) {
                consumer.accept(new Cell(cx, cy));
            }
        }
    }

    private interface CellConsumer {
        void accept(Cell cell);
    }

    private void setCell(Cell cell, boolean value) {
        int index = cell.y() * cellsWide + cell.x();
        if (value) {
            words[index >>> 6] |= 1L << (index & (Long.SIZE - 1));
        } else {
            words[index >>> 6] &= ~(1L << (index & (Long.SIZE - 1)));
        }
    }

    private boolean cell(int cx, int cy) {
        int index = cy * cellsWide + cx;
        return (words[index >>> 6] & (1L << (index & (Long.SIZE - 1)))) != 0;
    }

    private record CellRange(int x0, int y0, int x1, int y1) {}

    /**
     * The inclusive cell index range a rectangle covers, clipped to the
     * bitmap; {@code null} when the rectangle is empty or entirely outside
     * the bounds. A rectangle ending exactly on a cell boundary does not
     * touch the cell beyond it.
     */
    private CellRange coveredRange(Rect rect) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return null;
        }
        int x0 = Math.max(0, rect.x() / cellSize);
        int y0 = Math.max(0, rect.y() / cellSize);
        int x1 = divideRoundingUp(rect.right(), cellSize) - 1;
        int y1 = divideRoundingUp(rect.bottom(), cellSize) - 1;
        x1 = Math.min(x1, cellsWide - 1);
        y1 = Math.min(y1, cellsHigh - 1);
        if (x0 > x1 || y0 > y1) {
            return null;
        }
        return new CellRange(x0, y0, x1, y1);
    }
}
