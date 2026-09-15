package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.jetbrains.annotations.Nullable;

/**
 * Creates an overlay widget for the current client state, or returns {@code null} to indicate
 * that the overlay should not be active.
 *
 * @param <T> the overlay widget type
 */
@FunctionalInterface
public interface OverlayProvider<T extends Widget> {

    /**
     * Create the overlay widget for the given context, or return {@code null} if the overlay
     * should not be shown.
     */
    @Nullable T create(OverlayContext context);

}
