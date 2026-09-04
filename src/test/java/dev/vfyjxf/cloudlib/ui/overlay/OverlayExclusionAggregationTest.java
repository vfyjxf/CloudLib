package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayExclusion;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;

import static dev.vfyjxf.cloudlib.ui.overlay.OverlayTestMinecrafts.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayExclusionAggregationTest {
    @Test
    void aggregatesExclusionAreasAcrossActiveOverlays() {
        var register = new OverlayRegisterImpl();
        register.register(OverlayEntry.global("one", context -> new Widget(),
                OverlayExclusion.fixed(0, 0, 10, 10)));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen();
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
}
