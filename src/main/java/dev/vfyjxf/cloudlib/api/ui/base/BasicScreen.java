package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.base.host.ScreenSceneHost;
import dev.vfyjxf.cloudlib.api.ui.overlay.UIOverlay;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.overlay.UIOverlayImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.sizeOf;

public abstract class BasicScreen extends Screen {

    protected final WidgetGroup<Widget> mainGroup;
    private final Scene scene;
    private final UIOverlayImpl screenOverlay;

    /**
     * Note: Register init listener in constructor
     */
    protected BasicScreen() {
        super(Component.empty());
        //region setup main panel
        mainGroup = new WidgetGroup<>();
        {
            mainGroup.setFocusNode(new FocusScopeNode());
        }
        scene = new Scene(mainGroup);

        //endregion
        //region screen overlay
        WidgetGroup<Widget> overlayPanel = mainGroup.addWidget(new WidgetGroup<>());
        screenOverlay = new UIOverlayImpl(overlayPanel, true);
        //endregion
    }

    protected WidgetGroup<Widget> mainGroup() {
        return mainGroup;
    }

    protected Scene scene() {
        return scene;
    }

    public UIOverlay screenOverlay() {
        return screenOverlay;
    }

    @MustBeInvokedByOverriders
    @Override
    protected void init() {
        mainGroup.useStyle(UIStyle.of(
                sizeOf(width, height)
        ));
        scene.init();
        scene.mount(SceneContext.create(new ScreenSceneHost(this)));
        scene.setLayoutArea(width, height);
        scene.layout();
        mainGroup.applyLayout();
    }

    @Override
    public void onClose() {
        super.onClose();
        scene.destroy();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        mainGroup.useStyle(UIStyle.of(
                sizeOf(width, height)
        ));
        scene.setLayoutArea(width, height);
        scene.layout();
        mainGroup.applyLayout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        scene.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        scene.tick();
    }

    //region user input proxy

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return scene.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return scene.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return scene.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scene.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (scene.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (Minecraft.getInstance().options.keyInventory.consumeClick() && shouldCloseOnEsc()) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (scene.keyReleased(keyCode, scanCode, modifiers)) return true;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return scene.charTyped(codePoint, modifiers);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        scene.mouseMoved(mouseX, mouseY);
    }

    //endregion
}
