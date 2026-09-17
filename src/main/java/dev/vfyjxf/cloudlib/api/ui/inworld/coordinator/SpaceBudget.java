package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.OccupancyBitmap;

import java.util.Objects;

/**
 * The quantity feedback the coordinator hands elements at collect time
 * (§3.4): how much work area exists and how much of it is still free, so
 * elements can degrade <em>proactively</em> — container-query style — instead
 * of waiting to be rejected. The free figures are derived from the occupancy
 * bitmap (committed elements plus exclusion areas) and are smoothed across
 * frames by the coordinator, so a single frame's occupancy jitter never makes
 * an element flap tiers.
 *
 * @param workArea the rectangle elements may occupy, gui pixels
 * @param freeArea free screen area within the work area, px²
 * @param freeFraction {@code freeArea / workAreaArea}, in {@code [0, 1]}
 */
public record SpaceBudget(Rect workArea, double freeArea, double freeFraction) {

    public SpaceBudget {
        Objects.requireNonNull(workArea, "workArea");
        double area = (double) workArea.width() * workArea.height();
        if (!Double.isFinite(freeArea) || freeArea < 0 || freeArea > area) {
            throw new IllegalArgumentException("freeArea must be in [0, workAreaArea=" + area + "]: " + freeArea);
        }
        if (!Double.isFinite(freeFraction) || freeFraction < 0 || freeFraction > 1) {
            throw new IllegalArgumentException("freeFraction must be in [0, 1]: " + freeFraction);
        }
    }

    /**
     * Derives the budget from the work area and an occupancy bitmap covering
     * the screen: the free fraction is the unoccupied share of the bitmap's
     * cells under the work area.
     */
    public static SpaceBudget of(Rect workArea, OccupancyBitmap bitmap) {
        Objects.requireNonNull(bitmap, "bitmap");
        double area = (double) workArea.width() * workArea.height();
        double freeFraction = 1.0 - bitmap.occupiedFraction(workArea);
        return new SpaceBudget(workArea, area * freeFraction, freeFraction);
    }

    /** The work area's area in px². */
    public double workAreaArea() {
        return (double) workArea.width() * workArea.height();
    }

    /**
     * Slot headroom: how many elements of {@code elementArea} px² still fit
     * in the free area — 0 means "do not even try, degrade".
     *
     * @throws IllegalArgumentException if elementArea is not finite and positive
     */
    public int capacityFor(double elementArea) {
        if (!Double.isFinite(elementArea) || elementArea <= 0) {
            throw new IllegalArgumentException("elementArea must be finite and positive: " + elementArea);
        }
        long capacity = (long) Math.floor(freeArea / elementArea);
        return (int) Math.min(capacity, Integer.MAX_VALUE);
    }
}
