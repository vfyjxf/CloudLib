/**
 * In-world UI — contracts and pure geometry for placing UI surfaces in the
 * level.
 * <p>
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.Projection} is the world ↔ screen
 * conversion of one rendered frame; {@code render} holds the world-quad
 * geometry ({@code QuadBasis}) and the panel contract ({@code WorldUiPanel})
 * a renderer drives. Everything here is gui-scaled pixels on the screen side
 * and world blocks on the level side.
 */
package dev.vfyjxf.cloudlib.api.ui.inworld;
