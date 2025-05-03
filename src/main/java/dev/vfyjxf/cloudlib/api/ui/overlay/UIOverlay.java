package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.event.OverlayEvent;
import dev.vfyjxf.cloudlib.api.ui.widget.Widget;
import dev.vfyjxf.cloudlib.api.ui.widget.WidgetGroup;
import dev.vfyjxf.cloudlib.ui.UIManager;
import net.minecraft.client.Minecraft;

/**
 * Represents an overlay that can be attached to the screen or the game.
 * E.g. JEI's bookmark overlay or ingredient list overlay.
 */
public interface UIOverlay extends Renderable, EventHandler<OverlayEvent> {

    static UIOverlay current() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BasicScreen basicScreen) {
            return basicScreen.screenOverlay();
        }
        return UIManager.instance().attachedOverlay();
    }

    void init();

    boolean holdByScreen();

    boolean initialized();

    void tick();

    WidgetGroup<Widget> overlayGroup();

    Rect getBound();

    void setBound(Rect bounds);

    /**
     * @param mouseX the absolute x position of the mouse
     * @param mouseY the absolute y position of the mouse
     * @return true if the mouse is over the overlay
     */
    boolean isMouseOver(double mouseX, double mouseY);

}
