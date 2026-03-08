package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.base.*;
import dev.vfyjxf.cloudlib.api.ui.dump.DisplayVariant;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree.TraversalControl;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Main entry point for UI testing.
 * <p>
 * Manages the full lifecycle of a test scene: creation, widget tree construction,
 * initialization, mounting, layout, input simulation, and assertions.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * var scene = TestScene.create(800, 600);
 * scene.add(ButtonWidget.of("OK").key("ok"));
 * scene.setup();
 *
 * scene.tap("ok");
 * scene.assertThat("ok").hasLabel("OK").isVisible();
 * }</pre>
 */
public final class TestScene {

    private final TestSceneHost host;
    private final WidgetGroup<Widget> root;
    private final Scene scene;
    private boolean setupDone = false;
    private SceneContext sceneContext;

    // ==================== Input state tracking ====================
    private double mouseX = 0;
    private double mouseY = 0;
    private boolean ctrlDown = false;
    private boolean shiftDown = false;
    private boolean altDown = false;
    private int pressedButton = -1;

    private TestScene(TestSceneHost host) {
        this.host = host;
        this.root = new WidgetGroup<>();
        root.setFocusNode(new FocusScopeNode());
        this.scene = new Scene(root);
    }

    // ==================== Factory ====================

    /**
     * Creates a test scene with the specified dimensions. No Minecraft client required.
     */
    public static TestScene create(int width, int height) {
        return new TestScene(new TestSceneHost(width, height));
    }

    // ==================== Tree construction ====================

    /**
     * Adds a widget to the root group.
     */
    public <T extends Widget> T add(T widget) {
        root.addWidget(widget);
        return widget;
    }

    /**
     * Adds a widget with a key and explicit bounds.
     */
    public <T extends Widget> T add(T widget, Object key, int x, int y, int w, int h) {
        widget.setKey(key);
        widget.useStyle(UIStyle.of(
                positionAbsolute(),
                insetLeft(x), insetTop(y),
                sizeOf(w, h)
        ));
        root.addWidget(widget);
        return widget;
    }

    /**
     * Adds a widget with a key.
     */
    public <T extends Widget> T add(T widget, Object key) {
        widget.setKey(key);
        root.addWidget(widget);
        return widget;
    }

    /**
     * Sets explicit bounds on a widget via absolute positioning style.
     */
    public static void setBound(Widget widget, int x, int y, int w, int h) {
        widget.useStyle(UIStyle.of(
                positionAbsolute(),
                insetLeft(x), insetTop(y),
                sizeOf(w, h)
        ));
    }

    /**
     * Sets bounds on a widget via absolute positioning style.
     * Use this for nested widgets whose bounds need to be explicitly set.
     */
    public void setTrackedBound(Widget widget, int x, int y, int w, int h) {
        widget.useStyle(UIStyle.of(
                positionAbsolute(),
                insetLeft(x), insetTop(y),
                sizeOf(w, h)
        ));
    }

    /**
     * Sets key on a widget.
     */
    public static void setKey(Widget widget, Object key) {
        widget.setKey(key);
    }

    /**
     * Builds the widget tree using a builder lambda.
     */
    public void build(Consumer<WidgetGroup<Widget>> builder) {
        builder.accept(root);
    }

    // ==================== Lifecycle ====================

    /**
     * Performs init → mount → layout → applyLayout in one call.
     */
    public void setup() {
        init();
        mount();
        layout();
        setupDone = true;
    }

    public void init() {
        scene.init();
    }

    public void mount() {
        sceneContext = SceneContext.create(host);
        scene.mount(sceneContext);
    }

    /**
     * Handles dynamic widget additions after setup().
     * Initializes and mounts only newly-added (unmounted) widgets,
     * then re-applies layout bounds. Use this instead of init()+mount()
     * after adding widgets to an already-setup scene.
     */
    public void rebuild() {
        try {
            var method = Scene.class.getDeclaredMethod("rebuildRequired");
            method.setAccessible(true);
            method.invoke(scene);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke rebuildRequired", e);
        }
        layout();
    }

