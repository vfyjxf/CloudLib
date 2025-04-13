package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.overlay.UIOverlay;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.ui.overlay.UIOverlayImpl;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

public class UIManager {

    private static final UIManager INSTANCE = new UIManager();

    public static UIManager instance() {
        return INSTANCE;
    }

    private UIManager() {
    }

    private UIOverlayImpl overlay = null;

    @Nullable
    private BasicScreen screen;
    private boolean closeAll = false;

    public UIOverlay attachedOverlay() {
        return overlay;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScreenOpen(ScreenEvent.Opening event) {
        var nextScreen = event.getNewScreen();
        var minecraft = Minecraft.getInstance();
        if ((closeAll = (nextScreen == null)) || minecraft.level == null) return;
        if (event.getNewScreen() instanceof BasicScreen basicScreen) {
            screen = basicScreen;
        }
        createOverlay();
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        var minecraft = Minecraft.getInstance();
        boolean shouldInit = minecraft.level != null && overlay != null && !overlay.holdByScreen();
        if (shouldInit) {
            overlay.init();
        }
    }

    @SubscribeEvent
    public void onScreenClose(ScreenEvent.Closing event) {
        var closing = event.getScreen();
        if (closing == screen) {
            screen = null;
        }
        if (closeAll) {
            //TODO:add onClose event
            closeAll = false;
            overlay = null;
        }
    }

    private void createOverlay() {
        var minecraft = Minecraft.getInstance();
        if (screen != null || minecraft.level == null) return;
        if (overlay == null) {
            overlay = new UIOverlayImpl(new WidgetGroup<>(), false);
        }
    }

}
