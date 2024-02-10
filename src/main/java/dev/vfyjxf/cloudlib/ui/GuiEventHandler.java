package dev.vfyjxf.cloudlib.ui;


import dev.vfyjxf.cloudlib.api.annotations.Singleton;
import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.UIConfig;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;
import dev.vfyjxf.cloudlib.api.ui.overlay.IUIOverlay;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@Singleton
public class GuiEventHandler {


    public static GuiEventHandler getInstance() {
        return Singletons.get(GuiEventHandler.class);
    }

    private Screen screen;

    public GuiEventHandler() {

    }

    @SubscribeEvent
    public void onGuiInitPost(ScreenEvent.Init.Post event) {
        IUIOverlay overlay = Singletons.getNullable(IUIOverlay.class);
        if (overlay == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (screen == event.getScreen()) {
            overlay.update();
        } else {
            overlay.getMainGroup().clear();
            IUIRegistry.getInstance().getOverlayProviders().forEach(it -> it.build(overlay));
            overlay.init();
            overlay.update();
            UIConfig.getInstance().clear();
        }
        screen = event.getScreen();
    }

    @SubscribeEvent
    public void onBackgroundRender(ScreenEvent.BackgroundRendered event) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        double mouseX = ScreenUtil.getMouseX();
        double mouseY = ScreenUtil.getMouseY();
        float partialTicks = minecraft.getPartialTick();
        IUIOverlay overlay = Singletons.getNullable(IUIOverlay.class);
        if (overlay != null) {
            overlay.render(graphics, (int) mouseX, (int) mouseY, partialTicks);
        }
    }

    @SubscribeEvent
    public void onDrawScreenPost(ScreenEvent.Render.Post event) {

    }

    @SubscribeEvent
    public void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        IUIOverlay overlay = Singletons.getNullable(IUIOverlay.class);
        if (overlay != null) {
            boolean result = overlay.getMainGroup().mouseClicked(IInputContext.fromMouse(event.getMouseX(), event.getMouseY(), event.getButton()));
            if (result) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        IUIOverlay overlay = Singletons.getNullable(IUIOverlay.class);
        if (overlay != null) {
            boolean result = overlay.getMainGroup().keyPressed(IInputContext.fromKeyboard(event.getKeyCode(), event.getScanCode(), event.getModifiers(), ScreenUtil.getMouseX(), ScreenUtil.getMouseY()));
            if (result) event.setCanceled(true);
        }
    }

}
