package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;

/**
 * Offers candidate world-space center positions for a panel. The solver
 * evaluates each slot against the panel's world-space extent
 * ({@code panelWidth} × {@code panelHeight} world units).
 */
public interface PositionPolicy {

    List<PositionSlot> slots(IntentContext context, double panelWidth, double panelHeight);

    /** False pins the panel to its slots — it may never be moved by the solver. */
    default boolean canRelocate() {
        return true;
    }

    /** Mounted panels are drawn flush on the source surface and must use a source-surface orientation. */
    default boolean mounted() {
        return false;
    }
}
