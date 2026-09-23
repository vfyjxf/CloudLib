package dev.vfyjxf.cloudlib.api.ui.inworld.space;

/**
 * The space layers an element can occupy, as bitmask source. Elements carry
 * the set of layers they belong to and interact with the layers that set
 * intersects — which pairs actually collide is the coordinator's call, this
 * enum only supplies the bits.
 * <ul>
 *   <li>{@link #hudBase} — vanilla HUD surface (hotbar, health columns, …)</li>
 *   <li>{@link #hudOverlay} — overlays on top of the HUD (boss bars, toasts, …)</li>
 *   <li>{@link #screenPanel} — in-screen panels and floating windows</li>
 *   <li>{@link #worldAnchored} — world-anchored surfaces projected to screen</li>
 *   <li>{@link #indicator} — offscreen/edge indicators, exempt from rect
 *       avoidance</li>
 *   <li>{@link #debug} — debug overlays that never push anything</li>
 * </ul>
 */
public enum SpaceMask {
    hudBase, hudOverlay, screenPanel, worldAnchored, indicator, debug;

    /** The single bit this layer occupies. */
    public int bit() {
        return 1 << ordinal();
    }

    /** Whether the two masks share a layer. */
    public boolean intersects(SpaceMask other) {
        return (bit() & other.bit()) != 0;
    }

    /** Whether this mask is part of the given bit set. */
    public boolean within(int maskBits) {
        return (bit() & maskBits) != 0;
    }

    /** The bits of every layer. */
    public static int allBits() {
        int bits = 0;
        for (SpaceMask mask : values()) {
            bits |= mask.bit();
        }
        return bits;
    }
}
