package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.event.context.BubbleContext;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.performer.PerformerContainer;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree.TraversalControl;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.Inspector;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvents;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.ui.drag.DraggableManager;
import dev.vfyjxf.cloudlib.util.Checks;
import dev.vfyjxf.cloudlib.util.ScreenUtil;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.tree.TaffyTree;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * UI instance in specific context
 */
public final class Scene {

    public Scene(WidgetGroup<? extends Widget> root) {
        this.root = root;
        this.draggableManager = new DraggableManager(root);
        // Ensure root has a FocusScopeNode as the root focus scope
        if (!(root.focusNode instanceof FocusScopeNode)) {
            root.setFocusNode(new FocusScopeNode());
        }
        this.rootScope = (FocusScopeNode) root.focusNode;
    }

    //region tree
    private final WidgetGroup<? extends Widget> root;
    final TaffyTree tree = new TaffyTree();
    private final PathCache pathCache = new PathCache();

    public WidgetGroup<? extends Widget> root() {
        return root;
    }
    //endregion

    //region scheduler task

    private final Deque<Runnable> deferredTasks = new ArrayDeque<>();
    private MutableList<Runnable> postLayoutTasks = MutableLists.empty();
    private MutableList<Runnable> postRenderTasks = MutableLists.empty();
    private final MutableList<RecurringTask> perRenderTasks = MutableLists.empty();
    private final Deque<Runnable> nextTickTasks = new ArrayDeque<>();
    private final MutableList<TimedTask> timedTasks = MutableLists.empty();

    /**
     * Creates a cancellation flag and passes the cancel action to the given disposer.
     *
     * @return a supplier that returns {@code true} while the task is active
     */
    private static BooleanSupplier bindDisposer(Consumer<Runnable> disposer) {
        boolean[] active = {true};
        disposer.accept(() -> active[0] = false);
        return () -> active[0];
    }

    private record RecurringTask(Runnable callback, BooleanSupplier active) {
        void runIfActive() {
            if (active.getAsBoolean()) callback.run();
        }

        boolean isCancelled() {
            return !active.getAsBoolean();
        }
    }

    private static final class TimedTask {
        final Runnable callback;
        final BooleanSupplier active;
        final int intervalTicks; // 0 = one-shot, >0 = repeating
        int remainingTicks;

        TimedTask(Runnable callback, BooleanSupplier active, int delayTicks, int intervalTicks) {
            this.callback = callback;
            this.active = active;
            this.remainingTicks = delayTicks;
            this.intervalTicks = intervalTicks;
        }

        boolean isCancelled() {
            return !active.getAsBoolean();
        }
    }

    //region One-shot callbacks

    /**
     * Runs a callback <b>once</b> after the current (or next) frame
     * has finished rendering.
     *
     * <p>The callback executes after all layers are rendered and before widget cleanup,
     * guaranteeing that every widget has its final layout and visual state.</p>
     *
     * <h4>Typical use cases</h4>
     * <ul>
     *   <li>Measuring a widget's computed size/position after first layout</li>
     *   <li>Scrolling to a specific widget once it is visible</li>
     *   <li>Triggering an entrance animation after the first paint</li>
     * </ul>
     *
     * @param task the action to execute
     */
    public void postRender(Runnable task) {
        postRenderTasks.add(task);
    }

    /**
     * Runs a callback <b>once</b> after the current (or next) frame
     * has finished rendering, with a cancel action passed to {@code disposer}.
     *
     * @param task     the action to execute
     * @param disposer receives a {@link Runnable} that cancels this task when invoked;
     *                 typically {@code handle::onCleanup}
     */
    public void postRender(Runnable task, Consumer<Runnable> disposer) {
        var active = bindDisposer(disposer);
        postRenderTasks.add(() -> {
            if (active.getAsBoolean()) task.run();
        });
    }

    /**
     * Runs a callback <b>once</b> after the next layout pass completes
     * (i.e., after widget tree rebuild and Taffy layout finishes).
     *
     * <p>At this point every widget has valid layout information but the frame
     * has not been rendered yet. This is ideal for reading computed positions
     * and sizes without waiting for a full render.</p>
     *
     * <h4>Typical use cases</h4>
     * <ul>
     *   <li>Reading a widget's absolute position after dynamic content changes</li>
     *   <li>Adjusting overlay positions based on the target widget's layout</li>
     *   <li>Validating layout constraints are met (e.g., no overflow)</li>
     * </ul>
     *
     * @param task the action to execute
     */
    public void postLayout(Runnable task) {
        postLayoutTasks.add(task);
    }

    /**
     * Runs a callback <b>once</b> after the next layout pass completes,
     * with a cancel action passed to {@code disposer}.
     *
     * @param task     the action to execute
     * @param disposer receives a {@link Runnable} that cancels this task when invoked
     */
    public void postLayout(Runnable task, Consumer<Runnable> disposer) {
        var active = bindDisposer(disposer);
        postLayoutTasks.add(() -> {
            if (active.getAsBoolean()) task.run();
        });
    }

    /**
     * Defers a task to run at the <b>beginning</b> of the next render frame,
     * before layout and rendering.
     *
     * <p>Deferred tasks are executed in FIFO order and are useful for batching
     * state mutations so that only a single layout/render pass is triggered.
     * Tasks deferred during execution are drained in the same batch
     * (be careful not to create infinite loops).</p>
     *
     * <h4>Typical use cases</h4>
     * <ul>
     *   <li>Batching multiple state changes into a single layout pass</li>
     *   <li>Deferring work that must happen before the next paint</li>
     *   <li>Ensuring a piece of code runs after the current event handler completes
     *       but before the frame is built</li>
     * </ul>
     *
     * @param task the action to execute
     */
    public void defer(Runnable task) {
        deferredTasks.addLast(task);
    }

