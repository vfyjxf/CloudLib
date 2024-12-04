package dev.vfyjxf.cloudlib.api.ui.widgets;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.vfyjxf.cloudlib.api.event.EventChannel;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Dimension;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.RichTooltip;
import dev.vfyjxf.cloudlib.api.ui.UIContext;
import dev.vfyjxf.cloudlib.api.ui.animation.Animatable;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.layout.Flex;
import dev.vfyjxf.cloudlib.api.ui.layout.Margin;
import dev.vfyjxf.cloudlib.api.ui.layout.Padding;
import dev.vfyjxf.cloudlib.api.ui.layout.Resizer;
import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.cloudlib.ui.widgets.BasicWidget;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;


@SuppressWarnings("unchecked")
public abstract class Widget implements Renderable, EventHandler<WidgetEvent>, Animatable<Widget>, Resizer {

    public static Widget create() {
        return new BasicWidget();
    }

    //event
    protected final EventChannel<WidgetEvent> eventChannel = EventChannel.create(this);

    //data
    protected boolean initialized = false;
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
    protected Dimension size = Dimension.ZERO;

    //visual
    protected RenderableTexture background;
    protected RenderableTexture icon;
    protected Visibility visibility = Visibility.VISIBLE;
    @Nullable
    protected RichTooltip richTooltip;

    //layout
    protected final Margin margin = new Margin();
    protected final Padding padding = new Padding();
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
            Pos parentPos = parent.getPos();
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

    @Override
    public EventChannel<WidgetEvent> events() {
        return eventChannel;
    }

    public String getId() {
        return id;
    }

    @Contract("_ -> this")
    public Widget setId(String id) {
        this.id = id;
        return this;
    }

    @Contract("_-> this")
    @CanIgnoreReturnValue
    public final <T extends Widget> Widget asChild(WidgetGroup<T> parent) {
        if (parent == this) throw new IllegalArgumentException("Cannot add widget to itself");
        if (parent.add(parent.size(), (T) this)) {
            this.parent = parent;
            this.onPositionUpdate();
            parent.resizer.add(parent, this);
            if (this.flex.relative() == null) {
                this.flex.setRelative(parent);
            }
        }
        return this;
    }

    //////////////////////////////////////
    //********       Area      *********//
    //////////////////////////////////////

    public Pos getPos() {
        return position;
    }

    public Pos getAbsolute() {
        return absolute;
    }

    @Contract("_ -> this")
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

    @Contract("_ -> this")
    public Widget setSize(Dimension size) {
        var context = common();
        listeners(WidgetEvent.onSizeChanged).onSizeChanged(context, size);
        if (context.cancelled()) return this;
        this.size = size;
        return this;
    }

    //////////////////////////////////////
    //*******       Render     *********//
    //////////////////////////////////////

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


    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RichTooltip richTooltip = getTooltip();
        if (richTooltip != null)
            ScreenUtil.renderTooltip(Objects.requireNonNull(Minecraft.getInstance().screen), graphics, richTooltip, mouseX, mouseY);
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
        return tooltip(Component.translatable(key.key()));
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

    @Contract("_ -> this")
    public Widget tooltips(LangEntry... keys) {
        return this;
    }

    @Contract("_ -> this")
    public Widget tooltips(Component... components) {
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

    @ApiStatus.NonExtendable
    @Contract("_ -> this")
    public final Widget onInit(WidgetEvent.OnInit listener) {
        events().register(WidgetEvent.onInit, listener);
        return this;
    }

    @ApiStatus.NonExtendable
    @Contract("_ -> this")
    public final Widget onInitPost(WidgetEvent.OnInitPost listener) {
        events().register(WidgetEvent.onInitPost, listener);
        return this;
    }

    @ApiStatus.NonExtendable
    @Contract("_ -> this")
    public final Widget onUpdate(WidgetEvent.OnUpdate listener) {
        events().register(WidgetEvent.onUpdate, listener);
        return this;
    }

    @ApiStatus.NonExtendable
    @Contract("_ -> this")
    public final Widget onTick(WidgetEvent.OnTick listener) {
        events().register(WidgetEvent.onTick, listener);
        return this;
    }

    public int posX() {
        return getPos().x;
    }

    public int posY() {
        return getPos().y;
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
        return setPos(getPos().x + dx, getPos().y + dy);
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
        return setSize(new Dimension(width, height));
    }

    @Contract("_,_ -> this")
    public Widget setSize(double width, double height) {
        return setSize(new Dimension((int) width, (int) height));
    }

    public Widget setBound(int x, int y, int width, int height) {
        return setPos(x, y)
                .setSize(width, height);
    }

    public Rect getBounds() {
        return new Rect(getPos().x, getPos().y, getSize().width, getSize().height);
    }

    @Contract("_ -> this")
    public Widget setWidth(int width) {
        return setSize(width, getSize().height);
    }

    @Contract("_ -> this")
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

    //////////////////////////////////////
    //*******       Layout     *********//
    //////////////////////////////////////

    public Widget withModifier(Modifier modifier) {
        modifier.apply(this);
        return this;
    }

    public Widget resize() {
        resizer.apply(this);
        resizer.postApply(this);
        return this;
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
        return margin;
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

    @ApiStatus.NonExtendable
    @Contract("_ -> this")
    public final Widget onRender(WidgetEvent.OnRender listener) {
        return onEvent(WidgetEvent.onRender, listener);
    }

    @ApiStatus.NonExtendable
    @Contract("_ -> this")
    public final Widget onMouseClicked(InputEvent.OnMouseClicked listener) {
        return onEvent(InputEvent.onMouseClicked, listener);
    }

    @ApiStatus.NonExtendable
    @Contract("_ -> this")
    public final Widget onMouseReleased(InputEvent.OnMouseReleased listener) {
        return onEvent(InputEvent.onMouseReleased, listener);
    }

    @ApiStatus.NonExtendable
    public final Widget onKeyReleased(InputEvent.OnKeyReleased listener) {
        return onEvent(InputEvent.onKeyReleased, listener);
    }

    @ApiStatus.NonExtendable
    @Contract("_ -> this")
    public final Widget onKeyPressed(InputEvent.OnKeyPressed listener) {
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

    //////////////////////////////////////
    //*******       Animate    *********//
    //////////////////////////////////////

    @Override
    @Contract("_,_ -> this")
    public Widget interpolate(Widget next, float delta) {
        return this;
    }


    //////////////////////////////////////
    //*******       Utils      *********//
    //////////////////////////////////////

    public <T extends WidgetEvent> Widget onEvent(EventDefinition<T> definition, T listener) {
        events().register(definition, listener);
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

    @SuppressWarnings("unchecked")
    public <O extends Widget> O cast() {
        return (O) this;
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
}
