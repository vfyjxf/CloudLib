package dev.vfyjxf.nimbusprojection.api.presentation;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;

/**
 * Input handed to {@link PresentationDriver#flatten} when inspect mode
 * flattens a panel to screen space.
 *
 * @param presentation the panel's presentation descriptor
 * @param panel        the live panel
 * @param projection   world ↔ screen conversion for this frame
 * @param anchorScreen the anchor's projected screen position
 * @param naturalSize  the panel's natural content size in gui pixels
 */
public record FlattenContext<P extends Presentation>(
        P presentation,
        InworldPanel panel,
        Projection projection,
        FloatPos anchorScreen,
        Size naturalSize
) {
}
