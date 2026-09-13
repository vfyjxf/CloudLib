package dev.vfyjxf.nimbusprojection.api.panel;

/**
 * When a panel's resting form (card / nameplate / strip) is allowed to
 * present. The offer scan decides candidacy; the trigger decides
 * visibility — different content classes want different defaults, which is
 * why it lives on the spec instead of the provider.
 * <p>
 * Engaged or user-pinned panels always bypass the trigger — pinning is an
 * explicit "keep this up" gesture. The inspect layer (R) also bypasses it:
 * the flat projection is meant to show everything that exists.
 */
public enum PresentTrigger {

    /**
     * Present while the anchor sits inside the soft-focus cone (~25° off the
     * crosshair). Default for machines and containers — things you operate
     * by looking at them.
     */
    CONE,

    /**
     * Present whenever the anchor projects on screen within reach. For
     * resident nameplates: living entities, players, ambient markers —
     * things that should be visible on sight, not on aim.
     */
    VISIBLE,

    /**
     * Present only while the crosshair ray hits the anchor block itself —
     * {@code mc.hitResult} resolves to the anchored position. For fine or
     * dense targets (signs, item frames) where a cone would wallpaper every
     * nearby block with cards.
     */
    POINTED,
}
