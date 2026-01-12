package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import net.minecraft.client.gui.GuiGraphics;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Iterator;

//TODO:Refactor subWidget and GroupWidget
public class WidgetGroup<T extends Widget> extends Widget {

    //region Fields

    final MutableList<T> children = MutableLists.empty();
    private final MutableList<T> childrenView = children.asUnmodifiable();

    //endregion

    //region Widget Basic

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
    public void tick() {
        super.tick();
        for (T child : children) {
            if (child.active())
                child.tick();
        }
    }

    @Override
    public void layout() {
        if (!layoutByParent) {
            yogaNode.calculateLayout(
                getWidth(),
                getHeight()
            );
        }
        if (yogaNode.hasNewLayout()) {
            listeners(WidgetEvent.onResize).onResize(this);
            applyLayoutResult();
            this.onPositionUpdate();
            for (T child : children) {
                child.layout();
            }
            listeners(WidgetEvent.onResizePost).onResizePost(this);
        }
    }

    //endregion

    //region ScreeSpecBuilder Functions


    //endregion

    //region Group Basic

    public int size() {
        return children.size();
    }

    public @Unmodifiable MutableList<T> children() {
        return childrenView;
    }

    protected WidgetGroup<T> add(T widget) {
        this.add(children.size(), widget);
        return this;
    }

    protected T addWidget(T widget) {
        if (this.add(children.size(), widget)) {
            if (widget.parent != null) {
                widget.parent.yogaNode.removeChild(this.yogaNode);
                //TODO:Call OnRemove event for the widget
            }
            widget.parent = this;
            widget.onPositionUpdate();
            if (widget.layoutByParent) {
                this.yogaNode.addChildAt(widget.yogaNode, this.yogaNode.getChildCount());
            }
        } else {
            throw new IllegalArgumentException("Widget already exists in the group");
        }
        return widget;
    }

    protected boolean add(int index, T widget) {
        if (widget == this)
            throw new IllegalArgumentException("Cannot addGroup a widget to itself");
        if (!children.contains(widget)) {
            var context = common();
            listeners(WidgetEvent.onChildAdded).onChildAdded(widget, context);
            if (context.cancelled()) return false;
            children.add(index, widget);
            widget.root = root;
            listeners(WidgetEvent.onChildAddedPost).onChildAdded(widget, interruptible());
            return true;
        }
        return false;
    }

    protected boolean remove(Widget widget) {
        //noinspection SuspiciousMethodCalls
        int index = children.indexOf(widget);
        if (index < 0) return false;
        return remove(index);
    }

    protected boolean remove(int index) {
        if (index < 0 || index >= children.size()) return false;
        Widget child = children.get(index);
        child.listeners(WidgetEvent.onRemove).onRemove(child);
        child.setParent(null);
        return children.remove(index) != null;
    }

    protected void clear() {
        for (Iterator<T> iterator = children.iterator(); iterator.hasNext(); ) {
            T child = iterator.next();
            child.listeners(WidgetEvent.onRemove).onRemove(child);
            child.setParent(null);
            iterator.remove();
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
                graphics.pose().translate(child.position.x(), child.position.y(), 0);
                int relativeX = mouseX - child.position.x();
                int relativeY = mouseY - child.position.y();
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

    //endregion

    //region Group Utils

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

    @Override
    public String toString() {
        return "WidgetGroup{" +
               "key='" + (key == null ? "null" : key) + '\'' +
               ", children=" + children +
               ", initialized=" + initialized +
               ", root=" + (root == null ? "null" : root.key()) +
               ", parent=" + (parent == null ? "null" : parent.key()) +
               ", position=" + position +
               ", absolute=" + absolute +
               ", size=" + size +
               ", active=" + active +
               ", visibility=" + visibility +
               ", richTooltip=" + richTooltip +
               '}';
    }

    //endregion

}
