/**
 * Border measurement — turning a UI frame into attachable geometry.
 * <p>
 * {@link dev.vfyjxf.cloudlib.api.ui.border.RectBorder} measures a screen
 * rect: parametric edge points, corners, perimeter walking and
 * {@link dev.vfyjxf.cloudlib.api.ui.border.ScreenPort}s (attach point +
 * outward direction). {@link dev.vfyjxf.cloudlib.api.ui.border.BorderPoint}
 * is the selection spec — nearest / edge(t) / corner / center — that
 * callers use to pick where a connection lands.
 * <p>
 * The world-space counterpart lives in the inworld stack:
 * {@code QuadBorder} measures a {@code WorldUiPanel}'s live quad the same
 * way. Trace links consume both; measurement itself draws nothing —
 * stroking a border is always opt-in.
 */
package dev.vfyjxf.cloudlib.api.ui.border;
