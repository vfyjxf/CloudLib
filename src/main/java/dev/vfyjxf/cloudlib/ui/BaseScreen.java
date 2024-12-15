package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.ModularUI;
import dev.vfyjxf.cloudlib.api.ui.WidgetWindow;
import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.widgets.RootWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.ui.widgets.BasicPanel;
import mezz.jei.gui.input.MouseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

public abstract class BaseScreen extends Screen implements ModularUI {

    protected final BasicPanel<Widget> mainGroup;
    private final RootWidget rootWidget;
    private WidgetWindow displayWindow;
    private MutableList<WidgetWindow> windows;

    /**
     * Note: Register init listener in constructor
     */
    protected BaseScreen() {
        super(Component.empty());
        mainGroup = new BasicPanel<>();
        rootWidget = new RootWidget();
        mainGroup.setRoot(rootWidget);
        mainGroup.asChild(rootWidget);
        mainGroup.onInit(self -> {
            mainGroup.withModifier(
                    Modifier.builder()
                            .pos(0, 0)
                            .size(width, height)
            );
        });
    }

    protected BasicPanel<Widget> mainGroup() {
        return mainGroup;
    }

    public static Modifier Modifier() {
        return Modifier.EMPTY;
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
        rootWidget.init();
        mainGroup.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        mainGroup.render(guiGraphics, mouseX, mouseY, partialTick);
        mainGroup.renderOverlay(guiGraphics, mouseX, mouseY, partialTick);
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
        var context = InputContext.fromKeyboard(keyCode, scanCode, modifiers, MouseUtil.getX(), MouseUtil.getY());
        var ret = mainGroup.keyPressed(context);
        if (!ret) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        var context = InputContext.fromKeyboard(keyCode, scanCode, modifiers, MouseUtil.getX(), MouseUtil.getY(), true);
        var ret = mainGroup.keyReleased(context);
        if (!ret) {
            if (context.is(Minecraft.getInstance().options.keyInventory)) {
                onClose();
                return true;
            }
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
