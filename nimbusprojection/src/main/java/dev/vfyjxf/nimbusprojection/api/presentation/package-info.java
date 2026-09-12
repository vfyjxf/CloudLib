/**
 * The presentation driver SPI — how a {@code Presentation} descriptor
 * becomes geometry on screen.
 * <p>
 * {@link PresentationDriver} is the open extension point: register one per
 * {@code Presentation.type()} id. Drivers answer resolve/pick/flatten; the
 * runtime owns occlusion avoidance, chrome collision and focus — drivers
 * only report geometry through {@link PanelGeometry}.
 */
package dev.vfyjxf.nimbusprojection.api.presentation;
