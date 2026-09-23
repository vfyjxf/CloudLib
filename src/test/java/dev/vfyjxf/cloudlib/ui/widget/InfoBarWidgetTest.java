package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.api.ui.texture.BorderTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InfoBarWidget: the pure segment/mark geometry, the per-segment style classes
 * and the {@code ::part(track|fill|mark)} surface, the mounted layout including
 * the supplier-driven segment that stays live without a relayout, and the
 * theme-over-code height and textures.
 */
class InfoBarWidgetTest {

    // region geometry

    @Test
    void aSegmentRunsFromTheEndOfThePreviousOne() {
        assertEquals(new Rect(0, 0, 50, 10), InfoBarWidget.segmentBounds(100, 10, 0.0, 0.5));
        assertEquals(new Rect(50, 0, 25, 10), InfoBarWidget.segmentBounds(100, 10, 0.5, 0.25));
        // a stack that overruns the bar is clipped at the right edge
        assertEquals(new Rect(80, 0, 20, 10), InfoBarWidget.segmentBounds(100, 10, 0.8, 0.5));
        assertEquals(new Rect(100, 0, 0, 10), InfoBarWidget.segmentBounds(100, 10, 1.0, 0.5));
    }

    @Test
    void segmentsTileTheBarWithNoSeamAndNoOverlap() {
        List<Double> fractions = List.of(0.3, 0.2, 0.5);
        for (int width = 1; width <= 40; width++) {
            int end = 0;
            for (int i = 0; i < fractions.size(); i++) {
                Rect rect = InfoBarWidget.nthSegmentBounds(width, 4, fractions, i);
                assertEquals(end, rect.x(), "segment " + i + " starts where the last one ended, at width " + width);
                end = rect.right();
            }
            assertEquals(width, end, "the three segments tile the whole bar at width " + width);
        }
    }

    @Test
    void segmentsAccumulateTheirOffsets() {
        List<Double> fractions = List.of(0.5, 0.25, 0.25);
        assertEquals(new Rect(0, 0, 50, 8), InfoBarWidget.nthSegmentBounds(100, 8, fractions, 0));
        assertEquals(new Rect(50, 0, 25, 8), InfoBarWidget.nthSegmentBounds(100, 8, fractions, 1));
        assertEquals(new Rect(75, 0, 25, 8), InfoBarWidget.nthSegmentBounds(100, 8, fractions, 2));
        // out of range reads as an empty slice instead of throwing
        assertEquals(new Rect(100, 0, 0, 8), InfoBarWidget.nthSegmentBounds(100, 8, fractions, 3));
    }

    @Test
    void fractionsOutsideTheUnitRangeClamp() {
        assertEquals(0, InfoBarWidget.pixelAt(100, -1.0));
        assertEquals(100, InfoBarWidget.pixelAt(100, 4.0));
        assertEquals(96, InfoBarWidget.pixelAt(100, 0.96));
        assertEquals(0, InfoBarWidget.pixelAt(-10, 0.5), "a collapsed bar has no offsets");
        assertEquals(0.0, new InfoBarWidget.Segment(() -> -1.0, null).value());
        assertEquals(1.0, new InfoBarWidget.Segment(() -> 4.0, null).value());
    }

    @Test
    void aMarkIsAVerticalSlotCentredOnItsPosition() {
        assertEquals(new Rect(25, 0, 1, 10), InfoBarWidget.markBounds(100, 10, 0.25, 1));
        assertEquals(new Rect(24, 0, 2, 10), InfoBarWidget.markBounds(100, 10, 0.25, 2));
        // the ends stay inside the bar
        assertEquals(new Rect(0, 0, 1, 10), InfoBarWidget.markBounds(100, 10, 0.0, 1));
        assertEquals(new Rect(99, 0, 1, 10), InfoBarWidget.markBounds(100, 10, 1.0, 1));
        assertEquals(new Rect(0, 0, 0, 10), InfoBarWidget.markBounds(0, 10, 0.5, 1));
    }

