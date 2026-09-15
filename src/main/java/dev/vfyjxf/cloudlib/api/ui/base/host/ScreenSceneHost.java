package dev.vfyjxf.cloudlib.api.ui.base.host;

import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

/**
 * A scene host for a Minecraft screen.
 */
public record ScreenSceneHost(Screen screen) implements SceneHost {

    @Override
    public Font font() {
        return screen.font;
    }

    @Override
    public int width() {
        return screen.width;
    }

    @Override
    public int height() {
        return screen.height;
    }
}
