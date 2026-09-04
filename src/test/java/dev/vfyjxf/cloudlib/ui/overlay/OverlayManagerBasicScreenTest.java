package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;

import static dev.vfyjxf.cloudlib.ui.overlay.OverlayTestMinecrafts.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayManagerBasicScreenTest {
    @Test
    void attachesOverlayWidgetToGroup() {
        var register = new OverlayRegisterImpl();
        Widget widget = new Widget();
        register.register(OverlayEntry.screen("basic", context -> widget));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen();
        var group = new WidgetGroup<Widget>();

        manager.attachOverlays(screen, dummyContext(screen), group);

        assertTrue(group.children().contains(widget));
    }

    @Test
    void skipsWhenProviderReturnsNull() {
        var register = new OverlayRegisterImpl();
        register.register(OverlayEntry.screen("inactive", context -> null));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen();
        var group = new WidgetGroup<Widget>();

        manager.attachOverlays(screen, dummyContext(screen), group);

        assertTrue(group.children().isEmpty());
    }

    @Test
    void continuesWhenProviderThrows() {
        var register = new OverlayRegisterImpl();
        Widget widget = new Widget();
        register.register(OverlayEntry.screen("bad", context -> {
            throw new IllegalStateException("boom");
        }));
        register.register(OverlayEntry.screen("good", context -> widget));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen();
        var group = new WidgetGroup<Widget>();

        manager.attachOverlays(screen, dummyContext(screen), group);

        assertTrue(group.children().contains(widget));
    }

    @Test
    void detachRemovesOverlaysFromGroup() {
        var register = new OverlayRegisterImpl();
        Widget widget = new Widget();
        register.register(OverlayEntry.screen("clear", context -> widget));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen();
        var group = new WidgetGroup<Widget>();

        var runtimes = manager.attachOverlays(screen, dummyContext(screen), group);
        assertTrue(group.children().contains(widget));

        manager.detachOverlays(runtimes, group);
        assertFalse(group.children().contains(widget));
    }

    @Test
    void refreshRecreatesOverlayWidgetsAgainstLatestContext() {
        var register = new OverlayRegisterImpl();
        register.register(OverlayEntry.screen("refresh", context -> {
            Widget widget = new Widget();
            widget.setKey(context.width());
            return widget;
        }));

        OverlayManager manager = newManager(register);
        Screen screen = new TestScreen(320, 240);
        var group = new WidgetGroup<Widget>();

        var runtimes = manager.attachOverlays(screen, dummyContext(screen), group);
        Widget first = group.children().detect(widget -> widget.key() instanceof Integer);
        assertTrue(group.children().contains(first));

        ((TestScreen) screen).resizeTo(180, 240);
        var refreshed = manager.refreshOverlays(screen, dummyContext(screen), group, runtimes);
        Widget second = group.children().detect(widget -> widget.key() instanceof Integer);

        assertNotSame(first, second);
        assertEquals(180, second.key());
        assertEquals(1, refreshed.size());
    }
}