    @Test
    void theContentBandIsTheBarInsideItsFrame() {
        assertEquals(new Rect(1, 1, 98, 8), InfoBarWidget.contentBounds(100, 10, 1));
        assertEquals(new Rect(2, 2, 96, 4), InfoBarWidget.contentBounds(100, 8, 2));
        assertEquals(new Rect(0, 0, 100, 10), InfoBarWidget.contentBounds(100, 10, 0), "no frame, no inset");
        // a bar too small to hold its own frame keeps a channel rather than a negative box
        assertEquals(new Rect(1, 1, 1, 0), InfoBarWidget.contentBounds(3, 2, 1));
        assertEquals(new Rect(0, 0, 1, 1), InfoBarWidget.contentBounds(1, 1, 1));
    }

    @Test
    void aSegmentIsLaidOutInsideTheBandAtItsFractionOfTheBand() {
        Rect content = InfoBarWidget.contentBounds(100, 10, 1);
        assertEquals(new Rect(1, 1, 49, 8), InfoBarWidget.segmentBounds(content, 0.0, 0.5, 0), "half of the 98px band");
        assertEquals(new Rect(50, 1, 24, 8), InfoBarWidget.segmentBounds(content, 0.5, 0.25, 0));
        // a full segment stops at the band's trailing edge, never at the bar's
        assertEquals(new Rect(1, 1, 98, 8), InfoBarWidget.segmentBounds(content, 0.0, 1.0, 0));
    }

    @Test
    void aSegmentsLeadGapIsTrimmedOffItsLeadingEdgeAlone() {
        Rect content = InfoBarWidget.contentBounds(100, 10, 1);
        // [49, 73) with the hairline in front of it: the segment keeps its trailing edge, so the stack
        // still ends exactly where the band does
        assertEquals(new Rect(51, 1, 23, 8), InfoBarWidget.segmentBounds(content, 0.5, 0.25, 1));
        // a segment narrower than its own hairline collapses instead of inverting
        assertEquals(new Rect(1, 1, 0, 8), InfoBarWidget.segmentBounds(content, 0.0, 0.01, 1));
    }

    @Test
    void theStackKeepsTilingTheBandWithTheHairlineTakenOut() {
        List<Double> fractions = List.of(0.3, 0.2, 0.5);
        for (int width = 4; width <= 64; width += 3) {
            Rect content = InfoBarWidget.contentBounds(width, 7, 1);
            int end = content.x();
            for (int i = 0; i < fractions.size(); i++) {
                Rect rect = InfoBarWidget.nthSegmentBounds(content, fractions, i, 1);
                String where = "segment " + i + " at width " + width;
                assertEquals(content.height(), rect.height(), where);
                assertTrue(rect.x() >= end, where + " overlaps the one before it");
                assertTrue(rect.x() - end <= 1, where + " leaves more than the hairline between them");
                if (rect.width() > 0 && i > 0) {
                    assertEquals(end + 1, rect.x(), where + " has room, so it takes its whole hairline");
                }
                assertTrue(rect.right() <= content.right(), where + " leaves the band");
                end = rect.right();
            }
            assertEquals(content.right(), end, "the three segments tile the band at width " + width);
        }
    }

    @Test
    void aMarkSitsInsideTheBandToo() {
        Rect content = InfoBarWidget.contentBounds(100, 10, 1);
        // the same quarter of the *band*, not of the bar: 24 px along a 98 px channel
        assertEquals(new Rect(25, 1, 1, 8), InfoBarWidget.markBounds(content, 0.25, 1));
        assertEquals(new Rect(1, 1, 1, 8), InfoBarWidget.markBounds(content, 0.0, 1), "the ends stay inside");
        assertEquals(new Rect(98, 1, 1, 8), InfoBarWidget.markBounds(content, 1.0, 1));
        assertEquals(
            new Rect(1, 1, 0, 0),
            InfoBarWidget.markBounds(InfoBarWidget.contentBounds(2, 2, 1), 0.5, 1),
            "a collapsed band has no slot to draw in"
        );
    }

