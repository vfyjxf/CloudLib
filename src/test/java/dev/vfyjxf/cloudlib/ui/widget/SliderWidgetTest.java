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
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SliderWidget's part structure: the pure groove/fill/handle geometry, the
 * {@code ::part(track|fill|handle)} surface and the mounted layout the parts
 * land on.
 */
class SliderWidgetTest {

    // region geometry

    @Test
    void horizontalPartsRideTheEndsOfTheGroove() {
        var h = SliderWidget.Orientation.horizontal;
        assertEquals(new Rect(4, 6, 92, 4), SliderWidget.trackBounds(100, 16, 8, h));
        assertEquals(new Rect(4, 6, 46, 4), SliderWidget.fillBounds(100, 16, 8, 0.5, h));
        assertEquals(new Rect(46, 4, 8, 8), SliderWidget.handleBounds(100, 16, 8, 0.5, h));
        // the ends of the range: the fill is empty at 0 and full at 1, the handle flush with them
        assertEquals(new Rect(4, 6, 0, 4), SliderWidget.fillBounds(100, 16, 8, 0.0, h));
        assertEquals(new Rect(4, 6, 92, 4), SliderWidget.fillBounds(100, 16, 8, 1.0, h));
        assertEquals(new Rect(0, 4, 8, 8), SliderWidget.handleBounds(100, 16, 8, 0.0, h));
        assertEquals(new Rect(92, 4, 8, 8), SliderWidget.handleBounds(100, 16, 8, 1.0, h));
    }

    @Test
    void verticalPartsGrowUpward() {
        var v = SliderWidget.Orientation.vertical;
        assertEquals(new Rect(6, 4, 4, 92), SliderWidget.trackBounds(16, 100, 8, v));
        assertEquals(new Rect(6, 50, 4, 46), SliderWidget.fillBounds(16, 100, 8, 0.5, v));
        assertEquals(new Rect(4, 46, 8, 8), SliderWidget.handleBounds(16, 100, 8, 0.5, v));
        // full value fills to the top and parks the handle on the top edge
        assertEquals(new Rect(6, 4, 4, 92), SliderWidget.fillBounds(16, 100, 8, 1.0, v));
        assertEquals(new Rect(4, 0, 8, 8), SliderWidget.handleBounds(16, 100, 8, 1.0, v));
        assertEquals(new Rect(4, 92, 8, 8), SliderWidget.handleBounds(16, 100, 8, 0.0, v));
    }

    @Test
    void ratiosOutsideTheRangeClamp() {
        var h = SliderWidget.Orientation.horizontal;
        assertEquals(SliderWidget.fillBounds(100, 16, 8, 1.0, h), SliderWidget.fillBounds(100, 16, 8, 4.0, h));
        assertEquals(SliderWidget.handleBounds(100, 16, 8, 0.0, h), SliderWidget.handleBounds(100, 16, 8, -1.0, h));
    }

    @Test
    void valueMapsToARatioAndAnEmptyRangeIsNotNaN() {
        assertEquals(0.5, SliderWidget.create(0, 10, 5).ratio());
        assertEquals(1.0, SliderWidget.create(0, 10, 20).ratio(), "the value clamps into the range");
        assertEquals(0.0, SliderWidget.create(4, 4).ratio(), "an empty range sits at zero, never NaN");
    }

    // endregion

    // region parts

    @Test
    void theSliderCarriesTrackFillAndHandleParts() {
        SliderWidget slider = SliderWidget.create(0, 10, 5);
        assertEquals(List.of("track", "fill", "handle"), partNames(slider));
        for (Widget part : slider.children()) {
            assertEquals("slider", part.styleTag(), "a part answers to the slider's tag");
            assertFalse(part.interactive(), "parts never take the pointer away from the slider");
        }
    }

    @Test
    void partSelectorsPaintTheParts() {
        SliderWidget slider = SliderWidget.create(0, 10, 5);
        Theme theme = theme("""
                slider::part(track) { background: color(#102030) }
                ::part(handle) { background: color(#405060) }
                """);
        assertNotNull(background(theme, part(slider, "track")), "slider::part(track)");
        assertNotNull(background(theme, part(slider, "handle")), "the bare ::part(handle) form");
        assertNull(background(theme, part(slider, "fill")), "a part the sheet never names stays unpainted");
    }

    @Test
    void aHoveredSliderMatchesItsPartsHoverState() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            SliderWidget slider = fixture.add(SliderWidget.create(0, 10, 5));
            slider.useStyle(UIStyles.sizeOf(80, 16));
            fixture.stabilize();

