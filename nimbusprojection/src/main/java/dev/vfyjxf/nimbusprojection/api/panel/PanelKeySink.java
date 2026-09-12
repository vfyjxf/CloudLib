package dev.vfyjxf.nimbusprojection.api.panel;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.InputContext;

/**
 * Opt-in keyboard contract for panel content: scene key events target the
 * panel's chrome (the focus scope root), so content widgets never see keys
 * through normal bubbling. A content widget implementing this interface
 * receives every key the chrome left unconsumed — panel-level gestures
 * like quick-store live here instead of in the runtime's key loop.
 */
public interface PanelKeySink {

    /**
     * A key press the chrome did not consume. Return
     * {@link EventDispatch#consumed} to stop further handling,
     * {@link EventDispatch#pass} to let the event continue.
     */
    EventDispatch keyPressed(InputContext input);
}