    // endregion

    // region segments & marks

    @Test
    void aSingleSegmentBarIsTheCommonCase() {
        InfoBarWidget bar = InfoBarWidget.of(0.25);
        assertEquals(0.25, bar.fraction());
        assertEquals(1, parts(bar, "fill").size(), "one segment beside the trough");
        assertNull(bar.segments().getFirst().styleClass(), "a lone segment needs no semantic class");
    }

    @Test
    void aSupplierSegmentKeepsTheLiveFraction() {
        double[] fraction = {0.1};
        InfoBarWidget bar = InfoBarWidget.of(() -> fraction[0]);
        assertEquals(0.1, bar.fraction());
        fraction[0] = 0.75;
        assertEquals(0.75, bar.fraction(), "re-read per access, never cached");
    }

    @Test
    void segmentsCarryTheirSemanticClassAsARealChildClass() {
        InfoBarWidget bar = InfoBarWidget.segmented(
            InfoBarWidget.Segment.of(0.6, InfoBarWidget.styleOk),
            InfoBarWidget.Segment.of(0.2, InfoBarWidget.styleDanger)
        );
        List<Widget> fills = parts(bar, "fill");
        assertEquals(2, fills.size());
        assertTrue(fills.get(0).styleClasses().contains("ok"));
        assertTrue(fills.get(1).styleClasses().contains("danger"));
        assertEquals("info-bar", fills.get(0).styleTag(), "a fill answers to the bar's tag");
        assertFalse(fills.get(0).interactive(), "parts never take the pointer away from the bar");
    }

