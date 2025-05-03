package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.event.OverlayEvent;
import dev.vfyjxf.cloudlib.api.ui.overlay.UIOverlay;
import dev.vfyjxf.cloudlib.api.ui.widget.Widget;
import dev.vfyjxf.cloudlib.api.ui.widget.WidgetGroup;
import net.minecraft.client.gui.GuiGraphics;

public class UIOverlayImpl implements UIOverlay, EventHandler<OverlayEvent> {

    private final EventChannel<OverlayEvent> events = EventChannel.create(this);
    private final WidgetGroup<Widget> mainGroup;
    private final boolean holdByScreen;

    public UIOverlayImpl(WidgetGroup<Widget> mainGroup, boolean holdByScreen) {
        this.mainGroup = mainGroup;
        this.holdByScreen = holdByScreen;
        listeners(OverlayEvent.onOverlayBuild).onBuild(this);
    }

    @Override
    public void init() {
        if (!holdByScreen) {
            mainGroup.init();
        }
    }

    @Override
    public boolean holdByScreen() {
        return holdByScreen;
    }

    @Override
    public boolean initialized() {
        return mainGroup.initialized();
    }

    @Override
    public void tick() {
        mainGroup.tick();
    }

    @Override
    public WidgetGroup<Widget> overlayGroup() {
        return mainGroup;
    }

    @Override
    public Rect getBound() {
        return mainGroup.getBounds();
    }

    @Override
    public void setBound(Rect bounds) {
        mainGroup.setBound(bounds);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mainGroup.isMouseOver(mouseX, mouseY);
    }

    @Override
    public EventChannel<OverlayEvent> events() {
        return events;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        mainGroup.render(graphics, mouseX, mouseY, partialTicks);
    }
}
