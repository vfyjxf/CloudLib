/**
 * Layout-space bookkeeping for the inworld coordination system — pure logic,
 * no Minecraft types. Everything operates on gui-scaled screen pixels in the
 * standard GUI coordinate system: origin at the top-left corner, x growing
 * right, y growing down.
 * <p>
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.space.LayoutSpace} derives the
 * {@code viewport → safeArea → workArea} rectangle hierarchy;
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.space.OccupancyBitmap} answers
 * coarse occupancy queries, {@link dev.vfyjxf.cloudlib.api.ui.inworld.space.FreeRectIndex}
 * allocates in-screen blocks MaxRects-style, and
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.space.IntervalSet} /
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.space.RayFan} find free angular
 * space around world anchors.
 * <p>
 * Third-party avoidance goes through the public exclusion-area API:
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.space.InworldExclusions#register}.
 */
@org.jspecify.annotations.NullMarked
package dev.vfyjxf.cloudlib.api.ui.inworld.space;
