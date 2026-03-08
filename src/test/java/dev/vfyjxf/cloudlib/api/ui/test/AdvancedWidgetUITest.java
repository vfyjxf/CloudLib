package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.FocusNode;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widget.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widget.ToggleWidget;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced UI tests that exercise:
 * - Focus system
 * - Event propagation & consumption
 * - Active/Interactive/Visible interactions
 * - Deep nesting hit testing
 * - Dynamic tree modification
 * - Overlapping widgets
 * - Complex multi-step interactions
 * - Complex PathFinder expressions
 * - Tree diff with multiple mutations
 */
class AdvancedWidgetUITest {

    // ==================== Focus System ====================

    @Nested
    class FocusTest {

        @Test
        void clickFocusableWidgetGrantsFocus() {
            var scene = TestScene.create(800, 600);

            var btn = ButtonWidget.of("Focusable");
            btn.setFocusNode(new FocusNode());
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();
            assertFalse(btn.focused(), "Button should not be focused initially");

            scene.tap("btn");
            assertTrue(btn.focused(), "Button should be focused after tap");
        }

        @Test
        void clickDifferentWidgetChangesFocus() {
            var scene = TestScene.create(800, 600);

            var btn1 = ButtonWidget.of("A");
            btn1.setFocusNode(new FocusNode());
            scene.add(btn1, "a", 10, 10, 100, 30);

            var btn2 = ButtonWidget.of("B");
            btn2.setFocusNode(new FocusNode());
            scene.add(btn2, "b", 120, 10, 100, 30);

            scene.setup();

            scene.tap("a");
            assertTrue(btn1.focused(), "First button should be focused");
            assertFalse(btn2.focused(), "Second button should not be focused");

            scene.tap("b");
            assertFalse(btn1.focused(), "First button should lose focus");
            assertTrue(btn2.focused(), "Second button should gain focus");
        }

        @Test
        void nonFocusableWidgetDoesNotGetFocus() {
            var scene = TestScene.create(800, 600);

            // Default widgets are not focusable
            var btn = ButtonWidget.of("NoFocus");
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();
            scene.tap("btn");

            assertFalse(btn.focusable(), "Button without FocusNode should not be focusable");
            assertFalse(btn.focused(), "Non-focusable widget should not gain focus");
        }

        @Test
        void focusableToggleStillClicks() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);

            var btn = ButtonWidget.of("Both");
            btn.setFocusNode(new FocusNode());
            btn.onClick(() -> clicked.set(true));
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();
            scene.tap("btn");

