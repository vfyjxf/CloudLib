package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Dimension;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.RichTooltip;
import dev.vfyjxf.cloudlib.api.ui.animation.Animatable;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.inputs.InputContext;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.cloudlib.ui.widgets.BasicWidget;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.awt.Rectangle;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;


@SuppressWarnings("unchecked")
public abstract class Widget implements Renderable, EventHandler<WidgetEvent>, Animatable<Widget> {

    public static Widget create() {
        return new BasicWidget();
    }

    protected final EventChannel<WidgetEvent> eventChannel = EventChannel.create(this);
    protected boolean initialized = false;
    protected WidgetGroup<?> root;
    @Nullable
    protected WidgetGroup<?> parent;
    protected String id = UUID.randomUUID().toString();
    protected Pos position = Pos.ZERO;
    protected Pos absolute = calculateAbsolute();
    protected Dimension size = Dimension.ZERO;
    protected RenderableTexture background;
    protected RenderableTexture icon;
    protected boolean active = true;
    protected Visibility visibility = Visibility.VISIBLE;
    @Nullable
    protected RichTooltip richTooltip;

    protected Pos calculateAbsolute() {
        if (parent == null) return position;
        WidgetGroup<?> parent = this.parent;
        Pos absolute = position.copy();
        while (parent != null && parent != this) {
            Pos parentPos = parent.getPos();
            absolute.translate(parentPos.x, parentPos.y);
            parent = parent.parent();
        }
        return absolute;
    }

