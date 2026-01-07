package dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer;

/**
 * Standard rendering layer priorities.
 * <p>
 * Layers are rendered in order from lowest to highest priority.
 * Higher priority layers appear on top and receive input events first.
 */
public final class LayerPriority {
    
    /**
     * Background layer - decorative elements behind main content.
     */
    public static final int BACKGROUND = -100;
    
    /**
     * Base layer - normal UI content.
     */
    public static final int BASE = 0;
    
    /**
     * Floating content within the same logical container.
     */
    public static final int FLOATING = 50;
    
    /**
     * Dropdown menus and combo boxes.
     */
    public static final int DROPDOWN = 100;
    
    /**
     * Popup windows and dialogs.
     */
    public static final int POPUP = 200;
    
    /**
     * Tooltips.
     */
    public static final int TOOLTIP = 300;
    
    /**
     * Modal dialogs that block interaction with lower layers.
     */
    public static final int MODAL = 400;
    
    /**
     * Debug overlays and developer tools.
     */
    public static final int DEBUG = 900;
    
    /**
     * System-level overlays (notifications, etc).
     */
    public static final int SYSTEM = 1000;
    
    private LayerPriority() {}
}
