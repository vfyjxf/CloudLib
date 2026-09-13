package dev.vfyjxf.nimbusprojection.internal;

/**
 * Visual constants for the in-world UI chrome — the Minecraft-native warm
 * palette: walnut-dark translucent panels, gold accents (the game's own
 * selection color), warm paper text. This is the in-world fallback the
 * theme system overrides — deliberately NOT the cyan hacker look, which
 * stays the identity of the {@code cloudlib:hacker} screen theme.
 */
public final class NimbusPalette {

    private NimbusPalette() {}

    // region colors

    /** Panel background — deep warm charcoal, translucent. */
    public static final int bg = 0xD81A1410;

    /** Panel background when focused — slightly lighter walnut. */
    public static final int bgFocused = 0xE0221B14;

    /** Default thin border — warm gray-brown. */
    public static final int border = 0xB37A6B52;

    /** Border of the focused panel — MC selection gold. */
    public static final int borderFocused = 0xFFDF8C46;

    /** Primary accent — gold, used for brackets, nodes and hint keys. */
    public static final int accent = 0xFFDF8C46;

    /** Softer accent for focused panel fill details. */
    public static final int accentDim = 0x66DF8C46;

    /** Leader line between a panel and its world anchor — warm bone. */
    public static final int line = 0xB0EDE4D3;

    /** Leader line of the focused panel — gold. */
    public static final int lineFocused = 0xFFDF8C46;

    /** Leader line core over bright backgrounds — dark walnut so it still reads. */
    public static final int lineDark = 0xD81F150D;

    /** Focused leader line over bright backgrounds — deep bronze. */
    public static final int lineFocusedDark = 0xFF9C6B1E;

    /** Line halo used over bright backgrounds (inverse of the usual dark edge). */
    public static final int lineEdgeLight = 0x8CFFF2D8;

    /** Small square marker drawn at the anchor end of a leader line. */
    public static final int lineNode = 0xFFFFFFFF;

    /** dim box edges of the block scan frame */
    public static final int scanEdge = 0x59DF8C46;
    /** corner ticks of the scan frame */
    public static final int scanTick = 0xCCDF8C46;
    /** edges when the block's panel is focused/pointed */
    public static final int scanEdgeHot = 0x8CDF8C46;

    public static final int scanTickHot = 0xFFDF8C46;
    /** voxel-shape outline of a framed block — the vanilla hit-outline style thick lines */
    public static final int scanShape = 0x99DF8C46;

    public static final int scanShapeHot = 0xFFDF8C46;
    /** the bright segment sweeping the frame's top loop */
    public static final int scanSweep = 0xFFF0C08F;
    /** dark underlay drawn under leader lines so they stay readable on bright terrain */
    public static final int lineEdge = 0xB01A1208;

    /** Primary text — warm bone. */
    public static final int text = 0xFFEDE4D3;

    /** Dimmed text — secondary info, hints. */
    public static final int textDim = 0xFFB0A48C;

    /** Hint chip key text. */
    public static final int hintKey = 0xFFDF8C46;

    /** Title strip divider. */
    public static final int titleRule = 0x66DF8C46;

    /** The crosshair dot drawn while a panel is pointed at in world mode. */
    public static final int crosshair = 0xFFDF8C46;

    // endregion

    // region metrics — same grid as the hacker constants

    /** Title strip height inside the panel top padding. */
    public static final int titleHeight = 11;

    /** Hint strip height inside the panel bottom padding. */
    public static final int hintHeight = 10;

    /** Content padding. */
    public static final int padding = 4;

    /** Corner bracket arm length of the focus frame. */
    public static final int bracket = 5;

    // endregion
}
