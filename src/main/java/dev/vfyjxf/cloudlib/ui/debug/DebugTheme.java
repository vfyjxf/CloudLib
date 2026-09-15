package dev.vfyjxf.cloudlib.ui.debug;

/**
 * Color and sizing constants for the debug overlay.
 * <p>
 * Cirrus-inspired light pixel palette: matches the CalculatorCirrus asset set
 * so the DevTools window blends with the rest of the UI.
 */
final class DebugTheme {

    private DebugTheme() {
    }

    //region window

    static final int WINDOW_BG = 0xFFA8A8A8;
    static final int WINDOW_BORDER = 0xFF555555;
    static final int TITLE_BAR_BG = 0xFFA8A8A8;
    static final int TITLE_SEPARATOR = 0xFF555555;

    static final int DEFAULT_WIDTH = 260;
    static final int DEFAULT_DOCK_WIDTH = 220;
    static final int DEFAULT_DOCK_HEIGHT = 180;
    static final int MIN_WIDTH = 180;
    static final int MIN_HEIGHT = 120;
    static final int DOCK_MIN_SIZE = 160;
    static final int DOCK_MAX_WIDTH = 480;
    static final int DOCK_MAX_HEIGHT = 400;
    static final int TITLE_BAR_HEIGHT = 24;

    //endregion

    //region text

    static final int TEXT = 0xFF3F3F3F;
    static final int TEXT_DIM = 0xFF555555;
    static final int ACCENT = 0xFF3A8CFF;
    static final int CATEGORY = 0xFF2E7D6A;
    static final int PROP_NAME = 0xFF0066CC;
    static final int PROP_VALUE_STRING = 0xFF8B4513;
    static final int PROP_VALUE_NUMBER = 0xFF2E7D32;
    static final int PROP_VALUE_BOOL = 0xFF1565C0;
    static final int PROP_VALUE_DEFAULT = 0xFF555555;

    //endregion

    //region controls

    static final int BUTTON_HOVER_BG = 0x33FFFFFF;
    static final int BUTTON_ACTIVE_BG = 0xFF5AA5FF;
    static final int ICON = 0xFF555555;
    static final int ICON_HOVER = 0xFF3F3F3F;
    static final int ICON_ACTIVE = 0xFFFFFFFF;
    static final int ICON_CLOSE_HOVER = 0xFFFF5555;
    static final int INPUT_BG = 0xFFB0B0B0;
    static final int INPUT_BORDER = 0xFF555555;

    static final int ROW_SELECTED_BG = 0xFF5AA5FF;
    static final int ROW_HOVER_BG = 0x33FFFFFF;
    static final int INDENT_GUIDE = 0xFF8B8B8B;
    static final int SECTION_LINE = 0xFF8B8B8B;

    static final int SCROLLBAR_TRACK = 0x00000000;
    static final int SCROLLBAR_THUMB = 0xFF808080;

    static final int ROW_HEIGHT = 20;
    static final int ROW_INDENT = 16;
    static final int SPLITTER_HEIGHT = 6;

    //endregion

    //region box-model highlight (DevTools colors)

    static final int HL_MARGIN = 0x60F2994A;
    static final int HL_BORDER = 0x60FFE082;
    static final int HL_PADDING = 0x6034C759;
    static final int HL_CONTENT = 0x60347AC1;
    static final int HL_OUTLINE = 0xDD3794FF;
    static final int HL_OUTLINE_HOVER = 0x883794FF;
    static final int HL_LABEL_BG = 0xEE404040;
    static final int HL_LABEL_TEXT = 0xFFFFFFFF;

    //endregion

    //region box-model diagram

    static final int BM_MARGIN = 0x66F2994A;
    static final int BM_BORDER = 0x66FFE082;
    static final int BM_PADDING = 0x6634C759;
    static final int BM_CONTENT = 0x66347AC1;
    static final int BM_TEXT = 0xFF222222;
    static final int BM_BAND = 15;
    static final int BM_CONTENT_MIN = 14;

    //endregion

}
