package dev.vfyjxf.cloudlib.api.ui.sync;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.FocusScopeNode;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.base.host.ScreenSceneHost;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.sync.menu.BasicMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.sizeOf;

//TODO:Rework
public abstract class BasicMenuScreen<T extends BasicMenu<?>> extends AbstractContainerScreen<T> {

    protected final WidgetGroup<Widget> mainGroup;
    protected final Player player;
    private final Scene scene;

    public BasicMenuScreen(T menu, Inventory playerInventory) {
        super(menu, playerInventory, Component.empty());

        //region common usage
        this.player = playerInventory.player;
        //endregion

        //region setup main panel
        mainGroup = new WidgetGroup<>();
        {
            mainGroup.setFocusNode(new FocusScopeNode());
        }
        scene = new Scene(mainGroup);
        //endregion
    }

    protected CompositeWidget<Widget> mainGroup() {
        return mainGroup;
    }

    protected Scene scene() {
        return scene;
    }

    @MustBeInvokedByOverriders
    @Override
    protected void init() {
        mainGroup.useStyle(UIStyle.of(sizeOf(width, height)));
        scene.init();
        scene.mount(SceneContext.create(new ScreenSceneHost(this)));
        scene.setLayoutArea(width, height);
        scene.stabilize();
        super.init();
    }

    @Override
    public void onClose() {
        super.onClose();
        scene.destroy();
    }

    /**
     * Called by {@link AbstractContainerScreen#tick()} (which is final).
     * Ticks the scene first so widget logic runs before menu bookkeeping.
     */
    @Override
    protected void containerTick() {
        if (mainGroup.lifecycle().mounted()) {
            scene.tick();
        }
        menu.sendReveredDataToServer();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        scene.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {

    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
    }

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

}
