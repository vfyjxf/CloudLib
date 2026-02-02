package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Unmodifiable;

//TODO:Refactor subWidget and GroupWidget
public class CompositeWidget<T extends Widget> extends Widget {

    //region child

    final MutableList<T> children = MutableLists.empty();
    protected final MutableList<T> childrenView = children.asUnmodifiable();

    //endregion

    //region composite extra

    @MustBeInvokedByOverriders
    public void tick() {
        super.tick();
        for (T child : children) {
            if (child.active())
                child.tick();
        }
    }

    @Override
    public void applyLayout() {
        super.applyLayout();
        for (T child : children) {
            child.applyLayout();
        }
    }

    /**
     * Invalidates the cached absolute position of this widget and all descendants.
     */
    @Override
    protected void onPositionChanged() {
        super.onPositionChanged();
        for (T child : children) {
            child.onPositionChanged();
        }
    }

//endregion

    //region lifecycle

    @Override
    void init() {
        super.init();
    }

    @Override
    void mount(Scene scene, SceneContext context, SceneHandle handle) {
        super.mount(scene, context, handle);
    }

    @Override
    void unmount() {
        super.unmount();
    }

    @Override
    void destroy() {
        super.destroy();
    }

    //endregion

    //region group basic

    public @Unmodifiable MutableList<T> children() {
        return childrenView;
    }

    protected CompositeWidget<T> add(T widget) {
        this.add(children.size(), widget);
        return this;
    }

    protected <W extends T> W addWidget(W widget) {
        if (widget.lifecycle.destroyed()) {
            throw new IllegalArgumentException("Cannot add a destroyed widget");
        }
        if (widget.parent != null) {
            if (widget.parent == this) {
                throw new IllegalArgumentException("Widget already exists in the group");
            }
            widget.parent.remove(widget);
        }

        if (!this.add(children.size(), widget)) {
            throw new IllegalArgumentException("Widget already exists in the group");
        }

        widget.parent = this;
        return widget;
    }

    protected final boolean add(int index, T widget) {
        if (widget == this)
            throw new IllegalArgumentException("Cannot addGroup a widget to itself");
        if (!children.contains(widget)) {
            var context = common();
            listeners(WidgetEvent.onChildAdded).onChildAdded(widget, context);
            if (context.cancelled()) return false;
            children.add(index, widget);
            if (scene != null) {
                switch (widget.lifecycle) {
                    case created -> scene.addCreatedWidget(widget);
                    case unmounted -> {
                        scene.reuse(widget);
                        scene.addUnmountedWidget(widget);
                    }
                    default ->
                        throw new IllegalArgumentException("Illegal lifecycle: " + widget.lifecycle + " for widget: " + widget);
                }
                scene.invalidatePathCache();
            }
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
        listeners(WidgetEvent.onChildRemoved).onChildRemoved(child, interruptible());
        child.listeners(WidgetEvent.onRemove).onRemove(this, child);
        Widget widget = children.remove(index);
        if (widget != null) {
            widget.unmount();
            if (scene != null) {
                scene.invalidatePathCache();
            }
        }
        return widget != null;
    }

    protected void clear() {
        while (!children.isEmpty()) {
            remove(children.size() - 1);
        }
    }

    public boolean contains(T widget) {
        return children.contains(widget);
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
        for (T child : children) {
            canvas.pushTransform();
            canvas.translate(child.position.x(), child.position.y());
            int relativeX = mouseX - child.position.x();
            int relativeY = mouseY - child.position.y();
            child.renderWidget(canvas, relativeX, relativeY, partialTicks);
            canvas.popTransform();
        }
    }

    @Override
    protected void renderOverlayInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderOverlayInternal(canvas, mouseX, mouseY, partialTicks);
        for (T child : children) {
            child.renderOverlay(canvas, mouseX, mouseY, partialTicks);
        }
    }

    //endregion

    //region group utils

    @Override
    @MustBeInvokedByOverriders
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.add("childCount", children.size(), InspectionProperty.CATEGORY_BASIC);
    }

    //endregion

}