            fixture.scene.mouseMoved(4, 4);
            assertTrue(slider.hovered(), "the pointer sits on the slider");
            assertTrue(part(slider, "handle").styleStates().contains("hover"), "the part mirrors :hover");
            assertNotNull(
                background(theme("slider:hover::part(handle) { background: color(#FFFFFF) }"), part(slider, "handle")),
                "slider:hover::part(handle)"
            );
        }
    }

    @Test
    void aPressLightsTheHandleThroughPressed() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            SliderWidget slider = fixture.add(SliderWidget.create(0, 10, 5));
            slider.useStyle(UIStyles.sizeOf(80, 16));
            fixture.stabilize();
            assertFalse(slider.styleStates().contains("pressed"));

            fixture.scene.mouseClicked(40, 8, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            Widget handle = part(slider, "handle");
            assertTrue(slider.styleStates().contains("pressed"), "a press is a pressed slider");
            assertTrue(handle.styleStates().contains("pressed"), "the handle mirrors it");
            assertNotNull(
                background(theme("slider::part(handle):pressed { background: color(#101010) }"), handle),
                "slider::part(handle):pressed"
            );

            fixture.scene.mouseReleased(40, 8, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            assertFalse(slider.styleStates().contains("pressed"), "the release clears it");
        }
    }

    // endregion

    // region theme metrics

    @Test
    void themeMetricsDriveThePartGeometry() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            SliderWidget slider = fixture.add(SliderWidget.create(0, 10, 5));
            slider.useStyle(UIStyles.sizeOf(80, 16));
            fixture.stabilize();

            slider.applyThemeStyle(theme("slider { slider-track-size: 3px; slider-handle-size: 5px }").resolve(slider));
            fixture.stabilize();

            var h = SliderWidget.Orientation.horizontal;
            assertEquals(SliderWidget.trackBounds(80, 16, 5, 3, h), part(slider, "track").bounds());
            assertEquals(SliderWidget.fillBounds(80, 16, 5, 3, 0.5, h), part(slider, "fill").bounds());
            assertEquals(SliderWidget.handleBounds(80, 16, 5, 0.5, h), part(slider, "handle").bounds());
        }
    }

    @Test
    void theCodeMetricsStandWhenTheSheetDeclaresNone() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            SliderWidget slider = fixture.add(SliderWidget.create(0, 10, 5));
            slider.useStyle(UIStyles.sizeOf(80, 16));
            fixture.stabilize();

            var h = SliderWidget.Orientation.horizontal;
            assertEquals(SliderWidget.trackBounds(80, 16, 8, h), part(slider, "track").bounds());
            slider.setThumbSize(12);
            fixture.stabilize();
            assertEquals(SliderWidget.handleBounds(80, 16, 12, 0.5, h), part(slider, "handle").bounds());
        }
    }

    // endregion

    // region layout

    @Test
    void thePartsLandOnTheSliderGeometryWithoutTakingThePointer() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            SliderWidget slider = fixture.add(SliderWidget.create(0, 10, 5));
            slider.useStyle(UIStyles.sizeOf(80, 16));
            fixture.stabilize();

            var h = SliderWidget.Orientation.horizontal;
            assertEquals(80, slider.width(), "the parts never grow the slider");
            assertEquals(SliderWidget.trackBounds(80, 16, 8, h), part(slider, "track").bounds());
            assertEquals(SliderWidget.fillBounds(80, 16, 8, 0.5, h), part(slider, "fill").bounds());
            Rect handle = SliderWidget.handleBounds(80, 16, 8, 0.5, h);
            assertEquals(handle, part(slider, "handle").bounds());
            assertSame(slider, fixture.scene.hitTest(handle.centerX(), handle.centerY()), "the slider owns the input");
        }
    }

    @Test
    void aValueChangeMovesThePartsOnTheNextLayoutPass() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            SliderWidget slider = fixture.add(SliderWidget.create(0, 10, 0));
            slider.useStyle(UIStyles.sizeOf(80, 16));
            fixture.stabilize();
            assertEquals(0, part(slider, "handle").posX(), "an empty slider parks the handle at the start");

            slider.setValue(10);
            fixture.stabilize();
            var h = SliderWidget.Orientation.horizontal;
            assertEquals(SliderWidget.handleBounds(80, 16, 8, 1.0, h), part(slider, "handle").bounds());
            assertEquals(SliderWidget.fillBounds(80, 16, 8, 1.0, h), part(slider, "fill").bounds());
        }
    }

    @Test
    void orientationAndHandleSizeRepositionTheParts() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            SliderWidget slider = fixture.add(SliderWidget.create(0, 10, 5));
            slider.useStyle(UIStyles.sizeOf(80, 16));
            fixture.stabilize();

            slider.setOrientation(SliderWidget.Orientation.vertical);
            fixture.stabilize();
            assertEquals(
                SliderWidget.handleBounds(80, 16, 8, 0.5, SliderWidget.Orientation.vertical),
                part(slider, "handle").bounds()
            );

            slider.setThumbSize(12);
            fixture.stabilize();
            assertEquals(
                SliderWidget.handleBounds(80, 16, 12, 0.5, SliderWidget.Orientation.vertical),
                part(slider, "handle").bounds()
            );
        }
    }

    @Test
    void draggingStillDrivesTheValueWithThePartsInPlace() {
        try (WidgetTestScene fixture = new WidgetTestScene(80, 40)) {
            SliderWidget slider = fixture.add(SliderWidget.create(0, 10, 0));
            slider.useStyle(UIStyles.sizeOf(80, 16));
            fixture.stabilize();

            // the press lands on the handle's own pixels — the slider must still
            // be the target it drives
            fixture.scene.mouseClicked(4, 8, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            assertEquals(0.0, slider.value(), "the press at the groove's start pins the value");

            fixture.scene.mouseDragged(40, 8, GLFW.GLFW_MOUSE_BUTTON_LEFT, 36, 0);
            assertEquals(5.0, slider.value(), "the drag follows the pointer to the middle of the groove");

            fixture.scene.mouseReleased(40, 8, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            fixture.scene.mouseDragged(70, 8, GLFW.GLFW_MOUSE_BUTTON_LEFT, 30, 0);
            assertEquals(5.0, slider.value(), "the release ends the drag");
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
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "slider"), CssParser.parse(css));
    }

    /** The winning {@code background} declaration for a node, or null when no rule painted it. */
    private static @Nullable StyleValue<?> background(Theme theme, Widget node) {
        var key = Styles.byId("background");
        return key == null ? null : theme.resolve(node).get(key);
    }

    // endregion
}
