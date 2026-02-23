package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;

public class CompositeWidget<T extends Widget> extends Widget {

    //region child

    final MutableList<T> children = MutableLists.empty();
    protected final MutableList<T> childrenView = children.asUnmodifiable();

    /**
     * Lazily created sorted children list for rendering.
     * Only created when children have non-zero zIndex values.
     */
    private MutableList<T> renderOrderChildren;

    /**
     * Flag indicating whether the sorted children list needs to be rebuilt.
     */
    boolean childrenOrderDirty = true;

    //endregion

    //region composite extra

    @MustBeInvokedByOverriders
    public void tick() {
        super.tick();
        // Children ticking is handled non-recursively by Scene.
        // A child can be tickable without requiring its parent to be tickable.
    }

    @Override
    public void applyLayout() {
        super.applyLayout();
        for (T child : children) {
            child.applyLayout();
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
        addWidget(widget);
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
            childrenOrderDirty = true;
            if (scene != null) {
                switch (widget.lifecycle) {
                    case created -> scene.addCreatedWidget(widget);
                    case unmounted -> {
                        scene.reuse(widget);
                        scene.remountWidget(widget);
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

    protected final boolean removeWidget(Widget widget) {
        return remove(widget);
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
            childrenOrderDirty = true;
            if (scene != null) {
                scene.unmountWidget(widget);
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

    /**
     * Gets the children list to use for rendering, sorted by zIndex if needed.
     * <p>
     * The sorted list is lazily created and cached. It's only created when
     * at least one child has a non-zero zIndex or is in a non-content layer.
     * <p>
     * Widgets in non-content layers are excluded from this list as they are
     * rendered separately by Scene in their respective layers.
     *
     * @return the children list for rendering (only content layer widgets)
     * @implNote This is only for internal component implementations, and the rendering of some special components does not fully obey this list
     */
    protected MutableList<T> renderOrderChildren() {
        if (!childrenOrderDirty && renderOrderChildren != null) {
            return renderOrderChildren;
        }

        // Check if any child needs special handling (non-content layer or non-zero zIndex)
        boolean hasNonContentLayer = hasNonContentLayerChildren(children);
        boolean needsSorting = needsZIndexSorting(children);

        // If no special handling needed, return original list
        if (!hasNonContentLayer && !needsSorting) {
            renderOrderChildren = null;
            return children;
        }

        // Create or update the sorted list
        if (renderOrderChildren == null) {
            renderOrderChildren = MutableLists.empty();
        } else {
            renderOrderChildren.clear();
        }

        // Filter and sort: only include content layer widgets
        for (T child : children) {
            if (child.sceneLayer() == SceneLayer.content) {
                renderOrderChildren.add(child);
            } else if (scene != null) {
                // Ensure non-content layer widgets are added to the correct layer
                scene.addToLayer(child.sceneLayer(), child);
            }
        }

        if (needsSorting) {
            renderOrderChildren.sortThis(Comparator.comparingInt(Widget::zIndex));
        }
        childrenOrderDirty = false;
        return renderOrderChildren;
    }


    protected static <T extends Widget> boolean needsZIndexSorting(MutableList<T> children) {
        if (children.size() <= 1) return false;
        for (T child : children) {
            if (child.zIndex() != 0) return true;
        }
        return false;
    }

    protected static <T extends Widget> boolean hasNonContentLayerChildren(MutableList<T> children) {
        for (T child : children) {
            if (child.sceneLayer() != SceneLayer.content) return true;
        }
        return false;
    }

    /**
     * Marks the children render order as dirty, requiring a rebuild on next render.
     * Package-private: only Widget can call this through onRenderOrderChanged().
     */
    void markChildrenOrderDirty() {
        childrenOrderDirty = true;
    }

    /**
     * Renders this composite widget completely.
     * <p>
     * First renders this widget's own content via {@link #renderInternal},
     * then renders all children via {@link #renderChildren}.
     * Subclasses can override this to change the rendering order or add intermediate steps.
     */
    @Override
    public void render(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        var eventContext = common();
        listeners(WidgetEvent.onRender).onRender(canvas, mouseX, mouseY, partialTicks, this, eventContext);
        if (eventContext.cancelled()) return;
        renderInternal(canvas, mouseX, mouseY, partialTicks);
        renderChildren(canvas, mouseX, mouseY, partialTicks);
        listeners(WidgetEvent.onRenderPost).onRender(canvas, mouseX, mouseY, partialTicks, this, interruptible());
    }

    /**
     * Renders all children of this composite widget.
     * <p>
     * Children are rendered in zIndex order. Each child is rendered with proper transform applied.
     * Widgets in non-content layers are managed separately by Scene.
     * Subclasses can override this to customize how children are rendered (e.g., scrolling, clipping).
     *
     * @param canvas       the canvas for batched rendering
     * @param mouseX       relative mouse X (relative to this widget)
     * @param mouseY       relative mouse Y (relative to this widget)
     * @param partialTicks partial ticks
     */
    protected void renderChildren(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        canvas.renderChildren(renderOrderChildren(), mouseX, mouseY, partialTicks);
    }

    //endregion

    //region debug

    @Override
    @MustBeInvokedByOverriders
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.add("childCount", children.size(), InspectionProperty.categoryBasic);
    }

    //endregion

}
