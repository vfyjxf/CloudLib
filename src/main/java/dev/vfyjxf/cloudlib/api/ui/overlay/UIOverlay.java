package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.event.OverlayEvent;
import dev.vfyjxf.cloudlib.ui.UIManager;
import net.minecraft.client.Minecraft;

/**
 * Represents an overlay that can be attached to the screen or the game.
 * E.g. JEI's bookmark overlay or ingredient list overlay.
 */
//TODO:refactor overlay
public interface UIOverlay extends Renderable, EventHandler<OverlayEvent> {

    interface Provider {
        UIOverlay screenOverlay();
    }

    static UIOverlay current() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Provider provider) {
            return provider.screenOverlay();
        }
        return UIManager.instance().attachedOverlay();
    }

    void init();

    boolean holdByScreen();

    void tick();

    CompositeWidget<Widget> overlayGroup();

    Rect getBound();

    /**
     * @param mouseX the absolute x position of the mouse
     * @param mouseY the absolute y position of the mouse
     * @return true if the mouse is over the overlay
     */
    boolean isMouseOver(double mouseX, double mouseY);

}
