package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.sizeOf;
import static dev.vfyjxf.cloudlib.ui.overlay.OverlayTestMinecrafts.TestScreen;
import static dev.vfyjxf.cloudlib.ui.overlay.OverlayTestMinecrafts.newManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayEventHandlerLifecycleTest {

    @Test
    void setupGlobalMountsOverlayWidgetsBeforeManualLayout() throws Exception {
        var register = new OverlayRegisterImpl();
        Widget overlayWidget = new Widget();
        register.register(OverlayEntry.screen("lifecycle_mount_order", context -> overlayWidget));

        OverlayManager manager = newManager(register);
        OverlayEventHandler handler = new OverlayEventHandler(manager);
        Screen screen = new TestScreen(320, 240);

        Method setupGlobal = OverlayEventHandler.class.getDeclaredMethod("setupGlobal", Screen.class);
        setupGlobal.setAccessible(true);
        setupGlobal.invoke(handler, screen);

        assertTrue(overlayWidget.lifecycle().mounted(), "overlay widget should be mounted after setupGlobal");

        Scene scene = handler.activeOverlayScene();
        assertNotNull(scene, "overlay scene should be initialized");
        var root = scene.root();
        root.useStyle(UIStyle.of(sizeOf(screen.width, screen.height)));
        scene.setLayoutArea(screen.width, screen.height);
        scene.layout();
        root.applyLayout();
    }

    @Test
    void setupGlobalRefreshesOverlayWidgetsWhenScreenSizeChanges() throws Exception {
        var register = new OverlayRegisterImpl();
        register.register(OverlayEntry.screen("lifecycle_refresh", context -> {
            Widget widget = new Widget();
            widget.setKey(context.width());
            return widget;
        }));

        OverlayManager manager = newManager(register);
        OverlayEventHandler handler = new OverlayEventHandler(manager);
        Screen screen = new TestScreen(320, 240);

        Method setupGlobal = OverlayEventHandler.class.getDeclaredMethod("setupGlobal", Screen.class);
        setupGlobal.setAccessible(true);
        setupGlobal.invoke(handler, screen);

        Scene scene = handler.activeOverlayScene();
        assertNotNull(scene);
        Widget first = scene.root().children().detect(widget -> widget.key() instanceof Integer);

        ((TestScreen) screen).resizeTo(196, 240);
        Method refreshGlobal = OverlayEventHandler.class.getDeclaredMethod("refreshGlobal", Screen.class);
        refreshGlobal.setAccessible(true);
        refreshGlobal.invoke(handler, screen);

        Widget second = scene.root().children().detect(widget -> widget.key() instanceof Integer);
        assertNotSame(first, second);
        assertEquals(196, second.key());
    }
}
