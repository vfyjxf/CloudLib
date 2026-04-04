package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;

import static dev.vfyjxf.cloudlib.ui.overlay.OverlayTestMinecrafts.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayManagerSceneLifecycleTest {
    @Test
    void attachAndDetachOverlays() {
        var register = new OverlayRegisterImpl();
        Widget widget = new Widget();
        register.register(OverlayEntry.global("lifecycle", c -> widget));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen(320, 240);
        var group = new WidgetGroup<Widget>();

        var runtimes = manager.attachOverlays(screen, dummyContext(screen), group);
        assertFalse(runtimes.isEmpty());
        assertTrue(group.children().contains(widget));

        manager.detachOverlays(runtimes, group);
        assertFalse(group.children().contains(widget));
    }

    @Test
    void detachOnlyRemovesSpecifiedRuntimes() {
        var register = new OverlayRegisterImpl();
        Widget widgetA = new Widget();
        Widget widgetB = new Widget();
        register.register(OverlayEntry.global("a", c -> widgetA));
        register.register(OverlayEntry.global("b", c -> widgetB));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen(320, 240);
        var group = new WidgetGroup<Widget>();

        var runtimes = manager.attachOverlays(screen, dummyContext(screen), group);
        assertEquals(2, runtimes.size());

        manager.detachOverlays(runtimes.subList(0, 1), group);
        assertEquals(1, group.children().size());
    }
}
