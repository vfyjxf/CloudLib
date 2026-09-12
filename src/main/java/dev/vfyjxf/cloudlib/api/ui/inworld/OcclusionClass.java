package dev.vfyjxf.cloudlib.api.ui.inworld;

/**
 * What may visually cover a panel — the panel's occlusion priority.
 */
public enum OcclusionClass {

    /**
     * World geometry may occlude the panel (default): a face panel behind a
     * block is simply not visible. UI chrome still follows the avoidance /
     * folding rules.
     */
    OCCLUDED_BY_WORLD,

    /**
     * World geometry may not occlude the panel — it renders through terrain —
     * but other UI chrome may still cover it (subject to
     * {@link LayoutHint#occlusionTolerance}).
     */
    OCCLUDED_BY_UI_ONLY,

    /**
     * Nothing covers the panel: it renders on top of world geometry and all
     * other UI chrome. Reserved for markers that must stay visible.
     */
    ALWAYS_ON_TOP,

}
