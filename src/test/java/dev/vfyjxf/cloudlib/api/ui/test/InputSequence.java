package dev.vfyjxf.cloudlib.api.ui.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for complex user input sequences.
 * <p>
 * Supports mouse operations, keyboard input, modifier keys (Ctrl/Shift/Alt),
 * time advancement (ticks), and mid-sequence assertions.
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * InputSequence.begin()
 *     .moveTo("item-1")
 *     .mouseDown()
 *     .dragTo("trash-bin")
 *     .mouseUp()
 *     .executeOn(scene);
 *
 * InputSequence.begin()
 *     .moveTo("input").click()
 *     .holdCtrl()
 *     .keyPress(GLFW_KEY_A)
 *     .releaseCtrl()
 *     .typeText("hello")
 *     .executeOn(scene);
 * }</pre>
 */
public final class InputSequence {

    private final List<Step> steps = new ArrayList<>();

    private InputSequence() {}

    public static InputSequence begin() {
        return new InputSequence();
    }

    // ==================== Mouse movement ====================

    public InputSequence moveTo(WidgetFinder target) {
        steps.add(new MoveToFinder(target));
        return this;
    }

    public InputSequence moveTo(Object key) {
        return moveTo(WidgetFinder.byKey(key));
    }

    public InputSequence moveTo(double x, double y) {
        steps.add(new MoveToCoord(x, y));
        return this;
    }

    // ==================== Mouse buttons ====================

    public InputSequence mouseDown() {
        return mouseDown(0);
    }

    public InputSequence mouseDown(int button) {
        steps.add(new MouseDown(button));
        return this;
    }

    public InputSequence mouseUp() {
        return mouseUp(0);
    }

    public InputSequence mouseUp(int button) {
        steps.add(new MouseUp(button));
        return this;
    }

    public InputSequence click() {
        return click(0);
    }

    public InputSequence click(int button) {
        steps.add(new MouseDown(button));
        steps.add(new MouseUp(button));
        return this;
    }

    public InputSequence doubleClick() {
        return click().click();
    }

    public InputSequence rightClick() {
        return click(1);
    }

    // ==================== Drag ====================

    public InputSequence dragTo(WidgetFinder target) {
        steps.add(new DragToFinder(target));
        return this;
    }

    public InputSequence dragTo(Object key) {
        return dragTo(WidgetFinder.byKey(key));
    }

    public InputSequence dragTo(double x, double y) {
        steps.add(new DragToCoord(x, y));
        return this;
    }

    public InputSequence dragBy(double deltaX, double deltaY) {
        steps.add(new DragByDelta(deltaX, deltaY));
        return this;
    }

    // ==================== Scroll ====================

    public InputSequence scroll(double scrollX, double scrollY) {
        steps.add(new ScrollStep(scrollX, scrollY));
        return this;
    }

    // ==================== Keyboard ====================

    public InputSequence keyDown(int glfwKey) {
        steps.add(new KeyDown(glfwKey));
        return this;
    }

    public InputSequence keyUp(int glfwKey) {
        steps.add(new KeyUp(glfwKey));
        return this;
    }

    public InputSequence keyPress(int glfwKey) {
        steps.add(new KeyDown(glfwKey));
        steps.add(new KeyUp(glfwKey));
        return this;
    }

    public InputSequence typeChar(char ch) {
        steps.add(new TypeCharStep(ch));
        return this;
    }

    public InputSequence typeText(String text) {
        for (char ch : text.toCharArray()) {
            steps.add(new TypeCharStep(ch));
        }
        return this;
    }

    // ==================== Modifier keys ====================

    public InputSequence holdCtrl() {
        steps.add(new ModifierChange(Modifier.CTRL, true));
        return this;
    }

    public InputSequence releaseCtrl() {
        steps.add(new ModifierChange(Modifier.CTRL, false));
        return this;
    }

    public InputSequence holdShift() {
        steps.add(new ModifierChange(Modifier.SHIFT, true));
        return this;
    }

