package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.IEventContext;
import dev.vfyjxf.cloudlib.api.ui.drag.IDragConsumer;
import dev.vfyjxf.cloudlib.api.ui.event.IInputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;
import dev.vfyjxf.cloudlib.api.ui.modular.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import net.minecraft.client.gui.GuiGraphics;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WidgetGroup<T extends IWidget> extends Widget implements IWidgetGroup<T> {

    protected final MutableList<T> children = Lists.mutable.empty();


    @Override
    public IWidgetGroup<T> setUI(IModularUI ui) {
        super.setUI(ui);
        return this;
    }

    @Override
    public void init() {
        super.init();
        for (T child : children) {
            if (child.getUI() != this.ui) {
                child.setUI(this.ui);
            }
            child.init();
        }
    }

    @Override
    public void update() {
        super.update();
        if (!initialized) return;
        for (T child : children) {
            if (child.initialized())
                child.update();
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (T child : children) {
            if (child.active())
                child.tick();
        }
    }

    @Override
    public IWidgetGroup<T> self() {
        return this;
    }

    @Override
    public IDragConsumer<T> add(T draggable, Runnable onEnter) {
        if (!children.contains(draggable)) {
            draggable.asChild(this);
            onEnter.run();
        }
        return this;
    }

    @Override
    public void add(T draggable, int index, Runnable onEnter) {
        this.add(index, draggable);
        onEnter.run();
    }

    @Override
    public boolean accept(T draggable) {
        return false;
    }


    @Override
    public List<T> children() {
        return children;
    }

    @Override
    public IWidgetGroup<T> add(T widget) {
        return this.add(children.size(), widget);
    }

    @Override
    public IWidgetGroup<T> add(int index, T widget) {
        if (!children.contains(widget)) {
            children.add(index, widget);
        }
        return this;
    }

    @Override
    public boolean remove(T widget) {
        return children.remove(widget);
    }

    @Override
    public boolean remove(int index) {
        return children.remove(index) != null;
    }

    @Override
    public void clear() {
        for (T child : children) child.onDelete();
        children.clear();
    }

    @Override
    public boolean contains(T widget) {
        return children.contains(widget);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        graphics.pose().pushPose();
        {
            graphics.pose().translate(position.x, position.y, 0);
            IEventContext context = context();
            listener(IWidgetEvent.onRender).onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.isCancelled()) return;
            if (background != null)
                background.render(graphics);
            for (T child : children) {
                graphics.pose().pushPose();
                {
                    graphics.pose().translate(position.x, position.y, 0);
                    int translatedX = mouseX - this.position.x;
                    int translatedY = mouseY - this.position.y;
                    child.render(graphics, translatedX, translatedY, partialTicks);
                }
                graphics.pose().popPose();
            }
            listener(IWidgetEvent.onRenderPost).onRender(graphics, mouseX, mouseY, partialTicks, context());
        }
        graphics.pose().popPose();
    }

    @Override
    public List<IWidget> getWidgetById(Pattern regex) {
        List<IWidget> widgets = new ArrayList<>();
        for (IWidget child : children) {
            if (regex.matcher(child.getId()).matches()) {
                widgets.add(child);
            }
            if (child instanceof IWidgetGroup<?> group) {
                List<IWidget> result = group.getWidgetById(regex);
                widgets.addAll(result);
            }
        }
        return widgets;
    }

    @Override
    public List<IWidget> getContainedWidgets(boolean withInvisible, int maxDepth) {
        List<IWidget> widgets = new ArrayList<>();
        collectHelper(this, widgets, withInvisible, maxDepth, 0);
        return widgets;
    }


    private static void collectHelper(IWidgetGroup<?> group, List<IWidget> widgets, boolean withInvisible, int maxDepth, int depth) {
        if (depth > maxDepth) return;
        for (IWidget child : group.children()) {
            if (child instanceof IWidgetGroup) {
                collectHelper((IWidgetGroup<?>) child, widgets, withInvisible, maxDepth, depth + 1);
            } else {
                if (withInvisible || child.visible())
                    widgets.add(child);
            }
        }
    }

    @Override
    public boolean mouseClicked(IInputContext input) {
        if (!visible() || !active()) return false;
        IEventContext context = context();
        boolean result = events().get(IInputEvent.onMouseClicked).invoker()
                .onClicked(context, input);
        if (context.isCancelled()) return result;
        for (T child : children) {
            if (child.mouseClicked(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(IInputContext input) {
        if (!visible() || !active()) return false;
        IEventContext context = context();
        boolean result = events().get(IInputEvent.onMouseReleased).invoker()
                .onReleased(context, input);
        if (context.isCancelled()) return result;
        for (T child : children) {
            if (child.mouseReleased(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseMoved(double mouseX, double mouseY) {
        return super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(IInputContext input) {
        IEventContext context = context();
        boolean result = events().get(IInputEvent.onKeyPressed).invoker()
                .onKeyPressed(context, input);
        if (context.isCancelled()) return result;
        for (T child : children) {
            if (child.keyPressed(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(IInputContext input) {
        IEventContext context = context();
        boolean result = events().get(IInputEvent.onKeyReleased).invoker()
                .onKeyReleased(context, input);
        if (context.isCancelled()) return result;
        for (T child : children) {
            if (child.keyReleased(input)) {
                return true;
            }
        }
        return false;
    }
}