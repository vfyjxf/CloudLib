package dev.vfyjxf.cloudlib.ui.debug;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.debug.DebugOverlay;
import dev.vfyjxf.cloudlib.debug.Debugs;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.client.Minecraft;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.sizeOf;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * Per-scene debug controller behind {@link DebugOverlay}.
 * <p>
 * Owns a fully separate {@link Scene} hosting the DevTools window, plus the
 * box-model {@link HighlightRenderer}. The overlay never enters the inspected
 * scene's widget tree: rendering happens as a dedicated phase after the
 * inspected scene has finished rendering, and input is only pre-filtered
 * through this class while the overlay is open.
 */
public final class DebugOverlayImpl implements DebugOverlay {

    private final Scene inspected;
    private final HighlightRenderer highlightRenderer;

    private @Nullable Scene debugScene;
    private @Nullable DevToolsWindow window;

    private boolean open;
    private boolean inspectMode;
    private boolean highlightEnabled = true;

    private @Nullable Widget selected;
    private @Nullable Widget hoverTarget;

    /** A mouse drag that started inside the panel keeps receiving events. */
    private boolean panelDragging;

    private int lastWidth = -1;
    private int lastHeight = -1;
    private Insets lastInsets = Insets.zero;

    //region zoom / scale

    private float devToolsZoom = 2.0f;
    static final float[] ZOOM_LEVELS = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f};
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3.0f;

    private int originalGuiScale = -1;

    //endregion

    public DebugOverlayImpl(Scene inspected) {
        this.inspected = inspected;
        this.highlightRenderer = new HighlightRenderer(inspected);
    }

    //region DebugOverlay api

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void open() {
        ensureDebugScene();
        open = true;
        if (window != null) {
            window.refreshTree();
            window.syncButtons();
        }
    }

    @Override
    public void close() {
        open = false;
        inspectMode = false;
        hoverTarget = null;
        panelDragging = false;
        CursorHelper.reset();
        restoreGuiScale();
        onDockChanged();
    }

    @Override
    public boolean inspectMode() {
        return inspectMode;
    }

    @Override
    public void setInspectMode(boolean enabled) {
        this.inspectMode = enabled;
        if (!enabled) hoverTarget = null;
        if (window != null) window.syncButtons();
    }

    @Override
    public void select(@Nullable Widget widget) {
        if (widget != null && widget.scene() != inspected) widget = null;
        selected = widget;
        if (window != null) window.setSelected(widget);
    }

    @Override
    public @Nullable Widget selected() {
        return selected;
    }

    @Override
    public boolean highlightEnabled() {
        return highlightEnabled;
    }

    @Override
    public void setHighlightEnabled(boolean enabled) {
        this.highlightEnabled = enabled;
        if (window != null) window.syncButtons();
    }

    @Override
    public Insets contentInsets() {
        if (!open || window == null) return Insets.zero;
        return window.contentInsets();
    }

    //endregion

    //region package-private accessors for the debug UI

    Scene inspected() {
        return inspected;
    }

    void setHoverTarget(@Nullable Widget widget) {
        this.hoverTarget = widget;
    }

    void clearHoverTarget(Widget widget) {
        if (hoverTarget == widget) hoverTarget = null;
    }

    //endregion

    //region zoom / scale controls

    /**
     * @return total scale applied to the DevTools scene: {@code debugScale * devToolsZoom}
     */
    float devToolsScale() {
        return debugScale() * devToolsZoom;
    }

    float devToolsZoom() {
        return devToolsZoom;
    }

    void setDevToolsZoom(float value) {
        devToolsZoom = Math.clamp(value, MIN_ZOOM, MAX_ZOOM);
    }

    String currentZoomLabel() {
        String s = Float.toString(devToolsZoom);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s;
    }

    void onDockChanged() {
        if (inspected != null) {
            Insets insets = contentInsets();
            if (!insets.equals(lastInsets)) {
                lastInsets = insets;
                inspected.setDebugInsets(insets);
                inspected.root().useStyle(UIStyle.of(sizeOf(TaffyDimension.percent(1f))));
                inspected.stabilize();
            }
        }
    }

    String currentGuiScaleLabel() {
        int scale = Minecraft.getInstance().options.guiScale().get();
        return scale == 0 ? "A" : String.valueOf(scale);
    }

    void setGuiScale(int value) {
        var options = Minecraft.getInstance().options;
        int current = options.guiScale().get();
        if (originalGuiScale < 0) {
            originalGuiScale = current;
        }
        if (current != value) {
            options.guiScale().set(value);
            Minecraft.getInstance().resizeDisplay();
        }
        updateScaleLabel();
    }

    private void restoreGuiScale() {
        if (originalGuiScale >= 0) {
            var options = Minecraft.getInstance().options;
            if (options.guiScale().get() != originalGuiScale) {
                options.guiScale().set(originalGuiScale);
                Minecraft.getInstance().resizeDisplay();
            }
            originalGuiScale = -1;
        }
    }

    void updateScaleLabel() {
        if (window != null) window.updateScaleButton();
    }

    //endregion

    //region scene hooks (called by Scene)

    /**
     * Renders the highlight overlay and the debug scene.
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!open) return;
        ensureDebugScene();
        if (debugScene == null || window == null) return;

        relayoutIfNeeded();

        if (highlightEnabled) {
            try {
                highlightRenderer.render(graphics, hoverTarget, selected);
            } catch (Throwable t) {
                Debugs.log.error("Debug highlight render failed", t);
            }
        }

        float scale = devToolsScale();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1f);
        debugScene.render(graphics, Math.round(mouseX / scale), Math.round(mouseY / scale), partialTick);
        graphics.pose().popPose();
    }

    /**
     * Scale factor from physical screen pixels to GUI-scaled pixels.
     */
    private static float debugScale() {
        var window = Minecraft.getInstance().getWindow();
        int screenWidth = window.getScreenWidth();
        if (screenWidth <= 0) return 1f;
        return window.getGuiScaledWidth() / (float) screenWidth;
    }

    public void tick() {
        if (selected != null && (selected.scene() != inspected || !selected.lifecycle().mounted())) {
            select(null);
        }
        if (hoverTarget != null && (hoverTarget.scene() != inspected || !hoverTarget.lifecycle().mounted())) {
            hoverTarget = null;
        }

        if (open && debugScene != null) {
            // keep the inspected scene in sync with the docked panel size
            onDockChanged();
            debugScene.tick();
        }
    }

    public void dispose() {
        open = false;
        restoreGuiScale();
        if (debugScene != null) {
            debugScene.destroy();
        }
        debugScene = null;
        window = null;
        selected = null;
        hoverTarget = null;
    }

    //endregion

    //region input routing

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || debugScene == null || window == null) return false;
        double scale = devToolsScale();
        double dx = mouseX / scale, dy = mouseY / scale;
        if (inspectMode) {
            if (window.isOverPanel(dx, dy)) {
                panelDragging = true;
                debugScene.mouseClicked(dx, dy, button);
                return true;
            }
            select(inspected.hitTest(mouseX, mouseY));
            setInspectMode(false);
            return true;
        }
        if (window.isOverPanel(dx, dy)) {
            panelDragging = true;
            debugScene.mouseClicked(dx, dy, button);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!open || debugScene == null || window == null) return false;
        double scale = devToolsScale();
        double dx = mouseX / scale, dy = mouseY / scale;
        if (inspectMode) return true;
        if (panelDragging) {
            panelDragging = false;
            debugScene.mouseReleased(dx, dy, button);
            return true;
        }
        if (window.isOverPanel(dx, dy)) {
            debugScene.mouseReleased(dx, dy, button);
            return true;
        }
        return false;
    }

    public boolean mouseMoved(double mouseX, double mouseY) {
        if (!open || debugScene == null || window == null) return false;
        double scale = devToolsScale();
        double dx = mouseX / scale, dy = mouseY / scale;
        boolean overPanel = window.isOverPanel(dx, dy);
        if (inspectMode && !overPanel) {
            hoverTarget = inspected.hitTest(mouseX, mouseY);
        }
        debugScene.mouseMoved(dx, dy);
        return overPanel || inspectMode;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!open || debugScene == null || window == null) return false;
        if (inspectMode) return true;
        double scale = devToolsScale();
        double dx = mouseX / scale, dy = mouseY / scale;
        if (panelDragging || window.isOverPanel(dx, dy)) {
            debugScene.mouseDragged(dx, dy, button, dragX / scale, dragY / scale);
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!open || debugScene == null || window == null) return false;
        if (inspectMode) return true;
        double scale = devToolsScale();
        double dx = mouseX / scale, dy = mouseY / scale;
        if (window.isOverPanel(dx, dy)) {
            debugScene.mouseScrolled(dx, dy, scrollX, scrollY);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open || debugScene == null) return false;
        if (inspectMode && keyCode == InputConstants.KEY_ESCAPE) {
            setInspectMode(false);
            return true;
        }
        return debugScene.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!open || debugScene == null) return false;
        return debugScene.keyReleased(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!open || debugScene == null) return false;
        return debugScene.charTyped(codePoint, modifiers);
    }

    //endregion

    //region debug scene lifecycle

    private void ensureDebugScene() {
        if (debugScene != null) return;
        SceneHost host = new DebugSceneHost(() -> devToolsZoom);
        DevToolsWindow window = new DevToolsWindow(this, host);
        Scene scene = new Scene(window);
        scene.init();
        scene.mount(SceneContext.create(host));
        scene.setLayoutArea(host.width(), host.height());
        scene.layout();
        window.applyLayout();
        this.debugScene = scene;
        this.window = window;
        this.lastWidth = host.width();
        this.lastHeight = host.height();
    }

    private void relayoutIfNeeded() {
        if (debugScene == null || window == null) return;
        SceneHost host = debugScene.context().host();
        if (host.width() == lastWidth && host.height() == lastHeight) return;
        lastWidth = host.width();
        lastHeight = host.height();
        debugScene.setLayoutArea(lastWidth, lastHeight);
        window.clampToScreen();
        debugScene.layout();
        window.applyLayout();
    }

    //endregion

}
