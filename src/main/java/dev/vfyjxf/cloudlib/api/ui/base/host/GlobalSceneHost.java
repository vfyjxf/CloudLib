package dev.vfyjxf.cloudlib.api.ui.base.host;

import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

public class GlobalSceneHost implements SceneHost {

    @Override
    public Font font() {
        var mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) return mc.font;
        else return screen.font;
    }

    @Override
    public int width() {
        var mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) return mc.getWindow().getGuiScaledWidth();
        else return screen.width;
    }

    @Override
    public int height() {
        var mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) return mc.getWindow().getGuiScaledHeight();
        else return screen.height;
    }
}