    @Test
    void replacingASegmentsClassSwapsItOnTheSamePart() {
        InfoBarWidget bar = InfoBarWidget.segmented(InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk));
        Widget fill = parts(bar, "fill").getFirst();
        bar.setSegments(List.of(InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleWarn)));
        assertTrue(bar.children().contains(fill), "the part is reused, not rebuilt");
        assertTrue(fill.styleClasses().contains("warn"));
        assertFalse(fill.styleClasses().contains("ok"), "the old class is gone");
    }

    @Test
    void segmentsAndMarksAreChildrenInDeclarationOrder() {
        InfoBarWidget bar = InfoBarWidget.segmented(
            InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk),
            InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleInfo)
        );
        bar.mark(0.25);
        bar.mark(0.75);
        assertEquals(List.of("track", "frame", "fill", "fill", "mark", "mark"), partNames(bar));
        assertEquals(List.of(0.25, 0.75), bar.marks());

        bar.clearMarks();
        assertEquals(List.of("track", "frame", "fill", "fill"), partNames(bar));
        assertEquals(List.of(), bar.marks());
    }

    @Test
    void paintOrderIsByZIndexSoALateSegmentStillPaintsUnderTheMarkers() {
        InfoBarWidget bar = InfoBarWidget.of(0.5);
        bar.mark(0.5);
        bar.addSegment(0.25, InfoBarWidget.styleInfo);

        assertEquals(2, parts(bar, "fill").size());
        assertTrue(part(bar, "track").zIndex() < part(bar, "fill").zIndex(), "the trough paints first");
        assertTrue(
            part(bar, "fill").zIndex() < part(bar, "mark").zIndex(),
            "the marker paints over every segment, however late it was declared"
        );
        assertTrue(
            part(bar, "mark").zIndex() < part(bar, "frame").zIndex(),
            "and the frame paints over everything, so no fill can cover its own outline"
        );
    }

    @Test
    void theFrameMetricsAreTheBarsOwnNumbers() {
        InfoBarWidget bar = InfoBarWidget.of(0.5);
        assertEquals(1, bar.frameThickness(), "one pixel of ring by default");
        assertEquals(1, bar.segmentGap(), "and one pixel between two readings");

        bar.setFrameThickness(2);
        assertEquals(2, bar.frameThickness());
        bar.setFrameThickness(9);
        assertEquals(3, bar.frameThickness(), "past 3px the frame eats the channel it outlines");
        bar.setFrameThickness(0);
        assertEquals(0, bar.frameThickness(), "0 turns the ring off");
        assertNull(textureOf(part(bar, "frame")), "…and takes the code ring away with it");

        bar.setSegmentGap(2);
        assertEquals(2, bar.segmentGap());
        bar.setSegmentGap(-4);
        assertEquals(0, bar.segmentGap(), "0 tiles the stack edge to edge");
        bar.setSegmentGap(9);
        assertEquals(3, bar.segmentGap());
    }

    @Test
    void theFrameIsTheFallbackRingUnderAnySheet() {
        InfoBarWidget bar = InfoBarWidget.of(0.5);
        VisualTexture ring = new ColorTexture(0xFF123456);
        assertTrue(
            bar.frameTexture() instanceof BorderTexture,
            "the code fallback is a real 1px ring, so even an unstyled bar is framed"
        );

        bar.setFrameTexture(ring);
        assertSame(ring, textureOf(part(bar, "frame")));
    }

    @Test
    void theMarkSlotWidthStaysBatchable() {
        InfoBarWidget bar = InfoBarWidget.of(1.0);
        assertEquals(1, bar.markThickness());
        bar.setMarkThickness(2);
        assertEquals(2, bar.markThickness());
        bar.setMarkThickness(9);
        assertEquals(2, bar.markThickness(), "never wider than the 2px slot a bar can afford");
        bar.setMarkThickness(0);
        assertEquals(1, bar.markThickness());
    }

    // endregion

    // region parts & theme

    @Test
    void theBarCarriesTrackFillAndMarkParts() {
        InfoBarWidget bar = InfoBarWidget.of(0.5);
        assertEquals(List.of("track", "frame", "fill"), partNames(bar));
        bar.mark(0.5);
        assertEquals(List.of("track", "frame", "fill", "mark"), partNames(bar));
        for (Widget part : bar.children()) {
            assertEquals("info-bar", part.styleTag(), "a part answers to the bar's tag");
        }
    }

    @Test
    void thePartSelectorsPaintTheParts() {
        InfoBarWidget bar = InfoBarWidget.segmented(InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk));
        bar.mark(0.5);
        Theme theme = theme("""
                info-bar::part(track) { background: color(#333333) }
                info-bar::part(frame) { background: border-texture(#111111, 1) }
                info-bar::part(fill)  { background: color(#808080) }
                ::part(mark)          { background: color(#FFFFFF) }
                """);
        assertNotNull(background(theme, part(bar, "track")), "info-bar::part(track)");
        assertNotNull(background(theme, part(bar, "frame")), "info-bar::part(frame)");
        assertNotNull(background(theme, part(bar, "fill")), "the base info-bar::part(fill) form");
        assertNotNull(background(theme, part(bar, "mark")), "the bare ::part(mark) form");
        assertNull(background(theme, bar), "the sheet never paints the host itself");
    }

    @Test
    void aSemanticClassOutranksTheBaseFillRule() {
        InfoBarWidget bar = InfoBarWidget.segmented(
            InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleDanger),
            InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk)
        );
        Theme theme = theme("""
                info-bar::part(fill)         { background: color(#808080) }
                info-bar .danger::part(fill) { background: color(#E05252) }
                info-bar .ok::part(fill)     { background: color(#6BCB77) }
                """);
        assertColor(0xFFE05252, background(theme, parts(bar, "fill").get(0)));
        assertColor(0xFF6BCB77, background(theme, parts(bar, "fill").get(1)));
    }

    @Test
    void theSegmentSlotIsTheSegmentsOwnClass() {
        InfoBarWidget bar = InfoBarWidget.segmented(
            InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk),
            InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleDanger)
        );
        Theme theme = theme("info-bar .ok::part(fill) { background: color(#6BCB77) }");
        assertNotNull(background(theme, parts(bar, "fill").get(0)), ".ok hits the first segment");
        assertNull(background(theme, parts(bar, "fill").get(1)), "and nothing else");
    }

    @Test
    void theCodeTextureIsTheFallbackOfEveryPart() {
        InfoBarWidget bar = InfoBarWidget.segmented(InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk));
        bar.mark(0.5);
        ColorTexture track = new ColorTexture(0xFF010203);
        ColorTexture fill = new ColorTexture(0xFF040506);
        ColorTexture mark = new ColorTexture(0xFF070809);
        ColorTexture frame = new ColorTexture(0xFF0A0B0C);
        bar.setTrackTexture(track);
        bar.setFillTexture(fill);
        bar.setMarkTexture(mark);
        bar.setFrameTexture(frame);

        assertSame(track, textureOf(part(bar, "track")), "a bar with no sheet paints its code textures");
        assertSame(fill, textureOf(part(bar, "fill")));
        assertSame(mark, textureOf(part(bar, "mark")));
        assertSame(frame, textureOf(part(bar, "frame")));

        Theme theme = theme("""
                info-bar::part(track)    { background: color(#111111) }
                info-bar .ok::part(fill) { background: color(#222222) }
                info-bar::part(mark)     { background: color(#333333) }
                info-bar::part(frame)    { background: border-texture(#444444, 1) }
                """);
        for (Widget part : bar.children()) {
            part.applyThemeStyle(theme.resolve(part));
        }
        assertEquals(0xFF111111, colorOf(textureOf(part(bar, "track"))), "the sheet beats the code trough");
        assertEquals(0xFF222222, colorOf(textureOf(part(bar, "fill"))), "and the segment's own class beats it");
        assertEquals(0xFF333333, colorOf(textureOf(part(bar, "mark"))));
        assertTrue(textureOf(part(bar, "frame")) instanceof BorderTexture, "and the sheet's ring beats the code ring");
    }

    // endregion

    // region layout

    @Test
    void theSegmentsLandOnTheirFractions() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            InfoBarWidget bar = fixture.add(
                InfoBarWidget.segmented(
                    InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk),
                    InfoBarWidget.Segment.of(0.25, InfoBarWidget.styleDanger)
                )
            );
            bar.useStyle(UIStyles.sizeOf(100, 10));
            bar.mark(0.25);
            fixture.stabilize();

            assertEquals(100, bar.width(), "the parts never grow the bar");
            assertEquals(10, bar.height());
            assertEquals(new Rect(0, 0, 100, 10), part(bar, "track").bounds(), "the trough is the whole bar");
            assertEquals(new Rect(0, 0, 100, 10), part(bar, "frame").bounds(), "and the ring is drawn over all of it");
            // the fills and the marker live in the 1px-inset band, with the hairline taken off the second
            // segment's leading edge
            assertEquals(new Rect(1, 1, 49, 8), parts(bar, "fill").get(0).bounds());
            assertEquals(new Rect(51, 1, 23, 8), parts(bar, "fill").get(1).bounds());
            assertEquals(new Rect(25, 1, 1, 8), part(bar, "mark").bounds());
        }
    }

    @Test
    void aSupplierMovesASegmentWithNoRelayout() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            double[] absorbed = {0.1};
            InfoBarWidget bar = fixture.add(
                InfoBarWidget.segmented(
                    InfoBarWidget.Segment.of(0.5, InfoBarWidget.styleOk),
                    InfoBarWidget.Segment.of(() -> absorbed[0], InfoBarWidget.styleInfo)
                )
            );
            bar.useStyle(UIStyles.sizeOf(100, 10));
            fixture.stabilize();
            assertEquals(new Rect(51, 1, 8, 8), parts(bar, "fill").get(1).bounds());

            // the supplier moved with no setter call and no layout input at all:
            // the rects are re-read (the render hook's body) before the canvas
            // translates to the parts
            absorbed[0] = 0.4;
            WidgetPart.syncAll(bar);
            assertEquals(new Rect(51, 1, 38, 8), parts(bar, "fill").get(1).bounds());
        }
    }

    @Test
    void aMarkAddedLaterLandsOnTheCurrentWidth() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            InfoBarWidget bar = fixture.add(InfoBarWidget.of(0.5));
            bar.useStyle(UIStyles.sizeOf(100, 10));
            fixture.stabilize();

            bar.mark(0.75);
            fixture.stabilize();
            assertEquals(new Rect(74, 1, 1, 8), part(bar, "mark").bounds());
        }
    }

    @Test
    void theBarDeclaresItsFallbackHeightBelowTheTheme() {
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            InfoBarWidget bar = fixture.add(InfoBarWidget.of(0.5));
            bar.useStyle(UIStyles.widthOf(100));
            fixture.stabilize();
            assertEquals(7, bar.height(), "the code fallback — 5px of channel inside a 1px frame");

            // a sheet that owns the height wins over the declared fallback: the
            // parts make this a taffy container, so the height has to arrive
            // through the style, never through a measure function
            bar.applyThemeStyle(theme("info-bar { height: 9px }").resolve(bar));
            fixture.stabilize();
            assertEquals(9, bar.height(), "info-bar { height: … }");
            assertEquals(9, part(bar, "track").height(), "and the parts follow the bar");
        }
    }

    @Test
    void theSheetsRowGapIsRealEstateTheBarKeeps() {
        // `info-bar { margin: var(--bar-row-gap) 0 }` is the air the shipped sheets put between a bar and
        // the text line beside it — it has to move the bar, not just read well in the sheet
        try (WidgetTestScene fixture = new WidgetTestScene(220, 60)) {
            InfoBarWidget first = fixture.add(InfoBarWidget.of(0.5));
            InfoBarWidget second = fixture.add(InfoBarWidget.of(0.5));
            first.useStyle(UIStyles.widthOf(100));
            second.useStyle(UIStyles.widthOf(100));
            fixture.stabilize();
            int beforeX = second.bounds().x();
            int beforeY = second.bounds().y();

            second.applyThemeStyle(theme("info-bar { margin: 3px 0 }").resolve(second));
            fixture.stabilize();
            assertEquals(beforeY + 3, second.bounds().y(), "the vertical gap is real estate the bar owns");
            assertEquals(beforeX, second.bounds().x(), "…and only vertical: the row's columns keep their places");
        }
    }

    // endregion

    // region fixture

    private static List<String> partNames(Widget owner) {
        return ((CompositeWidget<?>) owner).children().stream().map(Widget::stylePart).toList();
    }

    private static List<Widget> parts(Widget owner, String name) {
        List<Widget> out = new ArrayList<>();
        for (Widget child : ((CompositeWidget<?>) owner).children()) {
            if (name.equals(child.stylePart())) {
                out.add(child);
            }
        }
        return out;
    }

    private static Widget part(Widget owner, String name) {
        return parts(owner, name).stream().findFirst()
                .orElseThrow(() -> new AssertionError("no ::part(" + name + ") on the widget"));
    }

    /** The texture a part paints — themed when the sheet painted it, the code texture otherwise. */
    private static @Nullable VisualTexture textureOf(Widget part) {
        return ((WidgetPart) part).texture();
    }

    private static int colorOf(@Nullable VisualTexture texture) {
        assertTrue(texture instanceof ColorTexture, "expected a color texture, got " + texture);
        return ((ColorTexture) Objects.requireNonNull(texture)).color();
    }

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "info-bar"), CssParser.parse(css));
    }

    /** The winning {@code background} declaration for a node, or null when no rule painted it. */
    private static @Nullable StyleValue<?> background(Theme theme, Widget node) {
        var key = Styles.byId("background");
        return key == null ? null : theme.resolve(node).get(key);
    }

    private static void assertColor(int expected, @Nullable StyleValue<?> value) {
        assertNotNull(value, "expected a resolved background");
        assertTrue(value.value() instanceof ColorTexture, "expected a color texture, got " + value.value());
        assertEquals(expected, ((ColorTexture) value.value()).color());
    }

    // endregion
}
