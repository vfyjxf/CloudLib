package dev.vfyjxf.cloudlib.api.ui.texture;

import dev.vfyjxf.cloudlib.Constants;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The built-in sprite tables ported from LDLib2 — the {@code built-in(<ns:NAME>)}
 * texture function vocabulary.
 * <p>
 * Three sheets ship under {@code assets/cloudlib/textures/gui/oreui/}; the sprites drawn
 * on them are a region of one sheet plus their nine-slice border:
 * <ul>
 *   <li>{@code ore:} — LDLib2's {@code OreSprites} (OreUI buttons, tabs, frames, slots,
 *       switches) on {@code ore_styles.png}</li>
 *   <li>{@code mc:} — LDLib2's {@code MCSprites} (vanilla-flavoured panels and scrollers)
 *       on {@code mc_styles.png}</li>
 *   <li>{@code gdp:} — LDLib2's {@code Sprites} (the GDP panel/button/border set) on
 *       {@code gdp_styles.png}</li>
 *   <li>{@code icon:} — the harvest check/cross pair, see {@link Icon}. {@code check} is
 *       reused verbatim from the ore sheet; {@code cross} is a generated standalone
 *       texture ({@code textures/gui/cross.png}), so this is the one table whose members
 *       do not all share a sheet.</li>
 * </ul>
 * Ids keep the LDLib2 constant names verbatim ({@code ore:BTN_DEFAULT}) and are listed in
 * the registry block below; the java fields follow the repo naming rule and are
 * lowerCamelCase. Lookup is case-insensitive, and LDLib2's {@code ui-ore} / {@code ui-mc}
 * / {@code ui-gdp} provider namespaces resolve to the same sprites.
 * <p>
 * Every sprite carries the wrap mode LDLib2 declares for it: {@code CLAMP} (its
 * {@code SpriteTexture} default) stretches the edges and the center and becomes
 * {@link NineSliceTexture.Wrap#stretch}, while the four {@code SCROLLER_*} sprites —
 * the only ones LDLib2 puts on {@code REPEAT} — keep their tiling as
 * {@link NineSliceTexture.Wrap#repeat}. Region and border numbers are unchanged from
 * upstream.
 */
public final class BuiltInTextures {

    private BuiltInTextures() {}

    /** Forces {@code <clinit>} — builds the id table. */
    public static void init() {}

    // region sheets

    /** LDLib2 {@code OreSprites.ORE} — {@code ore_styles.png}. */
    public static final ResourceLocation oreSheet = sheet("ore_styles.png");

    /** LDLib2 {@code MCSprites.MC} — {@code mc_styles.png}. */
    public static final ResourceLocation mcSheet = sheet("mc_styles.png");

    /** LDLib2 {@code Sprites.GDP} — {@code gdp_styles.png}. */
    public static final ResourceLocation gdpSheet = sheet("gdp_styles.png");

    /**
     * The generated harvest cross — {@code textures/gui/cross.png}, a single 10×10
     * image rather than a sheet. It is not ported from LDLib2; see {@link Icon#cross}
     * for how it was drawn.
     */
    public static final ResourceLocation crossImage = ResourceLocation
            .fromNamespaceAndPath(Constants.namespace, "textures/gui/cross.png");

    // endregion

    // region tables

    /** LDLib2 {@code OreSprites} — 48 sprites at their upstream coordinates. */
    public static final class Ore {

        private Ore() {}

        public static final NineSliceTexture btnDefault = sliced(oreSheet, 0, 0, 5, 7, 2, 2, 2, 4);
        public static final NineSliceTexture btnPressed = sliced(oreSheet, 5, 0, 5, 7, 2, 2, 2, 4);
        public static final NineSliceTexture btnDisabled = sliced(oreSheet, 10, 0, 5, 7, 2, 2, 2, 4);
        public static final NineSliceTexture btnDefaultGreen = sliced(oreSheet, 26, 0, 5, 7, 2, 2, 2, 4);
        public static final NineSliceTexture btnPressedGreen = sliced(oreSheet, 31, 0, 5, 7, 2, 2, 2, 4);
        public static final NineSliceTexture btnDefaultRed = sliced(oreSheet, 36, 0, 5, 7, 2, 2, 2, 4);
        public static final NineSliceTexture btnPressedRed = sliced(oreSheet, 41, 0, 5, 7, 2, 2, 2, 4);
        public static final NineSliceTexture btnDefaultSmall = sliced(oreSheet, 46, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnHoverSmall = sliced(oreSheet, 51, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnPressedSmall = sliced(oreSheet, 56, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnDisabledSmall = sliced(oreSheet, 61, 0, 3, 3, 1, 1, 1, 1);
        public static final NineSliceTexture btnDefaultSmallGreen = sliced(oreSheet, 64, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnHoverSmallGreen = sliced(oreSheet, 69, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnPressedSmallGreen = sliced(oreSheet, 74, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnDefaultSmallRed = sliced(oreSheet, 79, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnHoverSmallRed = sliced(oreSheet, 84, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnPressedSmallRed = sliced(oreSheet, 89, 0, 5, 5, 2, 2, 2, 2);
        public static final NineSliceTexture btnRectDefault = sliced(oreSheet, 0, 7, 13, 14, 2, 2, 2, 3);
        public static final NineSliceTexture btnRectDisabled = sliced(oreSheet, 0, 21, 13, 14, 2, 2, 2, 3);
        public static final NineSliceTexture btnRectHover = sliced(oreSheet, 0, 35, 13, 14, 2, 2, 2, 3);
        public static final NineSliceTexture slotLight = sliced(oreSheet, 15, 0, 3, 3, 1, 1, 1, 1);
        public static final NineSliceTexture slotGray = sliced(oreSheet, 18, 0, 3, 3, 1, 1, 1, 1);
        public static final NineSliceTexture rect = sliced(oreSheet, 21, 0, 3, 4, 1, 1, 1, 2);
        public static final NineSliceTexture rect2 = sliced(oreSheet, 24, 0, 3, 5, 1, 3, 1, 1);
        public static final NineSliceTexture whiteBorder = sliced(oreSheet, 15, 3, 3, 3, 1, 1, 1, 1);
        public static final ImageTexture switchOn = plain(oreSheet, 13, 7, 24, 14);
        public static final ImageTexture switchOff = plain(oreSheet, 13, 21, 24, 14);
        public static final NineSliceTexture tabOffDefault = sliced(oreSheet, 50, 7, 11, 7, 2, 2, 2, 4);
        public static final NineSliceTexture tabOffHover = sliced(oreSheet, 50, 14, 11, 7, 2, 2, 2, 4);
        public static final NineSliceTexture tabOffPressed = sliced(oreSheet, 50, 21, 11, 7, 2, 4, 2, 2);
        public static final NineSliceTexture tabOffDisabled = sliced(oreSheet, 50, 28, 11, 7, 2, 2, 2, 4);
        public static final NineSliceTexture tabOnDefault = sliced(oreSheet, 61, 7, 11, 7, 2, 4, 2, 2);
        public static final NineSliceTexture tabOnHover = sliced(oreSheet, 61, 14, 11, 7, 2, 4, 2, 2);
        public static final NineSliceTexture tabOnPressed = sliced(oreSheet, 61, 21, 11, 7, 2, 4, 2, 2);
        public static final NineSliceTexture tabOnDisabled = sliced(oreSheet, 61, 28, 11, 7, 2, 4, 2, 2);
        public static final NineSliceTexture tabOffDefaultGreen = sliced(oreSheet, 72, 7, 11, 7, 2, 2, 2, 4);
        public static final NineSliceTexture tabOffHoverGreen = sliced(oreSheet, 72, 7, 11, 7, 2, 2, 2, 4);
        public static final NineSliceTexture tabOnDefaultGreen = sliced(oreSheet, 83, 14, 11, 7, 2, 4, 2, 2);
        public static final NineSliceTexture tabOnHoverGreen = sliced(oreSheet, 83, 14, 11, 7, 2, 4, 2, 2);
        public static final NineSliceTexture border = sliced(oreSheet, 0, 71, 62, 64, 5, 5, 5, 7);
        public static final NineSliceTexture border2 = sliced(oreSheet, 62, 71, 62, 64, 5, 5, 5, 7);
        public static final NineSliceTexture border3 = sliced(oreSheet, 0, 135, 62, 64, 5, 5, 5, 7);
        public static final NineSliceTexture border4 = sliced(oreSheet, 62, 135, 62, 64, 5, 5, 5, 7);
        public static final NineSliceTexture border5 = sliced(oreSheet, 0, 199, 50, 52, 3, 3, 3, 5);
        public static final NineSliceTexture border6 = sliced(oreSheet, 128, 1, 128, 128, 6, 6, 6, 8);
        public static final NineSliceTexture border7 = sliced(oreSheet, 128, 128, 128, 128, 3, 3, 3, 5);
        public static final ImageTexture check = plain(oreSheet, 50, 35, 10, 10);
        public static final ImageTexture down = plain(oreSheet, 60, 35, 10, 10);

    }

    /**
     * LDLib2 {@code MCSprites} — 22 sprites at their upstream coordinates.
     * <p>
     * {@code SWITCH} keeps its upstream name as the id; the java field is
     * {@code switchTexture} because {@code switch} is a keyword. The four
     * {@code SCROLLER_*} sprites are the only ones LDLib2 draws with {@code REPEAT}.
     */
    public static final class Mc {

        private Mc() {}

        public static final NineSliceTexture rect = sliced(mcSheet, 0, 0, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture rectInverse = sliced(mcSheet, 16, 0, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture rectBorder = sliced(mcSheet, 32, 0, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture rectThin = sliced(mcSheet, 48, 0, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture border = sliced(mcSheet, 64, 0, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1 = sliced(mcSheet, 80, 0, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture border2 = sliced(mcSheet, 96, 0, 16, 16, 7, 7, 7, 7);
        public static final NineSliceTexture border4 = sliced(mcSheet, 112, 0, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture rect1 = sliced(mcSheet, 0, 16, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture rect2 = sliced(mcSheet, 16, 16, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture rect3 = sliced(mcSheet, 32, 16, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture rect4 = sliced(mcSheet, 48, 16, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture rect5 = sliced(mcSheet, 64, 16, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture rect6 = sliced(mcSheet, 80, 16, 16, 16, 1, 1, 1, 1);
        public static final NineSliceTexture tabOff = sliced(mcSheet, 0, 32, 16, 16, 4, 4, 4, 4);
        public static final NineSliceTexture tabOn = sliced(mcSheet, 16, 32, 16, 16, 4, 4, 4, 4);
        public static final NineSliceTexture rectBlack = sliced(mcSheet, 32, 32, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture scrollerV = repeating(mcSheet, 0, 48, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture scrollerVDark = repeating(mcSheet, 16, 48, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture scrollerH = repeating(mcSheet, 32, 48, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture scrollerHDark = repeating(mcSheet, 48, 48, 16, 16, 2, 2, 2, 2);
        public static final NineSliceTexture switchTexture = sliced(mcSheet, 64, 48, 16, 16, 2, 2, 2, 2);

    }

    /** LDLib2 {@code Sprites} — 82 sprites at their upstream coordinates. */
    public static final class Gdp {

        private Gdp() {}

        public static final NineSliceTexture rectRd = sliced(gdpSheet, 1, 29, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectRdLight = sliced(gdpSheet, 1, 15, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectRdDark = sliced(gdpSheet, 1, 43, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectRdSolid = sliced(gdpSheet, 1, 1, 13, 13, 2, 2, 2, 2);
        public static final NineSliceTexture rectRdT = sliced(gdpSheet, 15, 29, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectRdTLight = sliced(gdpSheet, 15, 15, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectRdTDark = sliced(gdpSheet, 15, 43, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectRdTSolid = sliced(gdpSheet, 15, 1, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rect = sliced(gdpSheet, 29, 29, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectLight = sliced(gdpSheet, 29, 15, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectDark = sliced(gdpSheet, 29, 43, 13, 13, 4, 4, 4, 4);
        public static final NineSliceTexture rectSolid = sliced(gdpSheet, 29, 1, 13, 13, 1, 1, 1, 1);
        public static final NineSliceTexture border = sliced(gdpSheet, 86, 131, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderDark = sliced(gdpSheet, 86, 148, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderTranslate = sliced(gdpSheet, 86, 165, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture borderThick = sliced(gdpSheet, 171, 154, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture borderThickDark = sliced(gdpSheet, 171, 171, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture borderThickTranslate = sliced(gdpSheet, 171, 165, 16, 16, 4, 4, 4, 4);
        public static final NineSliceTexture borderRt0 = sliced(gdpSheet, 103, 131, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderRt0Dark = sliced(gdpSheet, 103, 148, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderRt0Translate = sliced(gdpSheet, 103, 165, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture borderThickRt0 = sliced(gdpSheet, 188, 154, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture borderThickRt0Dark = sliced(gdpSheet, 188, 171, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture borderThickRt0Translate = sliced(gdpSheet, 188, 165, 16, 16, 4, 4, 4, 4);
        public static final NineSliceTexture borderRt1 = sliced(gdpSheet, 120, 131, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderRt1Dark = sliced(gdpSheet, 120, 148, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderRt1Translate = sliced(gdpSheet, 120, 165, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture borderThickRt1 = sliced(gdpSheet, 205, 154, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture borderThickRt1Dark = sliced(gdpSheet, 205, 171, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture borderThickRt1Translate = sliced(gdpSheet, 205, 165, 16, 16, 4, 4, 4, 4);
        public static final NineSliceTexture borderRt2 = sliced(gdpSheet, 137, 131, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderRt2Dark = sliced(gdpSheet, 137, 148, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderRt2Translate = sliced(gdpSheet, 137, 165, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture borderThickRt2 = sliced(gdpSheet, 222, 154, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture borderThickRt2Dark = sliced(gdpSheet, 222, 171, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture borderThickRt2Translate = sliced(gdpSheet, 222, 165, 16, 16, 4, 4, 4, 4);
        public static final NineSliceTexture borderRt3 = sliced(gdpSheet, 154, 131, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderRt3Dark = sliced(gdpSheet, 154, 148, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture borderRt3Translate = sliced(gdpSheet, 154, 165, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture border1 = sliced(gdpSheet, 86, 205, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Dark = sliced(gdpSheet, 86, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Translate = sliced(gdpSheet, 86, 239, 16, 16, 3, 5, 3, 3);
        public static final NineSliceTexture border1Thick = sliced(gdpSheet, 171, 205, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickDark = sliced(gdpSheet, 171, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickTranslate = sliced(gdpSheet, 171, 239, 16, 16, 3, 5, 3, 3);
        public static final NineSliceTexture border1Rt0 = sliced(gdpSheet, 103, 205, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Rt0Dark = sliced(gdpSheet, 103, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Rt0Translate = sliced(gdpSheet, 103, 239, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture border1ThickRt0 = sliced(gdpSheet, 188, 205, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickRt0Dark = sliced(gdpSheet, 188, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickRt0Translate = sliced(gdpSheet, 188, 239, 16, 16, 3, 5, 3, 3);
        public static final NineSliceTexture border1Rt1 = sliced(gdpSheet, 120, 205, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture border1Rt1Dark = sliced(gdpSheet, 120, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Rt1Translate = sliced(gdpSheet, 120, 165, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture border1ThickRt1 = sliced(gdpSheet, 205, 205, 16, 16, 6, 6, 6, 6);
        public static final NineSliceTexture border1ThickRt1Dark = sliced(gdpSheet, 205, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickRt1Translate = sliced(gdpSheet, 205, 239, 16, 16, 3, 5, 3, 3);
        public static final NineSliceTexture border1Rt2 = sliced(gdpSheet, 137, 205, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Rt2Dark = sliced(gdpSheet, 137, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Rt2Translate = sliced(gdpSheet, 137, 239, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture border1ThickRt2 = sliced(gdpSheet, 222, 205, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickRt2Dark = sliced(gdpSheet, 222, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickRt2Translate = sliced(gdpSheet, 222, 239, 16, 16, 3, 5, 3, 3);
        public static final NineSliceTexture border1Rt3 = sliced(gdpSheet, 154, 205, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Rt3Dark = sliced(gdpSheet, 154, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1Rt3Translate = sliced(gdpSheet, 154, 239, 16, 16, 3, 3, 3, 3);
        public static final NineSliceTexture border1ThickRt3 = sliced(gdpSheet, 239, 205, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickRt3Dark = sliced(gdpSheet, 239, 222, 16, 16, 5, 5, 5, 5);
        public static final NineSliceTexture border1ThickRt3Translate = sliced(gdpSheet, 239, 239, 16, 16, 3, 5, 3, 3);
        public static final NineSliceTexture scrollContainerV = sliced(gdpSheet, 48, 198, 5, 7, 2, 2, 2, 2);
        public static final NineSliceTexture scrollBarV = sliced(gdpSheet, 48, 174, 5, 7, 2, 2, 2, 2);
        public static final NineSliceTexture scrollBarLightV = sliced(gdpSheet, 48, 182, 5, 7, 2, 2, 2, 2);
        public static final NineSliceTexture scrollBarWhiteV = sliced(gdpSheet, 48, 190, 5, 7, 2, 2, 2, 2);
        public static final NineSliceTexture scrollContainerH = sliced(gdpSheet, 55, 200, 7, 5, 2, 2, 2, 2);
        public static final NineSliceTexture scrollBarH = sliced(gdpSheet, 55, 182, 7, 5, 2, 2, 2, 2);
        public static final NineSliceTexture scrollBarLightH = sliced(gdpSheet, 55, 188, 7, 5, 2, 2, 2, 2);
        public static final NineSliceTexture scrollBarWhiteH = sliced(gdpSheet, 55, 194, 7, 5, 2, 2, 2, 2);
        public static final NineSliceTexture progressContainer = sliced(gdpSheet, 237, 130, 18, 11, 4, 4, 4, 4);
        public static final NineSliceTexture progressBar = sliced(gdpSheet, 241, 164, 10, 3, 1, 1, 1, 1);
        public static final NineSliceTexture tab = sliced(gdpSheet, 242, 85, 13, 13, 3, 3, 3, 3);
        public static final NineSliceTexture tabDark = sliced(gdpSheet, 242, 71, 13, 13, 3, 3, 3, 3);
        public static final NineSliceTexture tabWhite = sliced(gdpSheet, 242, 113, 13, 13, 3, 3, 3, 3);

    }

    /**
     * The harvest check/cross pair — the "can / cannot" marks an info panel puts next to
     * a requirement line, both 10×10 and tintable through the css {@code color(...)}
     * modifier so a theme can recolour them to its own ok/danger inks.
     * <p>
     * {@link #check} is LDLib2's own check, reused verbatim from the ore sheet (its
     * {@code ore:CHECK} region, {@code (50,35)}); {@link #cross} is the generated
     * companion on its own file. The cross is drawn so the two read as one set: the same
     * 10×10 box with a one-pixel margin, the same one-pixel stroke weight, and the one
     * ink the check uses — the eight opaque pixels of its {@code (50,35)-(60,45)} region
     * are all {@code #E3E3E5}, everything else transparent. The figure is two eight-pixel
     * diagonals crossing in the middle, emitted by a small script from that same ink
     * rather than drawn by hand, so it is reproducible pixel for pixel from the numbers
     * above.
     */
    public static final class Icon {

        private Icon() {}

        /** LDLib2's check — the ore sheet's {@code (50,35)} region, reused as the "can" mark. */
        public static final ImageTexture check = Ore.check;

        /** The generated cross — the "cannot" mark, on {@code textures/gui/cross.png}. */
        public static final ImageTexture cross = ImageTexture.of(crossImage, 10, 10);

    }

    // endregion

    // region registry

    /** {@code <ns>:<NAME>} to sprite; ids compare case-insensitively. */
    private static final Map<String, VisualTexture> registry = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        // ore — LDLib2 OreSprites
        register("ore", "BTN_DEFAULT", Ore.btnDefault);
        register("ore", "BTN_PRESSED", Ore.btnPressed);
        register("ore", "BTN_DISABLED", Ore.btnDisabled);
        register("ore", "BTN_DEFAULT_GREEN", Ore.btnDefaultGreen);
        register("ore", "BTN_PRESSED_GREEN", Ore.btnPressedGreen);
        register("ore", "BTN_DEFAULT_RED", Ore.btnDefaultRed);
        register("ore", "BTN_PRESSED_RED", Ore.btnPressedRed);
        register("ore", "BTN_DEFAULT_SMALL", Ore.btnDefaultSmall);
        register("ore", "BTN_HOVER_SMALL", Ore.btnHoverSmall);
        register("ore", "BTN_PRESSED_SMALL", Ore.btnPressedSmall);
        register("ore", "BTN_DISABLED_SMALL", Ore.btnDisabledSmall);
        register("ore", "BTN_DEFAULT_SMALL_GREEN", Ore.btnDefaultSmallGreen);
        register("ore", "BTN_HOVER_SMALL_GREEN", Ore.btnHoverSmallGreen);
        register("ore", "BTN_PRESSED_SMALL_GREEN", Ore.btnPressedSmallGreen);
        register("ore", "BTN_DEFAULT_SMALL_RED", Ore.btnDefaultSmallRed);
        register("ore", "BTN_HOVER_SMALL_RED", Ore.btnHoverSmallRed);
        register("ore", "BTN_PRESSED_SMALL_RED", Ore.btnPressedSmallRed);
        register("ore", "BTN_RECT_DEFAULT", Ore.btnRectDefault);
        register("ore", "BTN_RECT_DISABLED", Ore.btnRectDisabled);
        register("ore", "BTN_RECT_HOVER", Ore.btnRectHover);
        register("ore", "SLOT_LIGHT", Ore.slotLight);
        register("ore", "SLOT_GRAY", Ore.slotGray);
        register("ore", "RECT", Ore.rect);
        register("ore", "RECT2", Ore.rect2);
        register("ore", "WHITE_BORDER", Ore.whiteBorder);
        register("ore", "SWITCH_ON", Ore.switchOn);
        register("ore", "SWITCH_OFF", Ore.switchOff);
        register("ore", "TAB_OFF_DEFAULT", Ore.tabOffDefault);
        register("ore", "TAB_OFF_HOVER", Ore.tabOffHover);
        register("ore", "TAB_OFF_PRESSED", Ore.tabOffPressed);
        register("ore", "TAB_OFF_DISABLED", Ore.tabOffDisabled);
        register("ore", "TAB_ON_DEFAULT", Ore.tabOnDefault);
        register("ore", "TAB_ON_HOVER", Ore.tabOnHover);
        register("ore", "TAB_ON_PRESSED", Ore.tabOnPressed);
        register("ore", "TAB_ON_DISABLED", Ore.tabOnDisabled);
        register("ore", "TAB_OFF_DEFAULT_GREEN", Ore.tabOffDefaultGreen);
        register("ore", "TAB_OFF_HOVER_GREEN", Ore.tabOffHoverGreen);
        register("ore", "TAB_ON_DEFAULT_GREEN", Ore.tabOnDefaultGreen);
        register("ore", "TAB_ON_HOVER_GREEN", Ore.tabOnHoverGreen);
        register("ore", "BORDER", Ore.border);
        register("ore", "BORDER_2", Ore.border2);
        register("ore", "BORDER_3", Ore.border3);
        register("ore", "BORDER_4", Ore.border4);
        register("ore", "BORDER_5", Ore.border5);
        register("ore", "BORDER_6", Ore.border6);
        register("ore", "BORDER_7", Ore.border7);
        register("ore", "CHECK", Ore.check);
        register("ore", "DOWN", Ore.down);

        // mc — LDLib2 MCSprites
        register("mc", "RECT", Mc.rect);
        register("mc", "RECT_INVERSE", Mc.rectInverse);
        register("mc", "RECT_BORDER", Mc.rectBorder);
        register("mc", "RECT_THIN", Mc.rectThin);
        register("mc", "BORDER", Mc.border);
        register("mc", "BORDER_1", Mc.border1);
        register("mc", "BORDER_2", Mc.border2);
        register("mc", "BORDER_4", Mc.border4);
        register("mc", "RECT_1", Mc.rect1);
        register("mc", "RECT_2", Mc.rect2);
        register("mc", "RECT_3", Mc.rect3);
        register("mc", "RECT_4", Mc.rect4);
        register("mc", "RECT_5", Mc.rect5);
        register("mc", "RECT_6", Mc.rect6);
        register("mc", "TAB_OFF", Mc.tabOff);
        register("mc", "TAB_ON", Mc.tabOn);
        register("mc", "RECT_BLACK", Mc.rectBlack);
        register("mc", "SCROLLER_V", Mc.scrollerV);
        register("mc", "SCROLLER_V_DARK", Mc.scrollerVDark);
        register("mc", "SCROLLER_H", Mc.scrollerH);
        register("mc", "SCROLLER_H_DARK", Mc.scrollerHDark);
        register("mc", "SWITCH", Mc.switchTexture);

        // gdp — LDLib2 Sprites
        register("gdp", "RECT_RD", Gdp.rectRd);
        register("gdp", "RECT_RD_LIGHT", Gdp.rectRdLight);
        register("gdp", "RECT_RD_DARK", Gdp.rectRdDark);
        register("gdp", "RECT_RD_SOLID", Gdp.rectRdSolid);
        register("gdp", "RECT_RD_T", Gdp.rectRdT);
        register("gdp", "RECT_RD_T_LIGHT", Gdp.rectRdTLight);
        register("gdp", "RECT_RD_T_DARK", Gdp.rectRdTDark);
        register("gdp", "RECT_RD_T_SOLID", Gdp.rectRdTSolid);
        register("gdp", "RECT", Gdp.rect);
        register("gdp", "RECT_LIGHT", Gdp.rectLight);
        register("gdp", "RECT_DARK", Gdp.rectDark);
        register("gdp", "RECT_SOLID", Gdp.rectSolid);
        register("gdp", "BORDER", Gdp.border);
        register("gdp", "BORDER_DARK", Gdp.borderDark);
        register("gdp", "BORDER_TRANSLATE", Gdp.borderTranslate);
        register("gdp", "BORDER_THICK", Gdp.borderThick);
        register("gdp", "BORDER_THICK_DARK", Gdp.borderThickDark);
        register("gdp", "BORDER_THICK_TRANSLATE", Gdp.borderThickTranslate);
        register("gdp", "BORDER_RT0", Gdp.borderRt0);
        register("gdp", "BORDER_RT0_DARK", Gdp.borderRt0Dark);
        register("gdp", "BORDER_RT0_TRANSLATE", Gdp.borderRt0Translate);
        register("gdp", "BORDER_THICK_RT0", Gdp.borderThickRt0);
        register("gdp", "BORDER_THICK_RT0_DARK", Gdp.borderThickRt0Dark);
        register("gdp", "BORDER_THICK_RT0_TRANSLATE", Gdp.borderThickRt0Translate);
        register("gdp", "BORDER_RT1", Gdp.borderRt1);
        register("gdp", "BORDER_RT1_DARK", Gdp.borderRt1Dark);
        register("gdp", "BORDER_RT1_TRANSLATE", Gdp.borderRt1Translate);
        register("gdp", "BORDER_THICK_RT1", Gdp.borderThickRt1);
        register("gdp", "BORDER_THICK_RT1_DARK", Gdp.borderThickRt1Dark);
        register("gdp", "BORDER_THICK_RT1_TRANSLATE", Gdp.borderThickRt1Translate);
        register("gdp", "BORDER_RT2", Gdp.borderRt2);
        register("gdp", "BORDER_RT2_DARK", Gdp.borderRt2Dark);
        register("gdp", "BORDER_RT2_TRANSLATE", Gdp.borderRt2Translate);
        register("gdp", "BORDER_THICK_RT2", Gdp.borderThickRt2);
        register("gdp", "BORDER_THICK_RT2_DARK", Gdp.borderThickRt2Dark);
        register("gdp", "BORDER_THICK_RT2_TRANSLATE", Gdp.borderThickRt2Translate);
        register("gdp", "BORDER_RT3", Gdp.borderRt3);
        register("gdp", "BORDER_RT3_DARK", Gdp.borderRt3Dark);
        register("gdp", "BORDER_RT3_TRANSLATE", Gdp.borderRt3Translate);
        register("gdp", "BORDER1", Gdp.border1);
        register("gdp", "BORDER1_DARK", Gdp.border1Dark);
        register("gdp", "BORDER1_TRANSLATE", Gdp.border1Translate);
        register("gdp", "BORDER1_THICK", Gdp.border1Thick);
        register("gdp", "BORDER1_THICK_DARK", Gdp.border1ThickDark);
        register("gdp", "BORDER1_THICK_TRANSLATE", Gdp.border1ThickTranslate);
        register("gdp", "BORDER1_RT0", Gdp.border1Rt0);
        register("gdp", "BORDER1_RT0_DARK", Gdp.border1Rt0Dark);
        register("gdp", "BORDER1_RT0_TRANSLATE", Gdp.border1Rt0Translate);
        register("gdp", "BORDER1_THICK_RT0", Gdp.border1ThickRt0);
        register("gdp", "BORDER1_THICK_RT0_DARK", Gdp.border1ThickRt0Dark);
        register("gdp", "BORDER1_THICK_RT0_TRANSLATE", Gdp.border1ThickRt0Translate);
        register("gdp", "BORDER1_RT1", Gdp.border1Rt1);
        register("gdp", "BORDER1_RT1_DARK", Gdp.border1Rt1Dark);
        register("gdp", "BORDER1_RT1_TRANSLATE", Gdp.border1Rt1Translate);
        register("gdp", "BORDER1_THICK_RT1", Gdp.border1ThickRt1);
        register("gdp", "BORDER1_THICK_RT1_DARK", Gdp.border1ThickRt1Dark);
        register("gdp", "BORDER1_THICK_RT1_TRANSLATE", Gdp.border1ThickRt1Translate);
        register("gdp", "BORDER1_RT2", Gdp.border1Rt2);
        register("gdp", "BORDER1_RT2_DARK", Gdp.border1Rt2Dark);
        register("gdp", "BORDER1_RT2_TRANSLATE", Gdp.border1Rt2Translate);
        register("gdp", "BORDER1_THICK_RT2", Gdp.border1ThickRt2);
        register("gdp", "BORDER1_THICK_RT2_DARK", Gdp.border1ThickRt2Dark);
        register("gdp", "BORDER1_THICK_RT2_TRANSLATE", Gdp.border1ThickRt2Translate);
        register("gdp", "BORDER1_RT3", Gdp.border1Rt3);
        register("gdp", "BORDER1_RT3_DARK", Gdp.border1Rt3Dark);
        register("gdp", "BORDER1_RT3_TRANSLATE", Gdp.border1Rt3Translate);
        register("gdp", "BORDER1_THICK_RT3", Gdp.border1ThickRt3);
        register("gdp", "BORDER1_THICK_RT3_DARK", Gdp.border1ThickRt3Dark);
        register("gdp", "BORDER1_THICK_RT3_TRANSLATE", Gdp.border1ThickRt3Translate);
        register("gdp", "SCROLL_CONTAINER_V", Gdp.scrollContainerV);
        register("gdp", "SCROLL_BAR_V", Gdp.scrollBarV);
        register("gdp", "SCROLL_BAR_LIGHT_V", Gdp.scrollBarLightV);
        register("gdp", "SCROLL_BAR_WHITE_V", Gdp.scrollBarWhiteV);
        register("gdp", "SCROLL_CONTAINER_H", Gdp.scrollContainerH);
        register("gdp", "SCROLL_BAR_H", Gdp.scrollBarH);
        register("gdp", "SCROLL_BAR_LIGHT_H", Gdp.scrollBarLightH);
        register("gdp", "SCROLL_BAR_WHITE_H", Gdp.scrollBarWhiteH);
        register("gdp", "PROGRESS_CONTAINER", Gdp.progressContainer);
        register("gdp", "PROGRESS_BAR", Gdp.progressBar);
        register("gdp", "TAB", Gdp.tab);
        register("gdp", "TAB_DARK", Gdp.tabDark);
        register("gdp", "TAB_WHITE", Gdp.tabWhite);

        // icon — the harvest check/cross pair
        register("icon", "CHECK", Icon.check);
        register("icon", "CROSS", Icon.cross);

    }

    /** The sprite registered under {@code <ns>:<NAME>}, or {@code null} when unknown. */
    public static @Nullable VisualTexture get(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String key = id.trim();
        VisualTexture hit = registry.get(key);
        if (hit != null) {
            return hit;
        }
        // LDLib2 registers the tables as the ui-ore / ui-mc / ui-gdp providers
        return key.regionMatches(true, 0, "ui-", 0, 3) ? registry.get(key.substring(3)) : null;
    }

    /** Every registered id, in table order. */
    public static List<String> ids() {
        return List.copyOf(registry.keySet());
    }

    /** Every registered sprite, unmodifiable. */
    public static Map<String, VisualTexture> all() {
        return Collections.unmodifiableMap(registry);
    }

    // endregion

    // region helpers

    /**
     * An LDLib2 {@code SpriteTexture} with a border — {@code setSprite(u,v,w,h)} plus
     * {@code setBorder(left,top,right,bottom)}, so the numbers above read exactly like the
     * upstream definitions. LDLib2 orders the border arguments left/top/right/bottom while
     * {@link NineSliceTexture#region} takes left/right/top/bottom — the swap happens here.
     * <p>
     * The edges and the center stretch, LDLib2's {@code CLAMP} default.
     */
    private static NineSliceTexture sliced(
        ResourceLocation sheet,
        int u,
        int v,
        int w,
        int h,
        int left,
        int top,
        int right,
        int bottom
    ) {
        return NineSliceTexture.region(sheet, u, v, w, h, left, right, top, bottom)
                .withWrap(NineSliceTexture.Wrap.stretch);
    }

    /**
     * A {@link #sliced} sprite under LDLib2's other wrap mode — {@code REPEAT}, which
     * tiles the edges and the center across their span instead of stretching them.
     */
    private static NineSliceTexture repeating(
        ResourceLocation sheet,
        int u,
        int v,
        int w,
        int h,
        int left,
        int top,
        int right,
        int bottom
    ) {
        return NineSliceTexture.region(sheet, u, v, w, h, left, right, top, bottom)
                .withWrap(NineSliceTexture.Wrap.repeat);
    }

    /** An LDLib2 borderless {@code SpriteTexture} — a plain {@code setSprite(u,v,w,h)} region. */
    private static ImageTexture plain(ResourceLocation sheet, int u, int v, int w, int h) {
        return ImageTexture.region(sheet, u, v, w, h);
    }

    private static ResourceLocation sheet(String file) {
        return ResourceLocation.fromNamespaceAndPath(Constants.namespace, "textures/gui/oreui/" + file);
    }

    private static void register(String namespace, String name, VisualTexture texture) {
        registry.put(namespace + ":" + name, texture);
    }

    // endregion
}
