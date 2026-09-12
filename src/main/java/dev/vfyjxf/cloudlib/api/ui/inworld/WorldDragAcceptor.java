package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.jetbrains.annotations.Nullable;

/**
 * The drop-side counterpart of {@link WorldDraggable}: a widget that accepts
 * a {@link WorldDrag} released over its surface — panel-to-panel transfers
 * like dropping a dragged stack onto a specific inventory slot.
 * <p>
 * The contract is deliberately thin: {@link #acceptWorldDrag} returns whether
 * the drop was consumed. The acceptor owns the semantics — which slot was
 * hit, whether the payload is legal, and which packet commits it — because
 * only the feature knows its own addressing scheme. The runtime detects the
 * drop target and hands over the live drag; nothing else is prescribed.
 */
public interface WorldDragAcceptor {

    /**
     * A drag ended over this widget.
     *
     * @param ctx    the target panel's context — the acceptor's own panel,
     *               not the drag's source
     * @param sceneX scene-space x of the release point
     * @param sceneY scene-space y of the release point
     * @return true = the drop was consumed (the stack is spoken for);
     *         false = fall through to the world-commit path
     */
    boolean acceptWorldDrag(WorldDrag drag, InworldPanelContext ctx, double sceneX, double sceneY);

    /** Nearest acceptor up the widget parent chain, or null. */
    static @Nullable WorldDragAcceptor find(@Nullable Widget hit) {
        Widget w = hit;
        while (w != null) {
            if (w instanceof WorldDragAcceptor a) return a;
            w = w.parent();
        }
        return null;
    }
}
