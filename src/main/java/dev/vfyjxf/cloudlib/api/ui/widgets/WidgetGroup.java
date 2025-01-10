package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import net.minecraft.client.gui.GuiGraphics;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WidgetGroup<T extends Widget> extends Widget {

    protected final MutableList<T> children = Lists.mutable.empty();
    protected final MutableList<T> childrenView = children.asUnmodifiable();

    private MutableList<T> toAdd = Lists.mutable.empty();
    private MutableList<T> toRemove = Lists.mutable.empty();

    //////////////////////////////////////
    //********      Basic      *********//
    //////////////////////////////////////

    @MustBeInvokedByOverriders
    @SuppressWarnings("all")
    public void init() {
        listeners(WidgetEvent.onInit).onInit(this);
        for (T child : children) {
            child.init();
        }
        initialized = true;
        listeners(WidgetEvent.onInitPost).onInit(this);
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

    @MustBeInvokedByOverriders
    public void tick() {
        super.tick();
        for (T child : children) {
            if (child.active())
                child.tick();
        }
    }

    @Override
    public void layout() {
        listeners(WidgetEvent.onResize).onResize(this);
        resizer.apply(this);
        for (T child : children) {
            child.layout();
        }
        resizer.postApply(this);
        this.onPositionUpdate();
        listeners(WidgetEvent.onResizePost).onResizePost(this);
    }

    //////////////////////////////////////
    //********    WidgetGroup  *********//
    //////////////////////////////////////

    public int size() {
        return children.size();
    }

    @Override
    public Widget setPos(Pos position) {
        super.setPos(position);
        for (T child : children) {
            child.onPositionUpdate();
        }
        return this;
    }

    @Unmodifiable
    public MutableList<T> children() {
        return childrenView;
    }

    public WidgetGroup<T> add(T widget) {
        this.add(children.size(), widget);
        return this;
    }

    public boolean add(int index, T widget) {
        if (widget == this)
            throw new IllegalArgumentException("Cannot add a widget to itself");
        if (!children.contains(widget)) {
            var context = common();
            listeners(WidgetEvent.onChildAdded).onChildAdded(widget, context);
            if (context.cancelled()) return false;
            children.add(index, widget);
            widget.setRoot(root);
            listeners(WidgetEvent.onChildAddedPost).onChildAdded(widget, interruptible());
            return true;
        }
        return false;
    }

    public boolean remove(Widget widget) {
        //noinspection SuspiciousMethodCalls
        int index = children.indexOf(widget);
        if (index < 0) return false;
        return remove(index);
    }

    public boolean remove(int index) {
        if (index < 0 || index >= children.size()) return false;
        Widget child = children.get(index);
        child.listeners(WidgetEvent.onRemove).onRemove(child);
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

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);
        for (T child : children) {
            graphics.pose().pushPose();
            {
                graphics.pose().translate(child.position.x, child.position.y, 0);
                int relativeX = mouseX - child.position.x;
                int relativeY = mouseY - child.position.y;
                child.renderWidget(graphics, relativeX, relativeY, partialTicks);
            }
            graphics.pose().popPose();
        }
    }

    @Override
    protected void renderOverlayInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderOverlayInternal(graphics, mouseX, mouseY, partialTicks);
        for (T child : children) {
            child.renderOverlay(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mouseClicked(InputContext input) {
        if (!visible() || !active()) return false;
        var context = common();
        boolean result = listeners(InputEvent.onMouseClicked).onClicked(input, context);
        if (context.cancelled()) return result;
        for (T child : children) {
            if (child.mouseClicked(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(InputContext input) {
        if (!visible() || !active()) return false;
        var context = common();
        boolean result = listeners(InputEvent.onMouseReleased).onReleased(input, context);
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
        if (!visible() || !active()) return false;
        var context = common();
        boolean result = listeners(InputEvent.onMouseScrolled).onScrolled(mouseX, mouseY, scrollX, scrollY, context);
        if (context.cancelled()) return result;
        for (T child : children) {
            if (child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible() || !active()) return false;
        var context = common();
        boolean result = listeners(InputEvent.onMouseDragged).onDragged(InputContext.fromMouse(mouseX, mouseY, button), deltaX, deltaY, context);
        if (context.cancelled()) return result;
        for (T child : children) {
            if (child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        for (T child : children) {
            child.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(InputContext input) {
        var context = common();
        boolean result = listeners(InputEvent.onKeyPressed).onKeyPressed(input, context);
        if (context.cancelled()) return result;
        for (T child : children) {
            if (child.keyPressed(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(InputContext input) {
        var context = common();
        boolean result = listeners(InputEvent.onKeyReleased).onKeyReleased(input, context);
        if (context.cancelled()) return result;
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
                "id='" + id + '\'' +
                ", children=" + children +
                ", initialized=" + initialized +
                ", root=" + (root == null ? "null" : root.getId()) +
                ", parent=" + (parent == null ? "null" : parent.getId()) +
                ", position=" + position +
                ", absolute=" + absolute +
                ", size=" + size +
                ", active=" + active +
                ", visibility=" + visibility +
                ", richTooltip=" + richTooltip +
                '}';
    }

    public <W extends T> W addChild(W widget) {
        widget.asChild(this);
        return widget;
    }

    /**
     * Builder style method to add a widget to the group
     */
    public WidgetGroup<T> child(T widget) {
        widget.asChild(this);
        return this;
    }

    /**
     * Builder style method to add a widget to the group
     */
    @SafeVarargs
    public final WidgetGroup<T> children(T... widgets) {
        for (T widget : widgets) {
            widget.asChild(this);
        }
        return this;
    }

    public <W extends T> W addWidget(W widget) {
        widget.asChild(this);
        return widget;
    }

    /**
     * Builder style method to add a widget to the group
     */
    public WidgetGroup<T> widget(T widget) {
        widget.asChild(this);
        return this;
    }

    /**
     * Builder style method to add a widget to the group
     */
    @SafeVarargs
    public final WidgetGroup<T> widgets(T... widgets) {
        for (T widget : widgets) {
            widget.asChild(this);
        }
        return this;
    }

    //////////////////////////////////////
    //********       Utils     *********//
    //////////////////////////////////////

    @Nullable
    public Widget getById(String id) {
        for (T child : children) {
            if (child.getId().equals(id)) {
                return child;
            }
        }
        return null;
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

    public final WidgetGroup<T> onChildAdded(WidgetEvent.OnChildAdded listener) {
        register(WidgetEvent.onChildAdded, listener);
        return this;
    }

    public final WidgetGroup<T> onChildAddedPost(WidgetEvent.OnChildAddedPost listener) {
        register(WidgetEvent.onChildAddedPost, listener);
        return this;
    }

    public final WidgetGroup<T> onChildRemoved(WidgetEvent.OnChildRemoved listener) {
        register(WidgetEvent.onChildRemoved, listener);
        return this;
    }

    public final WidgetGroup<T> onChildRemovedPost(WidgetEvent.OnChildRemovedPost listener) {
        register(WidgetEvent.onChildRemovedPost, listener);
        return this;
    }

}
