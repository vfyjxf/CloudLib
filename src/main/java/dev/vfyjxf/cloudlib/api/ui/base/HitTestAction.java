package dev.vfyjxf.cloudlib.api.ui.base;

/**
 * Defines how a layer participates in hit testing.
 */
public enum HitTestAction {
    /**
     * Layer participates in hit testing normally.
     * <p>
     * Widgets can receive mouse events, clicks, etc.
     */
    enabled,

    /**
     * Layer does not participate in hit testing.
     * <p>
     * Widgets render but never receive input events.
     * Events pass through to lower layers.
     */
    none,

    /**
     * Layer participates in hit testing and blocks lower layers when hit.
     * <p>
     * When a widget in this layer is hit, lower layers are not tested.
     * Use for modal dialogs.
     */
    blocking
}
