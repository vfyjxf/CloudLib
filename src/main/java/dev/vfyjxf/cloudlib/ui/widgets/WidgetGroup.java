package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.drag.IDragConsumer;
import dev.vfyjxf.cloudlib.api.ui.event.IInputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import net.minecraft.client.gui.GuiGraphics;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class WidgetGroup<T extends IWidget> extends Widget implements IWidgetGroup<T> {

    protected final MutableList<T> children = Lists.mutable.empty();
    protected final List<T> childrenView = children.asUnmodifiable();

    @Override
    public IWidgetGroup<T> setUI(@Nullable IModularUI ui) {
        super.setUI(ui);
        return this;
    }

    @Override
    @MustBeInvokedByOverriders
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
    @MustBeInvokedByOverriders
    public void update() {
        super.update();
        if (!initialized) return;
        for (T child : children) {
            if (child.initialized())
                child.update();
        }
    }

    @Override
    public void rebuild() {
        super.rebuild();
        for (T child : children) {
            if (child.initialized())
                child.rebuild();
        }
        this.clear();
    }

    @Override
    @MustBeInvokedByOverriders
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
    public int size() {
        return children.size();
    }

    @Override
    public List<T> children() {
        return childrenView;
    }

    @Override
    public IWidgetGroup<T> add(T widget) {
        this.add(children.size(), widget);
        return this;
    }

    @Override
    public boolean add(int index, T widget) {
        if (!children.contains(widget)) {
            var context = context();
            listeners(IWidgetEvent.onChildAdded).onChildAdded(context, widget);
            if (context.isCancelled()) return false;
            children.add(index, widget);
            listeners(IWidgetEvent.onChildAddedPost).onChildAdded(context, widget);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(T widget) {
        int index = children.indexOf(widget);
        if (index < 0) return false;
        return remove(index);
    }

    @Override
    public boolean remove(int index) {
        if (index < 0 || index >= children.size()) return false;
        IWidget child = children.get(index);
        child.onDelete();
        child.setUI(null);
        child.setParent(null);
        return children.remove(index) != null;
    }

    @Override
    public void clear() {
        for (int i = 0; i < children.size(); i++) {
            remove(i);
        }
        initialized = false;
    }

    @Override
    public boolean contains(T widget) {
        return children.contains(widget);
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);
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
    }

    @Override
    public List<IWidget> getById(Pattern regex) {
        List<IWidget> widgets = new ArrayList<>();
        for (IWidget child : children) {
            if (regex.matcher(child.getId()).matches()) {
                widgets.add(child);
            }
            if (child instanceof IWidgetGroup<?> group) {
                List<IWidget> result = group.getById(regex);
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
        var context = context();
        boolean result = listeners(IInputEvent.onMouseClicked).onClicked(context, input);
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
        var context = context();
        boolean result = listeners(IInputEvent.onMouseReleased).onReleased(context, input);
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
        var context = context();
        boolean result = listeners(IInputEvent.onKeyPressed).onKeyPressed(context, input);
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
        var context = context();
        boolean result = listeners(IInputEvent.onKeyReleased).onKeyReleased(context, input);
        if (context.isCancelled()) return result;
        for (T child : children) {
            if (child.keyReleased(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "WidgetGroup{" +
                "initialized=" + initialized +
                ", id='" + id + '\'' +
                ", position=" + position +
                ", absolute=" + absolute +
                ", size=" + size +
                ", active=" + active +
                ", visibility=" + visibility +
                ", moving=" + moving +
                '}';
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return childrenView.iterator();
    }
}
