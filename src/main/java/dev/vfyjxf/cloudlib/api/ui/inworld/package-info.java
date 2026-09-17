/**
 * In-world UI — contracts and pure geometry for placing UI surfaces in the
 * level.
 * <p>
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.Projection} is the world ↔ screen
 * conversion of one rendered frame; {@code render} holds the world-quad
 * geometry ({@code QuadBasis} and its orientation constructors/interpolation,
 * {@code QuadOrientation}), the panel contract ({@code WorldUiPanel}) and
 * the renderer batching contracts, {@code trace} the source↔panel trace
 * contracts, and {@code layout} the placement-resolution contracts.
 * {@code OffscreenProjector} computes off-screen/behind-target directions and
 * edge landing points, and {@code OcclusionProbe}/{@code OcclusionFade} the
 * line-of-sight sampling and fade policy behind occlusion fading. Everything
 * here is gui-scaled pixels on the screen side and world blocks on the level
 * side.
 */
@dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault
package dev.vfyjxf.cloudlib.api.ui.inworld;
