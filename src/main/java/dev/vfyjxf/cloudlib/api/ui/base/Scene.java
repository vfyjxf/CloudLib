package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.event.context.BubbleContext;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.performer.PerformerContainer;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree.TraversalControl;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvents;
import dev.vfyjxf.cloudlib.ui.drag.DraggableManager;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.tree.TaffyTree;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import mezz.jei.gui.input.MouseUtil;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * UI instance in specific context
 */
public final class Scene {

    public Scene(WidgetGroup<? extends Widget> root) {
        this.root = root;
        this.draggableManager = new DraggableManager(root);
    }

    //region tree
    private final WidgetGroup<? extends Widget> root;
    final TaffyTree tree = new TaffyTree();
    private final PathCache pathCache = new PathCache();

    public WidgetGroup<? extends Widget> root() {
        return root;
    }
    //endregion

    //region performer
    private final PerformerContainer performers = new PerformerContainer();
    //endregion


    //region activity

    public void tick() {
        if (!root.lifecycle.mounted()) {
            throw new IllegalStateException("Widget is not mounted!");
        }
        root.tick();
        context.tick();
    }

    //endregion

    //region layout

    private float width = Float.NaN;
    private float height = Float.NaN;

    TaffyTree layoutTree() {
        return tree;
    }

    public void setLayoutArea(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void layout() {
        tree.computeLayout(root.nodeId(), new TaffySize<>(
            Float.isNaN(width) ? AvailableSpace.MAX_CONTENT : AvailableSpace.definite(width),
            Float.isNaN(height) ? AvailableSpace.MAX_CONTENT : AvailableSpace.definite(height)
        ));
    }

    //endregion


    //region lifecycle management

    private final ObjectSet<Widget> createdWidgets = new ObjectLinkedOpenHashSet<>();
    private final ObjectSet<Widget> unmountedWidgets = new ObjectLinkedOpenHashSet<>();
    private SceneContext context;

    void addCreatedWidget(Widget widget) {
        if (widget.lifecycle != Lifecycle.created) {
            throw new IllegalArgumentException("Widget is not created!");
        }
        createdWidgets.add(widget);
    }

    public void init() {
        if (!root.lifecycle.initialized()) {
            WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
                widget.init();
                return TraversalControl.CONTINUE;
            });
        } else {
            for (Widget createdWidget : createdWidgets) {
                createdWidget.init();
            }
            createdWidgets.clear();
        }
    }

    public void mount(SceneContext context) {
        this.context = context;
        WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
            if (!widget.lifecycle.initialized()) {
                throw new IllegalArgumentException("Widget is not initialized!");
            }
            widget.mount(this, context);
            return TraversalControl.CONTINUE;
        });
    }

    public void reuse(Widget widget) {
        //TODO:如何阻止Widget自救
        if (!widget.lifecycle.unmounted()) {
            throw new IllegalArgumentException("Cannot reuse a widget that is not unmounted!");
        }
        unmountedWidgets.remove(widget);
    }

    public void unmount(Widget widget) {
        unmountedWidgets.add(widget);
    }

    public void destroy() {
        if (!root.lifecycle.unmounted()) {
            WidgetTree.walkBottomUp(root, true, -1, ((widget, depth) -> {
                widget.unmount();
                return TraversalControl.CONTINUE;
            }));
        }
        for (Widget widget : unmountedWidgets) {
            widget.destroy();
        }
        unmountedWidgets.clear();
    }

    private void rebuildRequired() {

    }

    private void destroyUnmountedWidgets() {
        for (Widget widget : unmountedWidgets) {
            widget.destroy();
        }
        unmountedWidgets.clear();
    }

    //endregion

    //region render

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        rebuildRequired();
        root.render(graphics, mouseX, mouseY, partialTick);
        root.renderOverlay(graphics, mouseX, mouseY, partialTick);
        root.renderTooltip(graphics, mouseX, mouseY);
        draggableManager.renderDragging(graphics, mouseX, mouseY, partialTick);
        destroyUnmountedWidgets();
    }

    //endregion

    //region user input

    private static final int doubleClickThreshold = 500;//500ms
    private static final int doubleClickRadius = 5;//5px

    private final DraggableManager draggableManager;
    private @Nullable Widget currentClickWidget;

    private @Nullable Widget lastClickedWidget;
    private int lastClickButton;
    private int clickCount;
    private long lastClickTime;
    private WidgetPath lastHoveredPath;

    private Widget focusWidget;
    private FloatPos lastClickPos;

    public void focus(Widget widget) {
        focusWidget = widget;
    }

    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that was clicked.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Widget target = WidgetTree.hitTest(root, mouseX, mouseY);
        if (target != null) {
            Widget focusable = findFocusable(target, mouseX, mouseY);
            if (focusable != null) {
                focus(focusable);
            }
            var input = InputContext.fromMouse(mouseX, mouseY, button);
            var bubble = target.bubble();
            currentClickWidget = target;
            lastClickButton = button;
            return handleBubbleEvent(
                target, bubble, InputEvents.onMouseClicked,
                (listener) -> listener.onClicked(input, bubble)
            );
        }
        return false;
    }

    private static @Nullable Widget findFocusable(Widget widget, double mouseX, double mouseY) {
        if (widget.focusable) return widget;
        Widget focusable = null;
        CompositeWidget<?> parent = widget.parent();
        while (parent != null && focusable == null) {
            if (parent.focusable && parent.isMouseOver(mouseX, mouseY)) {
                focusable = parent;
            }
            parent = parent.parent();
        }
        return focusable;
    }

    /**
     * Called when a mouse button is released within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that was released.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Widget target = WidgetTree.hitTest(root, mouseX, mouseY);
        if (target != null) {
            InputContext input = InputContext.fromMouse(mouseX, mouseY, button);
            var releaseContext = target.bubble();
            var result = handleBubbleEvent(
                target, releaseContext, InputEvents.onMouseReleased,
                (listener) -> listener.onReleased(input, releaseContext)
            );

            if (currentClickWidget == target && target.isMouseOver(mouseX, mouseY)) {
                boolean isContinuousClick = lastClickedWidget == target
                                            && lastClickButton == button
                                            && System.currentTimeMillis() - lastClickTime <= doubleClickThreshold
                                            && lastClickPos != null
                                            && Math.sqrt(Math.pow(mouseX - lastClickPos.x(), 2) + Math.pow(mouseY - lastClickPos.y(), 2)) <= doubleClickRadius;

                clickCount = isContinuousClick ? clickCount + 1 : 1;

                var clickContext = target.bubble();
                result |= handleBubbleEvent(
                    target, clickContext, InputEvents.onMouseClick,
                    (listener) -> listener.onClick(input, clickCount, clickContext)
                );
                lastClickedWidget = target;
                lastClickPos = new FloatPos(mouseX, mouseY);
                lastClickButton = button;
                lastClickTime = System.currentTimeMillis();
            }
            currentClickWidget = null;
            return result;
        }
        clickCount = 0;
        lastClickedWidget = null;
        lastClickPos = null;
        lastClickButton = Integer.MIN_VALUE;
        lastClickTime = 0;
        currentClickWidget = null;
        return false;
    }

    /**
     * Called when the mouse is moved within the GUI element.
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     */
    public void mouseMoved(double mouseX, double mouseY) {
        Widget target = WidgetTree.hitTest(root, mouseX, mouseY);
        if (target != null) {
            target.listeners(InputEvents.onMouseMoved).onMoved(mouseX, mouseY, target.interruptible());
        }
        //region mouse enter/leave
        if (target != null) {
            WidgetPath currentPath = target.path();
            if (lastHoveredPath == null) {
                for (int i = 0; i < currentPath.size(); i++) {
                    Widget widget = currentPath.get(i);
                    widget.listeners(InputEvents.onMouseEnter).onEnter(mouseX, mouseY, widget.interruptible());
                }
            } else {
                int forkIndex = currentPath.commonAncestorIndex(lastHoveredPath);
                for (int i = lastHoveredPath.size() - 1; i > forkIndex; i--) {
                    Widget widget = lastHoveredPath.get(i);
                    widget.listeners(InputEvents.onMouseLeave).onLeave(mouseX, mouseY, widget.interruptible());
                }
                for (int i = forkIndex + 1; i < currentPath.size(); i++) {
                    Widget widget = currentPath.get(i);
                    widget.listeners(InputEvents.onMouseEnter).onEnter(mouseX, mouseY, widget.interruptible());
                }
            }
            lastHoveredPath = currentPath;
        } else if (lastHoveredPath != null) {
            for (int i = lastHoveredPath.size() - 1; i >= 0; i--) {
                Widget widget = lastHoveredPath.get(i);
                widget.listeners(InputEvents.onMouseLeave).onLeave(mouseX, mouseY, widget.interruptible());
            }
            lastHoveredPath = null;
        }
        //endregion
    }

    /**
     * Called when the mouse is dragged within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that is being dragged.
     * @param dragX  the X distance of the drag.
     * @param dragY  the Y distance of the drag.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        Widget target = WidgetTree.hitTest(root, mouseX, mouseY);
        if (target != null) {
            var input = InputContext.fromMouse(mouseX, mouseY, button);
            var bubble = target.bubble();
            return handleBubbleEvent(
                target, bubble, InputEvents.onMouseDragged,
                (listener) -> listener.onDragged(input, dragX, dragY, bubble)
            );
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Widget target = WidgetTree.hitTest(root, mouseX, mouseY);
        if (target != null) {
            var bubble = target.bubble();
            return handleBubbleEvent(
                target, bubble, InputEvents.onMouseScrolled,
                (listener) -> listener.onScrolled(mouseX, mouseY, scrollX, scrollY, bubble)
            );
        }
        return false;
    }

    /**
     * Called when a keyboard key is pressed within the GUI element.
     * <p>
     *
     * @param keyCode   the key code of the pressed key.
     * @param scanCode  the scan code of the pressed key.
     * @param modifiers the keyboard modifiers.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusWidget == null) return false;
        Pos pos = root.absolutePos();
        double mouseX = MouseUtil.getX() - pos.x();
        double mouseY = MouseUtil.getY() - pos.y();
        var input = InputContext.fromKeyboard(keyCode, scanCode, modifiers, mouseX, mouseY);
        var bubble = focusWidget.bubble();
        return handleBubbleEvent(
            focusWidget, bubble, InputEvents.onKeyPressed,
            (listener) -> listener.onKeyPressed(input, bubble)
        );
    }

    /**
     * Called when a keyboard key is released within the GUI element.
     * <p>
     *
     * @param keyCode   the key code of the released key.
     * @param scanCode  the scan code of the released key.
     * @param modifiers the keyboard modifiers.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (focusWidget == null) return false;
        Pos pos = root.absolutePos();
        double mouseX = MouseUtil.getX() - pos.x();
        double mouseY = MouseUtil.getY() - pos.y();
        var input = InputContext.fromKeyboard(keyCode, scanCode, modifiers, mouseX, mouseY);
        var bubble = focusWidget.bubble();
        return handleBubbleEvent(
            focusWidget, bubble, InputEvents.onKeyReleased,
            (listener) -> listener.onKeyReleased(input, bubble)
        );
    }

    /**
     * Called when a character is typed within the GUI element.
     * <p>
     *
     * @param codePoint the code point of the typed character.
     * @param modifiers the keyboard modifiers.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusWidget != null) {
            var bubble = focusWidget.bubble();
            return handleBubbleEvent(
                focusWidget, bubble, InputEvents.onCharTyped,
                (listener) -> listener.onCharTyped(codePoint, modifiers, bubble)
            );
        }
        return false;
    }

    //endregion

    //region internal

    PerformerContainer performers() {
        return performers;
    }

    /**
     * Gets the cached path for a widget in this tree.
     *
     * @param widget the widget to get path for
     * @return the widget's path from leaf to root
     */
    WidgetPath pathOf(Widget widget) {
        return pathCache.get(widget);
    }

    /**
     * Marks all cached paths as stale.
     * <p>
     * Called by container mutations (add/remove/clear/reparent). Paths are rebuilt lazily.
     */
    void invalidatePathCache() {
        pathCache.invalidate();
    }

    private static <E extends InputEvent> boolean handleBubbleEvent(Widget target, BubbleContext bubble, EventDefinition<E> event, Function<E, EventDispatch> listenerInvoke) {
        WidgetPath path = target.path();
        EventDispatch action = EventDispatch.pass;
        //NOTE:target index is path.size() - 1
        //stage 1: capture
        for (int i = 0; i < path.size() - 1; i++) {
            Widget widget = path.get(i);
            bubble.setPhase(BubbleContext.Phase.capture);
            bubble.setCurrent(widget.events());
            action = EventDispatch.max(listenerInvoke.apply(widget.listeners(event)), action);
            if (bubble.consumed() || bubble.cancelled()) return action.handled();
        }
        //stage 2: target
        {
            bubble.setPhase(BubbleContext.Phase.target);
            bubble.setCurrent(target.events());
            action = EventDispatch.max(listenerInvoke.apply(target.listeners(event)), action);
            if (bubble.consumed() || bubble.cancelled()) return action.handled();
        }
        //stage 3: bubble
        bubble.setPhase(BubbleContext.Phase.bubble);
        for (int i = path.size() - 2; i >= 0; i--) {
            Widget widget = path.get(i);
            bubble.setCurrent(widget.events());
            action = EventDispatch.max(listenerInvoke.apply(widget.listeners(event)), action);
            if (bubble.consumed() || bubble.cancelled()) return action.handled();
        }
        return action.handled();
    }

    //endregion


}
