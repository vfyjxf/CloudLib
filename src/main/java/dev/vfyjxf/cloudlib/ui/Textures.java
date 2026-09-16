package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.NineSliceTexture;
import dev.vfyjxf.cloudlib.util.Locations;

/**
 * Shared GUI textures for CloudLib — pixel-art NineSlice backgrounds, scrollbar
 * assets, icons, tabs and toggles available to all UI widgets.
 * <p>
 * Assets are sourced from the CalculatorCirrus pixel set and use a light
 * palette with 4–10px borders suitable for NineSlice scaling.
 */
public final class Textures {

    private Textures() {}

    // region backgrounds

    /** Main window frame — 32×37, 10px border on all sides. */
    public static final NineSliceTexture frame = bg("frame", 32, 37, 10, 10, 10, 10);
    /** Flat panel background — 18×18, 4px border. */
    public static final NineSliceTexture flat = bg("flat", 18, 18, 4);
    /** Inset / recessed background — 18×18, 4px border. */
    public static final NineSliceTexture inset = bg("inset", 18, 18, 4);
    /** Darker background — 16×16, 4px border. */
    public static final NineSliceTexture dark = bg("dark", 16, 16, 4);
    /** Scrollbar track — 18×9, 4px border. */
    public static final NineSliceTexture scrollTrack = bg("scroll", 18, 9, 4, 4, 4, 4);
    /** Outlined inset — 20×20, 4px border. */
    public static final NineSliceTexture outlinedInset = bg("outlined_inset", 20, 20, 4);
    /** Outlined flat — 20×20, 4px border. */
    public static final NineSliceTexture outlinedFlat = bg("outlined_flat", 20, 20, 4);
    /** Dark border — 18×18, 4px border. */
    public static final NineSliceTexture borderDark = bg("border_dark", 18, 18, 4);
    /** Light border — 18×18, 4px border. */
    public static final NineSliceTexture borderLight = bg("border_light", 18, 18, 4);

    // endregion

    // region background — paged

    /** Paged arrow background — 9×10, 3px border. */
    public static final NineSliceTexture pagedArrow = bg("paged/arrow", 9, 10, 3);
    /** Paged button background — 8×10, 3px border. */
    public static final NineSliceTexture pagedButton = bg("paged/button", 8, 10, 3);

    // endregion

    // region background — slot

    /** Slot base — 18×18, 4px border. */
    public static final NineSliceTexture slotBase = bg("slot/base", 18, 18, 4);
    /** Slot dark — 18×18, 4px border. */
    public static final NineSliceTexture slotDark = bg("slot/dark", 18, 18, 4);

    // endregion

    // region background — sign

    /** Amount sign icon — 7×7. */
    public static final ImageTexture signAmount = img("background/sign/amount_sign", 7, 7);
    /** Sign bar — 45×4, 2px border. */
    public static final NineSliceTexture signBar = bg("sign/bar", 45, 4, 2);

    // endregion

    // region scrollbar

    /** Vertical scrollbar thumb — 7×8, border 2/3/4/3. */
    public static final NineSliceTexture scrollbarVertical = sb("scrollbar_vertical", 7, 8, 2, 3, 4, 3);
    /** Horizontal scrollbar thumb — 7×9, border 2/3/3/3. */
    public static final NineSliceTexture scrollbarHorizontal = sb("scrollbar_horizontal", 7, 9, 2, 3, 3, 3);
    /** Scroll track fill — 3×3, 1px border. */
    public static final NineSliceTexture scrollTrackFill = sb("scroll_track_fill", 3, 3, 1);

    // endregion

    // region icons — general (10×10 unless noted)

    public static final ImageTexture arrowDown = icon("arrow_down", 10, 10);
    public static final ImageTexture arrowDownSelected = icon("arrow_down_selected", 10, 10);
    public static final ImageTexture arrowRight = icon("arrow_right", 10, 10);
    public static final ImageTexture arrowUp = icon("arrow_up", 10, 10);
    public static final ImageTexture arrowUpSelected = icon("arrow_up_selected", 10, 10);
    public static final ImageTexture delete = icon("delete", 10, 10);
    public static final ImageTexture deleteSelected = icon("delete_selected", 10, 10);
    public static final ImageTexture search = icon("search", 10, 10);
    public static final ImageTexture searchSelected = icon("search_selected", 10, 10);
    public static final ImageTexture setting = icon("setting", 10, 10);
    public static final ImageTexture settingSelected = icon("setting_selected", 10, 10);
    public static final ImageTexture finished = icon("finished", 11, 11);
    public static final ImageTexture next = icon("next", 12, 12);
    public static final ImageTexture nextPressed = icon("next_pressed", 12, 12);
    public static final ImageTexture pause = icon("pause", 11, 11);
    public static final ImageTexture play = icon("play", 11, 11);
    public static final ImageTexture start = icon("start", 12, 12);
    public static final ImageTexture startPressed = icon("start_pressed", 12, 12);

