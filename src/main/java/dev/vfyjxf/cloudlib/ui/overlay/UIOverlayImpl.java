package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.event.OverlayEvent;
import dev.vfyjxf.cloudlib.api.ui.overlay.UIOverlay;
import net.minecraft.client.gui.GuiGraphics;

public class UIOverlayImpl implements UIOverlay, EventHandler<OverlayEvent> {

    private final EventChannel<OverlayEvent> events = EventChannel.create(this);
    private final CompositeWidget<Widget> overlayGroup;
    private final boolean holdByScreen;

    public UIOverlayImpl(CompositeWidget<Widget> overlayGroup, boolean holdByScreen) {
        this.overlayGroup = overlayGroup;
        this.holdByScreen = holdByScreen;
        listeners(OverlayEvent.onOverlayBuild).onBuild(this);
    }

    @Override
    public void init() {
        if (!holdByScreen) {
//            overlayGroup.init();
        }
    }

    @Override
    public boolean holdByScreen() {
        return holdByScreen;
    }

    @Override
    public void tick() {
        overlayGroup.tick();
    }

    @Override
    public CompositeWidget<Widget> overlayGroup() {
        return overlayGroup;
    }

    @Override
    public Rect getBound() {
        return overlayGroup.bounds();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return overlayGroup.isMouseOver(mouseX, mouseY);
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
