package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ProgressBarWidget's part structure: the pure trough/fill geometry per
 * direction, the {@code ::part(track|fill)} surface and the mounted layout —
 * including the supplier-driven fill that has to stay live without a relayout.
 */
class ProgressBarWidgetTest {

    // region geometry

    @Test
    void theFillGrowsFromTheDirectionsOrigin() {
        var ltr = ProgressBarWidget.Direction.leftToRight;
        var rtl = ProgressBarWidget.Direction.rightToLeft;
        var ttb = ProgressBarWidget.Direction.topToBottom;
        var btt = ProgressBarWidget.Direction.bottomToTop;

        assertEquals(new Rect(0, 0, 100, 10), ProgressBarWidget.trackBounds(100, 10));
        assertEquals(new Rect(0, 0, 25, 10), ProgressBarWidget.fillBounds(100, 10, 0.25, ltr));
        assertEquals(new Rect(75, 0, 25, 10), ProgressBarWidget.fillBounds(100, 10, 0.25, rtl));
        assertEquals(new Rect(0, 0, 100, 2), ProgressBarWidget.fillBounds(100, 10, 0.25, ttb));
        assertEquals(new Rect(0, 8, 100, 2), ProgressBarWidget.fillBounds(100, 10, 0.25, btt));
        // the ends: nothing at 0, the whole trough at 1
        assertEquals(new Rect(0, 0, 0, 10), ProgressBarWidget.fillBounds(100, 10, 0.0, ltr));
        assertEquals(new Rect(0, 0, 100, 10), ProgressBarWidget.fillBounds(100, 10, 1.0, ltr));
    }

    @Test
    void progressOutsideTheUnitRangeClamps() {
        var ltr = ProgressBarWidget.Direction.leftToRight;
        assertEquals(ProgressBarWidget.fillBounds(100, 10, 1.0, ltr), ProgressBarWidget.fillBounds(100, 10, 4.0, ltr));
        assertEquals(ProgressBarWidget.fillBounds(100, 10, 0.0, ltr), ProgressBarWidget.fillBounds(100, 10, -1.0, ltr));
        assertEquals(1.0, ProgressBarWidget.create(() -> 4.0).progress());
        assertEquals(0.0, ProgressBarWidget.create(() -> -1.0).progress());
    }

    // endregion

    // region parts

    @Test
    void theBarCarriesTrackAndFillParts() {
        ProgressBarWidget bar = ProgressBarWidget.create();
        assertEquals(List.of("track", "fill"), partNames(bar));
        for (Widget part : bar.children()) {
            assertEquals("progress-bar", part.styleTag(), "a part answers to the bar's tag");
            assertFalse(part.interactive(), "parts never take the pointer away from the bar");
        }
    }

    @Test
    void partSelectorsPaintTheParts() {
        ProgressBarWidget bar = ProgressBarWidget.create();
        Theme theme = theme("""
                progress-bar::part(fill) { background: color(#00FF00) }
                ::part(track) { background: color(#333333) }
                """);
        assertNotNull(background(theme, part(bar, "fill")), "progress-bar::part(fill)");
        assertNotNull(background(theme, part(bar, "track")), "the bare ::part(track) form");
        assertNull(background(theme, bar), "the sheet never paints the host itself");
    }

    // endregion

    // region layout

    @Test
    void thePartsLandOnTheBarGeometry() {
        try (WidgetTestScene fixture = new WidgetTestScene(120, 40)) {
            ProgressBarWidget bar = fixture.add(ProgressBarWidget.create(() -> 0.25));
            bar.useStyle(UIStyles.sizeOf(100, 10));
            fixture.stabilize();

            assertEquals(100, bar.width(), "the parts never grow the bar");
            assertEquals(new Rect(0, 0, 100, 10), part(bar, "track").bounds());
            assertEquals(new Rect(0, 0, 25, 10), part(bar, "fill").bounds());
        }
    }

    @Test
    void aSetterMovesTheFillOnTheNextLayoutPass() {
        try (WidgetTestScene fixture = new WidgetTestScene(120, 40)) {
            ProgressBarWidget bar = fixture.add(ProgressBarWidget.create(() -> 0.25));
            bar.useStyle(UIStyles.sizeOf(100, 10));
            fixture.stabilize();

            bar.setProgress(0.5);
            bar.setDirection(ProgressBarWidget.Direction.rightToLeft);
            fixture.stabilize();
            assertEquals(new Rect(50, 0, 50, 10), part(bar, "fill").bounds());
        }
    }

    @Test
    void aSupplierKeepsTheFillLiveWithoutARelayout() {
        try (WidgetTestScene fixture = new WidgetTestScene(120, 40)) {
            double[] progress = {0.1};
            ProgressBarWidget bar = fixture.add(ProgressBarWidget.create(() -> progress[0]));
            bar.useStyle(UIStyles.sizeOf(100, 10));
            fixture.stabilize();
            assertEquals(new Rect(0, 0, 10, 10), part(bar, "fill").bounds());

            // the supplier moved with no setter call and no layout input at all:
            // the rect is re-read (the render hook's body) before the canvas
            // translates to the part
            progress[0] = 0.8;
            WidgetPart.syncAll(bar);
            assertEquals(new Rect(0, 0, 80, 10), part(bar, "fill").bounds());
        }
    }

    // endregion

    // region fixture

    private static List<String> partNames(Widget owner) {
        return ((CompositeWidget<?>) owner).children().stream().map(Widget::stylePart).toList();
    }

    private static Widget part(Widget owner, String name) {
        return ((CompositeWidget<?>) owner).children().stream().filter(child -> name.equals(child.stylePart()))
                .findFirst().orElseThrow(() -> new AssertionError("no ::part(" + name + ") on the widget"));
    }

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "progress-bar"), CssParser.parse(css));
    }

    /** The winning {@code background} declaration for a node, or null when no rule painted it. */
    private static @Nullable StyleValue<?> background(Theme theme, Widget node) {
        var key = Styles.byId("background");
        return key == null ? null : theme.resolve(node).get(key);
    }

    // endregion
}
