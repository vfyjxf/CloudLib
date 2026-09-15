package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.BasicScreen;
import dev.vfyjxf.cloudlib.api.ui.base.FocusScopeNode;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.SceneHost;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayContext;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.sizeOf;

/**
 * Manages overlay lifecycle for non-{@link BasicScreen} screens.
 * <p>
 * {@code BasicScreen} handles its own overlays directly via {@link OverlayManager}.
 * This handler creates a standalone {@link Scene} for vanilla/non-CloudLib screens
 * and forwards NeoForge client events to it.
 */
public final class OverlayEventHandler {

    private final OverlayManager manager;
    private @Nullable Screen lastScreen;
    private @Nullable Scene globalScene;
    private @Nullable WidgetGroup<Widget> globalRoot;
    private List<OverlayRuntime<?>> globalRuntimes = List.of();
    private int lastWidth = -1;
    private int lastHeight = -1;

    public OverlayEventHandler(OverlayManager manager) {
        this.manager = manager;
    }

    public @Nullable Scene activeOverlayScene() {
        return globalScene;
    }

    public void refreshCurrentScreen() {
        Screen current = Minecraft.getInstance().screen;
        if (current != lastScreen) {
            handleScreenChange(current);
        } else if (current != null && globalScene != null && sizeChanged(current)) {
            refreshGlobal(current);
        }
    }

    @SubscribeEvent
    private void onClientTick(ClientTickEvent.Post event) {
        Screen current = Minecraft.getInstance().screen;
        if (current != lastScreen) {
            handleScreenChange(current);
        } else if (current != null && globalScene != null && sizeChanged(current)) {
            refreshGlobal(current);
        }
        if (globalScene != null) {
            globalScene.tick();
        }
    }

    private void handleScreenChange(@Nullable Screen screen) {
        clearGlobal();
        lastScreen = screen;
        if (screen != null && !(screen instanceof BasicScreen)) {
            setupGlobal(screen);
        }
    }

    private void setupGlobal(Screen screen) {
        OverlayContext context = manager.createContext(screen);

        WidgetGroup<Widget> root = new WidgetGroup<>();
        root.setFocusNode(new FocusScopeNode());
        root.useStyle(UIStyle.of(sizeOf(screen.width, screen.height)));

        Scene scene = new Scene(root);
        scene.init();
        scene.mount(SceneContext.create(SceneHost.of(screen)));
        scene.setLayoutArea(screen.width, screen.height);
        var runtimes = manager.attachOverlays(screen, context, root);
        scene.stabilize();
        scene.layout();
        root.applyLayout();

        globalScene = scene;
        globalRoot = root;
        globalRuntimes = runtimes;
        rememberSize(screen);
    }

    private void clearGlobal() {
        if (globalRoot != null && !globalRuntimes.isEmpty()) {
            manager.detachOverlays(globalRuntimes, globalRoot);
        }
        if (globalScene != null) {
            globalScene.destroy();
        }
        globalScene = null;
        globalRoot = null;
        globalRuntimes = List.of();
        lastWidth = -1;
        lastHeight = -1;
    }

    private boolean sizeChanged(Screen screen) {
        return screen.width != lastWidth || screen.height != lastHeight;
    }

    private void rememberSize(Screen screen) {
        lastWidth = screen.width;
        lastHeight = screen.height;
    }

    private void refreshGlobal(Screen screen) {
        if (globalScene == null || globalRoot == null) {
            return;
        }
        var root = globalScene.root();
        root.useStyle(UIStyle.of(sizeOf(screen.width, screen.height)));
        globalScene.setLayoutArea(screen.width, screen.height);
        globalScene.stabilize();
        globalScene.layout();
        root.applyLayout();
        OverlayContext context = manager.createContext(screen);
        globalRuntimes = manager.refreshOverlays(screen, context, globalRoot, globalRuntimes);
        globalScene.stabilize();
        globalScene.layout();
        root.applyLayout();
        rememberSize(screen);
    }

    @SubscribeEvent
    private void onScreenRender(ScreenEvent.Render.Post event) {
        if (globalScene == null || event.getScreen() != lastScreen) {
            return;
        }
        var root = globalScene.root();
        root.useStyle(UIStyle.of(sizeOf(event.getScreen().width, event.getScreen().height)));
        globalScene.setLayoutArea(event.getScreen().width, event.getScreen().height);
        globalScene.stabilize();
        globalScene.layout();
        root.applyLayout();
        globalScene.mouseMoved(event.getMouseX(), event.getMouseY());
        globalScene.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    @SubscribeEvent
    private void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (globalScene != null && globalScene.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (globalScene != null && globalScene.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (globalScene != null
            && globalScene.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (globalScene != null && globalScene.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (globalScene != null && globalScene.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (globalScene != null && globalScene.charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }
}
