package dev.vfyjxf.cloudlib.api.ui.inworld;

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
 * does not commit; it falls back to the spec's primary
 * {@link InworldPanelSpec#action}, so point-to-click and drag-to-trace can
 * share one key.
 */
public interface InworldTraceable {

    /**
     * A trace session is starting at {@code (x, y)} in content-local pixels —
     * the crosshair ray's unprojection when the press was aimed at the panel,
     * else the content center.
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
