package dev.vfyjxf.cloudlib.api.ui.widgets;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.vfyjxf.cloudlib.api.actor.ActorContainer;
import dev.vfyjxf.cloudlib.api.actor.ActorHolder;
import dev.vfyjxf.cloudlib.api.data.DataAttachable;
import dev.vfyjxf.cloudlib.api.data.DataContainer;
import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.UIContext;
import dev.vfyjxf.cloudlib.api.ui.animation.Animatable;
import dev.vfyjxf.cloudlib.api.ui.drag.DragProvider;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.layout.Flex;
import dev.vfyjxf.cloudlib.api.ui.layout.Margin;
import dev.vfyjxf.cloudlib.api.ui.layout.Offset;
import dev.vfyjxf.cloudlib.api.ui.layout.Padding;
import dev.vfyjxf.cloudlib.api.ui.layout.Resizer;
import dev.vfyjxf.cloudlib.api.ui.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.text.RichTooltip;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.cloudlib.ui.widgets.BasicWidget;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;


/**
 * The basic unit of the UI system.
 * <p>
 * A widget is a component that can be rendered on the screen.
 * It can be interacted with the mouse and keyboard.
 * <p>
 * Subsystem:
 * <ul>
 *     <li>Layout: Using {@link Resizer } and {@link Flex} to automatically calculates the position and size of widgets.</li>
 *     <li>Event: Implemented {@link EventHandler<WidgetEvent>} to allow combining different events to create complex widgets. </li>
 *     <li>Render: Provides events for rendering the widget itself, the widget's tooltip, the widget's overlay.</li>
 *     <li>Data: {@link DataAttachable} is implemented to allow components to attach additional data.</li>
 *     <li>Actor: {@link ActorContainer} bound to the {@link RootWidget#actors()} by default.</li>
 *   <ul>
 */
