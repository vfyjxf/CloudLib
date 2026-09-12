package dev.vfyjxf.cloudlib.api.ui.inworld;

import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * How a panel participates in layout — the single place for all
 * space-budget, zoning and occlusion declarations on a spec.
 * <p>
 * Every presentation driver reads the same hint object, so custom
 * presentations inherit the shared layout vocabulary instead of inventing
 * their own knobs.
 */
public final class LayoutHint {

    /**
     * Zoning: panels that cannot keep their preferred position merge into a
     * side rail keyed by {@code zone}; members of one zone stay adjacent.
     * {@code null} = derive from the panel key's parent path.
     */
    @Nullable String zone;
    /** Merge cap for the zone: at most this many members show; extras collapse into a "+N" badge. */
    int zoneLimit = Integer.MAX_VALUE;
    /**
     * Off-screen collapse: while the supplier allows, a panel whose anchor
     * leaves the camera view shrinks to a small edge indicator (diamond +
     * bearing tick + distance) instead of rendering the full panel.
     */
    @Nullable BooleanSupplier offscreenIndicator;
    /**
     * Whether the panel may fold to a chrome strip under space pressure.
     * The fold → hide → "+N" chain only applies to foldable panels.
     */
    boolean foldable = true;
    /**
     * How much of the panel's area (0..1) may be covered by other UI chrome
     * before the solver must act — "a little overlap is acceptable". 0 =
     * strict avoidance, 1 = never displaced by chrome.
     */
    double occlusionTolerance = 0.1;
    /** What may cover this panel at all — see {@link OcclusionClass}. */
    OcclusionClass occlusion = OcclusionClass.OCCLUDED_BY_WORLD;

    public static LayoutHint defaults() {
        return new LayoutHint();
    }

    public @Nullable String zone() {
        return zone;
    }

    public int zoneLimit() {
        return zoneLimit;
    }

    public boolean collapsesOffscreen() {
        return offscreenIndicator != null && offscreenIndicator.getAsBoolean();
    }

    public boolean foldable() {
        return foldable;
    }

    public double occlusionTolerance() {
        return occlusionTolerance;
    }

    public OcclusionClass occlusion() {
        return occlusion;
    }

    //region mutation

    /** Overrides the zoning group (see {@link #zone}). */
    public LayoutHint zone(@Nullable String zone) {
        this.zone = zone;
        return this;
    }

    /** Caps how many zone members may show at once; extras merge into a "+N" badge. */
    public LayoutHint zoneLimit(int zoneLimit) {
        this.zoneLimit = Math.max(1, zoneLimit);
        return this;
    }

    /** Allow this panel to collapse to an edge indicator whenever its anchor is off-screen. */
    public LayoutHint offscreenIndicator() {
        return offscreenIndicator(() -> true);
    }

    /** Caller-controlled off-screen collapse. */
    public LayoutHint offscreenIndicator(BooleanSupplier allowed) {
        this.offscreenIndicator = allowed;
        return this;
    }

    public LayoutHint foldable(boolean foldable) {
        this.foldable = foldable;
        return this;
    }

    /** Fraction (0..1) of the panel that may be covered by UI chrome before the solver must act. */
    public LayoutHint occlusionTolerance(double fraction) {
        this.occlusionTolerance = Math.max(0, Math.min(1, fraction));
        return this;
    }

    public LayoutHint occlusion(OcclusionClass occlusion) {
        this.occlusion = occlusion;
        return this;
    }

    //endregion
}