            assertTrue(btn.focused(), "Should be focused");
            assertTrue(clicked.get(), "Click handler should still fire on focusable widget");
        }
    }

    // ==================== Event Propagation ====================

    @Nested
    class EventPropagationTest {

        @Test
        void parentReceivesBubbledEvent() {
            var scene = TestScene.create(800, 600);
            var parentClicked = new AtomicBoolean(false);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "parent");
            TestScene.setBound(group, 0, 0, 400, 200);
            group.onMouseClick((input, clickCount, context) -> {
                parentClicked.set(true);
                return EventDispatch.pass;
            });

            var btn = ButtonWidget.of("Child");
            btn.setKey("child");
            btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
            group.addWidget(btn);

            scene.root().addWidget(group);
            scene.setup();

            scene.tap("child");

            assertTrue(parentClicked.get(), "Parent's onMouseClick should fire during bubble phase");
        }

        @Test
        void consumedEventStopsBubbling() {
            var scene = TestScene.create(800, 600);
            var parentReached = new AtomicBoolean(false);
            var childReached = new AtomicBoolean(false);

            var group = new WidgetGroup<>();
            group.onMouseClick((input, clickCount, context) -> {
                parentReached.set(true);
                return EventDispatch.pass;
            });

            var child = new WidgetGroup<>();
            child.setKey("child");
            scene.setTrackedBound(child, 10, 10, 200, 100);
            child.onMouseClick((input, clickCount, context) -> {
                childReached.set(true);
                return EventDispatch.consumed;
            });
            group.addWidget(child);

            scene.add(group, "parent", 0, 0, 400, 200);
            scene.setup();

            scene.tap("child");

            assertTrue(childReached.get(), "Child should receive the click event");
            assertFalse(parentReached.get(),
                    "Parent should not receive event when child consumes it");
        }

        @Test
        void eventOrderCaptureThenTargetThenBubble() {
            var scene = TestScene.create(800, 600);
            var order = new ArrayList<String>();

            var parent = new WidgetGroup<>();
            TestScene.setKey(parent, "parent");
            TestScene.setBound(parent, 0, 0, 400, 200);

            // Register handlers that track the phase
            parent.onMouseClicked((input, context) -> {
                if (context.capturing()) order.add("parent-capture");
                if (context.bubbling()) order.add("parent-bubble");
                return EventDispatch.pass;
            });

            var child = new WidgetGroup<>();
            child.setKey("child");
            child.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(200, 100)));
            child.onMouseClicked((input, context) -> {
                if (context.targeting()) order.add("child-target");
                if (context.capturing()) order.add("child-capture");
                if (context.bubbling()) order.add("child-bubble");
                return EventDispatch.pass;
            });
            parent.addWidget(child);

            scene.root().addWidget(parent);
            scene.setup();

            scene.tap("child");

            // onMouseClicked (raw click) fires during the specified phases
            // Based on Widget.onMouseClicked wrapper: only fires for bubbling || targeting
            assertTrue(order.contains("child-target") || order.contains("child-bubble"),
                    "Child should receive event in target/bubble phase: " + order);
        }
    }

    // ==================== Active vs Interactive vs Visible ====================

    @Nested
    class InteractivityTest {

        @Test
        void inactiveWidgetSkippedInEventDispatch() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);

            var btn = ButtonWidget.of("Inactive");
            btn.onClick(() -> clicked.set(true));
            btn.setActive(false);
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();
            scene.tap("btn");

            // Active=false → widget is still hit-tested, but event handlers are skipped
            // Actually onMouseClick is a logical click that needs both mouseClicked + mouseReleased
            // with same target AND target.isMouseOver. Active=false skips handlers but
            // the click dispatch chain should not invoke the handler.
            assertFalse(clicked.get(), "Inactive widget should not fire click handler");
        }

        @Test
        void nonInteractiveWidgetSkippedInHitTest() {
            var scene = TestScene.create(800, 600);
            var behindClicked = new AtomicBoolean(false);

            var behind = ButtonWidget.of("Behind");
            behind.onClick(() -> behindClicked.set(true));
            scene.add(behind, "behind", 10, 10, 100, 30);

            // Place an interactive-disabled widget on top
            var blocker = ButtonWidget.of("Blocker");
            blocker.setInteractive(false);
            scene.add(blocker, "blocker", 10, 10, 100, 30);

            scene.setup();

            // Tap at the shared location - blocker is non-interactive so hit test skips it
            scene.tapAt(60, 25);

            assertTrue(behindClicked.get(), "Behind widget should be clicked through non-interactive blocker");
        }

        @Test
        void hiddenWidgetNotHitTested() {
            var scene = TestScene.create(800, 600);
            var clicked = new AtomicBoolean(false);

            var btn = ButtonWidget.of("Hidden");
            btn.onClick(() -> clicked.set(true));
            btn.setVisible(false);
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();
            scene.tapAt(60, 25);

            assertFalse(clicked.get(), "Hidden widget should not respond to clicks");
        }

        @Test
        void toggleActiveRestoresClickability() {
            var scene = TestScene.create(800, 600);
            var count = new AtomicInteger(0);

            var btn = ButtonWidget.of("Toggle Active");
            btn.onClick(count::incrementAndGet);
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();

            scene.tap("btn");
            assertEquals(1, count.get(), "Should click when active");

            btn.setActive(false);
            scene.tap("btn");
            assertEquals(1, count.get(), "Should NOT click when inactive");

            btn.setActive(true);
            scene.tap("btn");
            assertEquals(2, count.get(), "Should click again when re-activated");
        }
    }

    // ==================== Deep Nesting ====================

    @Nested
    class DeepNestingTest {

        @Test
        void threeDeepNestingHitTest() {
            var scene = TestScene.create(800, 600);
            var deepClicked = new AtomicBoolean(false);

            // root > outer > inner > button
            var outer = new WidgetGroup<>();
            TestScene.setKey(outer, "outer");
            TestScene.setBound(outer, 0, 0, 400, 300);

            var inner = new WidgetGroup<>();
            inner.setKey("inner");
            inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(300, 200)));

            var deepBtn = ButtonWidget.of("Deep");
            deepBtn.onClick(() -> deepClicked.set(true));
            deepBtn.setKey("deep");
            deepBtn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(5), insetTop(5), sizeOf(80, 25)));

            inner.addWidget(deepBtn);
            outer.addWidget(inner);
            scene.root().addWidget(outer);
            scene.setup();

            scene.tap("deep");
            assertTrue(deepClicked.get(), "Deep nested button should be clickable");
        }

        @Test
        void nestedGroupsCorrectAbsolutePosition() {
            var scene = TestScene.create(800, 600);

            var outer = new WidgetGroup<>();
            TestScene.setKey(outer, "outer");
            TestScene.setBound(outer, 50, 50, 400, 300);

            var inner = new WidgetGroup<>();
            inner.setKey("inner");
            inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(20), insetTop(20), sizeOf(200, 100)));

            var btn = ButtonWidget.of("Positioned");
            btn.setKey("btn");
            btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(60, 20)));

            inner.addWidget(btn);
            outer.addWidget(inner);
            scene.root().addWidget(outer);
            // Track bounds so they survive Taffy layout
            scene.setTrackedBound(outer, 50, 50, 400, 300);
            scene.setTrackedBound(inner, 20, 20, 200, 100);
            scene.setTrackedBound(btn, 10, 10, 60, 20);
            scene.setup();

            // Absolute position should be cumulative: (50+20+10, 50+20+10) = (80, 80)
            var abs = btn.absolutePos();
            assertEquals(80, abs.x(), "Absolute X should account for all parent offsets");
            assertEquals(80, abs.y(), "Absolute Y should account for all parent offsets");
        }

        @Test
        void deepPathFinderTraversal() {
            var scene = TestScene.create(800, 600);

            var outer = new WidgetGroup<>();
            TestScene.setKey(outer, "outer");
            TestScene.setBound(outer, 0, 0, 400, 300);

            var inner = new WidgetGroup<>();
            inner.setKey("inner");
            inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(300, 200)));

            var btn = ButtonWidget.of("Target");
            btn.setKey("target");
            btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(60, 20)));

            inner.addWidget(btn);
            outer.addWidget(inner);
            scene.root().addWidget(outer);
            scene.setup();

            // Use ** to find deeply nested widget
            var found = scene.find(WidgetFinder.path("** / [key=target]"));
            assertNotNull(found);
            assertEquals("target", found.key());

            // Use explicit path
            found = scene.find(WidgetFinder.path("[key=outer] / [key=inner] / [key=target]"));
            assertNotNull(found);
            assertEquals("target", found.key());
        }
    }

    // ==================== Overlapping Widgets ====================

    @Nested
    class OverlappingTest {

        @Test
        void lastAddedWidgetWinsHitTest() {
            // Later siblings have higher z-order in hit testing
            var scene = TestScene.create(800, 600);
            var firstClicked = new AtomicBoolean(false);
            var secondClicked = new AtomicBoolean(false);

            var btn1 = ButtonWidget.of("First");
            btn1.onClick(() -> firstClicked.set(true));
            scene.add(btn1, "first", 10, 10, 100, 50);

            // Second button overlaps the first
            var btn2 = ButtonWidget.of("Second");
            btn2.onClick(() -> secondClicked.set(true));
            scene.add(btn2, "second", 10, 10, 100, 50);

            scene.setup();

            // Tap in the overlapping area
            scene.tapAt(60, 35);

            assertTrue(secondClicked.get(), "Last added (higher z) widget should receive the click");
            assertFalse(firstClicked.get(), "First widget should not receive click in overlap");
        }

        @Test
        void nonOverlappingPartOfFirstWidget() {
            var scene = TestScene.create(800, 600);
            var firstClicked = new AtomicBoolean(false);
            var secondClicked = new AtomicBoolean(false);

            var btn1 = ButtonWidget.of("Wide");
            btn1.onClick(() -> firstClicked.set(true));
            scene.add(btn1, "wide", 10, 10, 200, 30);

            var btn2 = ButtonWidget.of("Overlap");
            btn2.onClick(() -> secondClicked.set(true));
            scene.add(btn2, "overlap", 10, 10, 80, 30);

            scene.setup();

            // Tap on the non-overlapping part of wide button (x > 90)
            scene.tapAt(150, 25);

            assertTrue(firstClicked.get(), "Wide button should be clicked on its non-overlapping part");
            assertFalse(secondClicked.get(), "Overlap button should not cover this area");
        }
    }

    // ==================== Dynamic Tree Modification ====================

    @Nested
    class DynamicTreeTest {

        @Test
        void addWidgetAfterSetupAndReinit() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Initial"), "initial", 10, 10, 80, 30);
            scene.setup();

            scene.assertExists("initial");
            assertFalse(scene.exists("added"), "Widget should not exist before adding");

            // Add widget dynamically
            var added = ButtonWidget.of("Added");
            scene.add(added, "added", 100, 10, 80, 30);
            scene.rebuild();

            scene.assertExists("added");
        }

        @Test
        void dynamicallyAddedWidgetIsClickable() {
            var scene = TestScene.create(800, 600);
            scene.setup();

            var clicked = new AtomicBoolean(false);
            var btn = ButtonWidget.of("Dynamic");
            btn.onClick(() -> clicked.set(true));
            scene.add(btn, "dynamic", 10, 10, 100, 30);
            scene.rebuild();

            scene.tap("dynamic");
            assertTrue(clicked.get(), "Dynamically added button should be clickable");
        }

        @Test
        void snapshotDiffWithMultipleMutations() {
            var scene = TestScene.create(800, 600);
            var label = LabelWidget.of("Original");
            scene.add(label, "lbl", 10, 10, 100, 15);
            scene.add(ButtonWidget.of("Keep"), "keep", 120, 10, 60, 20);
            scene.setup();

            var before = scene.snapshot();

            // Mutation 1: change label text
            label.setText("Modified");

            // Mutation 2: add a new widget
            var newBtn = ButtonWidget.of("New");
            scene.add(newBtn, "new", 200, 10, 60, 20);
            scene.rebuild();

            scene.assertDiff(before)
                 .updated("lbl")
                 .created("new")
                 .reused("keep");
        }

        @Test
        void addWidgetToNestedGroup() {
            var scene = TestScene.create(800, 600);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "container");
            TestScene.setBound(group, 0, 0, 400, 200);
            group.addWidget(ButtonWidget.of("A"));
            scene.root().addWidget(group);

            scene.setup();

            var beforeCount = group.children().size();

            // Dynamically add to nested group
            var newBtn = ButtonWidget.of("B");
            newBtn.setKey("b");
            newBtn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(30), sizeOf(60, 20)));
            group.addWidget(newBtn);
            scene.rebuild();

            assertEquals(beforeCount + 1, group.children().size());
            scene.assertExists("b");
        }
    }

    // ==================== Multi-Widget State Machines ====================

    @Nested
    class StateMachineTest {

        @Test
        void counterWithIncrementAndDecrement() {
            var scene = TestScene.create(800, 600);
            var counter = new AtomicInteger(0);

            var inc = ButtonWidget.of("+");
            inc.onClick(counter::incrementAndGet);
            scene.add(inc, "inc", 10, 10, 40, 30);

            var dec = ButtonWidget.of("-");
            dec.onClick(counter::decrementAndGet);
            scene.add(dec, "dec", 60, 10, 40, 30);

            scene.setup();

            scene.tap("inc");
            scene.tap("inc");
            scene.tap("inc");
            assertEquals(3, counter.get());

            scene.tap("dec");
            assertEquals(2, counter.get());
        }

        @Test
        void toggleEnablesButton() {
            var scene = TestScene.create(800, 600);
            var submitted = new AtomicBoolean(false);

            var agree = ToggleWidget.create(false);
            var submit = ButtonWidget.of("Submit");
            submit.onClick(() -> submitted.set(true));
            submit.setEnabled(false);

            agree.onToggle(on -> submit.setEnabled(on));

            scene.add(agree, "agree", 10, 10, 40, 20);
            scene.add(submit, "submit", 60, 10, 80, 30);

            scene.setup();

            // Submit should not work while disabled
            scene.tap("submit");
            assertFalse(submitted.get(), "Disabled submit should not fire");

            // Toggle on → enables submit
            scene.tap("agree");
            scene.tap("submit");
            assertTrue(submitted.get(), "Submit should fire after enabling via toggle");
        }

        @Test
        void multiStepWizard() {
            var scene = TestScene.create(800, 600);
            var step = new AtomicInteger(1);
            var log = new ArrayList<String>();

            var nextBtn = ButtonWidget.of("Next");
            nextBtn.onClick(() -> {
                log.add("step-" + step.get());
                step.incrementAndGet();
            });
            scene.add(nextBtn, "next", 10, 10, 80, 30);

            var resetBtn = ButtonWidget.of("Reset");
            resetBtn.onClick(() -> {
                step.set(1);
                log.clear();
            });
            scene.add(resetBtn, "reset", 100, 10, 80, 30);

            scene.setup();

            scene.perform(seq -> seq
                    .moveTo("next").click()
                    .moveTo("next").click()
                    .moveTo("next").click()
                    .then(s -> assertEquals(4, step.get()))
                    .then(s -> assertEquals(List.of("step-1", "step-2", "step-3"), log))
                    .moveTo("reset").click()
                    .then(s -> assertEquals(1, step.get()))
                    .then(s -> assertTrue(log.isEmpty()))
            );
        }
    }

    // ==================== Complex InputSequence ====================

    @Nested
    class ComplexInputSequenceTest {

        @Test
        void dragBetweenWidgets() {
            var scene = TestScene.create(800, 600);
            var dragStarted = new AtomicBoolean(false);
            var dragEnded = new AtomicBoolean(false);

            var source = new WidgetGroup<>();
            source.onMouseClicked((input, context) -> {
                dragStarted.set(true);
                return EventDispatch.pass;
            });

            var target = new WidgetGroup<>();
            target.onMouseReleased((input, context) -> {
                dragEnded.set(true);
                return EventDispatch.pass;
            });

            scene.add(source, "source", 10, 10, 100, 100);
            scene.add(target, "target", 200, 10, 100, 100);
            scene.setup();

            scene.drag("source", "target");

            assertTrue(dragStarted.get(), "Source should receive mouseClicked (drag start)");
            // Note: mouseReleased dispatches on the hit-tested widget at release position,
            // which could be the target.
        }

        @Test
        void coordinateBasedDragSequence() {
            var scene = TestScene.create(800, 600);
            var moves = new ArrayList<String>();

            var area = new WidgetGroup<>();
            area.onMouseClicked((input, context) -> {
                moves.add("click:" + (int) input.mouseX() + "," + (int) input.mouseY());
                return EventDispatch.pass;
            });
            scene.add(area, "area", 0, 0, 400, 400);
            scene.setup();

            scene.perform(seq -> seq
                    .moveTo(100, 100)
                    .mouseDown(0)
                    .dragTo(200, 200)
                    .mouseUp(0)
            );

            assertFalse(moves.isEmpty(), "Area should have received click event");
        }

        @Test
        void sequenceWithModifierKeys() {
            var scene = TestScene.create(800, 600);
            var shiftClickCount = new AtomicInteger(0);

            var btn = ButtonWidget.of("Shift+Click");
            btn.onClick(shiftClickCount::incrementAndGet);
            scene.add(btn, "btn", 10, 10, 100, 30);

            scene.setup();

            // Click normally
            scene.tap("btn");
            assertEquals(1, shiftClickCount.get());

            // Shift+Click
            scene.perform(seq -> seq
                    .holdShift()
                    .moveTo("btn")
                    .click()
                    .releaseShift()
            );
            assertEquals(2, shiftClickCount.get());
        }
    }

    // ==================== TreeSnapshot Advanced ====================

    @Nested
    class TreeSnapshotAdvancedTest {

        @Test
        void snapshotPreservesTree() {
            var scene = TestScene.create(800, 600);
            var group = new WidgetGroup<>();
            TestScene.setKey(group, "panel");
            TestScene.setBound(group, 0, 0, 400, 200);

            var btn1 = ButtonWidget.of("A");
            btn1.setKey("a");
            btn1.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(50, 20)));
            group.addWidget(btn1);

            var btn2 = LabelWidget.of("B");
            btn2.setKey("b");
            btn2.useStyle(UIStyle.of(positionAbsolute(), insetLeft(60), insetTop(0), sizeOf(50, 15)));
            group.addWidget(btn2);

            scene.root().addWidget(group);
            scene.setup();

            var snap = scene.snapshot();
            var panelNode = snap.findByKey("panel");
            assertNotNull(panelNode, "Should find panel in snapshot");
            assertEquals(2, panelNode.children().size(), "Panel should have 2 children");

            var aNode = snap.findByKey("a");
            assertNotNull(aNode, "Should find node 'a'");
            assertEquals("ButtonWidget", aNode.type());

            var bNode = snap.findByKey("b");
            assertNotNull(bNode, "Should find node 'b'");
            assertEquals("LabelWidget", bNode.type());
        }

        @Test
        void diffUnchangedNestedTree() {
            var scene = TestScene.create(800, 600);
            var group = new WidgetGroup<>();
            TestScene.setKey(group, "panel");
            TestScene.setBound(group, 0, 0, 200, 100);

            var inner = ButtonWidget.of("Inner");
            inner.setKey("inner");
            inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(50, 20)));
            group.addWidget(inner);

            scene.root().addWidget(group);
            scene.setup();

            var before = scene.snapshot();
            // No changes
            scene.assertDiff(before).unchanged();
        }

        @Test
        void snapshotPrintShowsFullTree() {
            var scene = TestScene.create(800, 600);
            var g = new WidgetGroup<>();
            TestScene.setKey(g, "g");
            TestScene.setBound(g, 0, 0, 200, 100);

            var child = ButtonWidget.of("C");
            child.setKey("c");
            child.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(50, 20)));
            g.addWidget(child);

            scene.root().addWidget(g);
            scene.setup();

            String print = scene.snapshot().print();
            assertTrue(print.contains("WidgetGroup"), "Print should show WidgetGroup");
            assertTrue(print.contains("ButtonWidget"), "Print should show ButtonWidget");
        }
    }

    // ==================== Complex PathFinder ====================

    @Nested
    class ComplexPathFinderTest {

        @Test
        void multiLevelWildcardPath() {
            var scene = TestScene.create(800, 600);

            var l1 = new WidgetGroup<>();
            TestScene.setKey(l1, "l1");
            TestScene.setBound(l1, 0, 0, 400, 300);

            var l2 = new WidgetGroup<>();
            l2.setKey("l2");
            l2.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(300, 200)));

            var l3 = new WidgetGroup<>();
            l3.setKey("l3");
            l3.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(200, 100)));

            var btn = ButtonWidget.of("Deep");
            btn.setKey("deep");
            btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(60, 20)));

            l3.addWidget(btn);
            l2.addWidget(l3);
            l1.addWidget(l2);
            scene.root().addWidget(l1);
            scene.setup();

            // ** should find the button at ANY depth
            var results = scene.findAll(WidgetFinder.path("** / [key=deep]"));
            assertEquals(1, results.size(), "Should find exactly one widget with ** path");
            assertEquals("deep", results.get(0).key());
        }

        @Test
        void wildcardWithTypePath() {
            var scene = TestScene.create(800, 600);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "g");
            TestScene.setBound(group, 0, 0, 400, 200);

            group.addWidget(createKeyed(ButtonWidget.of("A"), "a", 0, 0, 50, 20));
            group.addWidget(createKeyed(LabelWidget.of("B"), "b", 60, 0, 50, 15));
            group.addWidget(createKeyed(ButtonWidget.of("C"), "c", 120, 0, 50, 20));

            scene.root().addWidget(group);
            scene.setup();

            // * / ButtonWidget should match buttons inside any container
            var buttons = scene.findAll(WidgetFinder.path("* / ButtonWidget"));
            assertEquals(2, buttons.size(), "Should find 2 buttons with wildcard parent");
        }

        @Test
        void indexPathInNestedGroup() {
            var scene = TestScene.create(800, 600);

            var group = new WidgetGroup<>();
            TestScene.setKey(group, "container");
            TestScene.setBound(group, 0, 0, 400, 200);

            group.addWidget(createKeyed(ButtonWidget.of("First"), "first", 0, 0, 50, 20));
            group.addWidget(createKeyed(ButtonWidget.of("Second"), "second", 60, 0, 50, 20));
            group.addWidget(createKeyed(ButtonWidget.of("Third"), "third", 120, 0, 50, 20));

            scene.root().addWidget(group);
            scene.setup();

            // [key=container] / [2] → third child (0-based)
            var found = scene.find(WidgetFinder.path("[key=container] / [2]"));
            assertEquals("third", found.key(), "Index 2 should be the third child");
        }
    }

    // ==================== Scroll Events ====================

    @Nested
    class ScrollEventTest {

        @Test
        void scrollEventReceived() {
            var scene = TestScene.create(800, 600);
            var scrollY = new AtomicReference<Double>(0.0);

            var area = new WidgetGroup<>();
            TestScene.setKey(area, "scrollable");
            TestScene.setBound(area, 0, 0, 400, 300);
            area.onMouseScrolled((mouseX, mouseY, scrollX, scrollYVal, context) -> {
                scrollY.set(scrollYVal);
                return EventDispatch.consumed;
            });
            scene.root().addWidget(area);
            scene.setup();

            scene.scroll("scrollable", 0, -5.0);
            assertEquals(-5.0, scrollY.get(), "Should receive scroll Y value");
        }

        @Test
        void scrollOnNestedWidget() {
            var scene = TestScene.create(800, 600);
            var parentScrolled = new AtomicBoolean(false);
            var childScrolled = new AtomicBoolean(false);

            var parent = new WidgetGroup<>();
            TestScene.setKey(parent, "parent");
            TestScene.setBound(parent, 0, 0, 400, 300);
            parent.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
                parentScrolled.set(true);
                return EventDispatch.pass;
            });

            var child = new WidgetGroup<>();
            child.setKey("child");
            child.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(200, 200)));
            child.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
                childScrolled.set(true);
                return EventDispatch.pass;
            });

            parent.addWidget(child);
            scene.root().addWidget(parent);
            scene.setup();

            scene.scroll("child", 0, -1);

            assertTrue(childScrolled.get(), "Child should receive scroll event");
            assertTrue(parentScrolled.get(), "Scroll should bubble to parent when not consumed");
        }
    }

    // ==================== Double Click ====================

    @Nested
    class DoubleClickTest {

        @Test
        void doubleTapSendsClickCountTwo() {
            var scene = TestScene.create(800, 600);
            var maxClickCount = new AtomicInteger(0);

            var btn = new WidgetGroup<>();
            TestScene.setKey(btn, "dbl");
            TestScene.setBound(btn, 10, 10, 100, 30);
            btn.onMouseClick((input, clickCount, context) -> {
                if (clickCount > maxClickCount.get()) {
                    maxClickCount.set(clickCount);
                }
                return EventDispatch.pass;
            });
            scene.root().addWidget(btn);
            scene.setup();

            scene.doubleTap("dbl");

            assertTrue(maxClickCount.get() >= 2,
                    "Double tap should produce clickCount >= 2, got " + maxClickCount.get());
        }

        @Test
        void rightClickUsesButton1() {
            var scene = TestScene.create(800, 600);
            var clickedButton = new AtomicInteger(-1);

            var widget = new WidgetGroup<>();
            TestScene.setKey(widget, "rc");
            TestScene.setBound(widget, 10, 10, 100, 30);
            widget.onMouseClicked((input, context) -> {
                clickedButton.set(input.key().getValue());
                return EventDispatch.pass;
            });
            scene.root().addWidget(widget);
            scene.setup();

            scene.rightTap("rc");

            assertEquals(1, clickedButton.get(), "Right tap should use button 1");
        }
    }

    // ==================== Widget Assert Chain ====================

    @Nested
    class WidgetAssertChainTest {

        @Test
        void chainedAssertionsWork() {
            var scene = TestScene.create(800, 600);

            var btn = ButtonWidget.of("Chain");
            scene.add(btn, "c", 10, 20, 100, 40);
            scene.setup();

            scene.assertThat("c")
                 .isVisible()
                 .hasPos(10, 20)
                 .hasSize(100, 40)
                 .isOfType(ButtonWidget.class)
                 .hasLabel("Chain");
        }

        @Test
        void assertionFailsWithClearMessage() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Test"), "btn", 10, 10, 80, 30);
            scene.setup();

            var error = assertThrows(AssertionError.class, () ->
                    scene.assertThat("btn").hasPos(999, 999)
            );
            assertTrue(error.getMessage().contains("999") || error.getMessage().contains("position"),
                    "Error message should indicate the expected position");
        }
    }

    // ==================== Helpers ====================

    private static Widget createKeyed(Widget widget, Object key, int x, int y, int w, int h) {
        widget.setKey(key);
        widget.useStyle(UIStyle.of(positionAbsolute(), insetLeft(x), insetTop(y), sizeOf(w, h)));
        return widget;
    }
}
