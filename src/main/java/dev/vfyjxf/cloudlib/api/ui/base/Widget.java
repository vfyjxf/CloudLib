package dev.vfyjxf.cloudlib.api.ui.base;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.vfyjxf.cloudlib.api.data.DataAttachable;
import dev.vfyjxf.cloudlib.api.data.DataContainer;
import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.performer.Backstage;
import dev.vfyjxf.cloudlib.api.performer.PerformerContainer;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvents;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.text.RichTooltip;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.cloudlib.util.Checks;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Supplier;


/**
 * The basic unit of the UI system.
 * <p>
 * widget is a component that can be rendered on the screen.
 * It can be interacted with the mouse and keyboard.
 * <p>
 * Subsystem:
 * <ul>
 *     <li>{@code Layout}: Using {@link TaffyTree } to automatically calculates the position and size of widgets.</li>
 *     <li>{@code Event}: Implemented {@link EventHandler<WidgetEvent>} to allow combining different events to create complex widgets. </li>
 *     <li>{@code Render}: Provides events for rendering the widget itself, the widget's tooltip, the widget's overlay.</li>
 *     <li>{@code Data}: {@link DataAttachable} is implemented to allow components to attach additional data.</li>
 *     <li>{@code Performer}: {@link PerformerContainer} bound to the {@link Scene#performers()} by default.</li>
 *   <ul>
 */
