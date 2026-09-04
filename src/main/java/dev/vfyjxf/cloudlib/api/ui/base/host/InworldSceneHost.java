package dev.vfyjxf.cloudlib.api.ui.base.host;

import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

public class InworldSceneHost implements SceneHost {
    @Override
    public Font font() {
        return Minecraft.getInstance().font;
    }

    @Override
    public int width() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    @Override
    public int height() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }
}
