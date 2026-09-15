package dev.vfyjxf.cloudlib.api.ui.base;

/**
 * Defines the coordinate space a widget's layout position is expressed in.
 * <p>
 * This controls how {@link Widget#sceneToLocal} and {@link Widget#localToScene}
 * traverse the ancestor chain during coordinate conversion. A widget whose
 * coordinate space is not {@link #parent} acts as a boundary — ancestor
 * transforms above it are skipped.
 *
 * @see Widget#coordinateSpace()
 * @see Widget#setCoordinateSpace(CoordinateSpace)
 */
public enum CoordinateSpace {

    /**
     * Layout position is relative to the parent widget (default).
     * <p>
     * Coordinate conversion walks the full ancestor viewport chain normally.
     */
    parent,

    /**
     * Layout position is absolute in scene space.
     * <p>
     * The widget's viewport position is relative to the scene origin, not its
     * parent. Coordinate conversion treats this widget as the root of its own
     * coordinate subtree — ancestor transforms above it are skipped.
     * <p>
     * Typically used by floating popups, overlays, and other elements that
     * compute their own scene-space position.
     */
    scene
}
