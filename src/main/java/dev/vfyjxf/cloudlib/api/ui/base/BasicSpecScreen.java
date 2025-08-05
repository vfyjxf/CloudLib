package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.overlay.UIOverlay;
import dev.vfyjxf.cloudlib.ui.overlay.UIOverlayImpl;
import mezz.jei.gui.input.MouseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.function.Consumer;

/**
 * Flow: Spec Define ->Build Builtin Group ->Screen Init ->
 * <p>
 * Apply Fragment to Plan -> Build User Group ->  -> Widget Init -> Layout
 * <p>
 * -> Render-> User Interaction->Check state on tick end->
 * State Change -> Rebuild Group -> Widget Init -> layout -> Render -> return to User Interaction
 */
public abstract class BasicSpecScreen extends Screen implements UIOverlay.Provider {

    private final WidgetManager manager;
    private RootWidget rootWidget;
    protected WidgetGroup<Widget> mainGroup;
    private UIOverlayImpl uiOverlay;
    //    private DraggableManager draggableManager;

    protected BasicSpecScreen(PlanFragment<Widget> mainGroup, PlanFragment<Widget> overlay) {
        super(Component.empty());
        this.manager = new WidgetManager(rootPlan -> {
            rootPlan.group(mainGroupSpec(
                    mainGroup, overlay,
                    group -> {
                        this.mainGroup = group;
//                        this.draggableManager = new DraggableManager(group);
                    },
                    group -> {
                        this.uiOverlay = new UIOverlayImpl(group, true);
                    }
            ));
            RootWidget root = new RootWidget();
            rootWidget = root;
            return root;
        });
    }

    private GroupDef<Widget> mainGroupSpec(
            PlanFragment<Widget> fragment, PlanFragment<Widget> overlayFragment,
            Consumer<WidgetGroup<Widget>> onMainGroupBuild,
            Consumer<WidgetGroup<Widget>> onOverlayGroupBuild
    ) {
        return plan -> {

            plan.group(overlayGroupSpec(overlayFragment, onOverlayGroupBuild));

            fragment.applyTo(plan);

            var mainGroup = new WidgetGroup<>();
            mainGroup.mark("main");
            mainGroup.onInit(self -> {
                self.setId("main");
                self.layoutByParent = false;
                self.setWidth(width);
                self.setHeight(height);
            });
            onMainGroupBuild.accept(mainGroup);
            return mainGroup;
        };
    }

    private GroupDef<Widget> overlayGroupSpec(
            PlanFragment<Widget> fragment,
            Consumer<WidgetGroup<Widget>> overlayConsumer
    ) {
        return plan -> {
            fragment.applyTo(plan);

            var overlayGroup = new WidgetGroup<>();
            overlayGroup.mark("overlay");
            overlayConsumer.accept(overlayGroup);
            return overlayGroup;
        };
    }

    @Override
    public final UIOverlay screenOverlay() {
        if (uiOverlay == null) {
            throw new IllegalStateException("UIOverlay is not initialized yet. Please ensure the screen is initialized before accessing the overlay.");
        }
        return uiOverlay;
    }

    @MustBeInvokedByOverriders
    @Override
    protected void init() {
        manager.rebuildRequired();
        rootWidget.init();
        mainGroup.layout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        mainGroup.render(graphics, mouseX, mouseY, partialTick);
        mainGroup.renderOverlay(graphics, mouseX, mouseY, partialTick);
        mainGroup.renderTooltip(graphics, mouseX, mouseY);
//        draggableManager.renderDragging(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        mainGroup.tick();
    }

    @Override
    protected void repositionElements() {
        manager.rebuildRequired();
        mainGroup.layout();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {return mainGroup.mouseClicked(InputContext.fromMouse(MouseUtil.getX(), MouseUtil.getY(), button));}

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {return mainGroup.mouseReleased(InputContext.fromMouse(MouseUtil.getX(), MouseUtil.getY(), button, true));}

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {return mainGroup.mouseDragged(mouseX, mouseY, button, dragX, dragY);}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {return mainGroup.mouseScrolled(mouseX, mouseY, scrollX, scrollY);}

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
    public boolean charTyped(char codePoint, int modifiers) {return super.charTyped(codePoint, modifiers);}

    @Override
    public void mouseMoved(double mouseX, double mouseY) {mainGroup.mouseMoved(mouseX, mouseY);}

    @Override
    protected void setInitialFocus() {}

    @Override
    protected void setInitialFocus(GuiEventListener listener) {}
}


