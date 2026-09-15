package dev.vfyjxf.cloudlib.api.ui.inworld.render;

/**
 * How a leader line's stroke is drawn — the shading recipe, independent
 * of where the path goes. All styles emit antialiased, joined polylines
 * sized in gui px; the differences are in the cross-section profile and
 * alpha treatment:
 * <ul>
 *   <li>{@link #CRISP} — a flat, hard-edged thin line with a ~1px feathered
 *       rim. The clean baseline.</li>
 *   <li>{@link #HALO} — the crisp core plus a wide, low-alpha halo ribbon
 *       underneath — the hologram/energy glow look.</li>
 *   <li>{@link #DISSOLVE} — the crisp line whose alpha ramps in over the
 *       first ~24 px out of the source port, so the line appears to
 *       emanate from the anchor rather than terminate on it.</li>
 *   <li>{@link #BEAM} — a world-width beam: the width is fixed in world
 *       units, so perspective tapers it naturally with distance instead
 *       of holding a constant screen size. Screen-space links render
 *       this as {@link #CRISP}.</li>
 *   <li>{@link #ENERGY} — the crisp line broken into marching dashes that
 *       flow from source toward the target.</li>
 * </ul>
 */
public enum TraceStyle {
    crisp,
    halo,
    dissolve,
    beam,
    energy
}
