package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

/**
 * Cursor types for interactive elements.
 * <p>
 * These represent the visual cursor that should be displayed when
 * hovering over a UI element.
 */
@ApiStatus.Experimental
public enum Cursor {
    /**
     * Default cursor (usually an arrow).
     */
    DEFAULT,

    /**
     * Pointer cursor (usually a hand pointing).
     */
    POINTER,

    /**
     * Hand cursor for clickable elements.
     */
    HAND,

    /**
     * Text cursor (I-beam) for text input.
     */
    TEXT,

    /**
     * Move cursor for draggable elements.
     */
    MOVE,

    /**
     * Not allowed cursor for disabled elements.
     */
    NOT_ALLOWED,

    /**
     * Wait cursor for loading states.
     */
    WAIT,

    /**
     * Crosshair cursor for precise selection.
     */
    CROSSHAIR,

    /**
     * North-South resize cursor.
     */
    RESIZE_NS,

    /**
     * East-West resize cursor.
     */
    RESIZE_EW,

    /**
     * Northeast-Southwest resize cursor.
     */
    RESIZE_NESW,

    /**
     * Northwest-Southeast resize cursor.
     */
    RESIZE_NWSE
}
