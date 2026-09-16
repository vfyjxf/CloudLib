package dev.vfyjxf.cloudlib.ui.debug;

import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import java.util.function.DoubleSupplier;

/**
 * Host for the DevTools scene, sized in physical screen pixels divided by the
 * DevTools zoom. The overlay then scales the scene back through
 * {@code debugScale() * devToolsZoom}, keeping the UI crisp at any zoom level.
 */
final class DebugSceneHost implements SceneHost {

    private final DoubleSupplier zoom;

    DebugSceneHost(DoubleSupplier zoom) {
        this.zoom = zoom;
    }

    @Override
    public Font font() {
        return Minecraft.getInstance().font;
    }

    @Override
    public int width() {
        var window = Minecraft.getInstance().getWindow();
        return (int) Math.max(1, window.getScreenWidth() / zoom.getAsDouble());
    }

    @Override
    public int height() {
        var window = Minecraft.getInstance().getWindow();
        return (int) Math.max(1, window.getScreenHeight() / zoom.getAsDouble());
    }

    float zoom() {
        return (float) zoom.getAsDouble();
    }
}