    /**
     * Runs a callback <b>once</b> at the start of the next game tick.
     *
     * <p>In Minecraft, ticks run at a fixed 20 Hz rate. This is useful for
     * deferring mutations that should not happen during the current event
     * dispatch (e.g., avoiding re-entrant modifications).</p>
     *
     * <h4>Typical use cases</h4>
     * <ul>
     *   <li>Deferring a widget tree mutation triggered by an event handler</li>
     *   <li>Ensuring an action runs outside the current input handling stack</li>
     *   <li>Coordinating with game-tick–bound logic (recipes, inventories)</li>
     * </ul>
     *
     * @param task the action to execute
     */
    public void nextTick(Runnable task) {
        nextTickTasks.addLast(task);
    }

    /**
     * Runs a callback <b>once</b> at the start of the next game tick,
     * with a cancel action passed to {@code disposer}.
     *
     * @param task     the action to execute
     * @param disposer receives a {@link Runnable} that cancels this task when invoked
     */
    public void nextTick(Runnable task, Consumer<Runnable> disposer) {
        var active = bindDisposer(disposer);
        nextTickTasks.addLast(() -> {
            if (active.getAsBoolean()) task.run();
        });
    }

    //endregion

    //region Persistent (recurring) callbacks

    /**
     * Registers a callback that runs <b>every frame</b> after rendering,
     * until canceled via the cancel action passed to {@code disposer}.
     *
     * <h4>Typical use cases</h4>
     * <ul>
     *   <li>Driving animations that need per-frame updates</li>
     *   <li>Continuous layout monitoring (e.g., sticky headers)</li>
     *   <li>Debug overlays that update every frame</li>
     * </ul>
     *
     * @param task     the action to execute each frame
     * @param disposer receives a {@link Runnable} that stops this recurring task
     *                 when invoked; typically {@code handle::onCleanup}
     */
    public void everyRender(Runnable task, Consumer<Runnable> disposer) {
        var active = bindDisposer(disposer);
        perRenderTasks.add(new RecurringTask(task, active));
    }

    //endregion

    //region Delayed & periodic tasks

    /**
     * Runs a one-shot callback after a specified number of game ticks.
     *
     * <h4>Typical use cases</h4>
     * <ul>
     *   <li>Delayed tooltip display (e.g., show after hovering 20 ticks)</li>
     *   <li>Auto-dismiss notifications after a timeout</li>
     *   <li>Debouncing rapid inputs (cancel &amp; reschedule on each keystroke)</li>
     * </ul>
     *
     * @param delayTicks number of ticks to wait before execution (1 tick ≈ 50ms)
     * @param task       the action to execute
     */
    public void delay(int delayTicks, Runnable task) {
        Checks.checkArgument(delayTicks > 0, "delayTicks must be > 0");
        timedTasks.add(new TimedTask(task, () -> true, delayTicks, 0));
    }

    /**
     * Runs a one-shot callback after a specified number of game ticks,
     * with a cancel action passed to {@code disposer}.
     *
     * @param delayTicks number of ticks to wait before execution (1 tick ≈ 50ms)
     * @param task       the action to execute
     * @param disposer   receives a {@link Runnable} that cancels this task when invoked
     */
    public void delay(int delayTicks, Runnable task, Consumer<Runnable> disposer) {
        Checks.checkArgument(delayTicks > 0, "delayTicks must be > 0");
        var active = bindDisposer(disposer);
        timedTasks.add(new TimedTask(task, active, delayTicks, 0));
    }

    /**
     * Runs a repeating callback every {@code intervalTicks} game ticks.
     *
     * <p>The first invocation happens after {@code intervalTicks} ticks.
     * Use {@link #interval(int, int, Runnable, Consumer)} for a custom initial delay.</p>
     *
     * <h4>Typical use cases</h4>
     * <ul>
     *   <li>Polling server data at a fixed rate</li>
     *   <li>Periodic progress bar updates</li>
     *   <li>Blinking cursor / flashing indicator</li>
     * </ul>
     *
     * @param intervalTicks ticks between each invocation
     * @param task          the action to execute
     * @param disposer      receives a {@link Runnable} that stops this repeating task;
     *                      typically {@code handle::onCleanup}
     */
    public void interval(int intervalTicks, Runnable task, Consumer<Runnable> disposer) {
        interval(intervalTicks, intervalTicks, task, disposer);
    }

    /**
     * Runs a repeating callback with a custom initial delay.
     *
     * @param initialDelay  ticks before the first invocation
     * @param intervalTicks ticks between subsequent invocations
     * @param task          the action to execute
     * @param disposer      receives a {@link Runnable} that stops this repeating task
     */
    public void interval(int initialDelay, int intervalTicks, Runnable task, Consumer<Runnable> disposer) {
        Checks.checkArgument(initialDelay > 0, "initialDelay must be > 0");
        Checks.checkArgument(intervalTicks > 0, "intervalTicks must be > 0");
        var active = bindDisposer(disposer);
        timedTasks.add(new TimedTask(task, active, initialDelay, intervalTicks));
    }

    //endregion

    //region Internal execution hooks

    /**
     * Drains all deferred tasks. Called at the start of each frame before layout.
     */
    private void drainDeferred() {
        int safetyLimit = 1000;
        while (!deferredTasks.isEmpty() && safetyLimit-- > 0) {
            Runnable task = deferredTasks.pollFirst();
            if (task != null) task.run();
        }
    }

    /**
     * Executes and clears one-shot post-layout tasks.
     */
    private void runPostLayout() {
        if (postLayoutTasks.isEmpty()) return;
        MutableList<Runnable> batch = postLayoutTasks;
        postLayoutTasks = MutableLists.empty();
        batch.forEach(Runnable::run);
    }

    /**
     * Executes one-shot and persistent post-render tasks.
     */
    private void runPostRender() {
        // One-shot
        if (!postRenderTasks.isEmpty()) {
            MutableList<Runnable> batch = postRenderTasks;
            postRenderTasks = MutableLists.empty();
            batch.forEach(Runnable::run);
        }
        // Persistent — prune cancelled entries
        if (!perRenderTasks.isEmpty()) {
            perRenderTasks.removeIf(RecurringTask::isCancelled);
            perRenderTasks.forEach(RecurringTask::runIfActive);
        }
    }

