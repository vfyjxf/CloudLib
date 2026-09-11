package dev.vfyjxf.cloudlib.ui.hacker;

/**
 * Visual constants for the in-world UI chrome — a hacker-mode look:
 * dark translucent panels, thin borders, corner brackets for focus,
 * and connector lines back to the world anchor.
 */
public final class HackerTheme {

    private HackerTheme() {
    }

    //region colors

    /** Panel background — dark blue-black, translucent. */
    public static final int BG = 0xD80A0E12;

    /** Panel background when focused — slightly lighter. */
    public static final int BG_FOCUSED = 0xE00D141A;

    /** Default thin border. */
    public static final int BORDER = 0xB35C6E7E;

    /** Border of the focused panel. */
    public static final int BORDER_FOCUSED = 0xFF57E6E6;

    /** Primary accent — cyan, used for brackets, tabs and line nodes. */
    public static final int ACCENT = 0xFF35D6D0;

    /** Softer accent for focused panel fill details. */
    public static final int ACCENT_DIM = 0x6635D6D0;

    /** Leader line between a panel and its world anchor. */
    public static final int LINE = 0xB0E8F4F8;

    /** Leader line of the focused panel. */
    public static final int LINE_FOCUSED = 0xFF57E6E6;

    /** Leader line core over bright backgrounds — dark slate so it still reads. */
    public static final int LINE_DARK = 0xD8121D26;

    /** Focused leader line over bright backgrounds — a deep teal. */
    public static final int LINE_FOCUSED_DARK = 0xFF159E97;

    /** Line halo used over bright backgrounds (inverse of the usual dark edge). */
    public static final int LINE_EDGE_LIGHT = 0x8CEAF6FF;

    /** Small square marker drawn at the anchor end of a leader line. */
    public static final int LINE_NODE = 0xFFFFFFFF;

    /** dim box edges of the block scan frame */
    public static final int SCAN_EDGE = 0x5935D6D0;
    /** corner ticks of the scan frame */
    public static final int SCAN_TICK = 0xCC35D6D0;
    /** edges when the block's panel is focused/pointed */
    public static final int SCAN_EDGE_HOT = 0x8C57E6E6;
    public static final int SCAN_TICK_HOT = 0xFF57E6E6;
    /** voxel-shape outline of a framed block — the vanilla hit-outline style thick lines */
    public static final int SCAN_SHAPE = 0x9935D6D0;
    public static final int SCAN_SHAPE_HOT = 0xFF57E6E6;
    /** the bright segment sweeping the frame's top loop */
    public static final int SCAN_SWEEP = 0xFF8FFFFF;
    /** dark underlay drawn under leader lines so they stay readable on bright terrain */
    public static final int LINE_EDGE = 0xB0050A0E;

    /** Primary text. */
    public static final int TEXT = 0xFFE8F4F8;

    /** Dimmed text — secondary info, hints. */
    public static final int TEXT_DIM = 0xFF7C93A3;

    /** Hint chip key text. */
    public static final int HINT_KEY = 0xFF35D6D0;

    /** Title strip divider. */
    public static final int TITLE_RULE = 0x6635D6D0;

    /** The crosshair dot drawn while a panel is pointed at in world mode. */
    public static final int CROSSHAIR = 0xFF35D6D0;

    //endregion

    //region metrics

    /** Title strip height inside the panel top padding. */
    public static final int TITLE_HEIGHT = 11;

    /** Hint strip height inside the panel bottom padding. */
    public static final int HINT_HEIGHT = 10;

    /** Content padding. */
    public static final int PADDING = 4;

    /** Corner bracket arm length of the focus frame. */
    public static final int BRACKET = 5;

    //endregion
}
