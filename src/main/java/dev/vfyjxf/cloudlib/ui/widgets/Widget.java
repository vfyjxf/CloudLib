package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.IEventChannel;
import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.IRenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.traits.ITrait;
import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltip;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.widgets.Visibility;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.cloudlib.event.EventChannel;
import dev.vfyjxf.cloudlib.math.Dimension;
import dev.vfyjxf.cloudlib.math.Point;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;


@SuppressWarnings("unchecked")
public class Widget implements IWidget {

    protected final EventChannel eventChannel = new EventChannel(this);
    protected IModularUI ui;
    protected boolean initialized = false;
    protected IWidgetGroup<?> root;
    @Nullable
    protected IWidgetGroup<?> parent;
    protected String id = UUID.randomUUID().toString();
    protected Point position = Point.ZERO;
    protected Point absolute = calculateAbsolute();
    protected Dimension size = Dimension.ZERO;
    protected IRenderableTexture background;
    protected IRenderableTexture icon;
    protected boolean active = true;
    protected Visibility visibility = Visibility.VISIBLE;
    protected boolean moving = false;
    protected ITrait traits = ITrait.EMPTY;
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
        if (invisible()) return;
        graphics.pose().pushPose();
        {
            graphics.pose().translate(position.x, position.y, 0);
            var context = common();
            listeners(IWidgetEvent.onRender).onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.isCancelled()) return;
            renderInternal(graphics, mouseX, mouseY, partialTicks);
            listeners(IWidgetEvent.onRenderPost).onRender(graphics, mouseX, mouseY, partialTicks, common());
        }
        graphics.pose().popPose();
    }

    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (background != null) background.render(graphics);
        if (icon != null) icon.render(graphics);
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
    public IWidgetGroup<? extends IWidget> root() {
        return root;
    }

    @ApiStatus.Internal
    @Override
    public final void setRoot(IWidgetGroup<? super IWidget> root) {
        this.root = root;
    }

    @Override
    public @Nullable IWidgetGroup<? extends IWidget> parent() {
        return parent;
    }

    @Override
    public IWidget setParent(@Nullable IWidgetGroup<? super IWidget> parent) {
        this.parent = parent;
        return this;
    }

    @Override
    @MustBeInvokedByOverriders
    public void init() {
        IWidget.super.init();
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
    public IEventChannel channel() {
        return eventChannel;
    }

    @Override
    public ITrait getTrait() {
        return traits;
    }

    @Override
    public ITrait setTrait(ITrait trait) {
        return traits = trait;
    }

    @Override
    public IWidget addTrait(ITrait trait) {
        traits = traits.then(trait);
        trait.init();
        return this;
    }

    @Override
    public boolean removeTrait(ITrait trait) {
//        trait.dispose();
//        return traits.remove(trait);
        //TODO: Implement
        return false;
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
    public Point getPos() {
        return position;
    }

    @Override
    public Point getAbsolute() {
        return absolute;
    }

    @Override
    public IWidget setPos(Point position) {
        var context = common();
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
        var context = common();
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
    public Visibility visibility() {
        return visibility;
    }

    @Override
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public IWidget tooltip(String text) {
        return tooltip(Component.literal(text));
    }

    @Override
    public IWidget tooltip(LangEntry key) {
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
    public IWidget tooltips(LangEntry... keys) {
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
        if (parent.add(parent.size(), (T) this)) return this;
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
                ", visibility=" + visibility +
                ", moving=" + moving +
                '}';
    }
}
