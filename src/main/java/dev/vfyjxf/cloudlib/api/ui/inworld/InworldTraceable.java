package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.jetbrains.annotations.Nullable;

/**
 * Opt-in "trace mode" for a panel's content widget — the Witness-style
 * drag interaction: while the interact key is held on a focused traceable
 * panel, the camera locks, the OS cursor is captured, and mouse motion is
 * unprojected onto the panel surface and delivered as content-local pixel
 * coordinates.
 * <p>
 * A session is one {@link #traceBegin} → zero or more {@link #traceMove} →
 * exactly one of {@link #traceCommit}/{@link #traceCancel}. What the drag
 * <em>means</em> is entirely up to the widget: draw a path through a node
 * grid, sketch a freehand glyph, scrub a dial, drag a handle — the manager
 * only guarantees the coordinate stream.
 * <p>
 * A quick tap of the interact key (under ~6 ticks with &lt;4px of motion)
 * does not commit; it falls back to the panel's primary action, so
 * point-to-click and drag-to-trace can share one key.
 */
public interface InworldTraceable {

    /**
     * Optional cursor snap point in content-local pixels — returned
     * <em>before</em> {@link #traceBegin} so the session can begin at a
     * canonical location. Witness panels activate by jumping the cursor onto
     * the start node; return that node's center here and the runtime warps
     * the physical cursor so the traced stroke genuinely starts there.
     * <p>
     * {@code null} (default) = the trace begins wherever the press landed.
     */
    default @Nullable FloatPos traceCursorStart() {
        return null;
    }

    /**
     * A trace session is starting at {@code (x, y)} in content-local pixels —
     * the crosshair ray's unprojection when the press was aimed at the panel,
     * else the content center. When {@link #traceCursorStart} is non-null the
     * argument is already the snapped point.
     *
     * @return false to refuse the session; the press then falls back to the
     * panel's primary action
     */
    default boolean traceBegin(InworldPanelContext context, float x, float y) {
        return true;
    }

    /** The trace cursor moved to {@code (x, y)}, content-local pixels. */
    void traceMove(float x, float y);

    /** The interact key was released — commit whatever was traced. */
    void traceCommit(InworldPanelContext context);

    /**
     * The session was aborted: ESC, the panel closing, or a tap that fell
     * back to the primary action. Clean up any in-progress stroke.
     */
    default void traceCancel() {
    }
}
