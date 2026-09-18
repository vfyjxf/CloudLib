package dev.vfyjxf.cloudlib.internal.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.inworld.render.WorldUiPanel;

import java.util.function.LongSupplier;

/**
 * The dirty-check cache behind a world panel's surface repaint: a panel
 * repaints only when its content version, logical size or granted supersample
 * factor changed since the last paint. While nothing changed, the previous
 * frame's texture is still complete — the world quad keeps sampling it, and
 * the whole {@code UiSurface.render} call (painter run, FBO bind/restore,
 * mip regeneration) is skipped.
 * <p>
 * Semantics of {@link #render}:
 * <ul>
 *   <li>a panel without a {@link WorldUiPanel#contentVersion(LongSupplier)
 *       content version} is always dirty — every frame repaints, exactly the
 *       pre-cache behavior (full backward compatibility);</li>
 *   <li>a versioned panel repaints on first sight, on every version change,
 *       and once after any size or supersample change (the texture storage is
 *       new, so the old texels are gone);</li>
 *   <li>a frame rendered unconditionally (the panel was version-less this
 *       frame, e.g. pointer-interactive) clears the cached version, so the
 *       next versioned frame repaints once rather than trusting a stale
 *       comparison.</li>
 * </ul>
 * One gate per panel, keyed by the renderer's identity map alongside the
 * supersample controllers; dropped with the panel (see {@code WorldUiRenderer}
 * {@code removePanel}/{@code clearPanels}).
 * <p>
 * Pure state (no GL) — the frame decision is unit-testable headless with the
 * render action swapped for a counter, mirroring {@code MipmapChain}.
 */
public final class RepaintGate {

    private boolean painted;
    private boolean versioned;
    private long version;
    private int width = -1;
    private int height = -1;
    private int supersample = -1;

    /**
     * Runs {@code action} (the surface repaint) only when the panel is dirty
     * this frame; returns whether it ran.
     *
     * @param granted the supersample factor granted to the panel this frame
     *     (after both hysteresis stages — a change here means a new texture)
     */
    public boolean render(WorldUiPanel panel, int granted, Runnable action) {
        LongSupplier contentVersion = panel.contentVersion();
        if (contentVersion == null || !painted || !versioned || !clean(panel, contentVersion, granted)) {
            action.run();
            painted = true;
            versioned = contentVersion != null;
            if (versioned) version = contentVersion.getAsLong();
            width = panel.width();
            height = panel.height();
            supersample = granted;
            return true;
        }
        return false;
    }

    private boolean clean(WorldUiPanel panel, LongSupplier contentVersion, int granted) {
        return version == contentVersion.getAsLong()
                && width == panel.width()
                && height == panel.height()
                && supersample == granted;
    }
}
