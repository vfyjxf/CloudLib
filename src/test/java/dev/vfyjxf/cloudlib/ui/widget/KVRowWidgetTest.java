package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * KVRowWidget: the two labelled columns, the shell-set label width that aligns
 * a detail list, the {@code text-dim} label ink and the part surface.
 */
class KVRowWidgetTest {

    // region construction

    @Test
    void aRowTakesAComponentOrRichTextValue() {
        KVRowWidget plain = KVRowWidget.of("Health", Component.literal("20"));
        assertEquals("Health", plain.label().toComponent().getString());
        assertEquals("20", plain.value().toComponent().getString());

        RichText rich = RichText.builder().color(0xFFFF0000).text("20").build();
        assertSame(rich, KVRowWidget.of(Component.literal("Health"), rich).value());
        assertSame(rich, KVRowWidget.of("Health", rich).value());
    }

    @Test
    void theLabelAndTheValueCanBeReplaced() {
        KVRowWidget row = KVRowWidget.of("Health", "20");
        row.setLabel("Toughness");
        row.setValue(Component.literal("4"));
        assertEquals("Toughness", row.label().toComponent().getString());
        assertEquals("4", row.value().toComponent().getString());

        RichText rich = RichText.builder().text("Rich").build();
        row.setValue(rich);
        assertSame(rich, row.value());
        row.setLabel(rich);
        assertSame(rich, row.label());
    }

    @Test
    void theRowIsALabelAndAValue() {
        KVRowWidget row = KVRowWidget.of("Health", "20");
        assertEquals(List.of("label", "value"), partNames(row));
        for (Widget part : row.children()) {
            assertEquals("kv-row", part.styleTag(), "a part answers to the row's tag");
            assertFalse(part.interactive(), "parts never take the pointer away from the row");
        }
    }

    // endregion

    // region theme

    @Test
    void thePartSelectorsPaintTheColumns() {
        KVRowWidget row = KVRowWidget.of("Health", "20");
        Theme theme = theme("""
                kv-row::part(label) { color: #FF00FF }
                kv-row::part(value) { background: color(#202020) }
                """);
        assertNotNull(theme.resolve(part(row, "label")).get(Styles.color), "kv-row::part(label)");
        assertNotNull(background(theme, part(row, "value")), "kv-row::part(value)");
        assertNull(background(theme, row), "the sheet never paints the host itself");
    }

    @Test
    void theLabelInkFallsBackToTheTextDimSlot() {
        KVRowWidget row = KVRowWidget.of("Health", "20");
        TextPartNode label = (TextPartNode) part(row, "label");
        assertEquals(0xFFAAAAAA, label.inkColor(), "the code ink with no sheet in play");

        // the slot is inherited, so a sheet can dim the labels by dimming the panel
        label.applyThemeStyle(theme("kv-row { text-dim: #808080 }").resolve(label));
        assertEquals(0xFF808080, label.inkColor());

        // and an explicit color on the part outranks the slot
        label.applyThemeStyle(theme("kv-row::part(label) { color: #FF0000 }").resolve(label));
        assertEquals(0xFFFF0000, label.inkColor());
    }

    @Test
    void theValueInkStaysTheEmphasisedOne() {
        KVRowWidget row = KVRowWidget.of("Health", "20");
        TextPartNode value = (TextPartNode) part(row, "value");
        assertEquals(0xFFFFFFFF, value.inkColor());

        value.applyThemeStyle(theme("kv-row { text-dim: #808080 }").resolve(value));
        assertEquals(0xFFFFFFFF, value.inkColor(), "only the label reads the dim slot");
    }

    @Test
    void theValueIsTheColumnThatGives() {
        KVRowWidget row = KVRowWidget.of("Health", "20");
        assertEquals(1f, part(row, "value").style().layoutStyle().getFlexGrow());
        assertEquals(0f, part(row, "label").style().layoutStyle().getFlexGrow(), "the label keeps its own width");
    }

    // endregion

    // region layout

    @Test
    void aRowLaysItsTwoColumnsOutWithAGap() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            KVRowWidget row = fixture.add(KVRowWidget.of("Health", "20"));
            fixture.stabilize();

            Widget label = part(row, "label");
            Widget value = part(row, "value");
            assertEquals(36, label.width(), "'Health' is six 6px glyphs");
            assertEquals(9, label.height());
            assertEquals(label.posX() + label.width() + 4, value.posX(), "the label, the gap, then the value");
            assertEquals(12, value.width());
            assertEquals(52, row.width(), "the row is exactly what its columns need");
        }
    }

    @Test
    void theSameLabelWidthLinesTheValueColumnsUp() {
        try (WidgetTestScene fixture = new WidgetTestScene(300, 60)) {
            KVRowWidget shortLabel = fixture.add(KVRowWidget.of("Age", "20"));
            KVRowWidget longLabel = fixture.add(KVRowWidget.of("Toughness", "4"));
            fixture.stabilize();
            assertTrue(
                part(shortLabel, "value").posX() != part(longLabel, "value").posX(),
                "un-aligned rows start their values at their own label widths"
            );

            // the shell measures the widest label once and hands it to every row
            shortLabel.labelWidth(60);
            longLabel.labelWidth(60);
            fixture.stabilize();
            assertEquals(60, part(shortLabel, "label").width());
            assertEquals(
                part(shortLabel, "value").posX(),
                part(longLabel, "value").posX(),
                "one label width, one value column"
            );
            assertEquals(64, part(shortLabel, "value").posX());
        }
    }

    @Test
    void aLabelWidthOfZeroHandsTheColumnBackToTheText() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            KVRowWidget row = fixture.add(KVRowWidget.of("Health", "20").labelWidth(60));
            fixture.stabilize();
            assertEquals(60, part(row, "label").width());

            row.labelWidth(0);
            fixture.stabilize();
            assertEquals(36, part(row, "label").width(), "auto width again");
            assertEquals(40, part(row, "value").posX());
        }
    }

    @Test
    void aStretchedRowGivesTheFreeWidthToTheValue() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            KVRowWidget row = fixture.add(KVRowWidget.of("Health", "20"));
            row.useStyle(UIStyles.widthOf(200));
            fixture.stabilize();

            assertEquals(36, part(row, "label").width(), "the label stays at its text width");
            assertEquals(160, part(row, "value").width(), "the value takes the rest of the row");
        }
    }

    @Test
    void aThemedGapBeatsTheCodeGap() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            KVRowWidget row = fixture.add(KVRowWidget.of("Health", "20"));
            fixture.stabilize();
            assertEquals(40, part(row, "value").posX());

            row.applyThemeStyle(theme("kv-row { column-gap: 12px }").resolve(row));
            fixture.stabilize();
            assertEquals(48, part(row, "value").posX(), "the row's defaults sit below the theme");
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
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "kv-row"), CssParser.parse(css));
    }

    private static @Nullable StyleValue<?> background(Theme theme, Widget node) {
        var key = Styles.byId("background");
        return key == null ? null : theme.resolve(node).get(key);
    }

    // endregion
}