    public void layout() {
        root.useStyle(UIStyle.of(sizeOf(host.width(), host.height())));
        scene.setLayoutArea(host.width(), host.height());
        try {
            scene.layout();
            root.applyLayout();
        } catch (Exception e) {
            // Layout may fail if MeasureFuncs need Font that isn't available.
        }
    }

    /**
     * Resizes the scene to new logical dimensions and re-runs layout.
     * Use this to test how the UI adapts to different window sizes.
     *
     * @param width  new logical width
     * @param height new logical height
     */
    public void resize(int width, int height) {
        host.resize(width, height);
        layout();
    }

    /**
     * Runs a test callback for each {@link DisplayVariant} in the standard set,
     * resizing the scene each time.
     *
     * <pre>{@code
     * scene.forEachSize(variant -> {
     *     assertThat(scene.find("header").width())
     *         .isEqualTo(variant.logicalWidth());
     * });
     * }</pre>
     */
    public void forEachSize(java.util.function.Consumer<DisplayVariant> test) {
        forEachSize(DisplayVariant.standardSet(), test);
    }

    /**
     * Runs a test callback for each {@link DisplayVariant} in the given list.
     */
    public void forEachSize(java.util.List<DisplayVariant> variants, java.util.function.Consumer<DisplayVariant> test) {
        for (DisplayVariant v : variants) {
            resize(v.logicalWidth(), v.logicalHeight());
            test.accept(v);
        }
    }

    public void tick() {
        scene.tick();
    }

    public void tick(int count) {
        for (int i = 0; i < count; i++) {
            scene.tick();
        }
    }

    public void destroy() {
        scene.destroy();
    }

    // ==================== Access ====================

    public Scene scene() {
        return scene;
    }

    public WidgetGroup<Widget> root() {
        return root;
    }

    // ==================== Finding ====================

