package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.IEventChannel;
import dev.vfyjxf.cloudlib.api.event.IEventDefinition;
import dev.vfyjxf.cloudlib.api.event.IEventHandler;
import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import dev.vfyjxf.cloudlib.api.ui.IRenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.animation.IAnimatable;
import dev.vfyjxf.cloudlib.api.ui.drag.IDraggable;
import dev.vfyjxf.cloudlib.api.ui.event.IInputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;
import dev.vfyjxf.cloudlib.api.ui.traits.ITrait;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.cloudlib.math.Dimension;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.math.Rectangle;
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
import java.util.function.Supplier;

public interface IWidget extends IRenderable, IDraggable, IEventHandler<IWidget>, IAnimatable<IWidget> {

    //////////////////////////////////////
    //********       Basic     *********//
    //////////////////////////////////////

    IModularUI getUI();

    IWidget setUI(@Nullable IModularUI ui);

    /**
     * maybe itself.
     */
    IWidgetGroup<? extends IWidget> root();

    @ApiStatus.Internal
    void setRoot(IWidgetGroup<? super IWidget> root);

    @Nullable
    IWidgetGroup<? extends IWidget> parent();

    @Contract
    IWidget setParent(@Nullable IWidgetGroup<? super IWidget> parent);

    /**
     * Construct the widget
     * include layout and addTrait.
     */
    @MustBeInvokedByOverriders
    default void init() {

    }

    @ApiStatus.NonExtendable
    default IWidget onInit(IWidgetEvent.OnInit listener) {
        channel().register(IWidgetEvent.onInit, listener);
        return this;
    }

    boolean initialized();

    /**
     * Call when the widget needs to be updated.
     * <p>
     * Such as:
     * 1.Screen size changed.
     * 2.Parent resize.
     */
    default void update() {

    }

    @ApiStatus.NonExtendable
    default IWidget onUpdate(IWidgetEvent.OnUpdate listener) {
        channel().register(IWidgetEvent.onUpdate, listener);
        return this;
    }

    /**
     * Only for debugging.
     */
    @ApiStatus.Internal
    void rebuild();

    /**
     * Call every tick.
     */
    default void tick() {

    }

    @ApiStatus.NonExtendable
    default IWidget onTick(IWidgetEvent.OnTick listener) {
        channel().register(IWidgetEvent.onTick, listener);
        return this;
    }

    String getId();

    IWidget setId(String id);

    /**
     * @return Relative coordinates relative to the upper left corner of the parent.
     */
    Point getPos();

    default int getX() {
        return getPos().x;
    }

    default int getY() {
        return getPos().y;
    }

    /**
     * @return Absolute coordinates relative to the upper left corner of the screen.
     */
    Point getAbsolute();

    IWidget setPos(Point position);


    default IWidget setPos(int x, int y) {
        return setPos(new Point(x, y));
    }


    default IWidget setX(int x) {
        return setPos(x, getY());
    }


    default IWidget setY(int y) {
        return setPos(getX(), y);
    }


    default IWidget translate(int dx, int dy) {
        return setPos(getPos().x + dx, getPos().y + dy);
    }

    Dimension getSize();

    default int getWidth() {
        return getSize().width;
    }

    default int getHeight() {
        return getSize().height;
    }

    IWidget setSize(Dimension size);

    default IWidget setSize(int width, int height) {
        return setSize(new Dimension(width, height));
    }

    default Rectangle getBounds() {
        return new Rectangle(getPos().x, getPos().y, getSize().width, getSize().height);
    }

    default IWidget setWidth(int width) {
        return setSize(width, getSize().height);
    }

    default IWidget setHeight(int height) {
        return setSize(getSize().width, height);
    }

    IWidget setBackground(IRenderableTexture background);

    IWidget setIcon(IRenderableTexture icon);

    boolean active();

    void setActive(boolean active);

    Visibility visibility();

