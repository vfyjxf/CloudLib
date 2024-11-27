package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.ModularUI;
import dev.vfyjxf.cloudlib.api.ui.inputs.InputContext;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import mezz.jei.gui.input.MouseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

public abstract class ModularScreen extends Screen implements ModularUI {

    protected final WidgetGroup<Widget> mainGroup = new WidgetGroup<>();

    protected ModularScreen() {
        super(Component.empty());
    }

    @Override
    public int getGuiLeft() {
        return mainGroup.getX();
    }

    @Override
    public int getGuiTop() {
        return mainGroup.getY();
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
        mainGroup.setPos(0, 0);
        mainGroup.setSize(width, height);
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
        return mainGroup.keyPressed(InputContext.fromKeyboard(keyCode, scanCode, modifiers, MouseUtil.getX(), MouseUtil.getY()));
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return mainGroup.keyReleased(InputContext.fromKeyboard(keyCode, scanCode, modifiers, MouseUtil.getX(), MouseUtil.getY(), true));
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
