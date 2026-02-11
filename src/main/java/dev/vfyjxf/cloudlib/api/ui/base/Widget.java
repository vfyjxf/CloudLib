package dev.vfyjxf.cloudlib.api.ui.base;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.vfyjxf.cloudlib.api.data.DataAttachable;
import dev.vfyjxf.cloudlib.api.data.DataContainer;
import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
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
import dev.vfyjxf.cloudlib.api.ui.effect.Effect;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvents;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.ZIndexProperty;
import dev.vfyjxf.cloudlib.api.ui.text.RichTooltip;

import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.cloudlib.util.Checks;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
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
     * Size of the widget.
     */
    Size size = Size.point;

    /**
     * The viewport managing this widget's coordinate transform pipeline.
     * Layout, content offset and user transforms are all managed by the viewport.
     */
    final Viewport viewport = Viewport.create();
    {
        // When this widget's viewport is invalidated, clear the absolute-pos cache
        // for this widget AND all its descendants.
        viewport.onInvalidate = this::invalidateAbsolutePos;
    }

    /**
     * Cached absolute (scene-space) position of this widget.
     * Cleared when this widget's or any ancestor's viewport is invalidated.
     */
    @Nullable Pos cachedAbsolutePos;

    /**
     * The scene layer this widget belongs to.
     * content layer is the default, widgets in higher layers are rendered on top.
     */
    SceneLayer sceneLayer = SceneLayer.content;

    final VisualContext visualContext = style.visualContext();
    protected boolean visible = true;
    protected RichTooltip richTooltip = RichTooltip.empty();
    //endregion

    //region state

    //region draggable
    protected boolean draggable = false;
    boolean dragging = false;

    //region fucus
    protected boolean focusable = false;
    boolean focused = false;


    //region hover
    boolean hovered = false;
    //endregion

    //region event
    protected final EventChannel<WidgetEvent> eventChannel = EventChannel.create(this);
    //endregion

    //region data attachment
    protected final DataContainer dataContainer = new DataContainer(this);
    //endregion

    //region z-index management
    {
        // Register listener for zIndex changes
        style.addChangeListener(ZIndexProperty.type, (oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                if (parent instanceof CompositeWidget<?> composite) {
                    composite.markChildrenOrderDirty();
                }
                // Notify scene to re-sort the layer this widget belongs to
                if (scene != null && sceneLayer != SceneLayer.content) {
                    scene.resortLayer(sceneLayer);
                }
            }
        });
    }

    //endregion

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
        // Register with layer if not in content layer
        if (sceneLayer != SceneLayer.content) {
            scene.addToLayer(sceneLayer, this);
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
        scene.removeFromAllLayers(this);
        this.scene = null;
        this.context = null;
        this.parent = null;
        scene.cleanupHandle(this);
        listeners(WidgetEvent.onUnmount).onUnmount();
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
     * Returns this widget's viewport for coordinate transform management.
     *
     * @return the viewport
     */
    public final Viewport viewport() {
        return viewport;
    }

    /**
     * @return the layout position of the widget, relative to its parent.
     */
    public final Pos pos() {
        return viewport.layoutPos();
    }

    /**
     * @return the scene position of the widget, computed by chaining viewport transforms
     *         from root to this widget.
     */
    public final Pos absolutePos() {
        Pos cached = cachedAbsolutePos;
        if (cached != null) return cached;
        FloatPos scene = localToScene(0, 0);
        cached = new Pos((int) scene.x, (int) scene.y);
        cachedAbsolutePos = cached;
        return cached;
    }

    /**
     * Converts a scene-space position to this widget's local space.
     * Walks from the root down through each ancestor's viewport inverse transform.
     */
    public FloatPos sceneToLocal(double x, double y) {
        WidgetPath path = path();
        double cx = x;
        double cy = y;
        for (int i = 0; i < path.size(); i++) {
            Widget w = path.get(i);
            FloatPos local = w.viewport.parentToLocal(cx, cy);
            cx = local.x;
            cy = local.y;
            // After transforming to w's local space, apply content offset to get
            // to content space (children's parent space) — but only if there are
            // more widgets to descend into.
            if (i < path.size() - 1) {
                cx += w.viewport.contentOffsetX;
                cy += w.viewport.contentOffsetY;
            }
        }
        return new FloatPos(cx, cy);
    }

    /**
     * Converts a local position to scene space.
     * Walks from this widget up through each ancestor's viewport forward transform.
     */
    public FloatPos localToScene(double x, double y) {
        double cx = x;
        double cy = y;
        Widget w = this;
        while (w != null) {
            // viewport.localToParent transforms from w's local space to w's parent's content space
            FloatPos p = w.viewport.localToParent(cx, cy);
            cx = p.x;
            cy = p.y;
            // Undo parent's content offset to get from parent's content space to parent's local space
            if (w.parent != null) {
                cx -= w.parent.viewport.contentOffsetX;
                cy -= w.parent.viewport.contentOffsetY;
            }
            w = w.parent;
        }
        return new FloatPos(cx, cy);
    }

    @Contract("_ -> this")
    protected Widget setPos(Pos position) {
        var context = common();
        listeners(WidgetEvent.onPositionChanged).onPositionChanged(position, context);
        if (context.cancelled()) return this;
        viewport.setLayout(position);
        return this;
    }

    /**
     * Called when the position changes. Override to react to position updates.
     */
    protected void onPositionChanged() {
        // no-op — viewport.invalidate() triggers invalidateAbsolutePos() via callback
    }

    /**
     * Clears the cached absolute position for this widget and recursively
     * for all descendants. Called when this widget's viewport is invalidated.
     */
    void invalidateAbsolutePos() {
        cachedAbsolutePos = null;
        if (this instanceof CompositeWidget<?> composite) {
            for (int i = 0; i < composite.children.size(); i++) {
                composite.children.get(i).invalidateAbsolutePos();
            }
        }
    }

    public final Size size() {
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

    public final int posX() {
        return pos().x();
    }

    public final int posY() {
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
    protected final Widget translate(int dx, int dy) {
        return setPos(pos().x() + dx, pos().y() + dy);
    }

    public final int width() {
        return size().width();
    }

    public final int height() {
        return size().height();
    }

    public final int right() {
        return posX() + width();
    }

    public final int bottom() {
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

    public final Rect bounds() {
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
     * Tests whether the given scene-space mouse position is over this widget.
     * Uses sceneToLocal to handle non-trivial transforms (scroll, zoom, rotation).
     *
     * @param mouseX the absolute x coordinate of the mouse (scene space)
     * @param mouseY the absolute y coordinate of the mouse (scene space)
     * @return true if the mouse maps into this widget's local bounds
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        FloatPos local = sceneToLocal(mouseX, mouseY);
        return local.x >= 0 && local.x <= size.width()
               && local.y >= 0 && local.y <= size.height();
    }

    public boolean isMouseOver(InputContext input) {
        return isMouseOver(input.mouseX(), input.mouseY());
    }

    public boolean intersects(Widget boundProvider) {
        return intersects(boundProvider.absoluteBounds());
    }

    public boolean intersects(int x, int y, int width, int height) {
        Pos p = pos();
        return p.x() >= x && p.y() >= y &&
               p.x() + this.size.width() <= x + width &&
               p.y() + this.size.height() <= y + height;
    }

    public boolean intersects(Rect bound) {
        return bound.intersects(absoluteBounds());
    }

    //endregion

    //region render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        SceneCanvas canvas = SceneCanvas.create(graphics);
        render(canvas, mouseX, mouseY, partialTicks);
    }

    /**
     * Renders this widget completely.
     * <p>
     * As a {@link Renderable} implementation, this method renders the entire widget
     * without checking visibility or dragging state. Those checks should be done
     * by the caller (e.g., {@link SceneCanvas#renderWidgets}).
     * <p>
     * This method fires render events and delegates to {@link #renderInternal}.
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

    /**
     * Checks if this widget should be rendered.
     * <p>
     * Used by {@link SceneCanvas#renderWidgets} to skip invisible or dragging widgets.
     *
     * @return true if the widget should be rendered
     */
    public boolean shouldRender() {
        return visible && !dragging;
    }

    /**
     * Internal rendering of this widget.
     * <p>
     * Override this method to customize widget rendering.
     * Default implementation renders background and icon textures.
     *
     * @param canvas       the canvas for batched rendering
     * @param mouseX       relative mouse X
     * @param mouseY       relative mouse Y
     * @param partialTicks partial ticks
     */
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        canvas.texture(visualContext.background(), 0, 0, width(), height());
        canvas.texture(visualContext.icon(), 0, 0, width(), height());
    }

    //endregion

    //region visibility & active

    public boolean visible() {
        return visible;
    }

    public boolean invisible() {
        return !visible;
    }

    /**
     * Sets the visibility of this widget.
     *
     * @param visible true to show, false to hide
     * @return this widget for chaining
     */
    public Widget setVisible(boolean visible) {
        this.visible = visible;
        return this;
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
        return active && visible;
    }

    //endregion

    //region scene layer & zIndex

    /**
     * Gets the scene layer this widget belongs to.
     *
     * @return the scene layer
     */
    public SceneLayer sceneLayer() {
        return sceneLayer;
    }

    /**
     * Sets the scene layer for this widget and registers with the Scene.
     * <p>
     * Changing the layer will move this widget to a different rendering layer.
     * The widget must be mounted for this to take effect.
     *
     * @param layer the scene layer
     * @return this widget for chaining
     */
    public Widget setSceneLayer(SceneLayer layer) {
        if (sceneLayer != layer) {
            SceneLayer oldLayer = sceneLayer;
            sceneLayer = layer;
            // If mounted, update the scene's layer registrations
            if (scene != null) {
                scene.removeFromLayer(oldLayer, this);
                if (layer != SceneLayer.content) {
                    scene.addToLayer(layer, this);
                }
            }
        }
        return this;
    }

    /**
     * Gets the z-index of this widget.
     * <p>
     * Z-index only affects ordering among siblings (children of the same parent)
     * and among widgets in the same layer.
     * Lower values render first (appear behind), higher values render last (appear on top).
     *
     * @return the z-index
     */
    public int zIndex() {
        return visualContext.zIndex();
    }

    //endregion

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

    public final Widget useStyle(UIStyle style) {
        style.apply(this.style);
        if (scene != null) {
            scene.tree.markDirty(nodeId);
        }
        return this;
    }

    public final Widget useStyle(StyleProperty... properties) {
        for (StyleProperty property : properties) {
            property.apply(this.style);
        }
        if (scene != null) {
            scene.tree.markDirty(nodeId);
        }
        return this;
    }

    //region effect

    /**
     * Applies multiple effects to this widget.
     *
     * @param effects the effects to apply
     * @return this widget for chaining
     */
    public final Widget useEffect(Effect... effects) {
        for (Effect effect : effects) {
            effect.apply(this);
        }
        return this;
    }

    //endregion

    public Layout layout() {
        Checks.checkArgument(layout != null, "layout is not applied");
        return layout;
    }

    public void applyLayout() {
        TaffyTree taffyTree = scene().layoutTree();
        if (nodeId != null && taffyTree.needsVisit(nodeId)) {
            Layout layout = taffyTree.getLayout(nodeId);
            this.layout = layout;
            Pos newPos = new Pos(layout.location().x, layout.location().y);
            Size newSize = new Size((int) layout.size().width, (int) layout.size().height);
            // Update viewport layout and widget size
            viewport.setLayout(newPos);
            viewport.setViewportSize(newSize);
            setSize(newSize);
            taffyTree.acknowledgeLayout(nodeId);
        }
    }

    public boolean needsLayout() {
        return scene().layoutTree().needsVisit(nodeId());
    }

    //endregion

    //region input

    @Contract("_ -> this")
    public Widget onMouseClicked(InputEvent.OnMouseClicked listener) {
        return onEvent(InputEvents.onMouseClicked, (input, context) -> {
            if (context.bubbling() || context.targeting()) listener.onClicked(input, context);
            return EventDispatch.pass;
        });
    }

    @Contract("_,_ -> this")
    public Widget onMouseClicked(InputEvent.OnMouseClicked listener, boolean capture) {
        return onEvent(InputEvents.onMouseClicked, ((input, context) -> {
            if ((capture && context.capturing()) || (!capture && (context.bubbling() || context.targeting())))
                return EventDispatch.pass;
            return listener.onClicked(input, context);
        }));
    }

    @Contract("_ -> this")
    public Widget onMouseClick(InputEvent.OnMouseClick listener) {
        return onEvent(InputEvents.onMouseClick, (input, clickCount, context) -> {
            if (context.targeting() || context.bubbling()) listener.onClick(input, clickCount, context);
            return EventDispatch.pass;
        });
    }

    @Contract("_,_ -> this")
    public Widget onMouseClick(InputEvent.OnMouseClick listener, boolean capture) {
        return onEvent(InputEvents.onMouseClick, ((input, clickCount, context) -> {
            if ((capture && context.capturing()) || (!capture && (context.bubbling() || context.targeting())))
                return EventDispatch.pass;
            return listener.onClick(input, clickCount, context);
        }));
    }

    @Contract("_ -> this")
    public Widget onMouseReleased(InputEvent.OnMouseReleased listener) {
        return onEvent(InputEvents.onMouseReleased, (input, context) -> {
            if (context.targeting() || context.bubbling()) listener.onReleased(input, context);
            return EventDispatch.pass;
        });
    }

    @Contract("_,_ -> this")
    public Widget onMouseReleased(InputEvent.OnMouseReleased listener, boolean capture) {
        return onEvent(InputEvents.onMouseReleased, ((input, context) -> {
            if ((capture && context.capturing()) || (!capture && (context.bubbling() || context.targeting())))
                return EventDispatch.pass;
            return listener.onReleased(input, context);
        }));
    }

    @Contract("_ -> this")
    public Widget onMouseDragged(InputEvent.OnMouseDragged listener) {
        return onEvent(InputEvents.onMouseDragged, ((input, deltaX, deltaY, context) -> {
            if (context.targeting() || context.bubbling()) listener.onDragged(input, deltaX, deltaY, context);
            return EventDispatch.pass;
        }));
    }

    @Contract("_,_ -> this")
    public Widget onMouseDragged(InputEvent.OnMouseDragged listener, boolean capture) {
        return onEvent(InputEvents.onMouseDragged, ((input, deltaX, deltaY, context) -> {
            if ((capture && context.capturing()) || (!capture && (context.bubbling() || context.targeting())))
                return EventDispatch.pass;
            return listener.onDragged(input, deltaX, deltaY, context);
        }));
    }

    public Widget onMouseEnter(InputEvent.OnMouseEnter listener) {
        return onEvent(InputEvents.onMouseEnter, listener);
    }

    public Widget onMouseLeave(InputEvent.OnMouseLeave listener) {
        return onEvent(InputEvents.onMouseLeave, listener);
    }

    public Widget onMouseScrolled(InputEvent.OnMouseScrolled listener) {
        return onEvent(InputEvents.onMouseScrolled, (mouseX, mouseY, scrollX, scrollY, context) -> {
            if (context.bubbling() || context.targeting())
                listener.onScrolled(mouseX, mouseY, scrollX, scrollY, context);
            return EventDispatch.pass;
        });
    }

    public Widget onMouseScrolled(InputEvent.OnMouseScrolled listener, boolean capture) {
        return onEvent(InputEvents.onMouseScrolled, ((mouseX, mouseY, scrollX, scrollY, context) -> {
            if ((capture && context.capturing()) || (!capture && (context.bubbling() || context.targeting())))
                return EventDispatch.pass;
            return listener.onScrolled(mouseX, mouseY, scrollX, scrollY, context);
        }));
    }

    @Contract("_ -> this")
    public Widget onKeyReleased(InputEvent.OnKeyReleased listener) {
        return onEvent(InputEvents.onKeyReleased, (input, context) -> {
            if (context.targeting() || context.bubbling()) listener.onKeyReleased(input, context);
            return EventDispatch.pass;
        });
    }

    @Contract("_,_ -> this")
    public Widget onKeyReleased(InputEvent.OnKeyReleased listener, boolean capture) {
        return onEvent(InputEvents.onKeyReleased, ((input, context) -> {
            if ((capture && context.capturing()) || (!capture && (context.bubbling() || context.targeting())))
                return EventDispatch.pass;
            return listener.onKeyReleased(input, context);
        }));
    }

    @Contract("_ -> this")
    public Widget onKeyPressed(InputEvent.OnKeyPressed listener) {
        return onEvent(InputEvents.onKeyPressed, (input, context) -> {
            if (context.targeting() || context.bubbling()) listener.onKeyPressed(input, context);
            return EventDispatch.pass;
        });
    }

    @Contract("_,_ -> this")
    public Widget onKeyPressed(InputEvent.OnKeyPressed listener, boolean capture) {
        return onEvent(InputEvents.onKeyPressed, ((input, context) -> {
            if ((capture && context.capturing()) || (!capture && (context.bubbling() || context.targeting())))
                return EventDispatch.pass;
            return listener.onKeyPressed(input, context);
        }));
    }

    public Widget onCharTyped(InputEvent.OnCharTyped listener) {
        return onEvent(InputEvents.onCharTyped, (codePoint, modifiers, context) -> {
            if (context.targeting() || context.bubbling()) listener.onCharTyped(codePoint, modifiers, context);
            return EventDispatch.pass;
        });
    }

    @Contract("_,_ -> this")
    public Widget onCharTyped(InputEvent.OnCharTyped listener, boolean capture) {
        return onEvent(InputEvents.onCharTyped, ((codePoint, modifiers, context) -> {
            if ((capture && context.capturing()) || (!capture && (context.bubbling() || context.targeting())))
                return EventDispatch.pass;
            return listener.onCharTyped(codePoint, modifiers, context);
        }));
    }

    //endregion

    //region focus

    public final Widget onFocus(WidgetEvent.OnFocus listener) {
        return onEvent(WidgetEvent.onFocus, listener);
    }

    public final Widget onFocusLost(WidgetEvent.OnFocusLost listener) {
        return onEvent(WidgetEvent.onFocusLost, listener);
    }

    public final Widget onFocusIn(WidgetEvent.OnFocusIn listener) {
        return onEvent(WidgetEvent.onFocusIn, listener);
    }

    public final Widget onFocusOut(WidgetEvent.OnFocusOut listener) {
        return onEvent(WidgetEvent.onFocusOut, listener);
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

    //TODO:set this by framework
    @Contract("_ -> this")
    public Widget setDragging(boolean dragging) {
        this.dragging = dragging;
        return this;
    }

    public boolean hovered() {
        return hovered;
    }

    public boolean focusable() {
        return focusable;
    }

    @Contract("_ -> this")
    protected Widget setFocusable(boolean focusable) {
        this.focusable = focusable;
        return this;
    }

    public boolean focused() {
        return focused;
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
        collector.add("position", pos(), InspectionProperty.CATEGORY_LAYOUT);
        collector.add("size", size, InspectionProperty.CATEGORY_LAYOUT);
        Pos abs = absolutePos();
        if (!abs.equals(pos())) {
            collector.add("absolute", abs, InspectionProperty.CATEGORY_LAYOUT);
        }

        // State info
        collector.addWithDefault("active", active, true, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("visible", visible, true, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("focused", focused, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("focusable", focusable, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("hovered", hovered, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("draggable", draggable, false, InspectionProperty.CATEGORY_STATE);
        collector.addWithDefault("dragging", dragging, false, InspectionProperty.CATEGORY_STATE);

        // Render info
        collector.addWithDefault("sceneLayer", sceneLayer().name(), SceneLayer.content.name(), InspectionProperty.CATEGORY_VISUAL);
        collector.addWithDefault("zIndex", zIndex(), 0, InspectionProperty.CATEGORY_VISUAL);

        // Tooltip info (only if non-empty)
        if (!richTooltip.isEmpty()) {
            collector.add("hasTooltip", true, InspectionProperty.CATEGORY_VISUAL);
        }

        // Style info - collect all applied style properties with "style-" prefix
        InspectionInfoCollector styleCollector = InspectionInfoCollector.create();
        style.collectStyleInspection(styleCollector);

        // Re-categorize style properties with "style-" prefix
        for (var property : styleCollector.getAll()) {
            String newCategory = "style-" + property.category();
            collector.add(InspectionProperty.withDefault(
                property.name(),
                property.value(),
                property.defaultValue(),
                newCategory
            ));
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