    public InputSequence releaseShift() {
        steps.add(new ModifierChange(Modifier.SHIFT, false));
        return this;
    }

    public InputSequence holdAlt() {
        steps.add(new ModifierChange(Modifier.ALT, true));
        return this;
    }

    public InputSequence releaseAlt() {
        steps.add(new ModifierChange(Modifier.ALT, false));
        return this;
    }

    // ==================== Combined shortcuts ====================

    public InputSequence ctrlClick(WidgetFinder target) {
        return holdCtrl().moveTo(target).click().releaseCtrl();
    }

    public InputSequence ctrlClick(Object key) {
        return ctrlClick(WidgetFinder.byKey(key));
    }

    public InputSequence shiftClick(WidgetFinder target) {
        return holdShift().moveTo(target).click().releaseShift();
    }

    public InputSequence shiftClick(Object key) {
        return shiftClick(WidgetFinder.byKey(key));
    }

    public InputSequence ctrlA() {
        return holdCtrl().keyPress(GLFW.GLFW_KEY_A).releaseCtrl();
    }

    public InputSequence ctrlC() {
        return holdCtrl().keyPress(GLFW.GLFW_KEY_C).releaseCtrl();
    }

    public InputSequence ctrlV() {
        return holdCtrl().keyPress(GLFW.GLFW_KEY_V).releaseCtrl();
    }

    public InputSequence ctrlZ() {
        return holdCtrl().keyPress(GLFW.GLFW_KEY_Z).releaseCtrl();
    }

    public InputSequence ctrlShift(int glfwKey) {
        return holdCtrl().holdShift().keyPress(glfwKey).releaseShift().releaseCtrl();
    }

    // ==================== Time ====================

    public InputSequence tick() {
        return tick(1);
    }

    public InputSequence tick(int count) {
        steps.add(new TickStep(count));
        return this;
    }

    // ==================== Mid-sequence assertions ====================

    public InputSequence then(Consumer<TestScene> assertion) {
        steps.add(new AssertStep(assertion));
        return this;
    }

    public InputSequence thenAssert(Object key, Consumer<WidgetAssert> assertion) {
        steps.add(new AssertStep(scene -> {
            Widget w = scene.find(key);
            assertion.accept(new WidgetAssert(w, "key=" + key));
        }));
        return this;
    }

    // ==================== Capture (for dump tests) ====================

    /**
     * Inserts a capture step into the sequence.
     * When executed via {@link dev.vfyjxf.cloudlib.api.ui.dump.DumpContext#interact},
     * this triggers a screenshot with the given suffix appended to the filename.
     * <p>
     * When executed via {@link TestScene}, capture steps are silently skipped.
     *
     * @param suffix filename suffix, e.g. "after_click" → "name_after_click.png"
     */
    public InputSequence thenCapture(String suffix) {
        steps.add(new CaptureStep(suffix));
        return this;
    }

    // ==================== Execution ====================

    public void executeOn(TestScene scene) {
        scene.executeSequence(this);
    }

    List<Step> steps() {
        return steps;
    }

    // ==================== Step types ====================

    sealed interface Step {}

    enum Modifier { CTRL, SHIFT, ALT }

    record MoveToFinder(WidgetFinder finder) implements Step {}
    record MoveToCoord(double x, double y) implements Step {}
    record MouseDown(int button) implements Step {}
    record MouseUp(int button) implements Step {}
    record DragToFinder(WidgetFinder finder) implements Step {}
    record DragToCoord(double x, double y) implements Step {}
    record DragByDelta(double deltaX, double deltaY) implements Step {}
    record ScrollStep(double scrollX, double scrollY) implements Step {}
    record KeyDown(int glfwKey) implements Step {}
    record KeyUp(int glfwKey) implements Step {}
    record TypeCharStep(char ch) implements Step {}
    record ModifierChange(Modifier modifier, boolean down) implements Step {}
    record TickStep(int count) implements Step {}
    record AssertStep(Consumer<TestScene> assertion) implements Step {}
    record CaptureStep(String suffix) implements Step {}
}
