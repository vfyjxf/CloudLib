package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.IEventContext;
import dev.vfyjxf.cloudlib.api.event.IEventManager;
import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.IRenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.constraints.IUIConstraints;
import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.traits.ITrait;
import dev.vfyjxf.cloudlib.api.ui.traits.IUITraits;
import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltip;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.data.lang.Lang;
import dev.vfyjxf.cloudlib.event.EventManager;
import dev.vfyjxf.cloudlib.math.Dimension;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.ui.traits.container.UITraits;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;


@SuppressWarnings("unchecked")
public class Widget implements IWidget {

    protected final EventManager<IWidget> eventManager = new EventManager<>(this);
    protected IModularUI ui;
    protected boolean initialized = false;
    protected IWidgetGroup<IWidget> root;
    @Nullable
    protected IWidgetGroup<?> parent;
    protected String id = UUID.randomUUID().toString();
    protected Point position = Point.ZERO;
    protected Point absolute = calculateAbsolute();
    protected Dimension size = Dimension.ZERO;
    protected IRenderableTexture background;
    protected IRenderableTexture icon;
    protected boolean active = true;
    protected boolean visible = true;
    protected boolean moving = false;
    protected IUITraits traits = new UITraits();
    @Nullable
    protected ITooltip tooltip;

    protected Point calculateAbsolute() {
        if (parent == null) return position;
        IWidgetGroup<?> parent = this.parent;
        Point absolute = position.copy();
        while (parent != null && parent != this) {
            Point parentPos = parent.getPos();
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
        if (!visible) return;
        graphics.pose().pushPose();
        {
            graphics.pose().translate(position.x, position.y, 0);
            IEventContext context = context();
            listeners(IWidgetEvent.onRender).onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.isCancelled()) return;
            if (background != null) background.render(graphics);
            if (icon != null) icon.render(graphics);
            listeners(IWidgetEvent.onRenderPost).onRender(graphics, mouseX, mouseY, partialTicks, context());
        }
        graphics.pose().popPose();
    }

    @Override
    public IWidget self() {
        return this;
    }

    @Override
    public boolean isMoving() {
        return moving;
    }

    @Override
    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    @Override
    public IModularUI getUI() {
        return ui;
    }

    @Override
    public IWidget setUI(@Nullable IModularUI ui) {
        this.ui = ui;
        return this;
    }

    @Override
    public IWidgetGroup<IWidget> root() {
        return root;
    }

    @Override
    public @Nullable IWidgetGroup<?> parent() {
        return parent;
    }

    @Override
    public IWidget setParent(@Nullable IWidgetGroup<?> parent) {
        this.parent = parent;
        return this;
    }

    @Override
    @MustBeInvokedByOverriders
    public void init() {
        IWidget.super.init();
        IWidgetGroup<?> parent = this.parent;
        while (parent != null && parent != parent.parent()) {
            parent = parent.parent();
        }
        this.root = (IWidgetGroup<IWidget>) parent;
        listeners(IWidgetEvent.onInit).onInit(this);
        initialized = true;
    }

    @Override
    @MustBeInvokedByOverriders
    public void update() {
        if (initialized)
            listeners(IWidgetEvent.onUpdate).onUpdate(this);
    }

    @Override
    @MustBeInvokedByOverriders
    public void rebuild() {
        initialized = false;
    }

    @Override
    @MustBeInvokedByOverriders
    public void tick() {
        listeners(IWidgetEvent.onTick).onTick();
    }

    @Override
    public boolean initialized() {
        return initialized;
    }

    @Override
    public IEventManager<IWidget> events() {
        return eventManager;
    }

    @Override
    public IUITraits traits() {
        return traits;
    }

    @Override
    public IWidget addTrait(ITrait trait) {
        traits.add(trait);
        trait.setHolder(this);
        trait.init();
        return this;
    }

    @Override
    public boolean removeTrait(ITrait trait) {
        trait.dispose();
        return traits.remove(trait);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public IWidget setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public IUIConstraints restraints() {
        return null;
    }

    @Override
    public Point getPos() {
        return position;
    }

    @Override
    public Point getAbsolute() {
        return absolute;
    }

    @Override
    public IWidget setPos(Point position) {
        IEventContext context = context();
        listeners(IWidgetEvent.onPositionChanged).onPositionChanged(context, position);
        if (context.isCancelled()) return this;
        this.position = position;
        return this;
    }

    @Override
    public Dimension getSize() {
        return size;
    }

    @Override
    public IWidget setSize(Dimension size) {
        IEventContext context = context();
        listeners(IWidgetEvent.onSizeChanged).onSizeChanged(context, size);
        if (context.isCancelled()) return this;
        this.size = size;
        return this;
    }

    @Override
    public IWidget setBackground(IRenderableTexture background) {
        this.background = background;
        return this;
    }

    @Override
    public IWidget setIcon(IRenderableTexture icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public boolean active() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public IWidget tooltip(String text) {
        return tooltip(Component.literal(text));
    }

    @Override
    public IWidget tooltip(Lang key) {
        return tooltip(Component.translatable(key.key()));
    }

    @Override
    public IWidget tooltip(Component component) {
        if (this.tooltip == null) {
            this.tooltip = ITooltip.create();
        }
        this.tooltip.add(component);
        return this;
    }

    @Override
    public IWidget tooltip(Supplier<Component> supplier) {
        if (this.tooltip == null) {
            this.tooltip = ITooltip.create();
        }
        this.tooltip.add(supplier);
        return this;
    }

    @Override
    public IWidget tooltips(Lang... keys) {
        return this;
    }

    @Override
    public IWidget tooltips(Component... components) {
        return this;
    }

    @Override
    public IWidget tooltip(ITooltip tooltip) {
        if (this.tooltip == null) {
            this.tooltip = ITooltip.create();
        }
        this.tooltip.ofAll(tooltip);
        return this;
    }

    @Override
    public @Nullable ITooltip getTooltip() {
        return tooltip;
    }

    @Override
    public IWidget setTooltip(@Nullable ITooltip tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @Override
    public <T extends IWidget> IWidget asChild(IWidgetGroup<T> parent) {
        parent.add((T) this);
        this.setUI(parent.getUI());
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
                ", visible=" + visible +
                ", moving=" + moving +
                '}';
    }
}
