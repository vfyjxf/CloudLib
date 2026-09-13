package dev.vfyjxf.nimbusprojection;

import dev.vfyjxf.cloudlib.api.css.CssError;
import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.css.Stylesheet;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.theme.Theme;
import dev.vfyjxf.cloudlib.api.ui.theme.ThemeEngine;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dogfoods the bundled in-world themes: every file must parse clean, every
 * declared tag must resolve to a non-empty style, and {@code text-shadow}
 * must resolve to {@code false} — in-world text never renders a drop shadow.
 */
class NimbusThemesTest {

    private static final String[] files = {"dark.css", "light.css", "holo.css"};
    private static final String[] tags = {
        "panel", "label", "button", "item-slot", "divider", "chip", "progress-bar", "scrollbar", "entity-tag"
    };

    static class Node extends Widget {
        private final String tag;

        Node(String tag) {
            this.tag = tag;
        }

        @Override
        public String styleTag() {
            return tag;
        }
    }

    @Test
    void allThemesParseCleanAndResolve() {
        for (String file : files) {
            Theme theme = load(file);
            for (String tag : tags) {
                UIStyle s = ThemeEngine.resolve(theme, new Node(tag));
                assertFalse(s.isEmpty(), () -> file + ": no properties resolved for <" + tag + ">");
            }
        }
    }

    @Test
    void inworldPanelColorsResolve() {
        Theme theme = load("light.css");
        Node w = new Node("inworld-panel");
        UIStyle s = ThemeEngine.resolve(theme, w);
        w.useStyle(s);
        var ctx = w.style();
        var vc = ctx.visualContext();
        System.out.println("light inworld-panel -> text="
                + (vc.textColor() == null ? "null" : String.format("0x%08X", vc.textColor()))
                + " bg="
                + vc.background() + " border="
                + vc.getProperty("border-texture", dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture.class) + " accent="
                + (ctx.get(Styles.accent) == null ? "null" : String.format("0x%08X", ctx.get(Styles.accent))));
        assertNotNull(vc.textColor(), "inworld-panel must resolve color");
    }

    @Test
    void inWorldTextIsNeverShadowed() {
        for (String file : files) {
            Theme theme = load(file);
            Node label = new Node("label");
            UIStyle s = ThemeEngine.resolve(theme, label);
            assertEquals(
                    Boolean.FALSE,
                    s.get(Styles.textShadow) == null
                            ? null
                            : s.get(Styles.textShadow).value(),
                    () -> file + " must set text-shadow: none at :root");
            // and the widget read path agrees
            label.useStyle(s);
            assertFalse(new LabelShadowProbe(label).effective());
        }
    }

    /** Reads the styled label's effective shadow the same way LabelWidget does. */
    private static final class LabelShadowProbe {
        private final Widget w;

        LabelShadowProbe(Widget w) {
            this.w = w;
        }

        boolean effective() {
            Boolean v = w.style().get(Styles.textShadow);
            return v != null ? v : true;
        }
    }

    private static Theme load(String file) {
        String path = "/assets/nimbusprojection/ui/themes/" + file;
        var in = NimbusThemesTest.class.getResourceAsStream(path);
        assertNotNull(in, path + " missing from classpath");
        String css;
        try {
            css = new String(in.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<CssError> errors = new ArrayList<>();
        Stylesheet sheet = CssParser.parse(css, errors);
        assertTrue(errors.isEmpty(), () -> file + " parse errors: " + errors);
        assertFalse(sheet.rules().isEmpty());
        return new Theme(ResourceLocation.fromNamespaceAndPath("nimbusprojection", file.replace(".css", "")), sheet);
    }
}