    void setVisibility(Visibility visibility);

    default boolean visible() {
        return visibility() == Visibility.VISIBLE;
    }

    default boolean invisible() {
        return visibility() != Visibility.VISIBLE;
    }

    default void setVisible(boolean visible) {
        setVisibility(visible ? Visibility.VISIBLE : Visibility.INVISIBLE);
    }

    default void hide() {
        setVisible(false);
    }

    default void show() {
        setVisible(true);
    }

    //////////////////////////////////////
    //********       layout     *********//
    //////////////////////////////////////

    default void measure(int width, int height) {

    }

    default IWidget onMeasure(IWidgetEvent.OnMeasure listener) {
        return onEvent(IWidgetEvent.onMeasure, listener);
    }

    default void layout(int x, int y, int width, int height) {

    }

    default IWidget onLayout(IWidgetEvent.OnLayout listener) {
        return onEvent(IWidgetEvent.onLayout, listener);
    }

    //////////////////////////////////////
    //********       Tools     *********//
    //////////////////////////////////////

    /**
     * @param mouseX the absolute x coordinate of the mouse
     * @param mouseY the absolute y coordinate of the mouse
     * @return true if the mouse is over this widget
     */
    default boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getAbsolute().x &&
                mouseX <= getAbsolute().x + getSize().width &&
                mouseY >= getAbsolute().y &&
                mouseY <= getAbsolute().y + getSize().height;
    }

    default boolean isMouseOver(IInputContext input) {
        return isMouseOver(input.mouseX(), input.mouseY());
    }


    /**
     * Top-down search
     */
    @Nullable
    default <T extends IWidget> T getWidgetOfType(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        return null;
    }

    default <T extends IWidget> void getWidgetsOfType(Class<T> type, List<T> result) {
        if (type.isInstance(this)) {
            result.add(type.cast(this));
        }
    }

    /**
     * down to top search.
     */
    @Nullable
    default <T extends IWidget> T findWidgetsOfType(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        var parent = parent();
        if (parent != null) {
            return parent.findWidgetsOfType(type);
        }
        return null;
    }

    default <T extends IWidget> void findWidgetsOfType(Class<T> type, List<T> result) {
        if (type.isInstance(this)) {
            result.add(type.cast(this));
        }
        var parent = parent();
        if (parent != null) {
            parent.findWidgetsOfType(type, result);
        }
    }

    @Nullable
    default IWidget getHoveredWidget(double mouseX, double mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            return this;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    default <O extends IWidget> O cast() {
        return (O) this;
    }

    //////////////////////////////////////
    //********     Tooltips    *********//
    //////////////////////////////////////

    @Nullable
    ITooltip getTooltip();

    IWidget setTooltip(@Nullable ITooltip tooltip);

    /**
     * Add single line tooltip
     */
    IWidget tooltip(String text);

    IWidget tooltip(LangEntry key);

    IWidget tooltip(Component component);

    IWidget tooltip(Supplier<Component> supplier);

    default IWidget tooltips(LangEntry... keys) {
        for (LangEntry key : keys) {
            tooltip(key);
        }
        return this;
    }

    default IWidget tooltips(Component... components) {
        for (Component component : components) {
            tooltip(component);
        }
        return this;
    }

    IWidget tooltip(ITooltip tooltip);

    default void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        ITooltip tooltip = getTooltip();
        if (tooltip != null)
            ScreenUtil.renderTooltip(Objects.requireNonNull(Minecraft.getInstance().screen), graphics, tooltip, mouseX, mouseY);
    }

    //////////////////////////////////////
    //********        Group    *********//
    //////////////////////////////////////

    /**
     * Note that this method should only be used when the IWidgetGroup doesn't care about the consistency of children types,
     * <p>
     * it's only for IWidgetGroup<IWidget>, if you need to add a widget to the group, use {@link IWidgetGroup#widget(IWidget)}.
     */
    <T extends IWidget> IWidget asChild(IWidgetGroup<T> parent);

    //////////////////////////////////////
    //********     Events      *********//
    //////////////////////////////////////

    @Override
    IEventChannel<IWidget> channel();

    default <T> IWidget onEvent(IEventDefinition<T> definition, T listener) {
        channel().register(definition, listener);
        return this;
    }

    @ApiStatus.NonExtendable
    default IWidget onRender(IWidgetEvent.OnRender listener) {
        return onEvent(IWidgetEvent.onRender, listener);
    }

    @ApiStatus.NonExtendable
    default IWidget onMouseClicked(IInputEvent.OnMouseClicked listener) {
        return onEvent(IInputEvent.onMouseClicked, listener);
    }

    @ApiStatus.NonExtendable
    default IWidget onMouseReleased(IInputEvent.OnMouseReleased listener) {
        return onEvent(IInputEvent.onMouseReleased, listener);
    }

    @ApiStatus.NonExtendable
    default IWidget onKeyReleased(IInputEvent.OnKeyReleased listener) {
        return onEvent(IInputEvent.onKeyReleased, listener);
    }

    @ApiStatus.NonExtendable
    default IWidget onKeyPressed(IInputEvent.OnKeyPressed listener) {
        return onEvent(IInputEvent.onKeyPressed, listener);
    }

    default void onDelete() {
        listeners(IWidgetEvent.onDelete).onDeleted(this);
    }

    //////////////////////////////////////
    //********   User Input    *********//
    //////////////////////////////////////

    default boolean mouseClicked(IInputContext input) {
        if (!visible() || !active() || !isMouseOver(input)) return false;
        return listeners(IInputEvent.onMouseClicked).onClicked(common(), input);
    }

    default boolean mouseReleased(IInputContext input) {
        if (!visible() || !active() || !isMouseOver(input)) return false;
        return listeners(IInputEvent.onKeyReleased).onKeyReleased(common(), input);
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    default boolean mouseMoved(double mouseX, double mouseY) {
        return false;
    }

    default boolean keyPressed(IInputContext input) {
        if (!visible() || !active()) return false;
        return listeners(IInputEvent.onKeyPressed).onKeyPressed(common(), input);
    }

    default boolean keyReleased(IInputContext input) {
        if (!visible() || !active()) return false;
        return listeners(IInputEvent.onKeyReleased).onKeyReleased(common(), input);
    }

    @Override
    default IWidget interpolate(IWidget next, float delta) {
        return this;
    }

    //TODO:scissor

    //////////////////////////////////////
    //********       Traits    *********//
    //////////////////////////////////////

    ITrait getTrait();

    ITrait setTrait(ITrait trait);

    IWidget addTrait(ITrait trait);

    default IWidget addTrait(ITrait... traits) {
        for (ITrait trait : traits) {
            addTrait(trait);
        }
        return this;
    }

    boolean removeTrait(ITrait trait);

    default IWidget fixedPosition() {
//        return this.addTrait(new FixedPositionTrait());
        throw new UnsupportedOperationException();
    }

    default IWidget relativeCoordinate(Supplier<Integer> x, Supplier<Integer> y) {
        throw new UnsupportedOperationException();
    }

    default IWidget relativeHorizontal(Supplier<Integer> x) {
        throw new UnsupportedOperationException();
    }

    default IWidget relativeVertical(Supplier<Integer> y) {
        throw new UnsupportedOperationException();
    }

    default IWidget autoSize(Supplier<Integer> width, Supplier<Integer> height) {
        throw new UnsupportedOperationException();
    }

    default IWidget autoWidth(Supplier<Integer> width) {
        throw new UnsupportedOperationException();
    }

    default IWidget autoHeight(Supplier<Integer> height) {
        throw new UnsupportedOperationException();
    }

    default IWidget dynamicSize() {
        throw new UnsupportedOperationException();
    }

}
