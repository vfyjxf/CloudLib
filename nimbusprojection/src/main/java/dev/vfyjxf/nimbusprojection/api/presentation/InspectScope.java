package dev.vfyjxf.nimbusprojection.api.presentation;

/**
 * Which panels flatten into the inspect layer while the inspect key is
 * held — the {@code inspectScope} config value.
 */
public enum InspectScope {

    /**
     * The focused panel (plus its group members) and every user-pinned
     * panel — the default: the flat layer shows what you were already
     * engaged with, not every panel in the world.
     */
    focusAndPinned,

    /** Every live panel in range — the "command canvas". */
    all,

    /** Only the focused panel/group. */
    focused,
}
