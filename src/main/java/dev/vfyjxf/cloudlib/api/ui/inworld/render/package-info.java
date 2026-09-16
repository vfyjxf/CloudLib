/**
 * World-space panel rendering contracts.
 * <p>
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.render.QuadBasis} is the pure
 * geometry of a UI surface placed in the world (origin + px→world basis);
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.render.WorldUiPanel} is the panel
 * contract — a w×h canvas with a {@code Placer}, {@code Painter} and optional
 * {@code LinesEmitter} — that a renderer resolves into a sorted translucent
 * world draw.
 */
package dev.vfyjxf.cloudlib.api.ui.inworld.render;
