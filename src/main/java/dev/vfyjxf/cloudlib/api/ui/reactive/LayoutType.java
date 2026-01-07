package dev.vfyjxf.cloudlib.api.ui.reactive;

/**
 * Layout types for group nodes.
 */
public enum LayoutType {
    /**
     * Vertical layout - children stacked top to bottom.
     */
    COLUMN,

    /**
     * Horizontal layout - children laid out left to right.
     */
    ROW,

    /**
     * Stack layout - children layered on top of each other.
     */
    STACK,

    /**
     * Flex layout with wrapping.
     */
    WRAP,

    /**
     * Absolute positioning - children positioned relative to parent.
     */
    ABSOLUTE,

    /**
     * Grid layout.
     */
    GRID
}
