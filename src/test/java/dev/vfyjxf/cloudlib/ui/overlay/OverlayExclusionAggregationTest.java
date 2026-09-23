package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayExclusion;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;

import static dev.vfyjxf.cloudlib.ui.overlay.OverlayTestMinecrafts.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayExclusionAggregationTest {
    @Test
    void aggregatesExclusionAreasAcrossActiveOverlays() {
        var register = new OverlayRegisterImpl();
        register.register(OverlayEntry.global("one", context -> new Widget(), OverlayExclusion.fixed(0, 0, 10, 10)));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen(320, 240);
        var group = new WidgetGroup<Widget>();

        manager.attachOverlays(screen, dummyContext(screen), group);

        var areas = manager.exclusionAreas(screen);
        assertEquals(1, areas.size());
        var area = areas.getFirst();
        assertEquals(0, area.getX());
        assertEquals(0, area.getY());
        assertEquals(10, area.getWidth());
        assertEquals(10, area.getHeight());
    }

    @Test
    void hiddenOverlayIsNotReported() {
        var register = new OverlayRegisterImpl();
        Widget widget = new Widget();
        register.register(OverlayEntry.global("hidden", context -> widget, OverlayExclusion.fixed(0, 0, 10, 10)));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen(320, 240);
        var group = new WidgetGroup<Widget>();

        manager.attachOverlays(screen, dummyContext(screen), group);
        widget.setVisible(false);

        assertTrue(manager.exclusionAreas(screen).isEmpty(), "a hidden panel must not report exclusion areas");
    }

    @Test
    void parkedOverlayOutsideTheScreenIsNotReported() {
        var register = new OverlayRegisterImpl();
        register.register(
            OverlayEntry.global("parked", context -> new Widget(), OverlayExclusion.fixed(1000, 1000, 50, 50))
        );

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen(320, 240);
        var group = new WidgetGroup<Widget>();

        manager.attachOverlays(screen, dummyContext(screen), group);

        assertTrue(
            manager.exclusionAreas(screen).isEmpty(),
            "a panel parked off-screen must not report exclusion areas"
        );
    }

    @Test
    void areasAreClippedToTheScreen() {
        var register = new OverlayRegisterImpl();
        register.register(
            OverlayEntry.global("clipped", context -> new Widget(), OverlayExclusion.fixed(300, 100, 50, 50))
        );

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen(320, 240);
        var group = new WidgetGroup<Widget>();

        manager.attachOverlays(screen, dummyContext(screen), group);

        var areas = manager.exclusionAreas(screen);
        assertEquals(1, areas.size());
        var area = areas.getFirst();
        assertEquals(300, area.getX());
        assertEquals(100, area.getY());
        assertEquals(20, area.getWidth());
        assertEquals(50, area.getHeight());
    }

    @Test
    void detachStopsReportingAreas() {
        var register = new OverlayRegisterImpl();
        register.register(
            OverlayEntry.global("detached", context -> new Widget(), OverlayExclusion.fixed(0, 0, 10, 10))
        );

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen(320, 240);
        var group = new WidgetGroup<Widget>();

        var runtimes = manager.attachOverlays(screen, dummyContext(screen), group);
        assertEquals(1, manager.exclusionAreas(screen).size());

        manager.detachOverlays(runtimes, group);

        assertTrue(manager.exclusionAreas(screen).isEmpty());
    }
}