    public Widget find(WidgetFinder finder) {
        Widget result = finder.findFirst(root);
        assertNotNull(result, () -> "No widget found matching: " + finder + "\nTree:\n" + printTree());
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends Widget> T find(WidgetFinder finder, Class<T> type) {
        Widget result = find(finder);
        assertTrue(type.isInstance(result), () -> "Widget found by " + finder + " is "
                + result.getClass().getSimpleName() + ", not " + type.getSimpleName());
        return (T) result;
    }

    public Widget find(Object key) {
        return find(WidgetFinder.byKey(key));
    }

    @SuppressWarnings("unchecked")
    public <T extends Widget> T find(Object key, Class<T> type) {
        return find(WidgetFinder.byKey(key), type);
    }

    public List<Widget> findAll(WidgetFinder finder) {
        return finder.findAll(root);
    }

    public boolean exists(WidgetFinder finder) {
        return finder.findFirst(root) != null;
    }

    public boolean exists(Object key) {
        return exists(WidgetFinder.byKey(key));
    }

    public int count(WidgetFinder finder) {
        return finder.findAll(root).size();
    }

    // ==================== Hit Testing ====================

    /**
     * Returns the widget that would receive input at the given coordinates,
     * or null if no widget is hit.
     */
    public @Nullable Widget hitTestAt(double x, double y) {
        return scene.hitTest(x, y);
    }

    /**
     * Asserts that the widget at the given coordinates has the expected key.
     */
    public void assertHitTarget(double x, double y, Object expectedKey) {
        Widget hit = hitTestAt(x, y);
        assertNotNull(hit, () -> "No widget at (" + x + ", " + y + ")\nTree:\n" + printTree());
        assertEquals(expectedKey, hit.key(),
                () -> "Expected hit target at (" + x + ", " + y + ") to be '" + expectedKey
                        + "' but was '" + hit.key() + "' (" + hit.getClass().getSimpleName() + ")");
    }

    /**
     * Asserts that no widget would receive input at the given coordinates.
     */
    public void assertNoHitAt(double x, double y) {
        Widget hit = hitTestAt(x, y);
        if (hit != null && hit != root) {
            fail("Expected no widget at (" + x + ", " + y + ") but found "
                    + hit.getClass().getSimpleName() + "[key=" + hit.key() + "]");
        }
    }

    // ==================== Focus management ====================

    /**
     * Returns the widget that currently holds primary focus, or null.
     */
    public @Nullable Widget focusedWidget() {
        return scene.focusingWidget();
    }

    /**
     * Requests primary focus for the widget with the given key.
     */
    public void requestFocus(Object key) {
        Widget w = find(key);
        scene.requestFocus(w);
    }

    /**
     * Clears all focus in the scene.
     */
    public void clearFocus() {
        scene.clearFocus();
    }

    /**
     * Asserts that the widget with the given key currently holds primary focus.
     */
    public void assertFocused(Object key) {
        Widget focused = focusedWidget();
        assertNotNull(focused, () -> "Expected '" + key + "' to be focused but no widget has focus\nTree:\n" + printTree());
        assertEquals(key, focused.key(),
                () -> "Expected '" + key + "' to be focused but '" + focused.key() + "' is focused");
    }

    /**
     * Asserts that no widget has focus.
     */
    public void assertNoFocus() {
        Widget focused = focusedWidget();
        assertNull(focused, () -> "Expected no focus but '" + focused.key() + "' is focused");
    }

    // ==================== Dynamic tree mutations ====================

    /**
     * Removes a widget from the tree by key.
     */
    public void remove(Object key) {
        Widget w = find(key);
        var parent = w.parent();
        assertNotNull(parent, () -> "Cannot remove root widget");
        assertTrue(parent instanceof WidgetGroup<?>, "Parent must be a WidgetGroup to remove");
        ((WidgetGroup<?>) parent).remove(w);
    }

    // ==================== Assertions ====================

    public WidgetAssert assertThat(WidgetFinder finder) {
        Widget w = find(finder);
        return new WidgetAssert(w, finder.toString());
    }

    public WidgetAssert assertThat(Object key) {
        Widget w = find(key);
        return new WidgetAssert(w, "key=" + key);
    }

    /**
     * Creates a chainable assertion object for a widget you already have a reference to.
     */
    public WidgetAssert assertThat(Widget widget) {
        return new WidgetAssert(widget, widget.key() != null ? "key=" + widget.key() : widget.getClass().getSimpleName());
    }

    public void assertWidget(Object key, Consumer<WidgetAssert> assertions) {
        assertions.accept(assertThat(key));
    }

    public void assertWidget(WidgetFinder finder, Consumer<WidgetAssert> assertions) {
        assertions.accept(assertThat(finder));
    }

    public void assertExists(WidgetFinder finder) {
        assertTrue(exists(finder), () -> "Expected widget to exist: " + finder + "\nTree:\n" + printTree());
    }

    public void assertExists(Object key) {
        assertExists(WidgetFinder.byKey(key));
    }

    public void assertNotExists(WidgetFinder finder) {
        assertFalse(exists(finder), "Expected widget NOT to exist: " + finder);
    }

    public void assertNotExists(Object key) {
        assertNotExists(WidgetFinder.byKey(key));
    }

    public void assertCount(WidgetFinder finder, int expected) {
        int actual = count(finder);
        assertEquals(expected, actual,
                () -> "Widget count mismatch for: " + finder + " (found " + actual + ")\nTree:\n" + printTree());
    }

    // ==================== Simple actions ====================

    public void tap(WidgetFinder finder) {
        Widget w = find(finder);
        tapWidget(w);
    }

    public void tap(Object key) {
        tap(WidgetFinder.byKey(key));
    }

    public void tapAt(double x, double y) {
        tapAt(x, y, 0);
    }

    public void tapAt(double x, double y, int button) {
        mouseX = x;
        mouseY = y;
        scene.mouseClicked(x, y, button);
        scene.mouseReleased(x, y, button);
    }

    /**
     * Taps at a position relative to a widget's top-left corner.
     * Useful for testing edge clicks (e.g., click the right side of a slider).
     */
    public void tapAt(Object key, int offsetX, int offsetY) {
        Widget w = find(key);
        var abs = w.absolutePos();
        double x = abs.x() + offsetX;
        double y = abs.y() + offsetY;
        tapAt(x, y);
    }

    public void doubleTap(WidgetFinder finder) {
        Widget w = find(finder);
        Pos center = widgetCenter(w);
        mouseX = center.x();
        mouseY = center.y();
        scene.mouseClicked(mouseX, mouseY, 0);
        scene.mouseReleased(mouseX, mouseY, 0);
        scene.mouseClicked(mouseX, mouseY, 0);
        scene.mouseReleased(mouseX, mouseY, 0);
    }

    public void doubleTap(Object key) {
        doubleTap(WidgetFinder.byKey(key));
    }

    public void rightTap(WidgetFinder finder) {
        Widget w = find(finder);
        Pos center = widgetCenter(w);
        mouseX = center.x();
        mouseY = center.y();
        scene.mouseClicked(mouseX, mouseY, 1);
        scene.mouseReleased(mouseX, mouseY, 1);
    }

    public void rightTap(Object key) {
        rightTap(WidgetFinder.byKey(key));
    }

    public void hover(WidgetFinder finder) {
        Widget w = find(finder);
        Pos center = widgetCenter(w);
        mouseX = center.x();
        mouseY = center.y();
        scene.mouseMoved(mouseX, mouseY);
    }

    public void hover(Object key) {
        hover(WidgetFinder.byKey(key));
    }

    public void scroll(WidgetFinder finder, double scrollX, double scrollY) {
        Widget w = find(finder);
        Pos center = widgetCenter(w);
        mouseX = center.x();
        mouseY = center.y();
        scene.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void scroll(Object key, double scrollX, double scrollY) {
        scroll(WidgetFinder.byKey(key), scrollX, scrollY);
    }

    public void drag(WidgetFinder from, WidgetFinder to) {
        Widget wFrom = find(from);
        Widget wTo = find(to);
        Pos fromCenter = widgetCenter(wFrom);
        Pos toCenter = widgetCenter(wTo);
        mouseX = fromCenter.x();
        mouseY = fromCenter.y();
        scene.mouseClicked(mouseX, mouseY, 0);
        double dx = toCenter.x() - fromCenter.x();
        double dy = toCenter.y() - fromCenter.y();
        scene.mouseDragged(toCenter.x(), toCenter.y(), 0, dx, dy);
        mouseX = toCenter.x();
        mouseY = toCenter.y();
        scene.mouseReleased(mouseX, mouseY, 0);
    }

    public void drag(Object fromKey, Object toKey) {
        drag(WidgetFinder.byKey(fromKey), WidgetFinder.byKey(toKey));
    }

    public void pressKey(int glfwKeyCode) {
        pressKey(glfwKeyCode, modifiers());
    }

    public void pressKey(int glfwKeyCode, int modifiers) {
        scene.keyPressed(glfwKeyCode, 0, modifiers);
        scene.keyReleased(glfwKeyCode, 0, modifiers);
    }

    public void typeText(WidgetFinder target, String text) {
        tap(target);
        for (char ch : text.toCharArray()) {
            scene.charTyped(ch, modifiers());
        }
    }

    public void typeText(Object key, String text) {
        typeText(WidgetFinder.byKey(key), text);
    }

    public void pressEnter() {
        pressKey(GLFW.GLFW_KEY_ENTER);
    }

    public void pressEscape() {
        pressKey(GLFW.GLFW_KEY_ESCAPE);
    }

    public void pressTab() {
        pressKey(GLFW.GLFW_KEY_TAB);
    }

    // ==================== InputSequence execution ====================

    public void perform(InputSequence sequence) {
        executeSequence(sequence);
    }

    public void perform(Consumer<InputSequence> builder) {
        var seq = InputSequence.begin();
        builder.accept(seq);
        executeSequence(seq);
    }

    void executeSequence(InputSequence sequence) {
        for (var step : sequence.steps()) {
            executeStep(step);
        }
        // Auto-release held modifiers at end of sequence
        ctrlDown = false;
        shiftDown = false;
        altDown = false;
    }

    private void executeStep(InputSequence.Step step) {
        switch (step) {
            case InputSequence.MoveToFinder(var finder) -> {
                Widget w = find(finder);
                Pos center = widgetCenter(w);
                mouseX = center.x();
                mouseY = center.y();
                scene.mouseMoved(mouseX, mouseY);
            }
            case InputSequence.MoveToCoord(double x, double y) -> {
                mouseX = x;
                mouseY = y;
                scene.mouseMoved(mouseX, mouseY);
            }
            case InputSequence.MouseDown(int button) -> {
                pressedButton = button;
                scene.mouseClicked(mouseX, mouseY, button);
            }
            case InputSequence.MouseUp(int button) -> {
                scene.mouseReleased(mouseX, mouseY, button);
                pressedButton = -1;
            }
            case InputSequence.DragToFinder(var finder) -> {
                Widget w = find(finder);
                Pos center = widgetCenter(w);
                double dx = center.x() - mouseX;
                double dy = center.y() - mouseY;
                int btn = pressedButton >= 0 ? pressedButton : 0;
                scene.mouseDragged(center.x(), center.y(), btn, dx, dy);
                mouseX = center.x();
                mouseY = center.y();
            }
            case InputSequence.DragToCoord(double x, double y) -> {
                double dx = x - mouseX;
                double dy = y - mouseY;
                int btn = pressedButton >= 0 ? pressedButton : 0;
                scene.mouseDragged(x, y, btn, dx, dy);
                mouseX = x;
                mouseY = y;
            }
            case InputSequence.DragByDelta(double deltaX, double deltaY) -> {
                int btn = pressedButton >= 0 ? pressedButton : 0;
                double newX = mouseX + deltaX;
                double newY = mouseY + deltaY;
                scene.mouseDragged(newX, newY, btn, deltaX, deltaY);
                mouseX = newX;
                mouseY = newY;
            }
            case InputSequence.ScrollStep(double scrollX, double scrollY) -> {
                scene.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            case InputSequence.KeyDown(int glfwKey) -> {
                scene.keyPressed(glfwKey, 0, modifiers());
            }
            case InputSequence.KeyUp(int glfwKey) -> {
                scene.keyReleased(glfwKey, 0, modifiers());
            }
            case InputSequence.TypeCharStep(char ch) -> {
                scene.charTyped(ch, modifiers());
            }
            case InputSequence.ModifierChange(var modifier, boolean down) -> {
                switch (modifier) {
                    case CTRL -> ctrlDown = down;
                    case SHIFT -> shiftDown = down;
                    case ALT -> altDown = down;
                }
            }
            case InputSequence.TickStep(int count) -> {
                tick(count);
            }
            case InputSequence.AssertStep(var assertion) -> {
                assertion.accept(this);
            }
            case InputSequence.CaptureStep ignored -> {
                // Capture steps are only meaningful in dump tests; silently skip in JUnit.
            }
        }
    }

    // ==================== Snapshot ====================

    public TreeSnapshot snapshot() {
        return TreeSnapshot.of(root);
    }

    public TreeDiffAssert assertDiff(TreeSnapshot before) {
        TreeSnapshot after = snapshot();
        return TreeDiffAssert.create(before, after);
    }

    public void assertTree(Consumer<TreeStructureAssert.NodeExpectation> expected) {
        TreeStructureAssert.assertTree(root, expected);
    }

    // ==================== Debug ====================

    /**
     * Returns a detailed tree dump including type, key, bounds, visibility, and lifecycle.
     * Useful for debugging test failures.
     */
    public String printTree() {
        StringBuilder sb = new StringBuilder();
        WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
            sb.append("  ".repeat(depth));
            sb.append(widget.getClass().getSimpleName());
            if (widget.key() != null) {
                sb.append("[").append(widget.key()).append("]");
            }
            sb.append(" (").append(widget.posX()).append(",").append(widget.posY());
            sb.append(" ").append(widget.width()).append("x").append(widget.height()).append(")");
            // State flags
            var flags = new ArrayList<String>();
            if (!widget.visible()) flags.add("hidden");
            if (!widget.active()) flags.add("inactive");
            if (!widget.interactive()) flags.add("non-interactive");
            if (widget.focused()) flags.add("focused");
            if (!flags.isEmpty()) sb.append(" ").append(flags);
            // Text content
            var text = WidgetInspector.textOf(widget);
            if (text != null) sb.append(" \"").append(text).append("\"");
            sb.append("\n");
            return TraversalControl.proceed;
        });
        return sb.toString();
    }

    /**
     * Lists all widget keys in the tree. Useful for quick diagnostics.
     */
    public List<Object> allKeys() {
        var keys = new ArrayList<>();
        WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
            if (widget.key() != null) keys.add(widget.key());
            return TraversalControl.proceed;
        });
        return keys;
    }

    // ==================== Nested Widget Builder ====================

    /**
     * Adds a WidgetGroup with children as a nested tree, tracking bounds for all widgets.
     *
     * <pre>{@code
     * scene.addGroup("panel", 0, 0, 400, 300, panel -> {
     *     scene.addInto(panel, ButtonWidget.of("OK"), "ok", 10, 10, 80, 30);
     *     scene.addInto(panel, ButtonWidget.of("Cancel"), "cancel", 100, 10, 80, 30);
     * });
     * }</pre>
     */
    public WidgetGroup<Widget> addGroup(Object key, int x, int y, int w, int h,
                                        Consumer<WidgetGroup<Widget>> builder) {
        var group = new WidgetGroup<Widget>();
        group.setKey(key);
        group.useStyle(UIStyle.of(
                positionAbsolute(),
                insetLeft(x), insetTop(y),
                sizeOf(w, h)
        ));
        builder.accept(group);
        root.addWidget(group);
        return group;
    }

    /**
     * Adds a widget as a child of a specific group, with tracked bounds.
     */
    public <T extends Widget> T addInto(WidgetGroup<? super T> parent, T widget,
                                        Object key, int x, int y, int w, int h) {
        widget.setKey(key);
        widget.useStyle(UIStyle.of(
                positionAbsolute(),
                insetLeft(x), insetTop(y),
                sizeOf(w, h)
        ));
        parent.addWidget(widget);
        return widget;
    }

    /**
     * Adds a widget as a child of a specific group, with a key only (no explicit bounds).
     */
    public <T extends Widget> T addInto(WidgetGroup<? super T> parent, T widget, Object key) {
        widget.setKey(key);
        parent.addWidget(widget);
        return widget;
    }

    // ==================== EventRecorder integration ====================

    /**
     * Attaches an EventRecorder to a widget. Shorthand for {@code EventRecorder.on(widget)}.
     * Call BEFORE {@code setup()}.
     */
    public EventRecorder record(Widget widget) {
        return EventRecorder.on(widget);
    }

    /**
     * Attaches a consuming EventRecorder to a widget.
     * Events recorded by this recorder will return {@code consumed}.
     */
    public EventRecorder recordConsuming(Widget widget) {
        return EventRecorder.consuming(widget);
    }

    // ==================== Helpers ====================

    private void tapWidget(Widget w) {
        Pos center = widgetCenter(w);
        mouseX = center.x();
        mouseY = center.y();
        scene.mouseClicked(mouseX, mouseY, 0);
        scene.mouseReleased(mouseX, mouseY, 0);
    }

    private Pos widgetCenter(Widget w) {
        Pos abs = w.absolutePos();
        return new Pos(abs.x() + w.width() / 2, abs.y() + w.height() / 2);
    }

    private int modifiers() {
        int mod = 0;
        if (ctrlDown) mod |= GLFW.GLFW_MOD_CONTROL;
        if (shiftDown) mod |= GLFW.GLFW_MOD_SHIFT;
        if (altDown) mod |= GLFW.GLFW_MOD_ALT;
        return mod;
    }
}
