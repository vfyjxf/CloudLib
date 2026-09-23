package dev.vfyjxf.cloudlib.api.ui.debug;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.jspecify.annotations.Nullable;

/**
 * DevTools-style debug overlay attached to a
 * {@link dev.vfyjxf.cloudlib.api.ui.base.Scene}.
 * <p>
 * The overlay is <b>not</b> part of the inspected scene: it owns a separate
 * scene for its UI and renders as an isolated phase after the inspected scene
 * has finished rendering. It never participates in the inspected scene's
 * layout, hit testing, event bubbling or ticking.
 * <p>
 * Obtain one via {@link dev.vfyjxf.cloudlib.api.ui.base.Scene#debugOverlay()};
 * returns {@code null} when debug mode is disabled.
 */
public interface DebugOverlay {

    /**
     * @return whether the overlay is currently visible and processing input.
     */
    boolean isOpen();

    /**
     * Opens the overlay, creating the debug UI on first use.
     */
    void open();

    /**
     * Closes the overlay. The debug UI is kept alive for a fast reopen.
     */
    void close();

    /**
     * Toggles the overlay open/closed.
     */
    default void toggle() {
        if (isOpen()) close();
        else open();
    }

    /**
     * @return whether inspect (element picker) mode is active. In this mode,
     * moving the mouse over the inspected scene highlights the hovered widget
     * and clicking selects it instead of triggering the UI.
     */
    boolean inspectMode();

    /**
     * Enables or disables inspect (element picker) mode.
     */
    void setInspectMode(boolean enabled);

    /**
     * Selects the widget shown in the details view. May be {@code null} to
     * clear the selection.
     */
    void select(@Nullable Widget widget);

    /**
     * @return the currently selected widget, or {@code null}.
     */
    @Nullable
    Widget selected();

    /**
     * @return whether box-model highlights are drawn over the inspected scene.
     */
    boolean highlightEnabled();

    /**
     * Enables or disables the box-model highlight overlay.
     */
    void setHighlightEnabled(boolean enabled);

    /**
     * @return the screen area reserved for the DevTools panel when docked,
     * in GUI-scaled pixels. Floating panels return {@link Insets#zero}.
     */
    default Insets contentInsets() {
        return Insets.zero;
    }
}
