package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.event.OverlayEvent;
import dev.vfyjxf.cloudlib.api.ui.overlay.SceneOverlay;
import net.minecraft.client.gui.GuiGraphics;

public class SceneOverlayImpl implements SceneOverlay, EventHandler<OverlayEvent> {

    private final EventChannel<OverlayEvent> events = EventChannel.create(this);
    private final WidgetGroup<Widget> overlayGroup;

    public SceneOverlayImpl(WidgetGroup<Widget> overlayGroup) {
        this.overlayGroup = overlayGroup;
        listeners(OverlayEvent.onOverlayBuild).onBuild(this);
    }

    @Override
    public WidgetGroup<Widget> overlayGroup() {
        return overlayGroup;
    }

    @Override
    public EventChannel<OverlayEvent> events() {
        return events;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        overlayGroup.render(graphics, mouseX, mouseY, partialTicks);
    }
}
