package dev.vfyjxf.cloudlib.api.ui.widgets;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.vfyjxf.cloudlib.api.event.IEventDefinition;
import dev.vfyjxf.cloudlib.api.event.IEventHolder;
import dev.vfyjxf.cloudlib.api.event.IEventManager;
import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import dev.vfyjxf.cloudlib.api.ui.IRenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.drag.IDraggable;
import dev.vfyjxf.cloudlib.api.ui.event.IInputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;
import dev.vfyjxf.cloudlib.api.ui.constraints.IUIConstraints;
import dev.vfyjxf.cloudlib.api.ui.traits.ITrait;
import dev.vfyjxf.cloudlib.data.lang.Lang;
import dev.vfyjxf.cloudlib.math.Dimension;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.math.Rectangle;
import dev.vfyjxf.cloudlib.ui.traits.AutoSizeUpdateTrait;
import dev.vfyjxf.cloudlib.ui.traits.DynamicSizeTrait;
import dev.vfyjxf.cloudlib.ui.traits.FixedPositionTrait;
import dev.vfyjxf.cloudlib.ui.traits.RelativeCoordinateTrait;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public interface IWidget extends IRenderable, IDraggable, IEventHolder<IWidget> {

    //////////////////////////////////////
    //********       Basic     *********//
    //////////////////////////////////////

    IModularUI getUI();

    @CanIgnoreReturnValue
    IWidget setUI(@Nullable IModularUI ui);

    /**
     * maybe itself.
     */
    IWidgetGroup<IWidget> root();

    @Nullable
    IWidgetGroup<?> parent();

    @CanIgnoreReturnValue
    IWidget setParent(@Nullable IWidgetGroup<?> parent);

    /**
     * Construct the widget
     * include layout and traits.
     */
    @MustBeInvokedByOverriders
    default void init() {

    }

    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default IWidget onInit(IWidgetEvent.OnInit listener) {
        events().register(IWidgetEvent.onInit, listener);
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

    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default IWidget onUpdate(IWidgetEvent.OnUpdate listener) {
        events().register(IWidgetEvent.onUpdate, listener);
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

    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default IWidget onTick(IWidgetEvent.OnTick listener) {
        events().register(IWidgetEvent.onTick, listener);
        return this;
    }

    List<ITrait> traits();

    @CanIgnoreReturnValue
    IWidget trait(ITrait trait);

    default IWidget traits(ITrait... traits) {
        for (ITrait attribute : traits) {
            trait(attribute);
        }
        return this;
    }

    boolean removeTrait(ITrait trait);

    String getId();

    @CanIgnoreReturnValue
    IWidget setId(String id);

    IUIConstraints restraints();

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

    @CanIgnoreReturnValue
    default IWidget setPos(int x, int y) {
        return setPos(new Point(x, y));
    }

    @CanIgnoreReturnValue
    default IWidget setX(int x) {
        return setPos(x, getY());
    }

    @CanIgnoreReturnValue
    default IWidget setY(int y) {
        return setPos(getX(), y);
    }

    @CanIgnoreReturnValue
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

    @CanIgnoreReturnValue
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

    boolean visible();

    void setVisible(boolean visible);

    default void hide() {
        setVisible(false);
    }

    default void show() {
        setVisible(true);
    }

    //////////////////////////////////////
    //********       tools     *********//
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
        return isMouseOver(input.getMouseX(), input.getMouseY());
    }


    /**
     * Top-down search
     */
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

    IWidget tooltip(Lang key);

    IWidget tooltip(Component component);

    IWidget tooltip(Supplier<Component> supplier);

    default IWidget tooltips(Lang... keys) {
        for (Lang key : keys) {
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
    @CanIgnoreReturnValue
    <T extends IWidget> IWidget asChild(IWidgetGroup<T> parent);

    //////////////////////////////////////
    //********     Events      *********//
    //////////////////////////////////////

    @Override
    IEventManager<IWidget> events();

    default <T> IWidget onEvent(IEventDefinition<T> definition, T listener) {
        events().register(definition, listener);
        return this;
    }

    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default IWidget onRender(IWidgetEvent.OnRender listener) {
        return onEvent(IWidgetEvent.onRender, listener);
    }

    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default IWidget onMouseClicked(IInputEvent.OnMouseClicked listener) {
        return onEvent(IInputEvent.onMouseClicked, listener);
    }

    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default IWidget onMouseReleased(IInputEvent.OnMouseReleased listener) {
        return onEvent(IInputEvent.onMouseReleased, listener);
    }

    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default IWidget onKeyReleased(IInputEvent.OnKeyReleased listener) {
        return onEvent(IInputEvent.onKeyReleased, listener);
    }

    @CanIgnoreReturnValue
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
        return listeners(IInputEvent.onMouseClicked).onClicked(context(), input);
    }

    default boolean mouseReleased(IInputContext input) {
        if (!visible() || !active() || !isMouseOver(input)) return false;
        return listeners(IInputEvent.onKeyReleased).onKeyReleased(context(), input);
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
        return listeners(IInputEvent.onKeyPressed).onKeyPressed(context(), input);
    }

    default boolean keyReleased(IInputContext input) {
        if (!visible() || !active()) return false;
        return listeners(IInputEvent.onKeyReleased).onKeyReleased(context(), input);
    }

    //TODO:scissor

    //////////////////////////////////////
    //********       Traits    *********//
    //////////////////////////////////////


    default IWidget fixedPosition() {
        return this.trait(new FixedPositionTrait());
    }

    default IWidget relativeCoordinate(Supplier<Integer> x, Supplier<Integer> y) {
        return this.trait(new RelativeCoordinateTrait(x, y));
    }

    default IWidget relativeHorizontal(Supplier<Integer> x) {
        return this.trait(new RelativeCoordinateTrait(x, this::getY));
    }

    default IWidget relativeVertical(Supplier<Integer> y) {
        return this.trait(new RelativeCoordinateTrait(this::getX, y));
    }

    default IWidget autoSize(Supplier<Integer> width, Supplier<Integer> height) {
        return this.trait(new AutoSizeUpdateTrait(width, height));
    }

    default IWidget autoWidth(Supplier<Integer> width) {
        return this.trait(new AutoSizeUpdateTrait(width, this::getHeight));
    }

    default IWidget autoHeight(Supplier<Integer> height) {
        return this.trait(new AutoSizeUpdateTrait(this::getWidth, height));
    }

    default IWidget dynamicSize() {
        return this.trait(new DynamicSizeTrait());
    }

}
