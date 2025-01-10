package dev.vfyjxf.cloudlib.ui;


import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class GuiEventHandler {


    public static GuiEventHandler getInstance() {
        return Singletons.get(GuiEventHandler.class);
    }

    private Screen screen;

    public GuiEventHandler() {
    }

    @SubscribeEvent
    public void onGuiInitPost(ScreenEvent.Init.Post event) {
//        UIOverlay overlay = UIOverlay.current();
//        if ( !overlay.holdByScreen()) {
//            overlay.init();
//        }
    }

    @SubscribeEvent
    public void onRenderPost(ScreenEvent.Render.Post event) {
//        UIOverlay overlay = UIOverlay.current();
//        if (overlay.holdByScreen()) return;
//        GuiGraphics graphics = event.getGuiGraphics();
//        overlay.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    @SubscribeEvent
    public void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
//        UIOverlay overlay = UIOverlay.current();
//        if (overlay.holdByScreen()) return;
//        boolean result = overlay.mainGroup().mouseClicked(InputContext.fromMouse(event.getMouseX(), event.getMouseY(), event.getButton()));
//        if (result) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
//        UIOverlay overlay = UIOverlay.current();
//        if (overlay.holdByScreen()) return;
//        var inputContext = InputContext.fromKeyboard(event.getKeyCode(), event.getScanCode(), event.getModifiers(), ScreenUtil.getMouseX(), ScreenUtil.getMouseY());
//        boolean result = overlay.mainGroup().keyPressed(inputContext);
//        if (result) event.setCanceled(true);
    }

}
