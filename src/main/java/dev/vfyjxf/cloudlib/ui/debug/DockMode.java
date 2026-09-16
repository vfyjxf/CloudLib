package dev.vfyjxf.cloudlib.ui.debug;

/**
 * Placement mode for the DevTools panel.
 */
enum DockMode {
    floating,
    dockRight,
    dockLeft,
    dockBottom,
    dockTop;

    boolean isDocked() {
        return this != floating;
    }

    boolean isHorizontal() {
        return this == dockLeft || this == dockRight;
    }

    boolean isVertical() {
        return this == dockTop || this == dockBottom;
    }
}
