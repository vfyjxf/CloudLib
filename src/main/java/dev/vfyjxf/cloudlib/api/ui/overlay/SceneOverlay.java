package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.event.OverlayEvent;
import net.minecraft.client.Minecraft;

/**
 * Represents an overlay that can be attached to the screen or the game.
 * E.g. JEI's bookmark overlay or ingredient list overlay.
 */
//TODO:refactor overlay
public interface SceneOverlay extends Renderable, EventHandler<OverlayEvent> {

    interface Provider {
        SceneOverlay screenOverlay();
    }

    static SceneOverlay current() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Provider provider) {
            return provider.screenOverlay();
        }
        throw new UnsupportedOperationException("Not Implemented");
    }

    WidgetGroup<Widget> overlayGroup();

}
