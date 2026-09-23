package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * SectionHeaderWidget: the title/rule part surface, the theme-resolved ink and
 * font size, and the mounted row — the rule takes whatever width the title
 * leaves.
 */
class SectionHeaderWidgetTest {

    // region construction

    @Test
    void theTitleTakesAComponentOrRichText() {
        assertEquals("Damage", SectionHeaderWidget.of("Damage").title().toComponent().getString());
        assertEquals("Damage", SectionHeaderWidget.of(Component.literal("Damage")).title().toComponent().getString());
        RichText rich = RichText.builder().color(0xFFFF0000).text("Damage").build();
        assertNotNull(SectionHeaderWidget.of(rich).title());

        SectionHeaderWidget header = SectionHeaderWidget.of("Old");
        header.setTitle("New");
        assertEquals("New", header.title().toComponent().getString());
        header.setTitle(Component.literal("Newest"));
        assertEquals("Newest", header.title().toComponent().getString());
        header.setTitle(rich);
        assertEquals(rich, header.title());
    }

    @Test
    void theHeaderIsATitleAndARule() {
        SectionHeaderWidget header = SectionHeaderWidget.of("Damage");
        assertEquals(List.of("title", "rule"), partNames(header));
        for (Widget part : header.children()) {
            assertEquals("section-header", part.styleTag(), "a part answers to the header's tag");
            assertFalse(part.interactive(), "parts never take the pointer away from the header");
        }
    }

    // endregion

    // region theme

    @Test
    void thePartSelectorsPaintTheParts() {
        SectionHeaderWidget header = SectionHeaderWidget.of("Damage");
        Theme theme = theme("""
                section-header::part(title) { color: color(#FF00FF) }
                section-header::part(rule)  { background: color(#333333) }
                """);
        assertNotNull(color(theme, part(header, "title")), "section-header::part(title)");
        assertNotNull(background(theme, part(header, "rule")), "section-header::part(rule)");
        assertNull(background(theme, header), "the sheet never paints the host itself");
    }

    @Test
    void theThemedInkWinsOverTheCodeColor() {
        SectionHeaderWidget header = SectionHeaderWidget.of("Damage");
        TextPartNode title = (TextPartNode) part(header, "title");
        assertEquals(0xFFFFFFFF, title.inkColor(), "the code ink with no sheet in play");

        title.applyThemeStyle(theme("section-header::part(title) { color: #FF00FF }").resolve(title));
        assertEquals(0xFFFF00FF, title.inkColor());

        header.setTitleColor(0xFF123456);
        assertEquals(0xFFFF00FF, title.inkColor(), "an explicit code color does not outrank the sheet");

        title.applyThemeStyle(theme("").resolve(title));
        assertEquals(0xFF123456, title.inkColor(), "and the code color is what is left without one");
    }

    @Test
    void theRuleFallsBackToTheCodeLine() {
        SectionHeaderWidget header = SectionHeaderWidget.of("Damage");
        PartNode rule = (PartNode) part(header, "rule");
        assertNotNull(rule.themedBackground(header.ruleTexture()), "the code line paints when the sheet does not");

        rule.applyThemeStyle(theme("section-header::part(rule) { background: color(#202020) }").resolve(rule));
        var themed = rule.themedBackground(header.ruleTexture());
        assertNotNull(themed);
        assertEquals(0xFF202020, ((ColorTexture) themed).color());
    }

    // endregion

    // region layout

    @Test
    void theRuleTakesTheWidthTheTitleLeaves() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            SectionHeaderWidget header = fixture.add(SectionHeaderWidget.of("Damage"));
            header.useStyle(UIStyles.widthOf(200));
            fixture.stabilize();

            Widget title = part(header, "title");
            Widget rule = part(header, "rule");
            assertEquals(200, header.width());
            assertEquals(0, title.posX());
            assertEquals(36, title.width(), "'Damage' is six 6px glyphs");
            assertEquals(9, title.height(), "on the font's 9px line");
            assertEquals(40, rule.posX(), "title + the 4px gap");
            assertEquals(160, rule.width(), "the rule takes the width the title left");
            assertEquals(1, rule.height());
            assertEquals(title.posY() + 4, rule.posY(), "and centres both parts");
        }
    }

    @Test
    void theGapAndTheRuleThicknessAreCodeDefaults() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            SectionHeaderWidget header = fixture.add(SectionHeaderWidget.of("Damage").setGap(10).setRuleThickness(3));
            header.useStyle(UIStyles.widthOf(200));
            fixture.stabilize();

            Widget title = part(header, "title");
            Widget rule = part(header, "rule");
            assertEquals(46, rule.posX());
            assertEquals(154, rule.width());
            assertEquals(3, rule.height());
            assertEquals(title.posY() + 3, rule.posY());
        }
    }

    @Test
    void aThemedFontSizeScalesTheTitle() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            SectionHeaderWidget header = fixture.add(SectionHeaderWidget.of("Damage"));
            header.useStyle(UIStyles.widthOf(200));
            fixture.stabilize();
            TextPartNode title = (TextPartNode) part(header, "title");
            assertEquals(36, title.width());
            assertEquals(9, title.height());

            // a sheet that doubles the ink size doubles both the glyphs and the row
            title.applyThemeStyle(theme("section-header::part(title) { font-size: 18px }").resolve(title));
            fixture.stabilize();
            assertEquals(2f, title.fontScale());
            assertEquals(72, title.width(), "font-size is read against the font's line height");
            assertEquals(18, title.height());
            assertEquals(76, part(header, "rule").posX(), "the row re-flows around the bigger title");
            assertEquals(124, part(header, "rule").width());
        }
    }

    @Test
    void aThemedColumnGapBeatsTheCodeGap() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            SectionHeaderWidget header = fixture.add(SectionHeaderWidget.of("Damage"));
            header.useStyle(UIStyles.widthOf(200));
            fixture.stabilize();
            assertEquals(40, part(header, "rule").posX());

            header.applyThemeStyle(theme("section-header { column-gap: 12px }").resolve(header));
            fixture.stabilize();
            assertEquals(48, part(header, "rule").posX(), "the host's defaults sit below the theme");
        }
    }

    @Test
    void theRuleNeverFallsBelowItsMinimum() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            SectionHeaderWidget header = fixture.add(SectionHeaderWidget.of("A very long heading indeed"));
            fixture.stabilize();
            assertEquals(12, part(header, "rule").width(), "the code minimum keeps a stub of line");

            header.setRuleMinWidth(0);
            fixture.stabilize();
            assertEquals(0, part(header, "rule").width(), "and the shell can hand the row to the title");
        }
    }

    // endregion

    // region fixture

    private static List<String> partNames(Widget owner) {
        return ((CompositeWidget<?>) owner).children().stream().map(Widget::stylePart).toList();
    }

    private static Widget part(Widget owner, String name) {
        for (Widget child : ((CompositeWidget<?>) owner).children()) {
            if (name.equals(child.stylePart())) {
                return child;
            }
        }
        throw new AssertionError("no ::part(" + name + ") on the widget");
    }

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "section-header"), CssParser.parse(css));
    }

    private static @Nullable StyleValue<?> background(Theme theme, Widget node) {
        var key = Styles.byId("background");
        return key == null ? null : theme.resolve(node).get(key);
    }

    private static @Nullable StyleValue<?> color(Theme theme, Widget node) {
        return theme.resolve(node).get(Styles.color);
    }

    // endregion
}
