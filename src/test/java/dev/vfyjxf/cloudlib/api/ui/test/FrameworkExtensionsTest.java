package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.FocusNode;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.ui.widget.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the framework extensions:
 * - EventRecorder
 * - Hit test debugging
 * - Nested widget builder (addGroup/addInto)
 * - Enhanced assertion messages
 * - WidgetAssert state/lifecycle checks
 */
class FrameworkExtensionsTest {

    // ==================== EventRecorder ====================

    @Nested
    class EventRecorderBasicTest {

        @Test
        void recordsSingleClick() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Test");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");

            rec.assertFired("click");
            rec.assertFired("mouseClicked");
            rec.assertFired("mouseReleased");
        }

        @Test
        void countsMultipleClicks() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Multi");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");
            scene.tap("btn");
            scene.tap("btn");

            rec.assertFiredTimes("click", 3);
            rec.assertFiredTimes("mouseClicked", 3);
        }

        @Test
        void noEventsOnUntouchedWidget() {
            var scene = TestScene.create(800, 600);
            var btn1 = ButtonWidget.of("A");
            var btn2 = ButtonWidget.of("B");
            var rec1 = scene.record(btn1);
            var rec2 = scene.record(btn2);
            scene.add(btn1, "a", 10, 10, 80, 30);
            scene.add(btn2, "b", 100, 10, 80, 30);
            scene.setup();

            scene.tap("a");

            rec1.assertFired("click");
            rec2.assertNothingFired();
        }

        @Test
        void clearResetsRecorder() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Clear");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");
            rec.assertFired("click");

            rec.clear();
            rec.assertNothingFired();

            scene.tap("btn");
            rec.assertFiredTimes("click", 1);
        }

        @Test
        void recordsEventOrder() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Order");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");

            // mouseClicked, mouseReleased, then click (logical)
            var names = rec.eventNames();
            assertTrue(names.indexOf("mouseClicked") < names.indexOf("click"),
                    "mouseClicked should fire before click: " + names);
        }

        @Test
        void consumingRecorderStopsBubbling() {
            var scene = TestScene.create(800, 600);
            var parentReached = new AtomicBoolean(false);

            var child = new WidgetGroup<>();
            var rec = EventRecorder.consuming(child);

            var parent = new WidgetGroup<>();
            parent.onMouseClick((input, clickCount, context) -> {
                parentReached.set(true);
                return EventDispatch.pass;
            });

            scene.addGroup("parent", 0, 0, 400, 200, p -> {
                // manually wire into the parent group we set up
            });

            // Re-approach: use addInto to build the nested tree properly
            var scene2 = TestScene.create(800, 600);
            var parent2 = new WidgetGroup<>();
            parent2.onMouseClick((input, clickCount, context) -> {
                parentReached.set(true);
                return EventDispatch.pass;
            });
            var child2 = new WidgetGroup<>();
            var rec2 = EventRecorder.consuming(child2);
            scene2.addInto(parent2, child2, "child", 10, 10, 200, 100);
            scene2.add(parent2, "parent", 0, 0, 400, 200);
            scene2.setup();

            scene2.tap("child");

            rec2.assertFired("click");
            assertFalse(parentReached.get(), "Consuming recorder should stop event propagation");
        }

        @Test
        void summaryIsReadable() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Summary");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");

            var summary = rec.summary();
            assertFalse(summary.contains("no events"), "Summary should show events");
            assertTrue(summary.contains("click"), "Summary should mention click");
        }

        @Test
        void assertFiredWithMatchesData() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Data");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");

            rec.assertFiredWith("click", entry ->
                    entry.data().containsKey("clickCount")
                            && (int) entry.data().get("clickCount") == 1);
        }
    }

    @Nested
    class EventRecorderKeyboardTest {

        @Test
        void recordsKeyEvents() {
            var scene = TestScene.create(800, 600);
            var widget = new WidgetGroup<>();
            widget.setFocusNode(new FocusNode());
            var rec = scene.record(widget);
            scene.add(widget, "input", 10, 10, 200, 20);
            scene.setup();

            scene.tap("input");
            scene.pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_A);

            rec.assertFired("keyPressed");
            rec.assertFired("keyReleased");
        }

        @Test
        void recordsCharTyped() {
            var scene = TestScene.create(800, 600);
            var widget = new WidgetGroup<>();
            widget.setFocusNode(new FocusNode());
            var rec = scene.record(widget);
            scene.add(widget, "input", 10, 10, 200, 20);
            scene.setup();

            scene.tap("input");
            scene.typeText("input", "Hi");

            rec.assertFired("charTyped");
            rec.assertFiredTimes("charTyped", 2);
        }
    }

    @Nested
    class EventRecorderScrollTest {

        @Test
        void recordsScrollEvent() {
            var scene = TestScene.create(800, 600);
            var area = new WidgetGroup<>();
            var rec = scene.record(area);
            scene.add(area, "area", 0, 0, 400, 300);
            scene.setup();

            scene.scroll("area", 0, 3.0);

            rec.assertFired("scroll");
            rec.assertFiredWith("scroll", e ->
                    (double) e.data().get("scrollY") == 3.0);
        }
    }

    // ==================== Hit Test Debugging ====================

    @Nested
    class HitTestDebugTest {

        @Test
        void hitTestReturnsCorrectWidget() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 10, 10, 100, 30);
            scene.add(ButtonWidget.of("B"), "b", 200, 10, 100, 30);
            scene.setup();

            var hit = scene.hitTestAt(60, 25);
            assertNotNull(hit);
            assertEquals("a", hit.key());
        }

        @Test
        void assertHitTargetPasses() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Target"), "target", 50, 50, 100, 40);
            scene.setup();

            scene.assertHitTarget(100, 70, "target");
        }

        @Test
        void assertNoHitAtEmptyRegion() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Small"), "s", 10, 10, 20, 20);
            scene.setup();

            // (500, 500) is far from the small widget
            scene.assertNoHitAt(500, 500);
        }

        @Test
        void hitTestLayeredWidgets() {
            var scene = TestScene.create(800, 600);
            // Later-added widget has higher z-order
            scene.add(ButtonWidget.of("Bottom"), "bottom", 50, 50, 100, 40);
            scene.add(ButtonWidget.of("Top"), "top", 80, 50, 100, 40);
            scene.setup();

            // At x=130, both overlap — "top" should win (higher z-order)
            scene.assertHitTarget(130, 70, "top");
            // At x=60, only "bottom" is there
            scene.assertHitTarget(60, 70, "bottom");
        }
    }

    // ==================== Nested Widget Builder ====================

    @Nested
    class NestedBuilderTest {

        @Test
        void addGroupCreatesNestedTree() {
            var scene = TestScene.create(800, 600);
            scene.addGroup("panel", 10, 10, 300, 200, panel -> {
                scene.addInto(panel, ButtonWidget.of("OK"), "ok", 10, 160, 80, 30);
                scene.addInto(panel, ButtonWidget.of("Cancel"), "cancel", 100, 160, 80, 30);
            });
            scene.setup();

            scene.assertExists("panel");
            scene.assertExists("ok");
            scene.assertExists("cancel");
            scene.assertThat("panel").hasChildCount(2);
        }

        @Test
        void addGroupBoundsPreservedAfterLayout() {
            var scene = TestScene.create(800, 600);
            scene.addGroup("container", 20, 20, 400, 300, container -> {
                scene.addInto(container, ButtonWidget.of("Inner"), "inner", 5, 5, 60, 25);
            });
            scene.setup();

            scene.assertThat("container").hasPos(20, 20).hasSize(400, 300);
            scene.assertThat("inner").hasPos(5, 5).hasSize(60, 25);
        }

        @Test
        void addGroupClickable() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);
            scene.addGroup("panel", 10, 10, 300, 200, panel -> {
                var btn = ButtonWidget.of("Click Me");
                btn.onClick(() -> clicked.set(true));
                scene.addInto(panel, btn, "btn", 10, 10, 80, 30);
            });
            scene.setup();

            scene.tap("btn");
            assertTrue(clicked.get());
        }

        @Test
        void deeplyNestedGroup() {
            var scene = TestScene.create(800, 600);
            scene.addGroup("outer", 0, 0, 400, 300, outer -> {
                var inner = new WidgetGroup<Widget>();
                scene.addInto(outer, inner, "inner", 10, 10, 300, 200);
                scene.addInto(inner, ButtonWidget.of("Deep"), "deep", 5, 5, 60, 20);
            });
            scene.setup();

            scene.assertExists("deep");
            scene.assertThat("deep").hasPos(5, 5).hasSize(60, 20);
        }
    }

    // ==================== Enhanced Assertion Messages ====================

    @Nested
    class AssertionMessageTest {

        @Test
        void findFailureShowsTree() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Exists"), "exists", 10, 10, 80, 30);
            scene.setup();

            var error = assertThrows(AssertionError.class, () -> scene.find("nonexistent"));
            assertTrue(error.getMessage().contains("Tree:"),
                    "Error message should include tree dump: " + error.getMessage());
            assertTrue(error.getMessage().contains("exists"),
                    "Error message should show existing widget keys: " + error.getMessage());
        }

        @Test
        void assertExistsFailureShowsTree() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
            scene.setup();

            var error = assertThrows(AssertionError.class, () -> scene.assertExists("c"));
            assertTrue(error.getMessage().contains("Tree:"));
        }

        @Test
        void allKeysReturnsExistingKeys() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "alpha", 10, 10, 80, 30);
            scene.add(ButtonWidget.of("B"), "beta", 100, 10, 80, 30);
            scene.setup();

            var keys = scene.allKeys();
            assertTrue(keys.contains("alpha"));
            assertTrue(keys.contains("beta"));
            assertEquals(2, keys.size());
        }
    }

    // ==================== WidgetAssert State Checks ====================

    @Nested
    class WidgetAssertStateTest {

        @Test
        void activeAndInteractive() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Active");
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.assertThat("btn")
                    .isActive()
                    .isInteractive()
                    .isVisible();
        }

        @Test
        void inactiveWidget() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Hidden");
            btn.setActive(false);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.assertThat("btn").isNotActive();
        }

        @Test
        void nonInteractiveWidget() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("NoInteract");
            btn.setInteractive(false);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.assertThat("btn").isNotInteractive();
        }

        @Test
        void mountedLifecycle() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Mounted");
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.assertThat("btn").isMounted();
        }

        @Test
        void focusableCheck() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("F");
            btn.setFocusNode(new FocusNode());
            scene.add(btn, "f", 10, 10, 100, 30);

            var btn2 = ButtonWidget.of("NF");
            scene.add(btn2, "nf", 120, 10, 100, 30);

            scene.setup();

            scene.assertThat("f").isFocusable();
            scene.assertThat("nf").isNotFocusable();
        }

        @Test
        void containsPointCheck() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Box"), "box", 50, 50, 100, 40);
            scene.setup();

            scene.assertThat("box").containsPoint(60, 60);
            scene.assertThat("box").containsPoint(100, 70);
        }
    }

    // ==================== printTree / allKeys ====================

    @Nested
    class DebugOutputTest {

        @Test
        void printTreeShowsStateFlags() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Hidden");
            btn.setVisible(false);
            scene.add(btn, "hidden", 10, 10, 80, 30);
            scene.add(ButtonWidget.of("Visible"), "vis", 100, 10, 80, 30);
            scene.setup();

            var tree = scene.printTree();
            assertTrue(tree.contains("hidden"), "Tree should show 'hidden' flag");
            assertTrue(tree.contains("Visible"), "Tree should show text content");
        }

        @Test
        void printTreeShowsFocusState() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Focus");
            btn.setFocusNode(new FocusNode());
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");

            var tree = scene.printTree();
            assertTrue(tree.contains("focused"), "Tree should show focused flag");
        }
    }

    // ==================== Combined: Using EventRecorder with addGroup ====================

    @Nested
    class CombinedTest {

        @Test
        void eventRecorderWithNestedBuilder() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Nested");
            var rec = scene.record(btn);
            scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                scene.addInto(panel, btn, "btn", 10, 10, 80, 30);
            });
            scene.setup();

            scene.tap("btn");
            rec.assertFired("click");
            rec.assertFiredTimes("click", 1);
        }

        @Test
        void hitTestWithNestedBuilder() {
            var scene = TestScene.create(800, 600);
            scene.addGroup("panel", 10, 10, 300, 200, panel -> {
                scene.addInto(panel, ButtonWidget.of("A"), "a", 5, 5, 60, 20);
                scene.addInto(panel, ButtonWidget.of("B"), "b", 70, 5, 60, 20);
            });
            scene.setup();

            // Widget "a" is at absolute (10+5, 10+5) = (15, 15), size 60x20
            scene.assertHitTarget(45, 25, "a");
            // Widget "b" is at absolute (10+70, 10+5) = (80, 15), size 60x20
            scene.assertHitTarget(110, 25, "b");
        }

        @Test
        void fullInteractionScenario() {
            // Simulate a dialog with OK/Cancel, verify click routing and event recording
            var scene = TestScene.create(800, 600);
            var okBtn = ButtonWidget.of("OK");
            var cancelBtn = ButtonWidget.of("Cancel");
            var okRec = scene.record(okBtn);
            var cancelRec = scene.record(cancelBtn);

            scene.addGroup("dialog", 100, 100, 300, 200, dialog -> {
                scene.addInto(dialog, LabelWidget.of("Are you sure?"), "msg", 10, 10, 280, 20);
                scene.addInto(dialog, okBtn, "ok", 50, 150, 80, 30);
                scene.addInto(dialog, cancelBtn, "cancel", 170, 150, 80, 30);
            });
            scene.setup();

            // Verify layout
            scene.assertThat("dialog").hasPos(100, 100).hasSize(300, 200);
            scene.assertThat("msg").hasText("Are you sure?");

            // Click OK
            scene.tap("ok");
            okRec.assertFired("click");
            cancelRec.assertNothingFired();

            // Click Cancel
            okRec.clear();
            scene.tap("cancel");
            cancelRec.assertFired("click");
            okRec.assertNothingFired();
        }

        @Test
        void inputSequenceWithEventRecorder() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("SeqTest");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 50, 50, 100, 40);
            scene.setup();

            scene.perform(seq -> seq
                    .moveTo(WidgetFinder.byKey("btn"))
                    .mouseDown(0)
                    .mouseUp(0)
            );

            rec.assertFired("mouseClicked");
            rec.assertFired("mouseReleased");
        }

        @Test
        void dynamicAddWithRecorder() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Initial"), "init", 10, 10, 80, 30);
            scene.setup();

            // Add widget dynamically
            var newBtn = ButtonWidget.of("Dynamic");
            var rec = scene.record(newBtn);
            scene.add(newBtn, "dyn", 100, 10, 80, 30);
            scene.rebuild();

            scene.tap("dyn");
            rec.assertFired("click");
        }
    }

    // ==================== EventRecorder new query methods ====================

    @Nested
    class EventRecorderQueryTest {

        @Test
        void lastEntryReturnsLatest() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Q");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");
            scene.tap("btn");

            var last = rec.lastEntry();
            assertNotNull(last);
            // Last event in a tap is "click"
            assertEquals("click", last.name());
        }

        @Test
        void firstEntryReturnsEarliest() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Q");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");

            var first = rec.firstEntry();
            assertNotNull(first);
            // First event in a tap is "mouseClicked"
            assertEquals("mouseClicked", first.name());
        }

        @Test
        void lastEntryOfFilters() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Q");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");
            scene.tap("btn");

            var lastClick = rec.lastEntryOf("click");
            assertNotNull(lastClick);
            assertEquals("click", lastClick.name());
            assertEquals(2, (int) lastClick.data().get("clickCount"));
        }

        @Test
        void assertOnlyFiredChecksExactSet() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Only");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");

            rec.assertOnlyFired("mouseClicked", "mouseReleased", "click");
        }

        @Test
        void assertOnlyFiredFailsOnExtra() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Only");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            scene.tap("btn");

            assertThrows(AssertionError.class, () ->
                    rec.assertOnlyFired("click"));
        }

        @Test
        void lastEntryNullWhenEmpty() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Empty");
            var rec = scene.record(btn);
            scene.add(btn, "btn", 10, 10, 100, 30);
            scene.setup();

            assertNull(rec.lastEntry());
            assertNull(rec.firstEntry());
            assertNull(rec.lastEntryOf("click"));
        }
    }

    // ==================== Relative Position Assertions ====================

    @Nested
    class RelativePositionTest {

        @Test
        void isLeftOfPasses() {
            var scene = TestScene.create(800, 600);
            var a = ButtonWidget.of("A");
            var b = ButtonWidget.of("B");
            scene.add(a, "a", 10, 10, 80, 30);
            scene.add(b, "b", 100, 10, 80, 30);
            scene.setup();

            scene.assertThat("a").isLeftOf(scene.find("b"));
        }

        @Test
        void isLeftOfFails() {
            var scene = TestScene.create(800, 600);
            var a = ButtonWidget.of("A");
            var b = ButtonWidget.of("B");
            scene.add(a, "a", 10, 10, 80, 30);
            scene.add(b, "b", 50, 10, 80, 30); // overlapping
            scene.setup();

            assertThrows(AssertionError.class, () ->
                    scene.assertThat("a").isLeftOf(scene.find("b")));
        }

        @Test
        void isAbovePasses() {
            var scene = TestScene.create(800, 600);
            var top = ButtonWidget.of("Top");
            var bottom = ButtonWidget.of("Bottom");
            scene.add(top, "top", 10, 10, 80, 30);
            scene.add(bottom, "bottom", 10, 50, 80, 30);
            scene.setup();

            scene.assertThat("top").isAbove(scene.find("bottom"));
        }

        @Test
        void isWithinPasses() {
            var scene = TestScene.create(800, 600);
            var inner = ButtonWidget.of("Inner");
            scene.addGroup("outer", 10, 10, 300, 200, outer -> {
                scene.addInto(outer, inner, "inner", 5, 5, 50, 20);
            });
            scene.setup();

            scene.assertThat("inner").isWithin(scene.find("outer"));
        }

        @Test
        void isWithinFails() {
            var scene = TestScene.create(800, 600);
            var inner = ButtonWidget.of("Inner");
            scene.addGroup("outer", 10, 10, 100, 50, outer -> {
                scene.addInto(outer, inner, "inner", 80, 40, 50, 20); // extends beyond
            });
            scene.setup();

            assertThrows(AssertionError.class, () ->
                    scene.assertThat("inner").isWithin(scene.find("outer")));
        }

        @Test
        void hasParentKeyPasses() {
            var scene = TestScene.create(800, 600);
            scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                scene.addInto(panel, ButtonWidget.of("Child"), "child", 10, 10, 80, 30);
            });
            scene.setup();

            scene.assertThat("child").hasParentKey("panel");
        }

        @Test
        void hasBoundsWithinPasses() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Box"), "box", 50, 50, 100, 40);
            scene.setup();

            scene.assertThat("box").hasBoundsWithin(0, 0, 800, 600);
            scene.assertThat("box").hasBoundsWithin(50, 50, 100, 40);
        }

        @Test
        void hasBoundsWithinFails() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Box"), "box", 50, 50, 100, 40);
            scene.setup();

            assertThrows(AssertionError.class, () ->
                    scene.assertThat("box").hasBoundsWithin(60, 60, 50, 20));
        }
    }

    // ==================== tapAt with offset ====================

    @Nested
    class TapAtOffsetTest {

        @Test
        void tapAtLeftEdge() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Wide");
            var rec = scene.record(btn);
            scene.add(btn, "wide", 100, 100, 200, 40);
            scene.setup();

            scene.tapAt("wide", 5, 20); // near left edge
            rec.assertFired("click");
            rec.assertFiredWith("mouseClicked", e ->
                    (double) e.data().get("mouseX") == 105.0);
        }

        @Test
        void tapAtRightEdge() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Wide");
            var rec = scene.record(btn);
            scene.add(btn, "wide", 100, 100, 200, 40);
            scene.setup();

            rec.clear();
            scene.tapAt("wide", 195, 20); // near right edge
            rec.assertFired("click");
            rec.assertFiredWith("mouseClicked", e ->
                    (double) e.data().get("mouseX") == 295.0);
        }
    }

    // ==================== assertThat from Widget reference ====================

    @Nested
    class AssertThatWidgetRefTest {

        @Test
        void assertThatWithDirectReference() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Direct");
            scene.add(btn, "direct", 10, 10, 80, 30);
            scene.setup();

            // Using widget reference instead of key
            scene.assertThat(btn)
                    .isVisible()
                    .hasPos(10, 10)
                    .hasSize(80, 30)
                    .hasLabel("Direct");
        }

        @Test
        void assertThatWidgetRefMounted() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Lifecycle");
            scene.add(btn, "lc", 10, 10, 80, 30);
            scene.setup();

            scene.assertThat(btn).isMounted().isActive().isInteractive();
        }
    }
}
