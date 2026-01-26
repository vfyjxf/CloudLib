package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.base.host.ScreenSceneHost;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

public interface SceneHost {

    static SceneHost of(Screen screen) {
        return new ScreenSceneHost(screen);
    }

    Font font();

    int width();

    int height();

}