@SuppressWarnings("unchecked")
public abstract class Widget implements
        Renderable, Animatable<Widget>,
        Resizer, EventHandler<WidgetEvent>,
        DataAttachable, ActorHolder {

    public static Widget create() {
        return new BasicWidget();
    }

    //event
    protected final EventChannel<WidgetEvent> eventChannel = EventChannel.create(this);

    //data
    protected boolean initialized = false;
    protected final DataContainer dataContainer = new DataContainer();
    /**
     * maybe itself
     */
    protected RootWidget root;
    @Nullable
    protected WidgetGroup<?> parent;
    protected String id = UUID.randomUUID().toString();
    protected boolean active = true;

    //bounds
    protected Pos position = Pos.ZERO;
    protected Pos absolute = calculateAbsolute();
    /**
     * The width and height of the widget,contains padding
     */
    protected Size size = Size.ZERO;

    //visual
    protected RenderableTexture background;
    protected RenderableTexture icon;
    protected Visibility visibility = Visibility.VISIBLE;
    @Nullable
    protected RichTooltip richTooltip;

    //draggable
    protected boolean draggable = false;
    protected boolean dragging = false;

    //layout
    protected final Flex flex = new Flex();
    protected Resizer resizer = flex;

    //////////////////////////////////////
    //********  Internal impl  *********//
    //////////////////////////////////////

    protected Pos calculateAbsolute() {
        if (parent == null) return position;
        WidgetGroup<?> parent = this.parent;
        Pos absolute = position.copy();
        while (parent != null && parent != this) {
            Pos parentPos = parent.getRelative();
            absolute.translate(parentPos.x, parentPos.y);
            parent = parent.parent();
        }
        return absolute;
    }

    protected void onPositionUpdate() {
        absolute = calculateAbsolute();
    }

    //////////////////////////////////////
    //********       Basic     *********//
    //////////////////////////////////////

    public RootWidget root() {
        return root;
    }

    @ApiStatus.Internal
    public final void setRoot(RootWidget root) {
        this.root = root;
    }

    public UIContext getContext() {
        return root.getContext();
    }

    @Flow(target = "this")
    public @Nullable WidgetGroup<? extends Widget> parent() {
        return parent;
    }

    @CanIgnoreReturnValue
    @Contract("_ -> this")
    public Widget setParent(@Nullable WidgetGroup<? super Widget> parent) {
        this.parent = parent;
        return this;
    }

    @MustBeInvokedByOverriders
    public void init() {
        listeners(WidgetEvent.onInit).onInit(this);
        initialized = true;
        listeners(WidgetEvent.onInitPost).onInit(this);
    }

    @MustBeInvokedByOverriders
    //TODO:Decide whether to keep this method
    public void update() {
        if (initialized)
            listeners(WidgetEvent.onUpdate).onUpdate(this);
    }

    @MustBeInvokedByOverriders
    public void tick() {
        listeners(WidgetEvent.onTick).onTick();
    }

    public boolean initialized() {
        return initialized;
    }

    public boolean interactable() {
        return active && visible();
    }

    @Override
    public @NotNull DataContainer dataContainer() {
        return dataContainer;
    }

    @Override
    public ActorContainer actors() {
        return root.actors();
    }

    @Override
    public EventChannel<WidgetEvent> events() {
        return eventChannel;
    }

    /**
     * @return the unique id of the widget.
     */
    public String getId() {
        return id;
    }

    @Contract("_ -> this")
    public Widget setId(String id) {
        this.id = id;
        return this;
    }

    @Contract("_ -> this")
    public Widget mark(String id) {
        return setId(id);
    }

    @Contract("_-> this")
    @CanIgnoreReturnValue
    public final <T extends Widget> Widget asChild(WidgetGroup<T> parent) {
        if (parent == this) throw new IllegalArgumentException("Cannot add widget to itself");
        if (parent.add(parent.size(), (T) this)) {
            if (this.parent != null) {
                this.parent.removeResizer(this.parent, this);
            }
            this.parent = parent;
            this.onPositionUpdate();
            parent.resizer.addResizer(parent, this);
            if (this.flex.relative() == null) {
                this.flex.setRelative(parent);
            }
        }
        return this;
    }

    @Contract("_ -> this")
    public Widget onInit(WidgetEvent.OnInit listener) {
        events().register(WidgetEvent.onInit, listener);
        return this;
    }

    @Contract("_ -> this")
    public Widget onInitPost(WidgetEvent.OnInitPost listener) {
        events().register(WidgetEvent.onInitPost, listener);
        return this;
    }

    @Contract("_ -> this")
    public Widget onRemove(WidgetEvent.OnRemove listener) {
        events().register(WidgetEvent.onRemove, listener);
        return this;
    }

    @Contract("_ -> this")
    public Widget onUpdate(WidgetEvent.OnUpdate listener) {
        events().register(WidgetEvent.onUpdate, listener);
        return this;
    }

    @Contract("_ -> this")
    public Widget onTick(WidgetEvent.OnTick listener) {
        events().register(WidgetEvent.onTick, listener);
        return this;
    }

    @Contract("_ -> this")
    public Widget onResize(WidgetEvent.OnResize listener) {
        events().register(WidgetEvent.onResize, listener);
        return this;
    }

    @Contract("_ -> this")
    public Widget onResizePost(WidgetEvent.OnResizePost listener) {
        events().register(WidgetEvent.onResizePost, listener);
        return this;
    }

    //////////////////////////////////////
    //********       Area      *********//
    //////////////////////////////////////

    /**
     * @return the relative position of the widget, relative to its parent.
     */
    public Pos getRelative() {
        return position;
    }

    /**
     * @return the absolute position of the widget, relative to the screen.
     */
    public Pos getAbsolute() {
        return absolute;
    }

    @Contract("_ -> this")
    public Widget setPos(Pos position) {
        var context = common();
        listeners(WidgetEvent.onPositionChanged).onPositionChanged(position, context);
        if (context.cancelled()) return this;
        this.position = position;
        onPositionUpdate();
        return this;
    }

    public Size getSize() {
        return size;
    }

    @Contract("_ -> this")
    public Widget setSize(Size size) {
        var context = common();
        listeners(WidgetEvent.onSizeChanged).onSizeChanged(size, context);
        if (context.cancelled()) return this;
        this.size = size;
        return this;
    }

    public int posX() {
        return getRelative().x;
    }

    public int posY() {
        return getRelative().y;
    }

    @Contract("_,_ -> this")
    public Widget setPos(int x, int y) {
        return setPos(new Pos(x, y));
    }

    @Contract("_ -> this")
    public Widget setPosX(int x) {
        return setPos(x, posY());
    }

    @Contract("_ -> this")
    public Widget setPosY(int y) {
        return setPos(posX(), y);
    }

    @Contract("_,_ -> this")
    public Widget translate(int dx, int dy) {
        return setPos(getRelative().x + dx, getRelative().y + dy);
    }

    public int getWidth() {
        return getSize().width;
    }

    public int getHeight() {
        return getSize().height;
    }

    public int right() {
        return posX() + getWidth();
    }

    public int bottom() {
        return posY() + getHeight();
    }

    @Contract("_,_ -> this")
    public Widget setSize(int width, int height) {
        return setSize(new Size(width, height));
    }

    @Contract("_,_ -> this")
    public Widget setSize(double width, double height) {
        return setSize(new Size((int) width, (int) height));
    }

    public Widget setBound(int x, int y, int width, int height) {
        return setPos(x, y)
                .setSize(width, height);
    }

    public Widget setBound(Rect rect) {
        return setBound(rect.x, rect.y, rect.width, rect.height);
    }

    public Rect getBounds() {
        return new Rect(getRelative().x, getRelative().y, getSize().width, getSize().height);
    }

    public Rect getAbsoluteBounds() {
        return new Rect(getAbsolute().x, getAbsolute().y, getSize().width, getSize().height);
    }

    @Contract("_ -> this")
    public Widget setWidth(int width) {
        return setSize(width, getSize().height);
    }

    @Contract("_ -> this")
    public Widget setHeight(int height) {
        return setSize(getSize().width, height);
    }

    //////////////////////////////////////
    //*******       Render     *********//
    //////////////////////////////////////

    /**
     * Render the widget with condition checks.
     *
     * @param graphics     the graphics
     * @param mouseX       the relative x coordinate of the mouse
     * @param mouseY       the relative y coordinate of the mouse
     * @param partialTicks the partial ticks
     */
    public final void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (invisible() || dragging) return;
        render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.pose().pushPose();
        {
            var context = common();
            listeners(WidgetEvent.onRender).onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.cancelled()) return;
            renderInternal(graphics, mouseX, mouseY, partialTicks);
            listeners(WidgetEvent.onRenderPost).onRender(graphics, mouseX, mouseY, partialTicks, interruptible());
        }
        graphics.pose().popPose();
    }

    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (background != null) background.render(graphics);
        if (icon != null) icon.render(graphics);
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        RichTooltip richTooltip = getTooltip();
        if (isMouseOver(mouseX, mouseY) && richTooltip != null) {
            var mousePos = ScreenUtil.getMousePos();
            ScreenUtil.renderTooltip(graphics, richTooltip, (int) mousePos.x, (int) mousePos.y);
        }
    }

    /**
     * Render the overlay of the widget.E.g. slot highlight.
     *
     * @param graphics     the graphics
     * @param mouseX       the relative x coordinate of the mouse
     * @param mouseY       the relative y coordinate of the mouse
     * @param partialTicks the partial ticks
     */
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

        if (invisible()) return;
        graphics.pose().pushPose();
        {
            graphics.pose().translate(position.x, position.y, 0);
            int relativeX = mouseX - position.x;
            int relativeY = mouseY - position.y;
            if (isMouseOverRelative(relativeX, relativeY)) {
                var context = common();
                listeners(WidgetEvent.onOverlayRender).onRender(graphics, relativeX, relativeY, partialTicks, context);
                if (context.cancelled()) return;
                renderOverlayInternal(graphics, relativeX, relativeY, partialTicks);

                listeners(WidgetEvent.onOverlayRenderPost).onRender(graphics, relativeX, relativeY, partialTicks, interruptible());
            }
        }
        graphics.pose().popPose();
    }

    protected void renderOverlayInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

    }

    @Contract("_ -> this")
    public Widget setBackground(RenderableTexture background) {
        this.background = background;
        return this;
    }

    @Contract("_ -> this")
    public Widget setIcon(RenderableTexture icon) {
        this.icon = icon;
        return this;
    }

    public boolean active() {
        return active;
    }

    public boolean inactive() {
        return !active;
    }

    @Contract("_ -> this")
    public Widget setActive(boolean active) {
        this.active = active;
        return this;
    }

    public Visibility visibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @Contract("_ -> this")
    public Widget tooltip(String text) {
        return tooltip(Component.literal(text));
    }

    @Contract("_ -> this")
    public Widget tooltip(LangEntry key) {
        return tooltip(key.get());
    }

    @Contract("_ -> this")
    public Widget tooltip(Component component) {
        if (this.richTooltip == null) {
            this.richTooltip = RichTooltip.create();
        }
        this.richTooltip.add(component);
        return this;
    }

    @Contract("_ -> this")
    public Widget tooltip(Supplier<Component> supplier) {
        if (this.richTooltip == null) {
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
        if (this.richTooltip == null) {
            this.richTooltip = RichTooltip.create();
        }
        this.richTooltip.ofAll(richTooltip);
        return this;
    }

    public @Nullable RichTooltip getTooltip() {
        return richTooltip;
    }

    @Contract("_ -> this")
    public Widget setTooltip(@Nullable RichTooltip richTooltip) {
        this.richTooltip = richTooltip;
        return this;
    }

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

    public boolean visible() {
        return visibility() == Visibility.VISIBLE;
    }

    public boolean invisible() {
        return visibility() != Visibility.VISIBLE;
    }

    public void setVisible(boolean visible) {
        setVisibility(visible ? Visibility.VISIBLE : Visibility.INVISIBLE);
    }

    public void hide() {
        setVisible(false);
    }

    public void show() {
        setVisible(true);
    }

    //////////////////////////////////////
    //*******       Layout     *********//
    //////////////////////////////////////

    public Widget withModifier(Modifier modifier) {
        this.events().registerManaged(WidgetEvent.onInitPost, modifier::apply, 1);
        return this;
    }

    /**
     * Resize and Layout the widget
     */
    public void layout() {
        listeners(WidgetEvent.onResize).onResize(this);
        resizer.apply(this);
        resizer.postApply(this);
        this.onPositionUpdate();
        listeners(WidgetEvent.onResizePost).onResizePost(this);
    }

    @Override
    public int getX() {
        return posX();
    }

    @Override
    public int getY() {
        return posY();
    }

    public Flex flex() {
        return flex;
    }

    public Margin margin() {
        return flex.margin();
    }

    public Padding padding() {
        return flex.padding();
    }

    public Offset offset() {
        return flex.offset();
    }

    public Resizer resizer() {
        return resizer;
    }

    public Widget resizer(Resizer resizer) {
        this.resizer = resizer;
        return this;
    }

    public Widget resetFlex() {
        this.flex.reset();
        return this;
    }

    //////////////////////////////////////
    //*******       Inputs     *********//
    //////////////////////////////////////

    @Contract("_ -> this")
    public Widget onMouseClicked(InputEvent.OnMouseClicked listener) {
        return onEvent(InputEvent.onMouseClicked, listener);
    }

    @Contract("_ -> this")
    public Widget onMouseReleased(InputEvent.OnMouseReleased listener) {
        return onEvent(InputEvent.onMouseReleased, listener);
    }

    @Contract("_ -> this")
    public Widget onMouseHover(InputEvent.OnMouseHover listener) {
        return onEvent(InputEvent.onMouseHover, listener);
    }

    @Contract("_ -> this")
    public Widget onMouseDragged(InputEvent.OnMouseDragged listener) {
        return onEvent(InputEvent.onMouseDragged, listener);
    }

    @Contract("_ -> this")
    public Widget onKeyReleased(InputEvent.OnKeyReleased listener) {
        return onEvent(InputEvent.onKeyReleased, listener);
    }

    @Contract("_ -> this")
    public Widget onKeyPressed(InputEvent.OnKeyPressed listener) {
        return onEvent(InputEvent.onKeyPressed, listener);
    }

    /**
     * @param input the input context
     * @return whether the event is consumed.
     */
    public boolean mouseClicked(InputContext input) {
        if (!interactable() || !isMouseOver(input)) return false;
        return listeners(InputEvent.onMouseClicked).onClicked(input, common());
    }

    /**
     * Most of the time, you should use this method to handle mouse click event.
     *
     * @param input the input context
     * @return whether the event is consumed.
     */
    public boolean mouseReleased(InputContext input) {
        if (!interactable() || !isMouseOver(input)) return false;
        return listeners(InputEvent.onKeyReleased).onKeyReleased(input, common());
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!interactable() || !isMouseOver(mouseX, mouseY)) return false;
        return listeners(InputEvent.onMouseScrolled).onScrolled(mouseX, mouseY, scrollX, scrollY, common());
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!interactable() || !isMouseOver(mouseX, mouseY)) return false;
        var input = InputContext.fromMouse(mouseX, mouseY, button);
        return listeners(InputEvent.onMouseDragged).onDragged(input, deltaX, deltaY, common());
    }

    public void mouseMoved(double mouseX, double mouseY) {
        listeners(InputEvent.onMouseMoved).onMoved(mouseX, mouseY, interruptible());
    }

    public boolean keyPressed(InputContext input) {
        if (!interactable()) return false;
        return listeners(InputEvent.onKeyPressed).onKeyPressed(input, common());
    }

    public boolean keyReleased(InputContext input) {
        if (!interactable()) return false;
        return listeners(InputEvent.onKeyReleased).onKeyReleased(input, common());
    }

    //////////////////////////////////////
    //*******      Draggable   *********//
    //////////////////////////////////////


    public boolean draggable() {
        return draggable;
    }

    @Contract("_ -> this")
    public Widget setDraggable(boolean draggable) {
        this.draggable = draggable;
        if (draggable) {
            onInitPost(self -> {
                addActor(DragProvider.ACTOR_KEY, DragProvider.fromWidget(this));
            });
        }
        return this;
    }

    public boolean dragging() {
        return dragging;
    }

    @Contract("_ -> this")
    public Widget setDragging(boolean dragging) {
        this.dragging = dragging;
        return this;
    }

    //////////////////////////////////////
    //*******       Animate    *********//
    //////////////////////////////////////

    @Override
    @Contract("_,_ -> this")
    public Widget interpolate(Widget next, float delta) {
        return this;
    }

    @Override
    public String toString() {
        return "Widget{" +
                "id='" + id + '\'' +
                ", initialized=" + initialized +
                ", root=" + (root == null ? "null" : root.getId()) +
                ", parent=" + (parent == null ? "null" : parent.getId()) +
                ", icon=" + icon +
                ", position=" + position +
                ", absolute=" + absolute +
                ", size=" + size +
                ", active=" + active +
                ", visibility=" + visibility +
                '}';
    }


    //////////////////////////////////////
    //*******       Utils      *********//
    //////////////////////////////////////

    public <T extends WidgetEvent> Widget onEvent(EventDefinition<T> definition, T listener) {
        EventHandler.super.onEvent(definition, listener);
        return this;
    }

    /**
     * @param mouseX the absolute x coordinate of the mouse
     * @param mouseY the absolute y coordinate of the mouse
     * @return true if the mouse is over this widget
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getAbsolute().x &&
                mouseX <= getAbsolute().x + getSize().width &&
                mouseY >= getAbsolute().y &&
                mouseY <= getAbsolute().y + getSize().height;
    }

    public boolean isMouseOver(InputContext input) {
        return isMouseOver(input.mouseX(), input.mouseY());
    }

    public boolean isMouseOverRelative(double mouseX, double mouseY) {
        return size.contains(mouseX, mouseY);
    }

    public boolean intersects(Widget boundProvider) {
        return intersects(boundProvider.getAbsoluteBounds());
    }

    public boolean intersects(int x, int y, int width, int height) {
        return this.position.x >= x && this.position.y >= y &&
                this.position.x + this.size.width <= x + width &&
                this.position.y + this.size.height <= y + height;
    }

    public boolean intersects(Rect bound) {
        return bound.intersects(getAbsoluteBounds());
    }

    @SuppressWarnings("unchecked")
    public <O extends Widget> O cast() {
        return (O) this;
    }

}