    // endregion

    // region icons — small (5×6)

    public static final class Small {
        private Small() {}

        public static final ImageTexture arrowLeft = icon("small/arrow_left", 5, 6);
        public static final ImageTexture arrowLeftSelected = icon("small/arrow_left_selected", 5, 6);
        public static final ImageTexture arrowRight = icon("small/arrow_right", 5, 6);
        public static final ImageTexture arrowRightSelected = icon("small/arrow_right_selected", 5, 6);
        public static final ImageTexture minus = icon("small/minus", 5, 6);
        public static final ImageTexture minusSelected = icon("small/minus_selected", 5, 6);
        public static final ImageTexture plus = icon("small/plus", 5, 6);
        public static final ImageTexture plusSelected = icon("small/plus_selected", 5, 6);
    }

    // endregion

    // region icons — middle (9×9)

    public static final class Middle {
        private Middle() {}

        public static final ImageTexture arrowLeft = icon("middle/arrow_left", 9, 9);
        public static final ImageTexture arrowLeftDown = icon("middle/arrow_left_down", 9, 9);
        public static final ImageTexture arrowRight = icon("middle/arrow_right", 9, 9);
        public static final ImageTexture arrowRightDown = icon("middle/arrow_right_down", 9, 9);
    }

    // endregion

    // region tabs

    public static final class Tab {
        private Tab() {}

        public static final ImageTexture select = img("tab/select", 21, 22);
        public static final ImageTexture hover = img("tab/hover", 21, 22);
        public static final ImageTexture unselect = img("tab/unselect", 20, 22);
    }

    // endregion

    // region toggle — button

    public static final class ToggleButton {
        private ToggleButton() {}

        public static final NineSliceTexture up = ns("toggle/button/up", 14, 16, 4);
        public static final NineSliceTexture hover = ns("toggle/button/hover", 14, 15, 4);
        public static final NineSliceTexture down = ns("toggle/button/down", 14, 15, 4);
    }

    // endregion

    // region toggle — checked

    public static final class Checked {
        private Checked() {}

        public static final ImageTexture select = img("toggle/checked/select", 10, 10);
        public static final ImageTexture selectHover = img("toggle/checked/select_hover", 10, 10);
        public static final ImageTexture unselect = img("toggle/checked/unselect", 10, 10);
        public static final ImageTexture unselectHover = img("toggle/checked/unselect_hover", 10, 10);

        public static final class Small {
            private Small() {}

            public static final ImageTexture select = img("toggle/checked/small/select", 7, 7);
            public static final ImageTexture selectHover = img("toggle/checked/small/select_hover", 7, 7);
            public static final ImageTexture unselect = img("toggle/checked/small/unselect", 7, 7);
            public static final ImageTexture unselectHover = img("toggle/checked/small/unselect_hover", 7, 7);
        }
    }

    // endregion

    // region toggle — switch (slide style)

    public static final class Switch {
        private Switch() {}

        public static final ImageTexture on = img("toggle/switch/slide_on", 20, 13);
        public static final ImageTexture onHover = img("toggle/switch/slide_on_hover", 20, 12);
        public static final ImageTexture off = img("toggle/switch/slide_off", 20, 13);
        public static final ImageTexture offHover = img("toggle/switch/slide_off_hover", 20, 12);
    }

    // endregion

    // region factories

    private static NineSliceTexture bg(String name, int w, int h, int border) {
        return NineSliceTexture.of(Locations.ofMod("textures/gui/background/" + name + ".png"), w, h, border);
    }

    private static NineSliceTexture bg(String name, int w, int h, int left, int right, int top, int bottom) {
        return NineSliceTexture.of(
                Locations.ofMod("textures/gui/background/" + name + ".png"), w, h, left, right, top, bottom);
    }

    private static NineSliceTexture sb(String name, int w, int h, int border) {
        return NineSliceTexture.of(Locations.ofMod("textures/gui/scrollbar/" + name + ".png"), w, h, border);
    }

    private static NineSliceTexture sb(String name, int w, int h, int left, int right, int top, int bottom) {
        return NineSliceTexture.of(
                Locations.ofMod("textures/gui/scrollbar/" + name + ".png"), w, h, left, right, top, bottom);
    }

    private static NineSliceTexture ns(String name, int w, int h, int border) {
        return NineSliceTexture.of(Locations.ofMod("textures/gui/" + name + ".png"), w, h, border);
    }

    private static ImageTexture icon(String name, int w, int h) {
        return ImageTexture.of(Locations.ofMod("textures/gui/icon/" + name + ".png"), w, h);
    }

    private static ImageTexture img(String name, int w, int h) {
        return ImageTexture.of(Locations.ofMod("textures/gui/" + name + ".png"), w, h);
    }

    // endregion
}
