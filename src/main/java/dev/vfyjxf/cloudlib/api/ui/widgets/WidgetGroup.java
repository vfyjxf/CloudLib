package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.inputs.InputContext;
import net.minecraft.client.gui.GuiGraphics;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class WidgetGroup<T extends Widget> extends Widget {

    protected final MutableList<T> children = Lists.mutable.empty();
    protected final List<T> childrenView = children.asUnmodifiable();

    @MustBeInvokedByOverriders
    public void init() {
        super.init();
        for (T child : children) {
            child.init();
        }
    }

    @MustBeInvokedByOverriders
    public void update() {
        super.update();
        if (!initialized) return;
        for (T child : children) {
            if (child.initialized())
                child.update();
        }
    }

    @ApiStatus.Internal
    public void rebuild() {
        super.rebuild();
        for (T child : children) {
            if (child.initialized())
                child.rebuild();
        }
        this.clear();
    }

    @MustBeInvokedByOverriders
    public void tick() {
        super.tick();
        for (T child : children) {
            if (child.active())
                child.tick();
        }
    }

    public WidgetGroup<T> self() {
        return this;
    }

    public void add(T draggable, int index, Runnable onEnter) {
        this.add(index, draggable);
        onEnter.run();
    }

    public boolean accept(T draggable) {
        return false;
    }

    public int size() {
        return children.size();
    }

    public List<T> children() {
        return childrenView;
    }

    public WidgetGroup<T> add(T widget) {
        this.add(children.size(), widget);
        return this;
    }

    public boolean add(int index, T widget) {
        if (!children.contains(widget)) {
            var context = common();
            listeners(WidgetEvent.onChildAdded).onChildAdded(context, widget);
            if (context.cancelled()) return false;
            children.add(index, widget);
            listeners(WidgetEvent.onChildAddedPost).onChildAdded(context, widget);
            return true;
        }
        return false;
    }

    public boolean remove(T widget) {
        int index = children.indexOf(widget);
        if (index < 0) return false;
        return remove(index);
    }

    public boolean remove(int index) {
        if (index < 0 || index >= children.size()) return false;
        Widget child = children.get(index);
        child.onDelete();
        child.setParent(null);
        return children.remove(index) != null;
    }

    public void clear() {
        for (int i = 0; i < children.size(); i++) {
            remove(i);
        }
        initialized = false;
    }

    public boolean contains(T widget) {
        return children.contains(widget);
    }


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

    public List<Widget> getById(Pattern regex) {
        List<Widget> widgets = new ArrayList<>();
        for (Widget child : children) {
            if (regex.matcher(child.getId()).matches()) {
                widgets.add(child);
            }
            if (child instanceof WidgetGroup<?> group) {
                List<Widget> result = group.getById(regex);
                widgets.addAll(result);
            }
        }
        return widgets;
    }

    public List<Widget> getContainedWidgets(boolean withInvisible, int maxDepth) {
        List<Widget> widgets = new ArrayList<>();
        collectHelper(this, widgets, withInvisible, maxDepth, 0);
        return widgets;
    }


    private static void collectHelper(WidgetGroup<?> group, List<Widget> widgets, boolean withInvisible, int maxDepth, int depth) {
        if (depth > maxDepth) return;
        for (Widget child : group.children()) {
            if (child instanceof dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup) {
                collectHelper((WidgetGroup<?>) child, widgets, withInvisible, maxDepth, depth + 1);
            } else {
                if (withInvisible || child.visible())
                    widgets.add(child);
            }
        }
    }

    public boolean mouseClicked(InputContext input) {
        if (!visible() || !active()) return false;
        var context = common();
        boolean result = listeners(InputEvent.onMouseClicked).onClicked(context, input);
        if (context.cancelled()) return result;
        for (T child : children) {
            if (child.mouseClicked(input)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(InputContext input) {
        if (!visible() || !active()) return false;
        var context = common();
        boolean result = listeners(InputEvent.onMouseReleased).onReleased(context, input);
        if (context.cancelled()) return result;
        for (T child : children) {
            if (child.mouseReleased(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseMoved(double mouseX, double mouseY) {
        return super.mouseMoved(mouseX, mouseY);
    }

    public boolean keyPressed(InputContext input) {
        var context = common();
        boolean result = listeners(InputEvent.onKeyPressed).onKeyPressed(context, input);
        if (context.cancelled()) return result;
        for (T child : children) {
            if (child.keyPressed(input)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyReleased(InputContext input) {
        var context = common();
        boolean result = listeners(InputEvent.onKeyReleased).onKeyReleased(context, input);
        if (context.cancelled()) return result;
        for (T child : children) {
            if (child.keyReleased(input)) {
                return true;
            }
        }
        return false;
    }


    public String toString() {
        return "WidgetGroup{" +
                "initialized=" + initialized +
                ", id='" + id + '\'' +
                ", position=" + position +
                ", absolute=" + absolute +
                ", size=" + size +
                ", active=" + active +
                ", visibility=" + visibility +
                '}';
    }

    @NotNull

    public Iterator<T> iterator() {
        return childrenView.iterator();
    }

    public WidgetGroup<T> widget(T widget) {
        widget.asChild(this);
        return this;
    }

    @SuppressWarnings("unchecked")
    public WidgetGroup<T> widgets(T... widgets) {
        for (T widget : widgets) {
            widget.asChild(this);
        }
        return this;
    }

    @Nullable
    public Widget getById(String id) {
        for (T child : children) {
            if (child.getId().equals(id)) {
                return child;
            }
        }
        return null;
    }

    @Nullable
    public Widget getHoveredWidget(double mouseX, double mouseY) {
        for (T child : children) {
            if (child.visible() && child.isMouseOver(mouseX, mouseY)) {
                return child;
            }
        }
        if (isMouseOver(mouseX, mouseY)) {
            return this;
        }
        return null;
    }

    public <W extends Widget> @Nullable W getWidgetOfType(Class<W> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        for (T child : children) {
            @Nullable W result = null;
            if (type.isInstance(child)) {
                result = type.cast(child);
            }
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public <W extends Widget> void getWidgetsOfType(Class<W> type, MutableList<W> result) {
        if (type.isInstance(this)) {
            result.add(type.cast(this));
        }
        for (T child : children) {
            if (type.isInstance(child)) {
                result.add(type.cast(child));
            }
        }
    }

    public <W extends Widget> @Nullable W findWidgetsOfType(Class<W> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        for (T child : children) {
            @Nullable W result = null;
            if (type.isInstance(child)) {
                result = type.cast(child);
            } else {
                var parent = child.parent();
                if (parent != null) {
                    result = parent.findWidgetsOfType(type);
                }
            }
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public <W extends Widget> void findWidgetsOfType(Class<W> type, MutableList<W> result) {
        if (type.isInstance(this)) {
            result.add(type.cast(this));
        }
        for (T child : children) {
            if (type.isInstance(child)) {
                result.add(type.cast(child));
            }
            var parent = child.parent();
            if (parent != null) {
                parent.findWidgetsOfType(type, result);
            }
        }
    }

    public List<Widget> getContainedWidgets(boolean withInvisible) {
        List<Widget> result = new ArrayList<>();
        for (T child : children) {
            if (child.visible() || withInvisible) {
                result.add(child);
                if (child instanceof WidgetGroup<?> group)
                    result.addAll((group).getContainedWidgets(withInvisible));
            }
        }
        return result;
    }

}
