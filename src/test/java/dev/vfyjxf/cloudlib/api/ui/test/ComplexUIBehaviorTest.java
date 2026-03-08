package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.*;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widget.*;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complex UI behavior tests covering:
 * - Focus management (transfer, events, scope, clearing)
 * - Click groups (outside click detection, multi-member groups)
 * - Widget lifecycle (init/mount/unmount/destroy ordering)
 * - Dynamic tree mutations (add/remove after setup, rebuild)
 * - Multi-step interactions (form workflows, state cascading)
 * - Event flow (capture/bubble phases, consumption, multi-level)
 * - Hit testing edge cases (overlapping, invisible, non-interactive)
 */
class ComplexUIBehaviorTest {

    // ==================== Focus Management ====================

    @Nested
    class FocusManagementTest {

        @Test
        void clickingFocusableWidgetGivesFocus() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("OK"), "ok", 10, 10, 80, 30);
            btn.setFocusNode(new FocusNode());
            scene.setup();

            scene.tap("ok");

            scene.assertFocused("ok");
            scene.assertThat("ok").isFocused();
        }

        @Test
        void clickingNonFocusableWidgetMoveFocusToAncestorScope() {
            // When clicking a non-focusable widget, the framework traverses ancestors
            // to find the nearest focusable container (root scope). Focus transfers
            // away from the previously focused widget.
            var scene = TestScene.create(800, 600);
            var btn1 = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var btn2 = scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
            btn1.setFocusNode(new FocusNode());
            // btn2 is NOT focusable
            scene.setup();

            scene.tap("a");
            scene.assertFocused("a");

            scene.tap("b");
            // Clicking non-focusable widget transfers focus to root scope,
            // so btn1 loses primary focus
            scene.assertThat("a").isNotFocused();
        }

        @Test
        void focusTransferBetweenWidgets() {
            var scene = TestScene.create(800, 600);
            var btn1 = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var btn2 = scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
            btn1.setFocusNode(new FocusNode());
            btn2.setFocusNode(new FocusNode());
            scene.setup();

            scene.tap("a");
            scene.assertFocused("a");
            scene.assertThat("a").isFocused();
            scene.assertThat("b").isNotFocused();

            scene.tap("b");
            scene.assertFocused("b");
            scene.assertThat("b").isFocused();
            scene.assertThat("a").isNotFocused();
        }

        @Test
        void focusEventsFireOnTransfer() {
            var scene = TestScene.create(800, 600);
            var btn1 = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var btn2 = scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
            btn1.setFocusNode(new FocusNode());
            btn2.setFocusNode(new FocusNode());

            var events = new ArrayList<String>();
            btn1.onFocus(ctx -> events.add("a:focus"));
            btn1.onFocusLost(ctx -> events.add("a:focusLost"));
            btn2.onFocus(ctx -> events.add("b:focus"));
            btn2.onFocusLost(ctx -> events.add("b:focusLost"));

            scene.setup();

            scene.tap("a");
            assertEquals(List.of("a:focus"), events);

            events.clear();
            scene.tap("b");
            // Old focus fires focusLost, new focus fires focus
            assertTrue(events.contains("a:focusLost"), "Expected a:focusLost, got: " + events);
            assertTrue(events.contains("b:focus"), "Expected b:focus, got: " + events);
        }

        @Test
        void focusInOutBubbleEvents() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var btn = ButtonWidget.of("Inner");
                btn.setKey("inner");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                btn.setFocusNode(new FocusNode());
                panel.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("inner"), 10, 10, 80, 30);

            var events = new ArrayList<String>();
            // Register bubble event on parent group
            group.onFocusIn((widget, ctx) -> {
                events.add("panel:focusIn:" + widget.key());
                return EventDispatch.pass;
            });
            group.onFocusOut((widget, ctx) -> {
                events.add("panel:focusOut:" + widget.key());
                return EventDispatch.pass;
            });

            scene.setup();
            scene.tap("inner");

            assertTrue(events.contains("panel:focusIn:inner"),
                    "FocusIn should bubble to parent. Events: " + events);
        }

        @Test
        void programmaticFocusRequest() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("Target"), "target", 10, 10, 80, 30);
            btn.setFocusNode(new FocusNode());

            var focused = new AtomicBoolean(false);
            btn.onFocus(ctx -> focused.set(true));

            scene.setup();
            scene.assertNoFocus();

            scene.requestFocus("target");
            scene.assertFocused("target");
            assertTrue(focused.get(), "onFocus should have fired");
        }

        @Test
        void clearFocusRemovesAllFocus() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            btn.setFocusNode(new FocusNode());

            var lost = new AtomicBoolean(false);
            btn.onFocusLost(ctx -> lost.set(true));

            scene.setup();
            scene.tap("a");
            scene.assertFocused("a");

            scene.clearFocus();
            scene.assertNoFocus();
            assertTrue(lost.get(), "onFocusLost should fire on clearFocus");
        }

        @Test
        void focusTransferFiresEventsInOrder() {
            var scene = TestScene.create(800, 600);
            var a = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var b = scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
            a.setFocusNode(new FocusNode());
            b.setFocusNode(new FocusNode());

            var events = new ArrayList<String>();
            a.onFocusOut((w, ctx) -> { events.add("a:focusOut"); return EventDispatch.pass; });
            a.onFocusLost(ctx -> events.add("a:focusLost"));
            b.onFocusIn((w, ctx) -> { events.add("b:focusIn"); return EventDispatch.pass; });
            b.onFocus(ctx -> events.add("b:focus"));

            scene.setup();
            scene.tap("a");
            events.clear();

            scene.tap("b");
            // The order should be: old loses focus, then new gains focus
            assertEquals(List.of("a:focusOut", "a:focusLost", "b:focusIn", "b:focus"), events,
                    "Focus transfer event order mismatch");
        }

        @Test
        void focusScopeRemembersPreviousFocus() {
            var scene = TestScene.create(800, 600);
            // Create a group with its own focus scope
            var group = new WidgetGroup<>();
            group.setKey("scope");
            group.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(400, 300)));
            group.setFocusNode(new FocusScopeNode());
            scene.setTrackedBound(group, 0, 0, 400, 300);

            var btn1 = ButtonWidget.of("A");
            btn1.setKey("scopeA");
            btn1.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
            btn1.setFocusNode(new FocusNode());
            group.addWidget(btn1);
            scene.setTrackedBound(btn1, 10, 10, 80, 30);

            var btn2 = ButtonWidget.of("B");
            btn2.setKey("scopeB");
            btn2.useStyle(UIStyle.of(positionAbsolute(), insetLeft(100), insetTop(10), sizeOf(80, 30)));
            btn2.setFocusNode(new FocusNode());
            group.addWidget(btn2);
            scene.setTrackedBound(btn2, 100, 10, 80, 30);

            scene.add(group);
            scene.setup();

            // Focus btn2 inside the scope
            scene.tap("scopeB");
            scene.assertFocused("scopeB");

            // FocusScopeNode should remember scopeB as its last focused child
            var scopeNode = (FocusScopeNode) group.focusNode();
            assertNotNull(scopeNode.focusedChild(), "Scope should remember focused child");
        }

        @Test
        void multipleFocusTransfersCycle() {
            var scene = TestScene.create(800, 600);
            var widgets = new Widget[5];
            for (int i = 0; i < 5; i++) {
                widgets[i] = scene.add(ButtonWidget.of("B" + i), "b" + i, i * 90, 10, 80, 30);
                widgets[i].setFocusNode(new FocusNode());
            }

            var focusCount = new AtomicInteger(0);
            var lostCount = new AtomicInteger(0);
            for (var w : widgets) {
                w.onFocus(ctx -> focusCount.incrementAndGet());
                w.onFocusLost(ctx -> lostCount.incrementAndGet());
            }

            scene.setup();

            // Cycle through all 5 widgets
            for (int i = 0; i < 5; i++) {
                scene.tap("b" + i);
                scene.assertFocused("b" + i);
            }

            assertEquals(5, focusCount.get(), "Each widget should gain focus once");
            assertEquals(4, lostCount.get(), "Only 4 widgets lose focus (first gain doesn't have prior)");
        }

        @Test
        void focusOnNestedWidgetBubblesUpToAllAncestors() {
            var scene = TestScene.create(800, 600);
            var outerEvents = new ArrayList<String>();
            var innerEvents = new ArrayList<String>();

            var outer = scene.addGroup("outer", 0, 0, 400, 300, outerPanel -> {
                var inner = new WidgetGroup<Widget>();
                inner.setKey("inner");
                inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(200, 200)));
                outerPanel.addWidget(inner);

                var btn = ButtonWidget.of("Deep");
                btn.setKey("deep");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(5), insetTop(5), sizeOf(80, 30)));
                btn.setFocusNode(new FocusNode());
                inner.addWidget(btn);
            });

            scene.setTrackedBound(scene.find("inner"), 10, 10, 200, 200);
            scene.setTrackedBound(scene.find("deep"), 5, 5, 80, 30);

            outer.onFocusIn((w, ctx) -> {
                outerEvents.add("focusIn:" + w.key());
                return EventDispatch.pass;
            });
            scene.find("inner").onFocusIn((w, ctx) -> {
                innerEvents.add("focusIn:" + w.key());
                return EventDispatch.pass;
            });

            scene.setup();
            scene.tap("deep");

            assertTrue(innerEvents.contains("focusIn:deep"),
                    "Inner container should see focusIn bubble. Got: " + innerEvents);
            assertTrue(outerEvents.contains("focusIn:deep"),
                    "Outer container should see focusIn bubble. Got: " + outerEvents);
        }
    }

    // ==================== Click Groups ====================

    @Nested
    class ClickGroupTest {

        @Test
        void clickOutsideGroupFiresEvent() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("Grouped"), "grouped", 10, 10, 80, 30);
            var outside = scene.add(ButtonWidget.of("Outside"), "outside", 200, 10, 80, 30);
            btn.setClickGroup("myGroup");

            var clickedOutside = new AtomicBoolean(false);
            btn.onClickOutside(ctx -> clickedOutside.set(true));

            scene.setup();

            // Click on the grouped widget itself — should NOT fire onClickOutside
            scene.tap("grouped");
            assertFalse(clickedOutside.get(), "Click on group member should NOT fire onClickOutside");

            // Click outside the group
            scene.tap("outside");
            assertTrue(clickedOutside.get(), "Click outside group should fire onClickOutside");
        }

        @Test
        void multiMemberClickGroupSharesOutsideDetection() {
            var scene = TestScene.create(800, 600);
            var a = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var b = scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
            var outside = scene.add(ButtonWidget.of("X"), "x", 300, 10, 80, 30);
            a.setClickGroup("group1");
            b.setClickGroup("group1");

            var outsideA = new AtomicInteger(0);
            var outsideB = new AtomicInteger(0);
            a.onClickOutside(ctx -> outsideA.incrementAndGet());
            b.onClickOutside(ctx -> outsideB.incrementAndGet());

            scene.setup();

            // Click on member A — no outside events for this group
            scene.tap("a");
            assertEquals(0, outsideA.get());
            assertEquals(0, outsideB.get());

            // Click on member B — still inside the group
            scene.tap("b");
            assertEquals(0, outsideA.get());
            assertEquals(0, outsideB.get());

            // Click outside the group — BOTH members should get notified
            scene.tap("x");
            assertEquals(1, outsideA.get(), "A should receive onClickOutside");
            assertEquals(1, outsideB.get(), "B should receive onClickOutside");
        }

        @Test
        void separateClickGroupsAreIndependent() {
            var scene = TestScene.create(800, 600);
            var a = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var b = scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
            a.setClickGroup("groupA");
            b.setClickGroup("groupB");

            var outsideA = new AtomicInteger(0);
            var outsideB = new AtomicInteger(0);
            a.onClickOutside(ctx -> outsideA.incrementAndGet());
            b.onClickOutside(ctx -> outsideB.incrementAndGet());

            scene.setup();

            // Click on A — outside for group B, not for group A
            scene.tap("a");
            assertEquals(0, outsideA.get(), "A is inside groupA");
            assertEquals(1, outsideB.get(), "B is outside: click was on A");

            // Click on B — outside for group A, not for group B
            scene.tap("b");
            assertEquals(1, outsideA.get(), "A is outside: click was on B");
            assertEquals(1, outsideB.get(), "B is still 1 (inside groupB)");
        }

        @Test
        void clickGroupWithNestedChild() {
            var scene = TestScene.create(800, 600);
            var outerBtn = scene.add(ButtonWidget.of("Outer"), "outer", 300, 10, 80, 30);

            var group = scene.addGroup("panel", 0, 0, 200, 200, panel -> {
                var inner = ButtonWidget.of("Inner");
                inner.setKey("inner");
                inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(inner);
            });
            group.setClickGroup("panelGroup");
            scene.setTrackedBound(scene.find("inner"), 10, 10, 80, 30);

            var outsideCount = new AtomicInteger(0);
            group.onClickOutside(ctx -> outsideCount.incrementAndGet());

            scene.setup();

            // Click on child inside the panel — counts as inside group (ancestor check)
            scene.tap("inner");
            assertEquals(0, outsideCount.get(), "Click on child should be inside group");

            // Click outside
            scene.tap("outer");
            assertEquals(1, outsideCount.get(), "Click outside should fire");
        }

        @Test
        void removeClickGroupStopsOutsideEvents() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var other = scene.add(ButtonWidget.of("B"), "b", 200, 10, 80, 30);
            btn.setClickGroup("g");

            var count = new AtomicInteger(0);
            btn.onClickOutside(ctx -> count.incrementAndGet());

            scene.setup();

            scene.tap("b");
            assertEquals(1, count.get());

            // Remove click group
            btn.setClickGroup(null);

            scene.tap("b");
            // Should still be 1 since group was removed
            assertEquals(1, count.get(), "After removing click group, no more outside events");
        }

        @Test
        void multipleClickOutsideHandlersAllFire() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("G"), "g", 10, 10, 80, 30);
            var other = scene.add(ButtonWidget.of("O"), "o", 200, 10, 80, 30);
            btn.setClickGroup("grp");

            var count1 = new AtomicInteger(0);
            var count2 = new AtomicInteger(0);
            btn.onClickOutside(ctx -> count1.incrementAndGet());
            btn.onClickOutside(ctx -> count2.incrementAndGet());

            scene.setup();
            scene.tap("o");

            assertEquals(1, count1.get());
            assertEquals(1, count2.get());
        }
    }

    // ==================== Lifecycle Events ====================

    @Nested
    class LifecycleEventTest {

        @Test
        void lifecycleEventsFireInCorrectOrder() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Test");
            btn.setKey("btn");

            var events = new ArrayList<String>();
            btn.onInit(self -> events.add("init"));
            btn.onMount((s, ctx, handle) -> events.add("mount"));

            scene.add(btn, "btn", 10, 10, 80, 30);
            assertTrue(events.isEmpty(), "No events before setup");

            scene.setup();
            assertEquals(List.of("init", "mount"), events,
                    "Init must come before mount");
        }

        @Test
        void destroyFiresAfterUnmount() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Test");

            var events = new ArrayList<String>();
            btn.onUnmount(() -> events.add("unmount"));
            btn.onDestroy(self -> events.add("destroy"));

            scene.add(btn, "btn", 10, 10, 80, 30);
            scene.setup();

            assertTrue(events.isEmpty(), "No unmount/destroy after setup");

            scene.destroy();
            assertEquals(List.of("unmount", "destroy"), events,
                    "Unmount must come before destroy");
        }

        @Test
        void lifecycleStateProgressesCorrectly() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Test");

            assertEquals(Lifecycle.created, btn.lifecycle(), "Initial state should be created");

            scene.add(btn, "btn", 10, 10, 80, 30);
            assertEquals(Lifecycle.created, btn.lifecycle(), "After add should still be created");

            scene.init();
            assertEquals(Lifecycle.initialized, btn.lifecycle(), "After init should be initialized");

            scene.mount();
            assertEquals(Lifecycle.mounted, btn.lifecycle(), "After mount should be mounted");

            scene.destroy();
            assertEquals(Lifecycle.destroyed, btn.lifecycle(), "After destroy should be destroyed");
        }

        @Test
        void multipleWidgetsInitMountInTreeOrder() {
            var scene = TestScene.create(800, 600);
            var events = new ArrayList<String>();

            var a = ButtonWidget.of("A");
            var b = ButtonWidget.of("B");
            var c = ButtonWidget.of("C");

            a.onInit(self -> events.add("init:a"));
            b.onInit(self -> events.add("init:b"));
            c.onInit(self -> events.add("init:c"));
            a.onMount((s, ctx, h) -> events.add("mount:a"));
            b.onMount((s, ctx, h) -> events.add("mount:b"));
            c.onMount((s, ctx, h) -> events.add("mount:c"));

            scene.add(a, "a", 10, 10, 80, 30);
            scene.add(b, "b", 100, 10, 80, 30);
            scene.add(c, "c", 190, 10, 80, 30);
            scene.setup();

            // All inits before all mounts (Scene.init walks tree, then mount walks tree)
            int lastInit = -1;
            int firstMount = events.size();
            for (int i = 0; i < events.size(); i++) {
                if (events.get(i).startsWith("init:")) lastInit = i;
                if (events.get(i).startsWith("mount:") && i < firstMount) firstMount = i;
            }
            assertTrue(lastInit < firstMount,
                    "All init events should come before mount events. Order: " + events);
        }

        @Test
        void nestedWidgetLifecycleEvents() {
            var scene = TestScene.create(800, 600);
            var events = new ArrayList<String>();

            var parent = new WidgetGroup<Widget>();
            var child = ButtonWidget.of("Child");

            parent.onInit(self -> events.add("parent:init"));
            parent.onMount((s, ctx, h) -> events.add("parent:mount"));
            child.onInit(self -> events.add("child:init"));
            child.onMount((s, ctx, h) -> events.add("child:mount"));

            parent.setKey("parent");
            parent.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(200, 200)));
            child.setKey("child");
            child.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
            parent.addWidget(child);
            scene.add(parent);
            scene.setTrackedBound(parent, 0, 0, 200, 200);
            scene.setTrackedBound(child, 10, 10, 80, 30);

            scene.setup();
            assertTrue(events.contains("parent:init"));
            assertTrue(events.contains("child:init"));
            assertTrue(events.contains("parent:mount"));
            assertTrue(events.contains("child:mount"));
        }

        @Test
        void lifecycleStateQueriesWork() {
            var scene = TestScene.create(800, 600);
            var btn = ButtonWidget.of("Test");

            var lifecycle = btn.lifecycle();
            assertTrue(lifecycle.created());
            assertFalse(lifecycle.initialized());
            assertFalse(lifecycle.mounted());

            scene.add(btn, "btn", 10, 10, 80, 30);
            scene.setup();

            lifecycle = btn.lifecycle();
            assertFalse(lifecycle.created());
            assertTrue(lifecycle.initialized());
            assertTrue(lifecycle.mounted());
        }

        @Test
        void onRemoveEventFiresWhenChildRemoved() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var child = ButtonWidget.of("Child");
                child.setKey("child");
                child.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(child);
            });
            scene.setTrackedBound(scene.find("child"), 10, 10, 80, 30);

            var removedFrom = new AtomicReference<Object>();
            scene.find("child").events().register(WidgetEvent.onRemove,
                    (parent, self) -> removedFrom.set(parent.key()));

            var childRemovedKey = new AtomicReference<Object>();
            group.events().register(WidgetEvent.onChildRemoved,
                    (w, ctx) -> childRemovedKey.set(w.key()));

            scene.setup();

            scene.remove("child");
            assertEquals("panel", removedFrom.get(), "onRemove should fire with parent");
            assertEquals("child", childRemovedKey.get(), "onChildRemoved should fire on parent");
        }

        @Test
        void widgetUnmountsWhenRemovedFromScene() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var child = ButtonWidget.of("Child");
                child.setKey("child");
                child.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(child);
            });
            scene.setTrackedBound(scene.find("child"), 10, 10, 80, 30);

            var unmounted = new AtomicBoolean(false);
            scene.find("child").onUnmount(() -> unmounted.set(true));

            scene.setup();
            assertTrue(scene.find("child").lifecycle().mounted());

            scene.remove("child");
            assertTrue(unmounted.get(), "Widget should be unmounted when removed");
        }
    }

    // ==================== Dynamic Tree Mutations ====================

    @Nested
    class DynamicTreeMutationTest {

        @Test
        void addWidgetAfterSetupAndRebuild() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Existing"), "existing", 10, 10, 80, 30);
            scene.setup();

            scene.assertExists("existing");
            assertFalse(scene.exists("dynamic"), "Dynamic widget should not exist yet");

            // Add a new widget after setup
            var dynamic = ButtonWidget.of("Dynamic");
            scene.add(dynamic, "dynamic", 200, 10, 80, 30);
            scene.rebuild();

            scene.assertExists("dynamic");
            assertTrue(dynamic.lifecycle().mounted(), "Dynamic widget should be mounted after rebuild");
        }

        @Test
        void removeWidgetFromTree() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var a = ButtonWidget.of("A");
                a.setKey("a");
                a.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(a);

                var b = ButtonWidget.of("B");
                b.setKey("b");
                b.useStyle(UIStyle.of(positionAbsolute(), insetLeft(100), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(b);
            });
            scene.setTrackedBound(scene.find("a"), 10, 10, 80, 30);
            scene.setTrackedBound(scene.find("b"), 100, 10, 80, 30);
            scene.setup();

            scene.assertExists("a");
            scene.assertExists("b");

            scene.remove("a");

            assertFalse(scene.exists("a"), "Removed widget should no longer exist in tree");
            scene.assertExists("b");
        }

        @Test
        void removeAndReaddWidget() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Steady"), "steady", 10, 10, 80, 30);

            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var removable = ButtonWidget.of("Removable");
                removable.setKey("removable");
                removable.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(50), sizeOf(100, 30)));
                panel.addWidget(removable);
            });
            scene.setTrackedBound(scene.find("removable"), 10, 50, 100, 30);
            scene.setup();

            // Remove
            scene.remove("removable");
            assertFalse(scene.exists("removable"));

            // Re-add a new widget with same key
            var newWidget = ButtonWidget.of("Readded");
            scene.addInto(group, newWidget, "removable", 10, 50, 100, 30);
            scene.rebuild();

            scene.assertExists("removable");
            assertTrue(newWidget.lifecycle().mounted());
        }

        @Test
        void clearGroupRemovesAllChildren() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                for (int i = 0; i < 5; i++) {
                    var btn = ButtonWidget.of("B" + i);
                    btn.setKey("b" + i);
                    btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(i * 80 + 10), insetTop(10), sizeOf(70, 30)));
                    panel.addWidget(btn);
                }
            });
            for (int i = 0; i < 5; i++) {
                scene.setTrackedBound(scene.find("b" + i), i * 80 + 10, 10, 70, 30);
            }
            scene.setup();

            for (int i = 0; i < 5; i++) {
                scene.assertExists("b" + i);
            }

            group.clear();

            for (int i = 0; i < 5; i++) {
                assertFalse(scene.exists("b" + i), "b" + i + " should be removed after clear");
            }
        }

        @Test
        void clearGroupFiresUnmountOnAllChildren() {
            var scene = TestScene.create(800, 600);
            var unmountedKeys = new ArrayList<String>();

            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                for (int i = 0; i < 3; i++) {
                    var btn = ButtonWidget.of("B" + i);
                    btn.setKey("b" + i);
                    btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(i * 80 + 10), insetTop(10), sizeOf(70, 30)));
                    int idx = i;
                    btn.onUnmount(() -> unmountedKeys.add("b" + idx));
                    panel.addWidget(btn);
                }
            });
            for (int i = 0; i < 3; i++) {
                scene.setTrackedBound(scene.find("b" + i), i * 80 + 10, 10, 70, 30);
            }
            scene.setup();

            group.clear();
            assertEquals(3, unmountedKeys.size(), "All 3 children should be unmounted");
        }

        @Test
        void removedWidgetCannotReceiveEvents() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var btn = ButtonWidget.of("Target");
                btn.setKey("target");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("target"), 10, 10, 80, 30);
            var rec = EventRecorder.on(scene.find("target"));
            scene.setup();

            // Click the button — should fire events
            scene.tap("target");
            rec.assertFired("click");

            rec.clear();
            scene.remove("target");

            // Tap where the widget used to be — should not fire events on it
            scene.tapAt(10 + 40, 10 + 15);
            rec.assertNothingFired();
        }

        @Test
        void addWidgetAfterSetupInitializesCorrectly() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("First"), "first", 10, 10, 80, 30);
            scene.setup();

            var events = new ArrayList<String>();
            var newBtn = ButtonWidget.of("Late");
            newBtn.onInit(self -> events.add("init"));
            newBtn.onMount((s, ctx, h) -> events.add("mount"));

            scene.add(newBtn, "late", 200, 10, 80, 30);
            assertTrue(events.isEmpty(), "No lifecycle events before rebuild");

            scene.rebuild();
            assertTrue(events.contains("init"), "Late-added widget should be initialized");
            assertTrue(events.contains("mount"), "Late-added widget should be mounted");
            assertTrue(newBtn.lifecycle().mounted());
        }

        @Test
        void removeFocusedWidgetClearsFocus() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var btn = ButtonWidget.of("Focusable");
                btn.setKey("focusable");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                btn.setFocusNode(new FocusNode());
                panel.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("focusable"), 10, 10, 80, 30);
            scene.setup();

            scene.tap("focusable");
            scene.assertFocused("focusable");

            scene.remove("focusable");
            // Focus should be cleared since the focused widget was removed
            scene.assertNoFocus();
        }

        @Test
        void snapshotReflectsTreeAfterMutation() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            scene.add(ButtonWidget.of("B"), "b", 100, 10, 80, 30);
            scene.setup();

            var before = scene.snapshot();

            // Add a new widget
            scene.add(ButtonWidget.of("C"), "c", 200, 10, 80, 30);
            scene.rebuild();

            var after = scene.snapshot();
            var diff = TreeDiffAssert.create(before, after);
            diff.created("c");  // "c" was added
        }
    }

    // ==================== Multi-Step Interactions ====================

    @Nested
    class MultiStepInteractionTest {

        @Test
        void toggleEnablesAndDisablesButton() {
            var scene = TestScene.create(800, 600);
            var toggle = ToggleWidget.create(false);
            var btn = ButtonWidget.of("Action");

            var clickCount = new AtomicInteger(0);
            btn.onClick(() -> clickCount.incrementAndGet());

            // Toggle controls button enabled state
            toggle.onToggle(on -> btn.setEnabled(on));
            // Explicitly disable button since toggle starts off
            btn.setEnabled(false);

            scene.add(toggle, "toggle", 10, 10, 40, 20);
            scene.add(btn, "btn", 60, 10, 80, 30);
            scene.setup();

            // Initially toggle is off, button is manually disabled
            assertFalse(toggle.toggled());
            assertFalse(btn.enabled());

            // Click button while disabled — onClick should NOT fire
            scene.tap("btn");
            assertEquals(0, clickCount.get(), "Disabled button should not fire onClick");

            // Turn toggle on
            scene.tap("toggle");
            assertTrue(toggle.toggled());
            assertTrue(btn.enabled(), "Button should be enabled after toggle on");

            // Click button while enabled
            scene.tap("btn");
            assertEquals(1, clickCount.get(), "Enabled button should fire onClick");

            // Turn toggle off again
            scene.tap("toggle");
            assertFalse(toggle.toggled());
            assertFalse(btn.enabled());
        }

        @Test
        void sliderValueChangeUpdatesLabelViCallback() {
            var scene = TestScene.create(800, 600);
            var slider = SliderWidget.create(0, 100, 50);
            var label = TextWidget.of("50");

            slider.onValueChanged(val -> label.setText(String.valueOf(val.intValue())));

            scene.add(slider, "slider", 10, 10, 200, 20);
            scene.add(label, "label", 10, 40, 100, 20);
            scene.setup();

            // Programmatically change slider value
            slider.setValue(75);
            assertEquals("75", label.text().getString(), "Label should update when slider value changes");

            slider.setValue(0);
            assertEquals("0", label.text().getString());
        }

        @Test
        void formValidationWithMultipleFields() {
            var scene = TestScene.create(800, 600);
            var field1 = TextFieldWidget.create();
            var field2 = TextFieldWidget.create();
            var submitBtn = ButtonWidget.of("Submit");
            field1.setFocusNode(new FocusNode());
            field2.setFocusNode(new FocusNode());

            var submitted = new AtomicBoolean(false);
            var lastValidation = new AtomicReference<String>("none");

            submitBtn.onClick(() -> {
                boolean valid = !field1.text().isEmpty() && !field2.text().isEmpty();
                if (valid) {
                    submitted.set(true);
                    lastValidation.set("pass");
                } else {
                    lastValidation.set("fail");
                }
            });

            scene.add(field1, "name", 10, 10, 200, 25);
            scene.add(field2, "email", 10, 45, 200, 25);
            scene.add(submitBtn, "submit", 10, 80, 100, 30);
            scene.setup();

            // Submit with empty fields
            scene.tap("submit");
            assertEquals("fail", lastValidation.get());
            assertFalse(submitted.get());

            // Fill in first field
            field1.setText("Alice");

            // Submit with one field still empty
            scene.tap("submit");
            assertEquals("fail", lastValidation.get());

            // Fill in second field
            field2.setText("alice@example.com");

            // Submit with both filled
            scene.tap("submit");
            assertEquals("pass", lastValidation.get());
            assertTrue(submitted.get());
        }

        @Test
        void toggleGroupExclusiveSelection() {
            var scene = TestScene.create(800, 600);
            // 3 toggles that act as radio buttons
            var toggles = new ToggleWidget[3];
            for (int i = 0; i < 3; i++) {
                toggles[i] = ToggleWidget.create(i == 0); // First one starts selected
                int idx = i;
                toggles[i].onToggle(on -> {
                    if (on) {
                        // Turn off all others
                        for (int j = 0; j < 3; j++) {
                            if (j != idx && toggles[j].toggled()) {
                                toggles[j].setToggled(false);
                            }
                        }
                    }
                });
                scene.add(toggles[i], "t" + i, i * 50, 10, 40, 20);
            }
            scene.setup();

            // Initially only t0 is toggled
            assertTrue(toggles[0].toggled());
            assertFalse(toggles[1].toggled());
            assertFalse(toggles[2].toggled());

            // Tap t2
            scene.tap("t2");
            assertFalse(toggles[0].toggled(), "t0 should be deselected");
            assertFalse(toggles[1].toggled());
            assertTrue(toggles[2].toggled(), "t2 should be selected");

            // Tap t1
            scene.tap("t1");
            assertFalse(toggles[0].toggled());
            assertTrue(toggles[1].toggled(), "t1 should be selected");
            assertFalse(toggles[2].toggled(), "t2 should be deselected");
        }

        @Test
        void inputSequenceWithMultipleStepsAndAssertions() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("Click Me"), "btn", 10, 10, 100, 30);
            btn.setFocusNode(new FocusNode());
            var rec = EventRecorder.on(btn);
            scene.setup();

            scene.perform(seq -> {
                seq.moveTo(WidgetFinder.byKey("btn"))
                   .mouseDown(0)
                   .mouseUp(0)
                   .then(ts -> ts.assertFocused("btn"))
                   .moveTo(WidgetFinder.byKey("btn"))
                   .mouseDown(0)
                   .mouseUp(0);
            });

            rec.assertFiredTimes("click", 2);
        }

        @Test
        void complexDragSequence() {
            var scene = TestScene.create(800, 600);
            var source = scene.add(ButtonWidget.of("Source"), "source", 10, 10, 80, 30);
            var target = scene.add(ButtonWidget.of("Target"), "target", 300, 10, 80, 30);
            var sourceRec = EventRecorder.on(source);
            var targetRec = EventRecorder.on(target);
            scene.setup();

            scene.perform(seq -> {
                seq.moveTo(WidgetFinder.byKey("source"))
                   .mouseDown(0)
                   .dragTo(WidgetFinder.byKey("target"))
                   .mouseUp(0);
            });

            // mouseClicked is dispatched on source (where press started)
            sourceRec.assertFired("mouseClicked");
            // mouseReleased is dispatched on target (where mouse currently is)
            targetRec.assertFired("mouseReleased");
        }

        @Test
        void visibilityToggleHidesAndShowsWidget() {
            var scene = TestScene.create(800, 600);
            var toggle = ToggleWidget.create(true);
            var content = scene.add(ButtonWidget.of("Content"), "content", 10, 50, 200, 30);

            toggle.onToggle(on -> content.setVisible(on));

            scene.add(toggle, "vis-toggle", 10, 10, 40, 20);
            scene.setup();

            scene.assertThat("content").isVisible();

            // Toggle off — hide content
            scene.tap("vis-toggle");
            scene.assertThat("content").isNotVisible();

            // Toggle on — show content
            scene.tap("vis-toggle");
            scene.assertThat("content").isVisible();
        }

        @Test
        void clickOnHiddenWidgetHitsWidgetBehind() {
            var scene = TestScene.create(800, 600);
            // Two overlapping buttons
            var back = scene.add(ButtonWidget.of("Back"), "back", 10, 10, 100, 40);
            var front = scene.add(ButtonWidget.of("Front"), "front", 10, 10, 100, 40);
            var backRec = EventRecorder.on(back);
            var frontRec = EventRecorder.on(front);
            scene.setup();

            // Tap front widget — front gets the click
            scene.tap("front");
            frontRec.assertFired("click");
            backRec.assertNothingFired();

            frontRec.clear();

            // Hide front
            front.setVisible(false);

            // Tap same spot — back widget should get the click
            scene.tapAt(60, 30);
            backRec.assertFired("click");
            frontRec.assertNothingFired();
        }

        @Test
        void nextTickTaskRunsOnTick() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            scene.setup();

            var executed = new AtomicBoolean(false);
            scene.scene().nextTick(() -> executed.set(true));

            assertFalse(executed.get(), "NextTick task should not run immediately");

            scene.tick();
            assertTrue(executed.get(), "NextTick task should run after tick");
        }

        @Test
        void intervalTaskRunsPeriodically() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            scene.setup();

            var count = new AtomicInteger(0);
            scene.scene().interval(1, 1, () -> count.incrementAndGet(), disposer -> {});

            scene.tick(5);
            assertTrue(count.get() >= 3, "Interval task should have run multiple times, but ran: " + count.get());
        }
    }

    // ==================== Event Flow & Bubbling ====================

    @Nested
    class EventFlowTest {

        @Test
        void eventBubblesFromChildToParent() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var btn = ButtonWidget.of("Child");
                btn.setKey("child");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("child"), 10, 10, 80, 30);

            var childRec = EventRecorder.on(scene.find("child"));
            var parentRec = EventRecorder.on(group);

            scene.setup();
            scene.tap("child");

            childRec.assertFired("click");
            parentRec.assertFired("click");
        }

        @Test
        void consumedEventDoesNotBubble() {
            var scene = TestScene.create(800, 600);
            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var btn = ButtonWidget.of("Child");
                btn.setKey("child");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("child"), 10, 10, 80, 30);

            // Child consumes the event
            var childRec = EventRecorder.consuming(scene.find("child"));
            var parentRec = EventRecorder.on(group);

            scene.setup();
            scene.tap("child");

            childRec.assertFired("click");
            parentRec.assertNotFired("click");
        }

        @Test
        void multiLevelBubbling() {
            var scene = TestScene.create(800, 600);

            var outer = scene.addGroup("outer", 0, 0, 400, 300, outerPanel -> {
                var inner = new WidgetGroup<Widget>();
                inner.setKey("inner");
                inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(200, 200)));
                outerPanel.addWidget(inner);

                var btn = ButtonWidget.of("Deep");
                btn.setKey("deep");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(5), insetTop(5), sizeOf(80, 30)));
                inner.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("inner"), 10, 10, 200, 200);
            scene.setTrackedBound(scene.find("deep"), 5, 5, 80, 30);

            var deepRec = EventRecorder.on(scene.find("deep"));
            var innerRec = EventRecorder.on(scene.find("inner"));
            var outerRec = EventRecorder.on(outer);

            scene.setup();
            scene.tap("deep");

            deepRec.assertFired("click");
            innerRec.assertFired("click");
            outerRec.assertFired("click");
        }

        @Test
        void middleLayerConsumptionStopsBubbling() {
            var scene = TestScene.create(800, 600);

            var outer = scene.addGroup("outer", 0, 0, 400, 300, outerPanel -> {
                var inner = new WidgetGroup<Widget>();
                inner.setKey("inner");
                inner.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(200, 200)));
                outerPanel.addWidget(inner);

                var btn = ButtonWidget.of("Deep");
                btn.setKey("deep");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(5), insetTop(5), sizeOf(80, 30)));
                inner.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("inner"), 10, 10, 200, 200);
            scene.setTrackedBound(scene.find("deep"), 5, 5, 80, 30);

            var deepRec = EventRecorder.on(scene.find("deep"));
            // Inner CONSUMES — should block outer from seeing it
            var innerRec = EventRecorder.consuming(scene.find("inner"));
            var outerRec = EventRecorder.on(outer);

            scene.setup();
            scene.tap("deep");

            deepRec.assertFired("click");
            innerRec.assertFired("click");
            outerRec.assertNotFired("click");
        }

        @Test
        void mouseClickedAndReleaseAreSeperateEvents() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var rec = EventRecorder.on(btn);
            scene.setup();

            scene.tap("a");

            rec.assertFired("mouseClicked");
            rec.assertFired("mouseReleased");
            rec.assertFired("click");
            assertEquals(3, rec.totalCount(), "Should have exactly 3 events (mouseClicked, mouseReleased, click). Got: " + rec.eventNames());
        }

        @Test
        void rightClickFiresWithButton1() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var rec = EventRecorder.on(btn);
            scene.setup();

            scene.rightTap("a");

            rec.assertFired("mouseClicked");
            rec.assertFiredWith("mouseClicked", e -> (int) e.data().get("button") == 1);
        }

        @Test
        void scrollEventReachesWidget() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 200, 100);
            var rec = EventRecorder.on(btn);
            scene.setup();

            scene.scroll("a", 0, 3.0);

            rec.assertFired("scroll");
            rec.assertFiredWith("scroll", e -> (double) e.data().get("scrollY") == 3.0);
        }

        @Test
        void keyEventOnFocusedWidget() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            btn.setFocusNode(new FocusNode());
            var rec = EventRecorder.on(btn);
            scene.setup();

            scene.tap("a");
            scene.pressKey(GLFW.GLFW_KEY_SPACE);

            rec.assertFired("keyPressed");
            rec.assertFired("keyReleased");
        }

        @Test
        void doubleClickFiresTwoClicks() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var rec = EventRecorder.on(btn);
            scene.setup();

            scene.doubleTap("a");

            rec.assertFiredTimes("click", 2);
            rec.assertFiredTimes("mouseClicked", 2);
            rec.assertFiredTimes("mouseReleased", 2);
        }

        @Test
        void eventRecorderClearResetsCounts() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 80, 30);
            var rec = EventRecorder.on(btn);
            scene.setup();

            scene.tap("a");
            rec.assertFired("click");
            assertEquals(3, rec.totalCount());

            rec.clear();
            assertEquals(0, rec.totalCount());
            rec.assertNothingFired();

            scene.tap("a");
            rec.assertFiredTimes("click", 1);
        }
    }

    // ==================== Hit Testing Edge Cases ====================

    @Nested
    class HitTestEdgeCaseTest {

        @Test
        void invisibleWidgetNotHit() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("Invisible"), "inv", 10, 10, 80, 30);
            scene.setup();

            scene.assertHitTarget(50, 25, "inv");

            btn.setVisible(false);
            // Root WidgetGroup always covers the entire area; so hit test may
            // return root instead of null. We verify the INVISIBLE widget is NOT hit.
            var hit = scene.hitTestAt(50, 25);
            assertTrue(hit == null || !"inv".equals(hit.key()),
                    "Invisible widget should not be the hit target");
        }

        @Test
        void nonInteractiveWidgetNotHit() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("Non-interactive"), "ni", 10, 10, 80, 30);
            scene.setup();

            scene.assertHitTarget(50, 25, "ni");

            btn.setInteractive(false);
            // non-interactive widgets are skipped in hit test
            var hit = scene.hitTestAt(50, 25);
            assertTrue(hit == null || !"ni".equals(hit.key()),
                    "Non-interactive widget should not be the hit target");
        }

        @Test
        void overlappingWidgetsLastAddedWins() {
            var scene = TestScene.create(800, 600);
            // Two buttons at same position — last added should be on top
            scene.add(ButtonWidget.of("Back"), "back", 10, 10, 100, 40);
            scene.add(ButtonWidget.of("Front"), "front", 10, 10, 100, 40);
            scene.setup();

            scene.assertHitTarget(60, 30, "front");
        }

        @Test
        void clickOutsideAllWidgetsHitsNothing() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Small"), "small", 10, 10, 50, 20);
            scene.setup();

            scene.assertNoHitAt(700, 500);
        }

        @Test
        void hitTestAtWidgetEdgeBoundary() {
            var scene = TestScene.create(800, 600);
            scene.add(ButtonWidget.of("Box"), "box", 100, 100, 50, 50);
            scene.setup();

            // Just inside top-left corner
            scene.assertHitTarget(100, 100, "box");
            // Just inside bottom-right corner
            scene.assertHitTarget(149, 149, "box");
            // Just outside right edge (boundary is inclusive at 149)
            scene.assertNoHitAt(151, 125);
            // Just outside bottom edge
            scene.assertNoHitAt(125, 151);
        }

        @Test
        void nestedWidgetHitTestRespectsContainerBounds() {
            var scene = TestScene.create(800, 600);
            scene.addGroup("panel", 50, 50, 200, 200, panel -> {
                var btn = ButtonWidget.of("Inside");
                btn.setKey("inside");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("inside"), 10, 10, 80, 30);
            scene.setup();

            // Absolute position of "inside" is (50+10, 50+10) = (60, 60)
            scene.assertHitTarget(100, 75, "inside");
        }

        @Test
        void hiddenChildInVisibleParentNotHit() {
            var scene = TestScene.create(800, 600);
            scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var visibleBtn = ButtonWidget.of("Vis");
                visibleBtn.setKey("vis");
                visibleBtn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                panel.addWidget(visibleBtn);

                var hiddenBtn = ButtonWidget.of("Hid");
                hiddenBtn.setKey("hid");
                hiddenBtn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(50), sizeOf(80, 30)));
                hiddenBtn.setVisible(false);
                panel.addWidget(hiddenBtn);
            });
            scene.setTrackedBound(scene.find("vis"), 10, 10, 80, 30);
            scene.setTrackedBound(scene.find("hid"), 10, 50, 80, 30);
            scene.setup();

            scene.assertHitTarget(50, 25, "vis");
            // Hidden button at (10,50) absolute — should not be hit
            var hit = scene.hitTestAt(50, 65);
            assertTrue(hit == null || !hit.key().equals("hid"),
                    "Hidden widget should not be the hit target");
        }

        @Test
        void activeButNotInteractiveWidgetPassesThrough() {
            var scene = TestScene.create(800, 600);
            // Stack: back is interactive, front is not
            var back = scene.add(ButtonWidget.of("Back"), "back", 10, 10, 100, 40);
            var front = scene.add(ButtonWidget.of("Front"), "front", 10, 10, 100, 40);
            scene.setup();

            // Both are interactive by default — front wins
            scene.assertHitTarget(60, 30, "front");

            // Make front non-interactive
            front.setInteractive(false);

            // Hit test should pass through to back
            scene.assertHitTarget(60, 30, "back");
        }

        @Test
        void tapAtOffsetHitsCorrectPosition() {
            var scene = TestScene.create(800, 600);
            var wide = scene.add(ButtonWidget.of("Wide"), "wide", 100, 100, 300, 50);
            var rec = EventRecorder.on(wide);
            scene.setup();

            // Tap near the left edge
            scene.tapAt("wide", 5, 25);
            rec.assertFired("click");
            rec.assertFiredWith("mouseClicked",
                    e -> Math.abs((double) e.data().get("mouseX") - 105) < 1);

            rec.clear();

            // Tap near the right edge
            scene.tapAt("wide", 295, 25);
            rec.assertFired("click");
            rec.assertFiredWith("mouseClicked",
                    e -> Math.abs((double) e.data().get("mouseX") - 395) < 1);
        }
    }

    // ==================== Integration Scenarios ====================

    @Nested
    class IntegrationScenarioTest {

        @Test
        void dropdownMenuClickOutsideCloses() {
            // Simulate: click button opens dropdown, click outside closes it
            var scene = TestScene.create(800, 600);
            var trigger = scene.add(ButtonWidget.of("Menu"), "trigger", 10, 10, 80, 30);
            var menuPanel = new WidgetGroup<Widget>();
            menuPanel.setKey("menu");
            menuPanel.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(45), sizeOf(120, 90)));
            menuPanel.setVisible(false);
            scene.add(menuPanel);
            scene.setTrackedBound(menuPanel, 10, 45, 120, 90);

            // Add menu items inside
            for (int i = 0; i < 3; i++) {
                var item = ButtonWidget.of("Item " + i);
                item.setKey("item" + i);
                item.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(i * 30), sizeOf(120, 30)));
                menuPanel.addWidget(item);
                scene.setTrackedBound(item, 0, i * 30, 120, 30);
            }

            // Trigger and menu share a click group
            trigger.setClickGroup("dropdown");
            menuPanel.setClickGroup("dropdown");

            trigger.onClick(() -> menuPanel.setVisible(!menuPanel.visible()));
            menuPanel.onClickOutside(ctx -> menuPanel.setVisible(false));

            var empty = scene.add(ButtonWidget.of("Empty"), "empty", 400, 300, 80, 30);
            scene.setup();

            // Initially closed
            scene.assertThat("menu").isNotVisible();

            // Click trigger to open
            scene.tap("trigger");
            scene.assertThat("menu").isVisible();

            // Click outside both trigger and menu → should close
            scene.tap("empty");
            scene.assertThat("menu").isNotVisible();
        }

        @Test
        void focusAndClickGroupInteraction() {
            var scene = TestScene.create(800, 600);
            // Use ButtonWidget instead of TextFieldWidget to avoid Font dependency
            var fieldA = ButtonWidget.of("A");
            var fieldB = ButtonWidget.of("B");
            fieldA.setFocusNode(new FocusNode());
            fieldB.setFocusNode(new FocusNode());
            fieldA.setClickGroup("formA");
            fieldB.setClickGroup("formB");

            var aClickedOutside = new AtomicInteger(0);
            var bClickedOutside = new AtomicInteger(0);
            fieldA.onClickOutside(ctx -> aClickedOutside.incrementAndGet());
            fieldB.onClickOutside(ctx -> bClickedOutside.incrementAndGet());

            scene.add(fieldA, "fieldA", 10, 10, 200, 25);
            scene.add(fieldB, "fieldB", 10, 50, 200, 25);
            scene.setup();

            // Click fieldA — gets focus, fieldB fires clickOutside
            scene.tap("fieldA");
            scene.assertFocused("fieldA");
            assertEquals(0, aClickedOutside.get());
            assertEquals(1, bClickedOutside.get());

            // Click fieldB — focus transfers, fieldA fires clickOutside
            scene.tap("fieldB");
            scene.assertFocused("fieldB");
            assertEquals(1, aClickedOutside.get());
            assertEquals(1, bClickedOutside.get());
        }

        @Test
        void dynamicFormWithAddRemoveFields() {
            var scene = TestScene.create(800, 600);
            var container = scene.addGroup("form", 0, 0, 400, 400, panel -> {
                var field1 = TextFieldWidget.create("initial");
                field1.setKey("f1");
                field1.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(200, 25)));
                panel.addWidget(field1);
            });
            scene.setTrackedBound(scene.find("f1"), 10, 10, 200, 25);

            var addBtn = scene.add(ButtonWidget.of("Add"), "add", 10, 400, 80, 30);

            var fieldCount = new AtomicInteger(1);
            addBtn.onClick(() -> {
                int idx = fieldCount.incrementAndGet();
                var newField = TextFieldWidget.create("");
                newField.setKey("f" + idx);
                newField.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(idx * 35 + 10), sizeOf(200, 25)));
                container.addWidget(newField);
                scene.setTrackedBound(newField, 10, idx * 35 + 10, 200, 25);
            });

            scene.setup();
            scene.assertCount(WidgetFinder.byType(TextFieldWidget.class), 1);

            // Click add button to add more fields
            scene.tap("add");
            scene.rebuild();
            scene.assertExists("f2");
            scene.assertCount(WidgetFinder.byType(TextFieldWidget.class), 2);

            scene.tap("add");
            scene.rebuild();
            scene.assertExists("f3");
            scene.assertCount(WidgetFinder.byType(TextFieldWidget.class), 3);

            // Remove f2
            scene.remove("f2");
            assertFalse(scene.exists("f2"));
            scene.assertCount(WidgetFinder.byType(TextFieldWidget.class), 2);
        }

        @Test
        void wizardStepByStepNavigation() {
            var scene = TestScene.create(800, 600);
            var currentStep = new AtomicInteger(1);

            var step1 = new WidgetGroup<Widget>();
            var step2 = new WidgetGroup<Widget>();
            var step3 = new WidgetGroup<Widget>();
            step1.setKey("step1");
            step2.setKey("step2");
            step3.setKey("step3");
            step1.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(400, 300)));
            step2.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(400, 300)));
            step3.useStyle(UIStyle.of(positionAbsolute(), insetLeft(0), insetTop(0), sizeOf(400, 300)));
            step2.setVisible(false);
            step3.setVisible(false);

            var nextBtn = ButtonWidget.of("Next");
            var prevBtn = ButtonWidget.of("Prev");

            Runnable updateVisibility = () -> {
                step1.setVisible(currentStep.get() == 1);
                step2.setVisible(currentStep.get() == 2);
                step3.setVisible(currentStep.get() == 3);
            };

            nextBtn.onClick(() -> {
                if (currentStep.get() < 3) {
                    currentStep.incrementAndGet();
                    updateVisibility.run();
                }
            });
            prevBtn.onClick(() -> {
                if (currentStep.get() > 1) {
                    currentStep.decrementAndGet();
                    updateVisibility.run();
                }
            });

            scene.add(step1);
            scene.add(step2);
            scene.add(step3);
            scene.add(nextBtn, "next", 300, 350, 80, 30);
            scene.add(prevBtn, "prev", 10, 350, 80, 30);
            scene.setTrackedBound(step1, 0, 0, 400, 300);
            scene.setTrackedBound(step2, 0, 0, 400, 300);
            scene.setTrackedBound(step3, 0, 0, 400, 300);
            scene.setup();

            // Step 1 visible
            scene.assertThat("step1").isVisible();
            scene.assertThat("step2").isNotVisible();
            scene.assertThat("step3").isNotVisible();

            // Navigate forward
            scene.tap("next");
            scene.assertThat("step1").isNotVisible();
            scene.assertThat("step2").isVisible();

            scene.tap("next");
            scene.assertThat("step2").isNotVisible();
            scene.assertThat("step3").isVisible();

            // Can't go past step 3
            scene.tap("next");
            scene.assertThat("step3").isVisible();
            assertEquals(3, currentStep.get());

            // Navigate backward
            scene.tap("prev");
            scene.assertThat("step2").isVisible();
            scene.assertThat("step3").isNotVisible();

            scene.tap("prev");
            scene.assertThat("step1").isVisible();

            // Can't go before step 1
            scene.tap("prev");
            scene.assertThat("step1").isVisible();
            assertEquals(1, currentStep.get());
        }

        @Test
        void complexStateTrackingWithSnapshotDiff() {
            var scene = TestScene.create(800, 600);
            var toggle = ToggleWidget.create(false);
            var slider = SliderWidget.create(0, 100, 50);
            var label = TextWidget.of("Off/50");

            Runnable updateLabel = () -> {
                String state = toggle.toggled() ? "On" : "Off";
                label.setText(state + "/" + (int) slider.value());
            };
            toggle.onToggle(on -> updateLabel.run());
            slider.onValueChanged(val -> updateLabel.run());

            scene.add(toggle, "toggle", 10, 10, 40, 20);
            scene.add(slider, "slider", 10, 40, 200, 20);
            scene.add(label, "label", 10, 70, 200, 20);
            scene.setup();

            var before = scene.snapshot();

            toggle.toggle();
            slider.setValue(75);

            var after = scene.snapshot();
            var diff = TreeDiffAssert.create(before, after);
            // TreeSnapshot tracks by identity; toggle/slider identity unchanged
            // so they are REUSED. Only label's text component changed.
            diff.reused("toggle");
            diff.reused("slider");
            diff.updated("label");

            assertEquals("On/75", label.text().getString());
        }

        @Test
        void fullWidgetLifecycleFromCreationThroughRemoval() {
            var scene = TestScene.create(800, 600);
            var events = new ArrayList<String>();

            var group = scene.addGroup("panel", 0, 0, 400, 300, panel -> {
                var btn = ButtonWidget.of("Lifecycle");
                btn.setKey("lc");
                btn.useStyle(UIStyle.of(positionAbsolute(), insetLeft(10), insetTop(10), sizeOf(80, 30)));
                btn.setFocusNode(new FocusNode());

                btn.onInit(self -> events.add("init"));
                btn.onMount((s, ctx, h) -> events.add("mount"));
                btn.onUnmount(() -> events.add("unmount"));
                btn.onFocus(ctx -> events.add("focus"));
                btn.onFocusLost(ctx -> events.add("focusLost"));

                panel.addWidget(btn);
            });
            scene.setTrackedBound(scene.find("lc"), 10, 10, 80, 30);
            scene.setup();

            assertTrue(events.contains("init"));
            assertTrue(events.contains("mount"));
            assertFalse(events.contains("focus"));

            // Click to focus
            scene.tap("lc");
            assertTrue(events.contains("focus"));
            scene.assertFocused("lc");

            // Remove — should unmount and clear focus
            scene.remove("lc");
            assertTrue(events.contains("unmount"));
            scene.assertNoFocus();

            // Full sequence verification
            int initIdx = events.indexOf("init");
            int mountIdx = events.indexOf("mount");
            int focusIdx = events.indexOf("focus");
            int unmountIdx = events.indexOf("unmount");
            assertTrue(initIdx < mountIdx, "init before mount");
            assertTrue(mountIdx < focusIdx, "mount before focus");
            assertTrue(focusIdx < unmountIdx, "focus before unmount");
        }

        @Test
        void inputSequenceWithModifiers() {
            var scene = TestScene.create(800, 600);
            var btn = scene.add(ButtonWidget.of("A"), "a", 10, 10, 100, 40);
            btn.setFocusNode(new FocusNode());
            var rec = EventRecorder.on(btn);
            scene.setup();

            scene.perform(seq -> {
                seq.moveTo(WidgetFinder.byKey("a"))
                   .mouseDown(0)
                   .mouseUp(0)
                   .holdCtrl()
                   .keyPress(GLFW.GLFW_KEY_A)
                   .releaseCtrl();
            });

            rec.assertFired("click");
            rec.assertFired("keyPressed");
            rec.assertFired("keyReleased");
        }

        @Test
        void stressTestManyWidgets() {
            var scene = TestScene.create(800, 600);
            int count = 50;
            for (int i = 0; i < count; i++) {
                scene.add(ButtonWidget.of("B" + i), "b" + i, (i % 10) * 80, (i / 10) * 40, 70, 30);
            }
            scene.setup();

            assertEquals(count, scene.findAll(WidgetFinder.byType(ButtonWidget.class)).size());

            // Tap several widgets and verify events
            for (int i = 0; i < 10; i++) {
                scene.tap("b" + i);
            }

            // Verify last tapped widget can be hit
            scene.assertHitTarget(35, 15, "b0");
            scene.assertHitTarget(8 * 80 + 35, 15, "b8");
        }
    }
}