    protected void onPositionUpdate() {
        absolute = calculateAbsolute();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (invisible()) return;
        graphics.pose().pushPose();
        {
            graphics.pose().translate(position.x, position.y, 0);
            var context = common();
            listeners(WidgetEvent.onRender).onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.cancelled()) return;
            renderInternal(graphics, mouseX, mouseY, partialTicks);
            listeners(WidgetEvent.onRenderPost).onRender(graphics, mouseX, mouseY, partialTicks, common());
        }
        graphics.pose().popPose();
    }

    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (background != null) background.render(graphics);
        if (icon != null) icon.render(graphics);
    }

    public WidgetGroup<? extends Widget> root() {
        return root;
    }

    @ApiStatus.Internal
    public final void setRoot(WidgetGroup<? super Widget> root) {
        this.root = root;
    }

    public @Nullable WidgetGroup<? extends Widget> parent() {
        return parent;
    }

    @com.google.errorprone.annotations.CanIgnoreReturnValue
    @org.jetbrains.annotations.Contract("_ -> this")
    public Widget setParent(@Nullable WidgetGroup<? super Widget> parent) {
        this.parent = parent;
        return this;
    }

    @MustBeInvokedByOverriders
    public void init() {

        listeners(WidgetEvent.onInit).onInit(this);
        initialized = true;
    }

    @MustBeInvokedByOverriders
    public void update() {
        if (initialized)
            listeners(WidgetEvent.onUpdate).onUpdate(this);
    }

    @ApiStatus.Internal
    @MustBeInvokedByOverriders
    public void rebuild() {
        initialized = false;
    }

    @MustBeInvokedByOverriders
    public void tick() {
        listeners(WidgetEvent.onTick).onTick();
    }

    public boolean initialized() {
        return initialized;
    }

    @Override
    public EventChannel<WidgetEvent> events() {
        return eventChannel;
    }

    public String getId() {
        return id;
    }

    public Widget setId(String id) {
        this.id = id;
        return this;
    }

    public Pos getPos() {
        return position;
    }

    public Pos getAbsolute() {
        return absolute;
    }

    public Widget setPos(Pos position) {
        var context = common();
        listeners(WidgetEvent.onPositionChanged).onPositionChanged(context, position);
        if (context.cancelled()) return this;
        this.position = position;
        return this;
    }

    public Dimension getSize() {
        return size;
    }

    public Widget setSize(Dimension size) {
        var context = common();
        listeners(WidgetEvent.onSizeChanged).onSizeChanged(context, size);
        if (context.cancelled()) return this;
        this.size = size;
        return this;
    }

    public Widget setBackground(RenderableTexture background) {
        this.background = background;
        return this;
    }

    public Widget setIcon(RenderableTexture icon) {
        this.icon = icon;
        return this;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Visibility visibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Widget tooltip(String text) {
        return tooltip(Component.literal(text));
    }

    public Widget tooltip(LangEntry key) {
        return tooltip(Component.translatable(key.key()));
    }

    public Widget tooltip(Component component) {
        if (this.richTooltip == null) {
            this.richTooltip = RichTooltip.create();
        }
        this.richTooltip.add(component);
        return this;
    }

    public Widget tooltip(Supplier<Component> supplier) {
        if (this.richTooltip == null) {
            this.richTooltip = RichTooltip.create();
        }
        this.richTooltip.add(supplier);
        return this;
    }

    public Widget tooltips(LangEntry... keys) {
        return this;
    }

    public Widget tooltips(Component... components) {
        return this;
    }

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

    public Widget setTooltip(@Nullable RichTooltip richTooltip) {
        this.richTooltip = richTooltip;
        return this;
    }

    public <T extends Widget> Widget asChild(WidgetGroup<T> parent) {
        if (parent.add(parent.size(), (T) this)) return this;
        this.parent = parent;
        this.onPositionUpdate();
        return this;
    }

    @Override
    public String toString() {
        return "Widget{" +
                "initialized=" + initialized +
                ", parent=" + (parent == null ? "null" : parent.getId()) +
                ", icon=" + icon +
                ", id='" + id + '\'' +
                ", position=" + position +
                ", absolute=" + absolute +
                ", size=" + size +
                ", active=" + active +
                ", visibility=" + visibility +
                '}';
    }

    @ApiStatus.NonExtendable
    public Widget onInit(WidgetEvent.OnInit listener) {
        events().register(WidgetEvent.onInit, listener);
        return this;
    }

    @ApiStatus.NonExtendable
    public Widget onUpdate(WidgetEvent.OnUpdate listener) {
        events().register(WidgetEvent.onUpdate, listener);
        return this;
    }

    @ApiStatus.NonExtendable
    public Widget onTick(WidgetEvent.OnTick listener) {
        events().register(WidgetEvent.onTick, listener);
        return this;
    }

    public int getX() {
        return getPos().x;
    }

    public int getY() {
        return getPos().y;
    }

    public Widget setPos(int x, int y) {
        return setPos(new Pos(x, y));
    }

    public Widget setX(int x) {
        return setPos(x, getY());
    }

    public Widget setY(int y) {
        return setPos(getX(), y);
    }

    public Widget translate(int dx, int dy) {
        return setPos(getPos().x + dx, getPos().y + dy);
    }

    public int getWidth() {
        return getSize().width;
    }

    public int getHeight() {
        return getSize().height;
    }

    public Widget setSize(int width, int height) {
        return setSize(new Dimension(width, height));
    }

    public Rectangle getBounds() {
        return new Rectangle(getPos().x, getPos().y, getSize().width, getSize().height);
    }

    public Widget setWidth(int width) {
        return setSize(width, getSize().height);
    }

    public Widget setHeight(int height) {
        return setSize(getSize().width, height);
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

    public void measure(int width, int height) {

    }

    public Widget onMeasure(WidgetEvent.OnMeasure listener) {
        return onEvent(WidgetEvent.onMeasure, listener);
    }

    public void layout(int x, int y, int width, int height) {

    }

    public Widget onLayout(WidgetEvent.OnLayout listener) {
        return onEvent(WidgetEvent.onLayout, listener);
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

    /**
     * Top-down search
     */
    @Nullable
    public <T extends Widget> T getWidgetOfType(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        return null;
    }

    public <T extends Widget> void getWidgetsOfType(Class<T> type, List<T> result) {
        if (type.isInstance(this)) {
            result.add(type.cast(this));
        }
    }

    /**
     * down to top search.
     */
    @Nullable
    public <T extends Widget> T findWidgetsOfType(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        var parent = parent();
        if (parent != null) {
            return parent.findWidgetsOfType(type);
        }
        return null;
    }

    public <T extends Widget> void findWidgetsOfType(Class<T> type, List<T> result) {
        if (type.isInstance(this)) {
            result.add(type.cast(this));
        }
        var parent = parent();
        if (parent != null) {
            parent.findWidgetsOfType(type, result);
        }
    }

    @Nullable
    public Widget getHoveredWidget(double mouseX, double mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            return this;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <O extends Widget> O cast() {
        return (O) this;
    }

    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RichTooltip richTooltip = getTooltip();
        if (richTooltip != null)
            ScreenUtil.renderTooltip(Objects.requireNonNull(Minecraft.getInstance().screen), graphics, richTooltip, mouseX, mouseY);
    }

    public <T extends WidgetEvent> Widget onEvent(EventDefinition<T> definition, T listener) {
        events().register(definition, listener);
        return this;
    }

    @ApiStatus.NonExtendable
    public Widget onRender(WidgetEvent.OnRender listener) {
        return onEvent(WidgetEvent.onRender, listener);
    }

    @ApiStatus.NonExtendable
    public Widget onMouseClicked(InputEvent.OnMouseClicked listener) {
        return onEvent(InputEvent.onMouseClicked, listener);
    }

    @ApiStatus.NonExtendable
    public Widget onMouseReleased(InputEvent.OnMouseReleased listener) {
        return onEvent(InputEvent.onMouseReleased, listener);
    }

    @ApiStatus.NonExtendable
    public Widget onKeyReleased(InputEvent.OnKeyReleased listener) {
        return onEvent(InputEvent.onKeyReleased, listener);
    }

    @ApiStatus.NonExtendable
    public Widget onKeyPressed(InputEvent.OnKeyPressed listener) {
        return onEvent(InputEvent.onKeyPressed, listener);
    }

    public void onDelete() {
        listeners(WidgetEvent.onDelete).onDeleted(this);
    }

    public boolean mouseClicked(InputContext input) {
        if (!visible() || !active() || !isMouseOver(input)) return false;
        return listeners(InputEvent.onMouseClicked).onClicked(common(), input);
    }

    public boolean mouseReleased(InputContext input) {
        if (!visible() || !active() || !isMouseOver(input)) return false;
        return listeners(InputEvent.onKeyReleased).onKeyReleased(common(), input);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean mouseMoved(double mouseX, double mouseY) {
        return false;
    }

    public boolean keyPressed(InputContext input) {
        if (!visible() || !active()) return false;
        return listeners(InputEvent.onKeyPressed).onKeyPressed(common(), input);
    }

    public boolean keyReleased(InputContext input) {
        if (!visible() || !active()) return false;
        return listeners(InputEvent.onKeyReleased).onKeyReleased(common(), input);
    }

    @Override
    public Widget interpolate(Widget next, float delta) {
        return this;
    }
}
