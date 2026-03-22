package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.base.host.ScreenSceneHost;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

/**
 * A host for a scene.
 *
 * @see ScreenSceneHost
 */
public interface SceneHost {

    static SceneHost of(Screen screen) {
        return new ScreenSceneHost(screen);
    }

    /**
     * Get the font of the scene host.
     *
     * @return the font of the scene host
     */
    Font font();

    /**
     * Get the width of the scene host.
     *
     * @return the width of the scene host
     */
    int width();

    /**
     * Get the height of the scene host.
     *
     * @return the height of the scene host
     */
    int height();

}
