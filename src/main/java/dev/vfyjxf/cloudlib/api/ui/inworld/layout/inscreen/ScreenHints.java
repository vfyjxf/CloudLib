package dev.vfyjxf.cloudlib.api.ui.inworld.layout.inscreen;

import org.jetbrains.annotations.Nullable;

/**
 * The part of inscreen placement the API side controls: preferences handed to
 * a {@link ScreenLayoutCoordinator}, which keeps full placement authority —
 * hints steer, they never decide.
 * <p>
 * All fields carry their default semantics: no corner preference, no zoning
 * group, no collapse, indicators on.
 */
public final class ScreenHints {

    /** The screen corner a panel prefers to gather into. */
    public enum Corner {
        topLeft,
        topRight,
        bottomLeft,
        bottomRight,
        /** Let the coordinator pick — typically the quadrant nearest the source's projection. */
        auto
    }

    private Corner corner = Corner.auto;
    private @Nullable String group;
    private int groupLimit = Integer.MAX_VALUE;
    private boolean collapseOffscreen;
    private boolean indicators = true;
    private boolean interactive = true;

    private ScreenHints() {}

    /** A fresh hints instance with all defaults. */
    public static ScreenHints create() {
        return new ScreenHints();
    }

    /** The preferred gathering corner. */
    public Corner corner() {
        return corner;
    }

    public ScreenHints corner(Corner corner) {
        this.corner = corner;
        return this;
    }

    /**
     * The zoning group this panel belongs to: panels sharing a group key are
     * kept adjacent when the coordinator gathers displaced panels. Null means
     * no group.
     */
    public @Nullable String group() {
        return group;
    }

    public ScreenHints group(@Nullable String group) {
        this.group = group;
        return this;
    }

    /**
     * Merge cap for the zoning group: at most this many members are shown at
     * once; the coordinator decides how the rest are represented (e.g. merged
     * into a count badge). Unlimited by default.
     */
    public int groupLimit() {
        return groupLimit;
    }

    public ScreenHints groupLimit(int groupLimit) {
        this.groupLimit = Math.max(1, groupLimit);
        return this;
    }

    /**
     * Whether the panel may collapse to a compact off-screen representation
     * when its source leaves the camera view.
     */
    public boolean collapseOffscreen() {
        return collapseOffscreen;
    }

    public ScreenHints collapseOffscreen(boolean collapseOffscreen) {
        this.collapseOffscreen = collapseOffscreen;
        return this;
    }

    /** Whether the coordinator may draw placement indicators (edge marks, badges) for this panel. */
    public boolean indicators() {
        return indicators;
    }

    public ScreenHints indicators(boolean indicators) {
        this.indicators = indicators;
        return this;
    }

    /**
     * Whether the panel participates in interaction — non-interactive panels
     * (passive tags) are the ones a coordinator typically displaces and zones.
     */
    public boolean interactive() {
        return interactive;
    }

    public ScreenHints interactive(boolean interactive) {
        this.interactive = interactive;
        return this;
    }
}
