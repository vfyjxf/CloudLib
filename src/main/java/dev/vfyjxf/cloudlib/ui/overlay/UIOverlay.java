package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.event.IEventManager;
import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.overlay.IUIOverlay;
import dev.vfyjxf.cloudlib.event.EventManager;
import dev.vfyjxf.cloudlib.math.Rectangle;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Color;

;

public class UIOverlay implements IUIOverlay {

    private final IEventManager<IUIOverlay> events = new EventManager<>(this);

    private IModularUI ui;
    private Rectangle bounds;
    private boolean initialized = false;
    private Color color = new Color(0x9917191D, true);

    public UIOverlay(IModularUI modularUI) {
        ui = modularUI;
        bounds = new Rectangle(0, ui.getScreenWidth(), 0, ui.getScreenHeight());
    }

    @Override
    public void init() {
        if (initialized) return;
        this.getMainGroup().init();
        initialized = true;
    }

    @Override
    public boolean initialized() {
        return initialized;
    }

    @Override
    public void update() {
        if (!initialized) return;
        this.getMainGroup().update();
    }

    @Override
    public void tick() {
        if (initialized)
            this.getMainGroup().tick();
    }

    @Override
    public IModularUI getUI() {
        return ui;
    }

    @Override
    public IUIOverlay setUI(IModularUI ui) {
        this.ui = ui;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        ui.getMainGroup().render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    @Override
    public boolean containsMouse(int mouseX, int mouseY) {
        return bounds.contains(mouseX, mouseY);
    }

    @Override
    public IEventManager<IUIOverlay> events() {
        return this.events;
    }
}