    /**
     * Processes tick-based scheduling: next-tick tasks, delayed tasks,
     * and interval tasks.
     */
    private void runTickTasks() {
        // Next-tick one-shots
        while (!nextTickTasks.isEmpty()) {
            Runnable task = nextTickTasks.pollFirst();
            if (task != null) task.run();
        }
        // Delayed & interval tasks
        if (!timedTasks.isEmpty()) {
            timedTasks.removeIf(TimedTask::isCancelled);
            for (int i = timedTasks.size() - 1; i >= 0; i--) {
                TimedTask task = timedTasks.get(i);
                if (--task.remainingTicks <= 0) {
                    if (task.active.getAsBoolean()) {
                        task.callback.run();
                    }
                    if (task.intervalTicks > 0 && task.active.getAsBoolean()) {
                        task.remainingTicks = task.intervalTicks;
                    } else {
                        timedTasks.remove(i);
                    }
                }
            }
        }
    }

    //endregion

    //endregion

    //region activity

    public void tick() {
        if (!root.lifecycle.mounted()) {
            throw new IllegalStateException("Widget is not mounted!");
        }
        runTickTasks();
        // Non-recursive tick: collect and tick all tickable widgets in the tree
        // so that a child can be tickable without requiring its parent to also be tickable.
        WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
            if (widget.tickable()) {
                widget.tick();
            }
            return WidgetTree.TraversalControl.proceed;
        });
        context.tick();
    }

    //endregion

    //region layout

    private float width = Float.NaN;
    private float height = Float.NaN;

    public TaffyTree layoutTree() {
        return tree;
    }

    public void setLayoutArea(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void layout() {
        tree.computeLayout(root.nodeId(), new TaffySize<>(
                Float.isNaN(width) ? AvailableSpace.MAX_CONTENT : AvailableSpace.definite(width),
                Float.isNaN(height) ? AvailableSpace.MAX_CONTENT : AvailableSpace.definite(height)
        ));
    }

    //endregion

    //region lifecycle management

    private final ObjectSet<Widget> createdWidgets = new ObjectLinkedOpenHashSet<>();
    private final ObjectSet<Widget> remountWidgets = new ObjectLinkedOpenHashSet<>();
    private final ObjectSet<Widget> destroyingWidgets = new ObjectLinkedOpenHashSet<>();
    private final ObjectSet<Widget> retainedWidgets = new ObjectLinkedOpenHashSet<>();
    private SceneContext context;

    //region scene handle

    final SceneHandle globalHandle = SceneHandle.create(this);
    private final Object2ObjectOpenHashMap<Widget, SceneHandle> widgetHandles = new Object2ObjectOpenHashMap<>();

    SceneHandle handleOf(Widget widget) {
        return widgetHandles.computeIfAbsent(widget, k -> SceneHandle.create(this));
    }

    void cleanupHandle(Widget widget) {
        SceneHandle handle = widgetHandles.remove(widget);
        if (handle != null) {
            handle.cleanup();
        }
    }
    //endregion

    void addCreatedWidget(Widget widget) {
        if (widget.lifecycle != Lifecycle.created) {
            throw new IllegalArgumentException("Impossible!!! Widget: " + widget + " is not created!");
        }
        createdWidgets.add(widget);
    }

    public void init() {
        if (!root.lifecycle.initialized()) {
            WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
                widget.init();
                return TraversalControl.proceed;
            });
        } else {
            for (Widget createdWidget : createdWidgets) {
                createdWidget.init();
            }
            createdWidgets.clear();
        }
    }

    public void mount(SceneContext context) {
        this.context = context;
        setLayoutArea(context.width(), context.height());
        WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
            if (!widget.lifecycle.initialized()) {
                throw new IllegalArgumentException("Widget: " + widget + " is not initialized!");
            }
            widget.mount(this, this.context, handleOf(widget));
            return TraversalControl.proceed;
        });
    }

    public void reuse(Widget widget) {
        if (!widget.lifecycle.unmounted()) {
            throw new IllegalArgumentException("Cannot reuse widget: " + widget + " because it is not unmounted!");
        }
//        if (!destroyingWidgets.remove(widget)) {
//            throw new IllegalStateException("Widget: " + widget + " is not being destroyed!");
//        }
        //TODO:完善reuse的流程，让上面的检查能够工作
        WidgetTree.walkBreadthFirst(widget, true, -1, (w, depth) -> {
            destroyingWidgets.remove(w);
            retainedWidgets.add(w);
            return TraversalControl.proceed;
        });
    }

    void remountWidget(Widget widget) {
        if (!widget.lifecycle.unmounted()) {
            throw new IllegalArgumentException("Cannot add widget: " + widget + " because it is not unmounted!");
        }
        restoreParentLinks(widget);
        WidgetTree.walkBreadthFirst(widget, true, -1, (w, depth) -> {
            retainedWidgets.remove(w);
            return TraversalControl.proceed;
        });
        remountWidgets.add(widget);
    }

    private void restoreParentLinks(Widget widget) {
        if (widget instanceof CompositeWidget<?> composite) {
            for (Widget child : composite.children()) {
                child.parent = composite;
                restoreParentLinks(child);
            }
        }
    }

    void unmountWidget(Widget widget) {
        // Widget was queued for creation but never initialized/mounted — just discard
        if (createdWidgets.remove(widget)) {
            return;
        }
        if (!widget.lifecycle.mounted()) {
            throw new IllegalArgumentException("Cannot unmount widget: " + widget + " because it is not mounted!");
        }
        // Clean up focus state if the unmounting widget or its descendants had focus
        handleFocusWidgetUnmount(widget);
        WidgetTree.walkBottomUp(widget, true, -1, (w, depth) -> {
            w.unmount();
            destroyingWidgets.add(w);
            return TraversalControl.proceed;
        });
    }

    public void destroy() {
        if (!root.lifecycle.unmounted()) {
            WidgetTree.walkBottomUp(root, true, -1, ((widget, depth) -> {
                widget.unmount();
                destroyingWidgets.add(widget);
                return TraversalControl.proceed;
            }));
        }
        globalHandle.cleanup();
        for (Widget widget : destroyingWidgets) {
            widget.destroy();
        }
        destroyingWidgets.clear();
        destroyRetainedWidgets();
    }

    private void rebuildRequired() {
        if (!createdWidgets.isEmpty()) {
            for (Widget created : createdWidgets) {
                WidgetTree.walkBreadthFirst(created, true, -1, (widget, depth) -> {
                    widget.init();
                    return TraversalControl.proceed;
                });
            }
            for (Widget created : createdWidgets) {
                if (created.lifecycle.mounted()) continue;
                WidgetTree.walkBreadthFirst(created, true, -1, (widget, depth) -> {
                    widget.mount(this, this.context, handleOf(widget));
                    return TraversalControl.proceed;
                });
            }
            createdWidgets.clear();
        }
        if (!remountWidgets.isEmpty()) {
            for (Widget widget : remountWidgets) {
                WidgetTree.walkBreadthFirst(widget, true, -1, (w, depth) -> {
                    w.mount(this, context, handleOf(w));
                    return TraversalControl.proceed;
                });
            }
            remountWidgets.clear();
        }
        if (tree.needsVisit(root.nodeId())) {
            layout();
            root.applyLayout();
        }
    }

    private void cleanWidgets() {
        for (Widget widget : destroyingWidgets) {
            widget.destroy();
        }
        destroyingWidgets.clear();
    }

    private void destroyRetainedWidgets() {
        for (Widget widget : retainedWidgets) {
            if (widget.lifecycle.unmounted()) {
                widget.destroy();
            }
        }
        retainedWidgets.clear();
    }

    /**
     * Applies pending scene mutations without performing a render pass.
     * Useful for automation flows that trigger UI updates and need a stable
     * widget tree immediately afterward.
     */
    public void stabilize() {
        drainDeferred();
        rebuildRequired();
        runPostLayout();
    }

    //endregion

    //region render

    //region layer management

    private final Map<SceneLayer, MutableList<Widget>> extraLayers = new Object2ObjectLinkedOpenHashMap<>();

    {
        for (SceneLayer layer : SceneLayer.values()) {
            extraLayers.put(layer, MutableLists.empty());
        }
    }

    private @Nullable Inspector inspector;

    /**
     * Adds a widget to the specified layer.
     * <p>
     * The widget will be rendered in this layer and sorted by zIndex within the layer.
     * Widget must be mounted to this scene.
     *
     * @param layer  the layer to add the widget to
     * @param widget the widget to add
     */
    public void addToLayer(SceneLayer layer, Widget widget) {
        if (widget.scene != this) {
            throw new IllegalArgumentException("Widget must be mounted to this scene");
        }
        if (layer == SceneLayer.debug) {
            if (!(widget instanceof Inspector debugger)) {
                throw new IllegalArgumentException("Only Inspector can be added to debug layer");
            }
            this.inspector = debugger;
            return;
        }
        removeFromAllLayers(widget);
        var layerWidgets = extraLayers.get(layer);
        if (!layerWidgets.contains(widget)) {
            layerWidgets.add(widget);
            layerWidgets.sortThis(Comparator.comparingInt(Widget::zIndex));
        }
    }

    /**
     * Removes a widget from the specified layer.
     *
     * @param layer  the layer to remove the widget from
     * @param widget the widget to remove
     */
    public void removeFromLayer(SceneLayer layer, Widget widget) {
        extraLayers.get(layer).remove(widget);
    }

    /**
     * Removes a widget from all layers.
     *
     * @param widget the widget to remove
     */
    void removeFromAllLayers(Widget widget) {
        for (var layerWidgets : extraLayers.values()) {
            layerWidgets.remove(widget);
        }
    }

    /**
     * Re-sorts the specified layer by zIndex.
     * Called when a widget's zIndex changes.
     *
     * @param layer the layer to re-sort
     */
    void resortLayer(SceneLayer layer) {
        extraLayers.get(layer).sortThis(Comparator.comparingInt(Widget::zIndex));
    }


    //endregion

    //region tooltip

    private Tooltip hoverTooltip = new Tooltip();
    private boolean hoverRefreshQueued = false;
    private double queuedHoverMouseX = 0.0;
    private double queuedHoverMouseY = 0.0;

    public void setHoverTooltip(Tooltip tooltip) {
        this.hoverTooltip = Checks.checkNotNull(tooltip, "tooltip");
    }

    public void requestHoverRefresh(double mouseX, double mouseY) {
        queuedHoverMouseX = mouseX;
        queuedHoverMouseY = mouseY;
        if (hoverRefreshQueued) {
            return;
        }
        hoverRefreshQueued = true;
        defer(() -> {
            hoverRefreshQueued = false;
            if (!root.lifecycle.mounted()) {
                return;
            }
            mouseMoved(queuedHoverMouseX, queuedHoverMouseY);
        });
    }

    private void refreshHoverTooltip(double mouseX, double mouseY) {
        Tooltip resolved = resolveHoverTooltip(mouseX, mouseY);
        setHoverTooltip(resolved != null ? resolved : Tooltip.create());
    }

    private @Nullable Tooltip resolveHoverTooltip(double mouseX, double mouseY) {
        WidgetPath path = lastHoveredPath;
        if (path == null) {
            return null;
        }
        for (int i = path.size() - 1; i >= 0; i--) {
            Widget widget = path.get(i);
            if (!isMountedInThisScene(widget)) {
                continue;
            }
            FloatPos local = widget.sceneToLocal(mouseX, mouseY);
            Tooltip tooltip = widget.hoverTooltip((int) local.x, (int) local.y);
            if (tooltip != null && tooltip.notEmpty()) {
                return tooltip;
            }
        }
        return null;
    }

    private record TooltipInstance(Tooltip tooltip, ClientTooltipPositioner positioner) {
    }

    //endregion

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drainDeferred();
        rebuildRequired();
        runPostLayout();
        SceneCanvas canvas = SceneCanvas.create(graphics);
        {
            // Root widget: apply its full viewport transform (includes layout position)
            canvas.pushViewport(root.viewport());
            FloatPos localMouse = root.viewport().parentToLocal(mouseX, mouseY);
            root.render(canvas, (int) localMouse.x, (int) localMouse.y, partialTick);
            canvas.popViewport();
        }
        for (SceneLayer layer : SceneLayer.extraLayers) {
            var layerWidgets = extraLayers.get(layer);
            for (int i = 0; i < layerWidgets.size(); i++) {
                Widget widget = layerWidgets.get(i);
                if (!isMountedInThisScene(widget) || !widget.shouldRender()) continue;

                if (widget.coordinateSpace == CoordinateSpace.scene) {
                    // Scene-positioned: layout is in scene space,
                    // only push the widget's own viewport.
                    canvas.pushViewport(widget.viewport());
                    FloatPos localMouse = widget.viewport().parentToLocal(mouseX, mouseY);
                    widget.render(canvas, (int) localMouse.x, (int) localMouse.y, partialTick);
                    canvas.popViewport();
                } else {
                    // Parent-relative: push the full ancestor viewport chain
                    // so the widget renders at the correct position.
                    WidgetPath path = widget.path();
                    int pathSize = path.size();
                    for (int p = 0; p < pathSize; p++) {
                        canvas.pushViewport(path.get(p).viewport());
                    }
                    FloatPos localMouse = widget.sceneToLocal(mouseX, mouseY);
                    widget.render(canvas, (int) localMouse.x, (int) localMouse.y, partialTick);
                    for (int p = 0; p < pathSize; p++) {
                        canvas.popViewport();
                    }
                }
            }
        }

        canvas.flushBatch();

        if (hoverTooltip.notEmpty()) {
            ScreenUtil.renderTooltip(graphics, hoverTooltip, mouseX, mouseY);
        }

        if (inspector != null) {
            canvas.pushViewport(inspector.viewport());
            FloatPos localMouse = inspector.viewport().parentToLocal(mouseX, mouseY);
            inspector.render(canvas, (int) localMouse.x, (int) localMouse.y, partialTick);
            canvas.popViewport();
            canvas.flushBatch();
        }

        draggableManager.renderDragging(graphics, mouseX, mouseY, partialTick);
        runPostRender();
        cleanWidgets();
    }

    //endregion

    //region user input

    private static final int doubleClickThreshold = 500;//500ms
    private static final int doubleClickRadius = 5;//5px

    private final DraggableManager draggableManager;
    private @Nullable Widget currentClickWidget;

    private @Nullable Widget lastClickedWidget;
    private int lastClickButton;
    private int clickCount;
    private long lastClickTime;
    private WidgetPath lastHoveredPath;

    private FloatPos lastClickPos;

    //region click region

    /**
     * Registry of click-region groups.
     * Key: the group identifier passed to {@link Widget#setClickGroup(Object)}.
     * Value: all currently-mounted widgets that share that group key.
     */
    private final Map<Object, MutableList<Widget>> clickGroups = new Object2ObjectOpenHashMap<>();

    /**
     * Registers a widget with a click-region group.
     * Called from {@link Widget#setClickGroup(Object)} and {@link Widget#mount}.
     */
    void registerClickGroup(Widget widget, Object groupKey) {
        clickGroups.computeIfAbsent(groupKey, k -> MutableLists.of()).add(widget);
    }

    /**
     * Removes a widget from a click-region group.
     * Called from {@link Widget#setClickGroup(Object)} and {@link Widget#unmount}.
     */
    void unregisterClickGroup(Widget widget, Object groupKey) {
        var list = clickGroups.get(groupKey);
        if (list != null) {
            list.remove(widget);
            if (list.isEmpty()) {
                clickGroups.remove(groupKey);
            }
        }
    }

    /**
     * After a mouse click is dispatched normally, checks every click-region group.
     * If the click target (or any of its ancestors) is not a member of a group,
     * fires {@link WidgetEvent#onClickOutside} on all members of that group.
     */
    private void dispatchClickOutside(Widget target) {
        if (clickGroups.isEmpty()) return;
        for (var entry : clickGroups.entrySet()) {
            MutableList<Widget> members = entry.getValue();
            boolean inside = false;
            for (int i = 0; i < members.size(); i++) {
                Widget member = members.get(i);
                if (member == target || target.path().isAncestor(member)) {
                    inside = true;
                    break;
                }
            }
            if (!inside) {
                // Snapshot to protect against concurrent modification
                // (an onClickOutside handler might change click groups)
                var snapshot = MutableLists.ofAll(members);
                for (int i = 0; i < snapshot.size(); i++) {
                    Widget member = snapshot.get(i);
                    if (member.lifecycle.mounted()) {
                        member.listeners(WidgetEvent.onClickOutside)
                              .onClickOutside(member.interruptible());
                    }
                }
            }
        }
    }

    //endregion

    //region focus

    private final FocusScopeNode rootScope;
    private @Nullable FocusNode primaryFocus;

    /**
     * @return the root focus scope for this scene.
     */
    public FocusScopeNode rootScope() {
        return rootScope;
    }

    /**
     * @return the current primary focus node, or null if nothing is focused.
     */
    public @Nullable FocusNode primaryFocus() {
        return primaryFocus;
    }

    /**
     * @return the widget that currently holds primary focus, or null.
     */
    public @Nullable Widget focusingWidget() {
        return primaryFocus != null ? primaryFocus.owner : null;
    }

    /**
     * Requests primary focus for the given node.
     */
    public void requestFocus(FocusNode node) {
        if (node.owner == null || !node.owner.lifecycle.mounted()) return;
        if (!node.canRequestFocus) return;
        doFocus(node);
    }

    /**
     * Convenience: requests focus for the widget's focus node.
     */
    public void requestFocus(Widget widget) {
        if (widget.lifecycle.mounted() && widget.focusNode != null) {
            requestFocus(widget.focusNode);
        }
    }

    /**
     * Removes focus from the given node.
     * If the node is the primary focus or an ancestor of it, focus is cleared.
     */
    public void unfocus(FocusNode node) {
        if (primaryFocus == null) return;
        if (primaryFocus == node || node.hasFocus) {
            doClearFocus();
        }
    }

    /**
     * Convenience: removes focus from the widget's focus node.
     */
    public void requestUnfocus(Widget widget) {
        if (widget.focusNode != null) {
            unfocus(widget.focusNode);
        }
    }

    /**
     * Clears all focus in this scene.
     */
    public void clearFocus() {
        if (primaryFocus != null) doClearFocus();
    }

    //endregion

    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that was clicked.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Widget target = hitTest(mouseX, mouseY);
        if (target != null) {
            Widget focusable = findFocusable(target, mouseX, mouseY);
            if (focusable != null && focusable.focusNode != null) {
                requestFocus(focusable.focusNode);
            }
            var input = InputContext.fromMouse(mouseX, mouseY, button);
            var bubble = target.bubble();
            currentClickWidget = target;
            lastClickButton = button;
            boolean result = handleBubbleEvent(
                    target, bubble, InputEvents.onMouseClicked,
                    (listener) -> listener.onClicked(input, bubble)
            );
            refreshHoverTooltip(mouseX, mouseY);
            return result;
        }
        return false;
    }

    private static @Nullable Widget findFocusable(Widget widget, double mouseX, double mouseY) {
        if (widget.focusable()) return widget;
        Widget focusable = null;
        CompositeWidget<?> parent = widget.parent();
        while (parent != null && focusable == null) {
            if (parent.focusable() && parent.isMouseOver(mouseX, mouseY)) {
                focusable = parent;
            }
            parent = parent.parent();
        }
        return focusable;
    }

    /**
     * Called when a mouse button is released within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that was released.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Widget target = hitTest(mouseX, mouseY);
        if (target != null) {
            InputContext input = InputContext.fromMouse(mouseX, mouseY, button);
            var releaseContext = target.bubble();
            var result = handleBubbleEvent(
                    target, releaseContext, InputEvents.onMouseReleased,
                    (listener) -> listener.onReleased(input, releaseContext)
            );

            if (currentClickWidget == target && target.isMouseOver(mouseX, mouseY)) {
                boolean isContinuousClick = lastClickedWidget == target
                        && lastClickButton == button
                        && System.currentTimeMillis() - lastClickTime <= doubleClickThreshold
                        && lastClickPos != null
                        && Math.sqrt(Math.pow(mouseX - lastClickPos.x(), 2) + Math.pow(mouseY - lastClickPos.y(), 2)) <= doubleClickRadius;

                clickCount = isContinuousClick ? clickCount + 1 : 1;

                var clickContext = target.bubble();
                result |= handleBubbleEvent(
                        target, clickContext, InputEvents.onMouseClick,
                        (listener) -> listener.onClick(input, clickCount, clickContext)
                );
                lastClickedWidget = target;
                lastClickPos = new FloatPos(mouseX, mouseY);
                lastClickButton = button;
                lastClickTime = System.currentTimeMillis();
                dispatchClickOutside(target);
            }
            currentClickWidget = null;
            refreshHoverTooltip(mouseX, mouseY);
            return result;
        }
        clickCount = 0;
        lastClickedWidget = null;
        lastClickPos = null;
        lastClickButton = Integer.MIN_VALUE;
        lastClickTime = 0;
        currentClickWidget = null;
        return false;
    }

    /**
     * Called when the mouse is moved within the GUI element.
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     */
    public void mouseMoved(double mouseX, double mouseY) {
        Widget target = hitTest(mouseX, mouseY);
        if (target != null) {
            target.listeners(InputEvents.onMouseMoved).onMoved(mouseX, mouseY, target.interruptible());
            if (!isMountedInThisScene(target)) {
                target = null;
            }
        }
        //region mouse enter/leave
        if (target != null) {
            WidgetPath currentPath = target.path();
            if (lastHoveredPath == null) {
                for (int i = 0; i < currentPath.size(); i++) {
                    Widget widget = currentPath.get(i);
                    if (!isMountedInThisScene(widget)) continue;
                    widget.hovered = true;
                    widget.listeners(InputEvents.onMouseEnter).onEnter(mouseX, mouseY, widget.interruptible());
                }
            } else {
                int forkIndex = currentPath.commonAncestorIndex(lastHoveredPath);
                for (int i = lastHoveredPath.size() - 1; i > forkIndex; i--) {
                    Widget widget = lastHoveredPath.get(i);
                    if (!isMountedInThisScene(widget)) continue;
                    widget.hovered = false;
                    widget.listeners(InputEvents.onMouseLeave).onLeave(mouseX, mouseY, widget.interruptible());
                }
                for (int i = forkIndex + 1; i < currentPath.size(); i++) {
                    Widget widget = currentPath.get(i);
                    if (!isMountedInThisScene(widget)) continue;
                    widget.hovered = true;
                    widget.listeners(InputEvents.onMouseEnter).onEnter(mouseX, mouseY, widget.interruptible());
                }
            }
            lastHoveredPath = isMountedInThisScene(target) ? currentPath : null;
        } else if (lastHoveredPath != null) {
            for (int i = lastHoveredPath.size() - 1; i >= 0; i--) {
                Widget widget = lastHoveredPath.get(i);
                if (!isMountedInThisScene(widget)) continue;
                widget.hovered = false;
                widget.listeners(InputEvents.onMouseLeave).onLeave(mouseX, mouseY, widget.interruptible());
            }
            lastHoveredPath = null;
        }
        //endregion
        refreshHoverTooltip(mouseX, mouseY);
    }

    /**
     * Called when the mouse is dragged within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that is being dragged.
     * @param dragX  the X distance of the drag.
     * @param dragY  the Y distance of the drag.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        Widget target = hitTest(mouseX, mouseY);
        if (target != null) {
            var input = InputContext.fromMouse(mouseX, mouseY, button);
            var bubble = target.bubble();
            return handleBubbleEvent(
                    target, bubble, InputEvents.onMouseDragged,
                    (listener) -> listener.onDragged(input, dragX, dragY, bubble)
            );
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Widget target = hitTest(mouseX, mouseY);
        if (target != null) {
            var bubble = target.bubble();
            boolean result = handleBubbleEvent(
                    target, bubble, InputEvents.onMouseScrolled,
                    (listener) -> listener.onScrolled(mouseX, mouseY, scrollX, scrollY, bubble)
            );
            refreshHoverTooltip(mouseX, mouseY);
            return result;
        }
        return false;
    }

    /**
     * Called when a keyboard key is pressed within the GUI element.
     * <p>
     *
     * @param keyCode   the key code of the pressed key.
     * @param scanCode  the scan code of the pressed key.
     * @param modifiers the keyboard modifiers.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Widget fw = focusingWidget();
        if (fw == null || !fw.lifecycle.mounted()) return false;
        var localMouse = root.sceneToLocal(ScreenUtil.getMouseX(), ScreenUtil.getMouseY());
        double mouseX = localMouse.x;
        double mouseY = localMouse.y;
        var input = InputContext.fromKeyboard(keyCode, scanCode, modifiers, mouseX, mouseY);
        var bubble = fw.bubble();
        return handleBubbleEvent(
                fw, bubble, InputEvents.onKeyPressed,
                (listener) -> listener.onKeyPressed(input, bubble)
        );
    }

    /**
     * Called when a keyboard key is released within the GUI element.
     * <p>
     *
     * @param keyCode   the key code of the released key.
     * @param scanCode  the scan code of the released key.
     * @param modifiers the keyboard modifiers.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        Widget fw = focusingWidget();
        if (fw == null || !fw.lifecycle.mounted()) return false;
        var localMouse = root.sceneToLocal(ScreenUtil.getMouseX(), ScreenUtil.getMouseY());
        double mouseX = localMouse.x;
        double mouseY = localMouse.y;
        var input = InputContext.fromKeyboard(keyCode, scanCode, modifiers, mouseX, mouseY);
        var bubble = fw.bubble();
        return handleBubbleEvent(
                fw, bubble, InputEvents.onKeyReleased,
                (listener) -> listener.onKeyReleased(input, bubble)
        );
    }

    /**
     * Called when a character is typed within the GUI element.
     * <p>
     *
     * @param codePoint the code point of the typed character.
     * @param modifiers the keyboard modifiers.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    public boolean charTyped(char codePoint, int modifiers) {
        Widget fw = focusingWidget();
        if (fw != null && fw.lifecycle.mounted()) {
            var bubble = fw.bubble();
            return handleBubbleEvent(
                    fw, bubble, InputEvents.onCharTyped,
                    (listener) -> listener.onCharTyped(codePoint, modifiers, bubble)
            );
        }
        return false;
    }

    //endregion

    //region hit test & bubble event

    public @Nullable Widget hitTest(double mouseX, double mouseY) {
        if (!isMountedInThisScene(root)) {
            return null;
        }
        // Extra layers (floating, overlay, …) — highest priority, checked in reverse order
        for (int i = SceneLayer.extraLayers.size() - 1; i >= 0; i--) {
            SceneLayer layer = SceneLayer.extraLayers.get(i);
            if (layer.hitTestMode() == HitTestAction.none) continue;
            var widgets = extraLayers.get(layer);

            for (int j = widgets.size() - 1; j >= 0; j--) {
                Widget layerWidget = widgets.get(j);
                if (!isMountedInThisScene(layerWidget)) continue;
                double hitX, hitY;
                boolean inSceneSpace = layerWidget.coordinateSpace == CoordinateSpace.scene || layerWidget.parent() == null;
                if (inSceneSpace) {
                    hitX = mouseX;
                    hitY = mouseY;
                } else {
                    var parent = layerWidget.parent();
                    if (!isMountedInThisScene(parent)) continue;
                    FloatPos local = parent.sceneToLocal(mouseX, mouseY);
                    hitX = local.x + parent.viewport().contentOffsetX();
                    hitY = local.y + parent.viewport().contentOffsetY();
                }

                Widget hit = mountedHit(WidgetTree.hitTest(layerWidget, hitX, hitY));
                if (hit != null) return hit;
            }
        }

        // Content layer (root tree)
        Widget rootHit = mountedHit(WidgetTree.hitTest(root, mouseX, mouseY));
        if (rootHit != null) return rootHit;

        // Debug layer — inspector has the lowest priority so it never shadows
        // other widgets during mouse tracking.
        if (inspector != null) {
            return mountedHit(WidgetTree.hitTest(inspector, mouseX, mouseY));
        }

        return null;
    }

    private boolean isMountedInThisScene(@Nullable Widget widget) {
        return widget != null && widget.lifecycle.mounted() && widget.scene == this;
    }

    private @Nullable Widget mountedHit(@Nullable Widget widget) {
        return isMountedInThisScene(widget) ? widget : null;
    }

    public static <E extends WidgetEvent> boolean handleBubbleEvent(
            Widget target, BubbleContext bubble,
            EventDefinition<E> event, Function<E, EventDispatch> listenerInvoke
    ) {
        WidgetPath path = target.path();
        EventDispatch action = EventDispatch.pass;
        //NOTE:target index is path.size() - 1
        //stage 1: capture — skip inactive widgets
        bubble.setPhase(BubbleContext.Phase.capture);
        for (int i = 0; i < path.size() - 1; i++) {
            Widget widget = path.get(i);
            if (widget.inactive()) continue;
            bubble.setCurrent(widget.events());
            action = EventDispatch.max(listenerInvoke.apply(widget.listeners(event)), action);
            if (bubble.consumed() || bubble.cancelled()) return action.handled();
        }
        //stage 2: target — skip if inactive
        if (target.active()) {
            bubble.setPhase(BubbleContext.Phase.target);
            bubble.setCurrent(target.events());
            action = EventDispatch.max(listenerInvoke.apply(target.listeners(event)), action);
            if (bubble.consumed() || bubble.cancelled()) return action.handled();
        }
        //stage 3: bubble — skip inactive widgets
        bubble.setPhase(BubbleContext.Phase.bubble);
        for (int i = path.size() - 2; i >= 0; i--) {
            Widget widget = path.get(i);
            if (widget.inactive()) continue;
            bubble.setCurrent(widget.events());
            action = EventDispatch.max(listenerInvoke.apply(widget.listeners(event)), action);
            if (bubble.consumed() || bubble.cancelled()) return action.handled();
        }
        return action.handled();
    }

    //endregion

    //region internal

    final PerformerContainer performers = new PerformerContainer();

    /**
     * Gets the cached path for a widget in this tree.
     *
     * @param widget the widget to get path for
     * @return the widget's path from leaf to root
     */
    WidgetPath pathOf(Widget widget) {
        return pathCache.get(widget);
    }

    /**
     * Marks all cached paths as stale.
     * <p>
     * Called by container mutations (add/remove/clear/reparent). Paths are rebuilt lazily.
     */
    void invalidatePathCache() {
        pathCache.invalidate();
    }

    //region focus internals

    private void handleFocusWidgetUnmount(Widget widget) {
        if (primaryFocus == null) return;
        FocusNode node = widget.focusNode;
        if (node != null && (primaryFocus == node || node.hasFocus)) {
            doClearFocus();
            return;
        }
        if (primaryFocus.owner != null && primaryFocus.owner.path().isAncestor(widget)) {
            doClearFocus();
        }
    }

    /**
     * Focus transitions follow this event sequence:
     * <ol>
     *     <li>Old focus path: clear {@code hasFocus} on diverging branch</li>
     *     <li>Old focus: {@code hasPrimaryFocus = false}</li>
     *     <li>Old focus: fire {@link WidgetEvent#onFocusOut} (bubbling)</li>
     *     <li>Old focus: fire {@link WidgetEvent#onFocusLost} (local)</li>
     *     <li>New focus path: set {@code hasFocus} on diverging branch</li>
     *     <li>New focus: {@code hasPrimaryFocus = true}</li>
     *     <li>New focus: fire {@link WidgetEvent#onFocusIn} (bubbling)</li>
     *     <li>New focus: fire {@link WidgetEvent#onFocus} (local)</li>
     * </ol>
     */
    private void doFocus(FocusNode node) {
        FocusNode oldFocus = primaryFocus;
        if (oldFocus == node) return;

        Widget newWidget = node.owner;
        assert newWidget != null && newWidget.lifecycle.mounted();
        WidgetPath newPath = newWidget.path();

        if (oldFocus != null && oldFocus.owner != null && oldFocus.owner.lifecycle.mounted()) {
            Widget oldWidget = oldFocus.owner;
            WidgetPath oldPath = oldWidget.path();
            int forkIndex = newPath.commonAncestorIndex(oldPath);

            for (int i = oldPath.size() - 1; i > forkIndex; i--) {
                FocusNode fn = oldPath.get(i).focusNode;
                if (fn != null) fn.hasFocus = false;
            }

            oldFocus.hasPrimaryFocus = false;

            var bubble = oldWidget.bubble();
            handleBubbleEvent(oldWidget, bubble, WidgetEvent.onFocusOut,
                    (listener) -> listener.onFocusOut(oldWidget, bubble)
            );
            oldWidget.listeners(WidgetEvent.onFocusLost).onFocusLost(oldWidget.interruptible());

            for (int i = forkIndex + 1; i < newPath.size(); i++) {
                FocusNode fn = newPath.get(i).focusNode;
                if (fn != null) fn.hasFocus = true;
            }
        } else {
            if (oldFocus != null) {
                oldFocus.hasPrimaryFocus = false;
                oldFocus.hasFocus = false;
            }
            for (int i = 0; i < newPath.size(); i++) {
                FocusNode fn = newPath.get(i).focusNode;
                if (fn != null) fn.hasFocus = true;
            }
        }

        primaryFocus = node;
        node.hasPrimaryFocus = true;
        node.hasFocus = true;

        updateScopeFocusedChild(node);

        var bubble = newWidget.bubble();
        handleBubbleEvent(
                newWidget, bubble, WidgetEvent.onFocusIn,
                (listener) -> listener.onFocusIn(newWidget, bubble)
        );
        newWidget.listeners(WidgetEvent.onFocus).onFocus(newWidget.interruptible());
    }

    private void doClearFocus() {
        if (primaryFocus == null) return;
        FocusNode oldFocus = primaryFocus;
        Widget oldWidget = oldFocus.owner;
        primaryFocus = null;

        if (oldWidget != null && oldWidget.lifecycle.mounted()) {
            WidgetPath oldPath = oldWidget.path();
            for (int i = 0; i < oldPath.size(); i++) {
                FocusNode fn = oldPath.get(i).focusNode;
                if (fn != null) fn.hasFocus = false;
            }
            oldFocus.hasPrimaryFocus = false;

            var bubble = oldWidget.bubble();
            handleBubbleEvent(oldWidget, bubble, WidgetEvent.onFocusOut,
                    (listener) -> listener.onFocusOut(oldWidget, bubble)
            );
            oldWidget.listeners(WidgetEvent.onFocusLost).onFocusLost(oldWidget.interruptible());
        } else {
            oldFocus.hasPrimaryFocus = false;
            oldFocus.hasFocus = false;
        }
    }

    private void updateScopeFocusedChild(FocusNode node) {
        FocusScopeNode scope = node.enclosingScope();
        while (scope != null) {
            scope.focusedChild = node;
            scope = scope.enclosingScope();
        }
    }

    //endregion

    //endregion


}
