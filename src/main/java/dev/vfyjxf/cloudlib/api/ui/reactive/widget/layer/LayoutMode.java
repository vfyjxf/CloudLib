package dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer;

/**
 * Defines how a widget participates in its parent's layout calculation.
 */
public enum LayoutMode {
    /**
     * Normal layout flow - widget takes up space and affects sibling positioning.
     */
    NORMAL,
    
    /**
     * Floating - widget is positioned within parent but doesn't affect sibling layout.
     * Other widgets are laid out as if this widget doesn't exist.
     */
    FLOATING,
    
    /**
     * Absolute positioning relative to parent.
     * Widget uses explicit x/y coordinates and doesn't affect layout.
     */
    ABSOLUTE,
    
    /**
     * Overlay - widget is rendered in a separate layer above normal content.
     * Completely separate from parent's layout calculation.
     */
    OVERLAY
}
