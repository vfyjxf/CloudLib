package dev.vfyjxf.cloudlib.ui.debug;

/**
 * Color and sizing constants for the debug overlay.
 * <p>
 * Cirrus-inspired light pixel palette: matches the CalculatorCirrus asset set
 * so the DevTools window blends with the rest of the UI.
 */
final class DebugTheme {

    private DebugTheme() {}

    // region window

    static final int windowBg = 0xFFA8A8A8;
    static final int windowBorder = 0xFF555555;
    static final int titleBarBg = 0xFFA8A8A8;
    static final int titleSeparator = 0xFF555555;

    static final int defaultWidth = 260;
    static final int defaultDockWidth = 220;
    static final int defaultDockHeight = 180;
    static final int minWidth = 180;
    static final int minHeight = 120;
    static final int dockMinSize = 160;
    static final int dockMaxWidth = 480;
    static final int dockMaxHeight = 400;
    static final int titleBarHeight = 24;

    // endregion

    // region text

    static final int text = 0xFF3F3F3F;
    static final int textDim = 0xFF555555;
    static final int accent = 0xFF3A8CFF;
    static final int category = 0xFF2E7D6A;
    static final int propName = 0xFF0066CC;
    static final int propValueString = 0xFF8B4513;
    static final int propValueNumber = 0xFF2E7D32;
    static final int propValueBool = 0xFF1565C0;
    static final int propValueDefault = 0xFF555555;

    // endregion

    // region controls

    static final int buttonHoverBg = 0x33FFFFFF;
    static final int buttonActiveBg = 0xFF5AA5FF;
    static final int icon = 0xFF555555;
    static final int iconHover = 0xFF3F3F3F;
    static final int iconActive = 0xFFFFFFFF;
    static final int iconCloseHover = 0xFFFF5555;
    static final int inputBg = 0xFFB0B0B0;
    static final int inputBorder = 0xFF555555;

    static final int rowSelectedBg = 0xFF5AA5FF;
    static final int rowHoverBg = 0x33FFFFFF;
    static final int indentGuide = 0xFF8B8B8B;
    static final int sectionLine = 0xFF8B8B8B;

    static final int scrollbarTrack = 0x00000000;
    static final int scrollbarThumb = 0xFF808080;

    static final int rowHeight = 20;
    static final int rowIndent = 16;
    static final int splitterHeight = 6;

    // endregion

    // region box-model highlight (DevTools colors)

    static final int hlMargin = 0x60F2994A;
    static final int hlBorder = 0x60FFE082;
    static final int hlPadding = 0x6034C759;
    static final int hlContent = 0x60347AC1;
    static final int hlOutline = 0xDD3794FF;
    static final int hlOutlineHover = 0x883794FF;
    static final int hlLabelBg = 0xEE404040;
    static final int hlLabelText = 0xFFFFFFFF;

    // endregion

    // region box-model diagram

    static final int bmMargin = 0x66F2994A;
    static final int bmBorder = 0x66FFE082;
    static final int bmPadding = 0x6634C759;
    static final int bmContent = 0x66347AC1;
    static final int bmText = 0xFF222222;
    static final int bmBand = 15;
    static final int bmContentMin = 14;

    // endregion

}
