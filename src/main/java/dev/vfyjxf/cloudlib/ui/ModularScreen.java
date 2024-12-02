package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.ModularUI;
import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.widgets.RootWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.ui.widgets.BasicPanel;
import mezz.jei.gui.input.MouseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

public abstract class ModularScreen extends Screen implements ModularUI {

    protected final BasicPanel<Widget> mainGroup;

    /**
     * Note: Register init listener in constructor
     */
    protected ModularScreen() {
        super(Component.empty());
        mainGroup = new BasicPanel<>();
        var root = new RootWidget();
        mainGroup.setRoot(root);
        mainGroup.asChild(root);
        mainGroup.onInit(self -> {
            mainGroup.withModifier(
                    Modifier.start()
                            .pos(0, 0)
                            .size(width, height)
            );
        });
        mainGroup.onInitPost(self -> {
            mainGroup.resize();
        });
    }

    @Override
    public int getGuiLeft() {
        return mainGroup.posX();
    }

    @Override
    public int getGuiTop() {
        return mainGroup.posY();
    }

    @Override
    public int getXSize() {
        return mainGroup.getWidth();
    }

    @Override
    public int getYSize() {
        return mainGroup.getHeight();
    }

    @MustBeInvokedByOverriders
    @Override
    protected void init() {
        mainGroup.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        mainGroup.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        mainGroup.tick();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mainGroup.mouseClicked(InputContext.fromMouse(MouseUtil.getX(), MouseUtil.getY(), button));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return mainGroup.mouseReleased(InputContext.fromMouse(MouseUtil.getX(), MouseUtil.getY(), button, true));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return mainGroup.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return mainGroup.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        var ret = mainGroup.keyPressed(InputContext.fromKeyboard(keyCode, scanCode, modifiers, MouseUtil.getX(), MouseUtil.getY()));
        if (!ret) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        var ret = mainGroup.keyReleased(InputContext.fromKeyboard(keyCode, scanCode, modifiers, MouseUtil.getX(), MouseUtil.getY(), true));
        if (!ret) {
            return super.keyReleased(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mainGroup.mouseMoved(mouseX, mouseY);
    }
}
