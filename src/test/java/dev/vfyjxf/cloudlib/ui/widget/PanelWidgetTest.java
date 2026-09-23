package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.Textures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PanelWidget's theme-first background: a {@code panel { background }} rule (and
 * the class variants) paints the body, while {@code setBackgroundTexture} stays
 * the code texture a sheet that styles nothing leaves in place.
 */
class PanelWidgetTest {

    @BeforeAll
    static void boot() {
        Bootstrap.bootStrap();
    }

    @Test
    void theCodeTexturePaintsWhenTheSheetPaintsNothing() {
        PanelWidget panel = PanelWidget.create();
        assertSame(Textures.flat, panel.paintedBackground(), "the code texture, with no sheet in play");

        ColorTexture code = new ColorTexture(0xFF010203);
        panel.setBackgroundTexture(code);
        assertSame(code, panel.paintedBackground(), "and the setter still owns the fallback");
    }

    @Test
    void aPanelRulePaintsTheBody() {
        PanelWidget panel = PanelWidget.create();
        panel.setBackgroundTexture(new ColorTexture(0xFF010203));
        panel.applyThemeStyle(theme("panel { background: color(#123456) }").resolve(panel));

        assertEquals(0xFF123456, colorOf(panel.paintedBackground()), "panel { background } reaches the body");
    }

    @Test
    void theClassVariantsPaintTheBodyToo() {
        PanelWidget panel = PanelWidget.create();
        panel.addStyleClass("inset");
        panel.applyThemeStyle(theme("panel.inset { background: color(#ABCDEF) }").resolve(panel));

        assertEquals(0xFFABCDEF, colorOf(panel.paintedBackground()), "panel.inset is a body paint");
    }

    @Test
    void theStateVariantsPaintTheBodyToo() {
        PanelWidget panel = PanelWidget.create();
        panel.applyThemeStyle(theme("panel:focused { background: color(#FEDCBA) }").resolve(panel));

        // no focus in a headless scene, so the rule must not fire — the mechanism
        // the shipped `panel:focused` rule rides on is the same one, either way
        assertSame(Textures.flat, panel.paintedBackground(), "an unmatched state stays on the code texture");
    }

    @Test
    void theSheetStillOutranksTheCodeTexture() {
        PanelWidget panel = PanelWidget.create();
        panel.setBackgroundTexture(new ColorTexture(0xFF010203));
        panel.applyThemeStyle(theme("panel { background: color(#123456) }").resolve(panel));

        assertEquals(0xFF123456, colorOf(panel.paintedBackground()), "theme first");

        panel.applyThemeStyle(UIStyle.empty);
        assertEquals(0xFF010203, colorOf(panel.paintedBackground()), "the code texture as the fallback");
    }

    @Test
    void aLayoutOnlyRuleNeverBlanksTheBody() {
        PanelWidget panel = PanelWidget.create();
        panel.applyThemeStyle(theme("panel { padding: 6px }").resolve(panel));
        assertSame(Textures.flat, panel.paintedBackground(), "a sheet that paints nothing leaves the body alone");
    }

    // region fixture

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "panel"), CssParser.parse(css));
    }

    private static int colorOf(VisualTexture texture) {
        assertTrue(texture instanceof ColorTexture, "expected a color texture, got " + texture);
        return ((ColorTexture) texture).color();
    }

    // endregion
}
