package dev.vfyjxf.cloudlib.ui.debug;

/**
 * Placement mode for the DevTools panel.
 */
enum DockMode {

    FLOAT,
    DOCK_RIGHT,
    DOCK_LEFT,
    DOCK_BOTTOM,
    DOCK_TOP;

    boolean isDocked() {
        return this != FLOAT;
    }

    boolean isHorizontal() {
        return this == DOCK_LEFT || this == DOCK_RIGHT;
    }

    boolean isVertical() {
        return this == DOCK_TOP || this == DOCK_BOTTOM;
    }

}
