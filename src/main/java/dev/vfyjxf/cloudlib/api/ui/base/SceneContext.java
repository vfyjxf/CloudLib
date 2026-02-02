package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    private @Nullable SceneCanvas canvas;

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

    // ==================== Canvas ====================

    /**
     * Returns the current canvas, if available.
     * <p>
     * Only valid during a render pass. Returns null outside of render.
     *
     * @return the current canvas, or null
     */
    public @Nullable SceneCanvas canvas() {
        return canvas;
    }

    /**
     * Begins a render pass with the given GuiGraphics.
     * <p>
     * Called internally by Scene at the start of rendering.
     *
     * @param graphics the graphics to wrap
     * @return the canvas
     */
    SceneCanvas beginRender(GuiGraphics graphics) {
        this.canvas = SceneCanvas.create(graphics);
        return this.canvas;
    }

    /**
     * Ends the current render pass.
     * <p>
     * Called internally by Scene after rendering completes.
     */
    void endRender() {
        canvas = null;
    }
}
