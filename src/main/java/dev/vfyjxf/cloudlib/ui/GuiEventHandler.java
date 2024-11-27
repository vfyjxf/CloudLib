package dev.vfyjxf.cloudlib.ui;


import dev.vfyjxf.cloudlib.api.annotations.Singleton;
import dev.vfyjxf.cloudlib.api.ui.overlay.UIOverlay;
import dev.vfyjxf.cloudlib.api.ui.inputs.InputContext;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
        UIOverlay overlay = Singletons.getNullable(UIOverlay.class);
        if (overlay == null) return;
        if (screen == event.getScreen()) {
            overlay.update();
        } else {
            overlay.getMainGroup().clear();
            overlay.init();
            overlay.update();
        }
        overlay.getMainGroup().show();
        screen = event.getScreen();
        throw new NullPointerException();
    }

    @SubscribeEvent
    public void onRenderPost(ScreenEvent.Render.Post event) {
        UIOverlay overlay = Singletons.getNullable(UIOverlay.class);
        if (overlay == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        double mouseX = ScreenUtil.getMouseX();
        double mouseY = ScreenUtil.getMouseY();
        float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        overlay.render(graphics, (int) mouseX, (int) mouseY, partialTicks);
    }

    @SubscribeEvent
    public void onDrawScreenPost(ScreenEvent.Render.Post event) {

    }

    @SubscribeEvent
    public void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        UIOverlay overlay = Singletons.getNullable(UIOverlay.class);
        if (overlay == null) return;
        boolean result = overlay.getMainGroup().mouseClicked(InputContext.fromMouse(event.getMouseX(), event.getMouseY(), event.getButton()));
        if (result) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        UIOverlay overlay = Singletons.getNullable(UIOverlay.class);
        if (overlay == null) return;
        var inputContext = InputContext.fromKeyboard(event.getKeyCode(), event.getScanCode(), event.getModifiers(), ScreenUtil.getMouseX(), ScreenUtil.getMouseY());
        boolean result = overlay.getMainGroup().keyPressed(inputContext);
        if (result) event.setCanceled(true);
    }

}
