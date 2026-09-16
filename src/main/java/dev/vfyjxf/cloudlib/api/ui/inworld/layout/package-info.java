/**
 * Layout contracts — resolving where an in-world UI surface lands for a frame.
 * <p>
 * A {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.Layout} turns a
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutContext} into a
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutOutput} (world quad,
 * screen rect, or deferred to the inscreen coordinator); implementations are
 * registered per intent type in a
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutRegistry}. The
 * {@code inscreen} package holds the screen-placement coordinator and the
 * hints the API side steers it with.
 */
@dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault
package dev.vfyjxf.cloudlib.api.ui.inworld.layout;
