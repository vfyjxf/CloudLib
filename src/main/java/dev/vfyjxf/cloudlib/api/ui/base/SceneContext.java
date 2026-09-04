package dev.vfyjxf.cloudlib.api.ui.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class SceneContext {

    /**
     *
     * @param host the scene host
     * @return create a scene context for the given scene host in gui screen scene
     */
    public static SceneContext create(SceneHost host) {
        return new SceneContext(host);
    }

    private final SceneHost host;
    private long tickCount = 0;

    private SceneContext(SceneHost host) {
        this.host = host;
    }

    public @Nullable Screen currentScreen() {
        return Minecraft.getInstance().screen;
    }

    @Nullable
    public <T extends Screen> T typedScreen(Class<T> type) {
        var screen = currentScreen();
        return type.isInstance(screen) ? type.cast(screen) : null;
    }

    public SceneHost host() {
        return host;
    }

    public Font font() {
        return host.font();
    }

    public int width() {
        return host.width();
    }

    public int height() {
        return host.height();
    }

    public long tickCount() {
        return tickCount;
    }

    long tick() {
        return tickCount++;
    }

}