@SuppressWarnings("unchecked")
@CanIgnoreReturnValue
public class Widget
    implements Renderable,
               EventHandler<WidgetEvent>,
               DataAttachable,
               Backstage {

    //region core

    Lifecycle lifecycle = Lifecycle.created;

    Scene scene;
    SceneContext context;

    /**
     * Normally,only root widget doesn't have a parent.
     */
    @UnknownNullability
    CompositeWidget<?> parent;

    @Nullable Object key;

    /**
     * The blueprint of the widget.
     */
    @Nullable Blueprint<?> blueprint;

    final StateSlot.StateContext stateContext = new StateSlot.StateContext();

    protected boolean active = true;
    //endregion

    //region layout & style

    @Nullable NodeId nodeId;

    protected final StyleContext style = new StyleContext(this);

    Layout layout;

    //endregion

    //region area & visual

    /**
     * Relative position of the widget, relative to its parent.
     */
    Pos position = Pos.origin;
    /**
     * Cached absolute position of the widget, relative to the root widget.
     */
    @Nullable Pos absolute = null;
    /**
     * Size of the widget.
     */
    Size size = Size.point;

    final VisualContext visualContext = style.visualContext();
    protected boolean visible = true;
    protected RichTooltip richTooltip = RichTooltip.empty();
    //endregion

    //region state

    //region draggable
    protected boolean draggable = false;
    protected boolean dragging = false;

    //region fucus
    protected boolean focusable = false;
    protected boolean focused = false;


    //region hover
    protected boolean hovered = false;
    //endregion

    //region event
    protected final EventChannel<WidgetEvent> eventChannel = EventChannel.create(this);
    //endregion

    //region data attachment
    protected final DataContainer dataContainer = new DataContainer(this);
    //endregion

    public Widget() {}

    //region capability

    @Override
    public DataContainer data() {
        return dataContainer;
    }

    @Override
    public PerformerContainer performers() {
        return scene().performers();
    }

    @Override
    public EventChannel<WidgetEvent> events() {
        return eventChannel;
    }

    //endregion

    //region basic

    public final Lifecycle lifecycle() {
        return lifecycle;
    }

    public final Scene scene() {
        Checks.checkArgument(lifecycle.mounted(), "Widget is not mounted!");
        return scene;
    }

    public SceneContext context() {
        Checks.checkArgument(lifecycle.mounted(), "Widget is not mounted!");
        return context;
    }

    public final @Nullable Object key() {
        return key;
    }

    public final @UnknownNullability CompositeWidget<? extends Widget> parent() {
        return parent;
    }

    public final WidgetPath path() {
        Checks.checkArgument(lifecycle.mounted(), "Widget is not mounted!");
        return this.scene.pathOf(this);
    }

    public final NodeId nodeId() {
        return Checks.checkNotNull(nodeId, "Widget is not mounted or destroyed!");
    }

    //endregion

    //region lifecycle

    public Widget onInit(WidgetEvent.OnInit listener) {
        events().register(WidgetEvent.onInit, listener);
        return this;
    }

    public Widget onMount(WidgetEvent.OnMount listener) {
        events().register(WidgetEvent.onMount, listener);
        return this;
    }

    public Widget onUnmount(WidgetEvent.OnUnmount listener) {
        events().register(WidgetEvent.onUnmount, listener);
        return this;
    }

    public Widget onDestroy(WidgetEvent.OnDestroy listener) {
        events().register(WidgetEvent.onDestroy, listener);
        return this;
    }

    void init() {
        if (lifecycle == Lifecycle.destroyed) {
            throw new IllegalArgumentException("Widget is already destroyed!");
        }
        if (!lifecycle.initialized()) {
            listeners(WidgetEvent.onInit).onInit(this);
        }
        lifecycle = Lifecycle.initialized;
    }

    void mount(Scene scene, SceneContext context, SceneHandle handle) {
        if (lifecycle == Lifecycle.destroyed) {
            throw new IllegalArgumentException("Widget is already destroyed!");
        }
        if (!lifecycle.initialized()) {
            throw new IllegalArgumentException("Widget is not initialized!");
        }
        this.scene = scene;
        this.context = context;
        this.nodeId = scene.tree.newLeaf(this.style.layoutStyle());
        //TODO:Blueprint support!
        if (parent != null) {
            scene.tree.insertChildAtIndex(parent.nodeId(), parent.children.indexOf(this), nodeId);
        }
        listeners(WidgetEvent.onMount).onMount(scene, context, handle);
        lifecycle = Lifecycle.mounted;
    }

    void unmount() {
        //TODO:Blueprint support!
        var scene = this.scene;
        if (parent != null) {
            scene.tree.removeChild(parent.nodeId(), nodeId);
        }
        scene.tree.remove(nodeId);
        this.scene = null;
        this.context = null;
        this.parent = null;
        scene.cleanupHandle(this);
        listeners(WidgetEvent.onUnmount).onUnmount();
        scene.unmount(this);
        lifecycle = Lifecycle.unmounted;
    }

    void destroy() {
        //TODO:Should we destroy a widget doesn't unmount?
        if (!lifecycle.unmounted()) {
            throw new IllegalArgumentException("Widget is not unmounted!");
        }
        this.parent = null;
        this.key = null;
        this.scene = null;
        this.context = null;
        this.nodeId = null;
        listeners(WidgetEvent.onDestroy).onDestroy(this);
        events().clearAllListeners();
        lifecycle = Lifecycle.destroyed;
    }

    public void onStateChanged() {

    }

    //endregion

    //region activity

    public Widget onTick(WidgetEvent.OnTick listener) {
        events().register(WidgetEvent.onTick, listener);
        return this;
    }

    @MustBeInvokedByOverriders
    public void tick() {
        listeners(WidgetEvent.onTick).onTick();
    }

    //endregion

    //region area

    /**
     * @return the relative position of the widget, relative to its parent.
     */
    public Pos pos() {
        return position;
    }

    /**
     * @return the absolute position of the widget, relative to the root widget.
     */
    public Pos absolutePos() {
        if (absolute == null) {
            if (parent == null) absolute = position;
            else absolute = parent.absolutePos().translate(position.x(), position.y());
        }
        return absolute;
    }

    @Contract("_ -> this")
    protected Widget setPos(Pos position) {
        var context = common();
        listeners(WidgetEvent.onPositionChanged).onPositionChanged(position, context);
        if (context.cancelled()) return this;
        this.position = position;
        onPositionChanged();
        return this;
    }

    /**
     * Invalidates the cached absolute position.
     */
    protected void onPositionChanged() {
        this.absolute = null;
    }

    public Size size() {
        return size;
    }

    @Contract("_ -> this")
    protected Widget setSize(Size size) {
        var context = common();
        listeners(WidgetEvent.onSizeChanged).onSizeChanged(size, context);
        if (context.cancelled()) return this;
        this.size = size;
        return this;
    }

    public int posX() {
        return pos().x();
    }

    public int posY() {
        return pos().y();
    }

    @Contract("_,_ -> this")
    protected Widget setPos(int x, int y) {
        return setPos(new Pos(x, y));
    }

    @Contract("_ -> this")
    protected Widget setPosX(int x) {
        return setPos(x, posY());
    }

    @Contract("_ -> this")
    protected Widget setPosY(int y) {
        return setPos(posX(), y);
    }

    @Contract("_,_ -> this")
    protected Widget translate(int dx, int dy) {
        return setPos(pos().x() + dx, pos().y() + dy);
    }

    public int width() {
        return size().width();
    }

    public int height() {
        return size().height();
    }

    public int right() {
        return posX() + width();
    }

    public int bottom() {
        return posY() + height();
    }

    @Contract("_,_ -> this")
    protected Widget setSize(int width, int height) {
        return setSize(new Size(width, height));
    }

    @Contract("_,_ -> this")
    protected Widget setSize(double width, double height) {
        return setSize(new Size((int) width, (int) height));
    }

    protected Widget setBound(int x, int y, int width, int height) {
        return setPos(x, y)
            .setSize(width, height);
    }

    protected Widget setBound(Rect rect) {
        return setBound(rect.x(), rect.y(), rect.width(), rect.height());
    }

    public Rect bounds() {
        return new Rect(pos().x(), pos().y(), size().width(), size().height());
    }

    public Rect absoluteBounds() {
        return new Rect(absolutePos().x(), absolutePos().y(), size().width(), size().height());
    }

    @Contract("_ -> this")
    protected Widget setWidth(int width) {
        return setSize(width, size().height());
    }

    @Contract("_ -> this")
    protected Widget setHeight(int height) {
        return setSize(size().width(), height);
    }

    //endregion

    //region area test

    /**
     * @param mouseX the absolute x coordinate of the mouse
     * @param mouseY the absolute y coordinate of the mouse
     * @return true if the mouse is over this widget
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= absolutePos().x() &&
               mouseX <= absolutePos().x() + size().width() &&
               mouseY >= absolutePos().y() &&
               mouseY <= absolutePos().y() + size().height();
    }

    public boolean isMouseOver(InputContext input) {
        return isMouseOver(input.mouseX(), input.mouseY());
    }

    public boolean isMouseOverRelative(double mouseX, double mouseY) {
        return size.contains(mouseX, mouseY);
    }

    public boolean intersects(Widget boundProvider) {
        return intersects(boundProvider.absoluteBounds());
    }

    public boolean intersects(int x, int y, int width, int height) {
        return this.position.x() >= x && this.position.y() >= y &&
               this.position.x() + this.size.width() <= x + width &&
               this.position.y() + this.size.height() <= y + height;
    }

    public boolean intersects(Rect bound) {
        return bound.intersects(absoluteBounds());
    }

    //endregion

    //region render

    /**
     * Render the widget with condition checks.
     *
     * @param canvas       the canvas
     * @param mouseX       the relative x coordinate of the mouse
     * @param mouseY       the relative y coordinate of the mouse
     * @param partialTicks the partial ticks
     */
    public final void renderWidget(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        if (invisible() || dragging) return;
        render(canvas, mouseX, mouseY, partialTicks);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        SceneCanvas canvas = SceneCanvas.create(graphics);
        render(canvas, mouseX, mouseY, partialTicks);
    }

    /**
     * Renders this widget using the batched canvas.
     *
     * @param canvas       the canvas for batched rendering
     * @param mouseX       relative mouse X
     * @param mouseY       relative mouse Y
     * @param partialTicks partial ticks
     */
    public void render(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        var eventContext = common();
        listeners(WidgetEvent.onRender).onRender(canvas, mouseX, mouseY, partialTicks, this, eventContext);
        if (eventContext.cancelled()) return;
        renderInternal(canvas, mouseX, mouseY, partialTicks);
        listeners(WidgetEvent.onRenderPost).onRender(canvas, mouseX, mouseY, partialTicks, this, interruptible());
    }

    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        canvas.texture(visualContext.background(), 0, 0, width(), height());
        canvas.texture(visualContext.icon(), 0, 0, width(), height());
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        RichTooltip richTooltip = tooltip();
        if (isMouseOver(mouseX, mouseY) && !richTooltip.isEmpty()) {
            var mousePos = ScreenUtil.getMousePos();
            ScreenUtil.renderTooltip(graphics, richTooltip, (int) mousePos.x, (int) mousePos.y);
        }
    }

    /**
     * Render the overlay of the widget.
     * <p>
     * E.g. slot highlight.
     * </p>
     *
     * @param canvas       the canvas
     * @param mouseX       the relative x coordinate of the mouse
     * @param mouseY       the relative y coordinate of the mouse
     * @param partialTicks the partial ticks
     */
    public void renderOverlay(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        if (invisible()) return;
        canvas.pushTransform();
        {
            canvas.translate(position.x(), position.y());
            int relativeX = mouseX - position.x();
            int relativeY = mouseY - position.y();
            if (isMouseOverRelative(relativeX, relativeY)) {
                var context = common();
                listeners(WidgetEvent.onOverlayRender).onRender(canvas, relativeX, relativeY, partialTicks, context);
                if (context.cancelled()) return;
                renderOverlayInternal(canvas, relativeX, relativeY, partialTicks);

                listeners(WidgetEvent.onOverlayRenderPost).onRender(canvas, relativeX, relativeY, partialTicks, interruptible());
            }
        }
        canvas.popTransform();
    }

    protected void renderOverlayInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {

    }

    //endregion

    //region visibility & active

    public boolean visible() {
        return visible;
    }

    public boolean invisible() {
        return !visible;
    }

    public boolean active() {
        return active;
    }

    public boolean inactive() {
        return !active;
    }

    public Widget setActive(boolean active) {
        this.active = active;
        return this;
    }

    public boolean interactable() {
        return active && visible();
    }

    //region tooltip

    //TODO:rename and refactor

    public Widget tooltip(String text) {
        return tooltip(Component.literal(text));
    }

    public Widget tooltip(LangEntry key) {
        return tooltip(key.get());
    }

    public Widget tooltip(Component component) {
        if (this.richTooltip == RichTooltip.empty()) {
            this.richTooltip = RichTooltip.create();
        }
        this.richTooltip.add(component);
        return this;
    }

    public Widget tooltip(Supplier<Component> supplier) {
        if (this.richTooltip == RichTooltip.empty()) {
            this.richTooltip = RichTooltip.create();
        }
        this.richTooltip.add(supplier);
        return this;
    }


    /**
     * NOTE: key must be without format args, if you want to use format args,use {@link #tooltip(LangEntry, Object...)}
     *
     * @param keys the keys to be translated.
     */
    @Contract("_ -> this")
    public Widget tooltips(LangEntry... keys) {
        for (LangEntry key : keys) {
            tooltip(key);
        }
        return this;
    }

    public Widget tooltip(LangEntry key, Object... args) {
        return tooltip(key.get(args));
    }

    @Contract("_ -> this")
    public Widget tooltips(Component... components) {
        for (Component component : components) {
            tooltip(component);
        }
        return this;
    }

    @Contract("_ -> this")
    public Widget tooltip(RichTooltip richTooltip) {
        if (this.richTooltip == RichTooltip.empty()) {
            this.richTooltip = RichTooltip.create();
        }
        this.richTooltip.addAll(richTooltip);
        return this;
    }

    public RichTooltip tooltip() {
        return richTooltip;
    }

    @Contract("_ -> this")
    public Widget setTooltip(RichTooltip richTooltip) {
        this.richTooltip = richTooltip;
        return this;
    }

    //endregion

    //region render hooks

    @Contract("_ -> this")
    public Widget onRender(WidgetEvent.OnRender listener) {
        return onEvent(WidgetEvent.onRender, listener);
    }

    public Widget onRenderPost(WidgetEvent.OnRenderPost listener) {
        return onEvent(WidgetEvent.onRenderPost, listener);
    }

    @Contract("_ -> this")
    public Widget onOverlayRender(WidgetEvent.OnOverlayRender listener) {
        return onEvent(WidgetEvent.onOverlayRender, listener);
    }

    //endregion

    //region style & layout

    public StyleContext style() {
        return style;
    }

    public Widget applyStyle(UIStyle style) {
        style.apply(this.style);
        return this;
    }

    public void applyLayout() {
        TaffyTree taffyTree = scene.layoutTree();
        if (nodeId != null && taffyTree.needsVisit(nodeId)) {
            Layout layout = taffyTree.getLayout(nodeId);
            this.layout = layout;
            setPos(new Pos(layout.location().x, layout.location().y));
            setSize(layout.size().width, layout.size().height);
            taffyTree.acknowledgeLayout(nodeId);
        }
    }

    //endregion

    //region input

    @Contract("_ -> this")
    public Widget onMouseClicked(InputEvent.OnMouseClicked listener) {
        return onEvent(InputEvents.onMouseClicked, listener);
    }

    @Contract("_ -> this")
    public Widget onMouseClick(InputEvent.OnMouseClick listener) {
        return onEvent(InputEvents.onMouseClick, listener);
    }

    @Contract("_ -> this")
    public Widget onMouseReleased(InputEvent.OnMouseReleased listener) {
        return onEvent(InputEvents.onMouseReleased, listener);
    }

    @Contract("_ -> this")
    public Widget onMouseDragged(InputEvent.OnMouseDragged listener) {
        return onEvent(InputEvents.onMouseDragged, listener);
    }

    public Widget onMouseEnter(InputEvent.OnMouseEnter listener) {
        return onEvent(InputEvents.onMouseEnter, listener);
    }

    public Widget onMouseLeave(InputEvent.OnMouseLeave listener) {
        return onEvent(InputEvents.onMouseLeave, listener);
    }


    @Contract("_ -> this")
    public Widget onKeyReleased(InputEvent.OnKeyReleased listener) {
        return onEvent(InputEvents.onKeyReleased, listener);
    }

    @Contract("_ -> this")
    public Widget onKeyPressed(InputEvent.OnKeyPressed listener) {
        return onEvent(InputEvents.onKeyPressed, listener);
    }

    public Widget onCharTyped(InputEvent.OnCharTyped listener) {
        return onEvent(InputEvents.onCharTyped, listener);
    }

    //endregion

    //region draggable
    public boolean draggable() {
        return draggable;
    }

    @Contract("_ -> this")
    public Widget setDraggable(boolean draggable) {
        throw new UnsupportedOperationException("Not Implemented");
//        this.draggable = draggable;
//        if (draggable) {
//            onMount((Scene scene, SceneContext context, SceneHandle handle) -> {
//                addWeakPerformer(DragProvider.scenario, DragProvider.fromWidget(this), this);
//            });
//        }
//        return this;
    }

    public boolean dragging() {
        return dragging;
    }

    @Contract("_ -> this")
    public Widget setDragging(boolean dragging) {
        this.dragging = dragging;
        return this;
    }

    public boolean hovered() {
        return hovered;
    }

    @Contract("_ -> this")
    public Widget setHovered(boolean hovered) {
        this.hovered = hovered;
        return this;
    }

    public boolean focusable() {
        return focusable;
    }

    @Contract("_ -> this")
    public Widget setFocusable(boolean focusable) {
        this.focusable = focusable;
        return this;
    }

    public boolean focused() {
        return focused;
    }

    @Contract("_ -> this")
    public Widget setFocused(boolean focused) {
        this.focused = focused;
        return this;
    }

    //endregion

    //region utils

    public <T extends WidgetEvent> Widget onEvent(EventDefinition<T> definition, T listener) {
        EventHandler.super.onEvent(definition, listener);
        return this;
    }

    @Override
    public <E extends WidgetEvent> Widget when(EventDefinition<E> definition, E listener) {
        EventHandler.super.when(definition, listener);
        return this;
    }

    /**
     * A magic cast method that allows you to cast the widget to a specific type.
     *
     * @param <O> the type of the widget.
     * @return the widget cast to the specific type.
     */
    public <O extends Widget> O cast() {
        return (O) this;
    }

    //endregion

    //region inspection

    /**
     * Collects inspection information for this widget.
     * <p>
     * Subclasses can override this method to provide Inspector-friendly properties.
     * Always call {@code super.collectInspectionInfo(...)} first to include base widget properties.
     *
     * @param collector the collector to add properties to
     */
    @MustBeInvokedByOverriders
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        // Basic info
        collector.addWithDefault("key", key, null, InspectionProperty.CATEGORY_BASIC);
        collector.addWithDefault("lifecycle", lifecycle.name(), Lifecycle.mounted.name(), InspectionProperty.CATEGORY_BASIC);

        // Layout info
        collector.add("position", position, InspectionProperty.CATEGORY_LAYOUT);
        collector.add("size", size, InspectionProperty.CATEGORY_LAYOUT);
        if (absolute != null && !absolute.equals(position)) {
            collector.add("absolute", absolute, InspectionProperty.CATEGORY_LAYOUT);
        }

        // State info
        collector.addWithDefault("active", active, true, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("visible", visible, true, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("focused", focused, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("focusable", focusable, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("hovered", hovered, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("draggable", draggable, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("dragging", dragging, false, InspectionProperty.CATEGORY_STATE);

        // Tooltip info (only if non-empty)
        if (!richTooltip.isEmpty()) {
            collector.add("hasTooltip", true, InspectionProperty.CATEGORY_VISUAL);
        }
    }

    /**
     * Gets the inspection type name for this widget.
     *
     * @return the inspection type name
     */
    public String inspectionTypeName() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        InspectionInfoCollector collector = InspectionInfoCollector.create();
        collector.setWidgetType(inspectionTypeName());
        if (key != null) {
            collector.setWidgetId(key.toString());
        }
        collectInspectionInfo(collector);
        return collector.toCompactString();
    }

    //endregion

}
