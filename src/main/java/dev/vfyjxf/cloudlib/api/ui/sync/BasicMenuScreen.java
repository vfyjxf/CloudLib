package dev.vfyjxf.cloudlib.api.ui.sync;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.window.WidgetWindow;
import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.overlay.UIOverlay;
import dev.vfyjxf.cloudlib.api.ui.sync.menu.BasicMenu;
import dev.vfyjxf.cloudlib.api.ui.widget.RootWidget;
import dev.vfyjxf.cloudlib.api.ui.widget.Widget;
import dev.vfyjxf.cloudlib.api.ui.widget.WidgetGroup;
import dev.vfyjxf.cloudlib.ui.drag.DraggableManager;
import dev.vfyjxf.cloudlib.ui.overlay.UIOverlayImpl;
import mezz.jei.gui.input.MouseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

public abstract class BasicMenuScreen<T extends BasicMenu<?>> extends AbstractContainerScreen<T> {

    protected final WidgetGroup<Widget> mainGroup;
    protected final Player player;
    private final RootWidget rootWidget;
    private final UIOverlay screenOverlay = null;
    private final DraggableManager draggableManager;
    private WidgetWindow displayWindow;
    private MutableList<WidgetWindow> windows;

    public BasicMenuScreen(T menu, Inventory playerInventory) {
        super(menu, playerInventory, Component.empty());

        //region common usage
        this.player = playerInventory.player;
        //endregion

        //region setup main panel
        rootWidget = new RootWidget();
        rootWidget.mark("root");
        mainGroup = new WidgetGroup<>();
        {
            mainGroup.setRoot(rootWidget);
            mainGroup.mark("main");
            mainGroup.asChild(rootWidget);
            mainGroup.onInit(self -> {
                mainGroup.withModifier(
                        Modifier.builder()
                                .size(width, height)
                );
            });
            draggableManager = new DraggableManager(mainGroup);
        }
        //endregion
        //region screen overlay
//        var overlayPanel = mainGroup.addWidget(new WidgetGroup<>());
//        overlayPanel.mark("overlay");
//        screenOverlay = new UIOverlayImpl(overlayPanel, true);
        //endregion
    }

    protected WidgetGroup<Widget> mainGroup() {
        return mainGroup;
    }

    public UIOverlay screenOverlay() {
        return screenOverlay;
    }

    public static Modifier Modifier() {
        return Modifier.EMPTY;
    }

    @MustBeInvokedByOverriders
    @Override
    protected void init() {
        rootWidget.init();
        mainGroup.layout();
        super.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        mainGroup.render(graphics, mouseX, mouseY, partialTick);
        mainGroup.renderOverlay(graphics, mouseX, mouseY, partialTick);
        mainGroup.renderTooltip(graphics, mouseX, mouseY);
        draggableManager.renderDragging(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {

    }

    protected void containerTick() {
        mainGroup.tick();
        menu.sendReveredDataToServer();
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
            if (context.is(Minecraft.getInstance().options.keyInventory) && shouldCloseOnEsc()) {
                onClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        var context = InputContext.fromKeyboard(keyCode, scanCode, modifiers, MouseUtil.getX(), MouseUtil.getY(), true);
        var ret = mainGroup.keyReleased(context);
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
