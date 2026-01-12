package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.UIContext;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DSLStateReconcileComplexTest {

    private static void printDiff(String label, TestTreeDiff.Snapshot before, TestTreeDiff.Snapshot after) {
        String report = TestTreeDiff.report(label, before, after);
        System.out.println("\n--- TREE DIFF REPORT: " + label + " ---\n" + report);
        assertTrue(
            report.contains("(UPDATED") || report.contains("(REUSED)") || report.contains("(REPLACED") || report.contains("(CREATED)") || report.contains("(REMOVED)"),
            "Tree diff should contain at least one marker.\n" + report
        );
    }

    private static @Nullable Widget findByKey(Widget root, Object key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (key.equals(root.key())) {
            return root;
        }
        if (root instanceof WidgetGroup<?> group) {
            for (Widget child : group.children()) {
                Widget found = findByKey(child, key);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Widget> T requireByKey(Widget root, Object key, Class<T> type) {
        Widget found = findByKey(root, key);
        assertNotNull(found, () -> "Expected to find widget with key=" + key);
        assertTrue(type.isInstance(found), () -> "Expected key=" + key + " to be instance of " + type.getSimpleName() + " but was " + found.getClass().getName());
        return type.cast(found);
    }

    private static TestDSL.TestStackWidget requireOnlyChildStack(RootWidget root) {
        assertEquals(1, root.children().size(), "Root should have exactly 1 child (the mounted stack/group)");
        Widget child = root.children().get(0);
        assertTrue(child instanceof TestDSL.TestStackWidget, "Expected root child to be a TestStackWidget but was " + child.getClass().getName());
        return (TestDSL.TestStackWidget) child;
    }

    private static List<Object> directChildKeys(WidgetGroup<?> group) {
        List<Object> keys = new ArrayList<>();
        for (Widget child : group.children()) {
            keys.add(child.key);
        }
        return keys;
    }

    private static List<Object> expectedKeysForItems(int n, boolean reverse, boolean injectAfterItem1) {
        List<Object> keys = new ArrayList<>();
        for (int pos = 0; pos < n; pos++) {
            int idx = reverse ? (n - 1 - pos) : pos;
            keys.add("item-" + idx);
            if (injectAfterItem1 && idx == 1) {
                keys.add("injected");
            }
        }
        return keys;
    }

    @Test
    void useState_triggers_subscription_and_reconcile_updates_text_in_place() {
        UIContext context = null;
        RootWidget root = new RootWidget();

        Blueprint<?> tree = TestDSL.VStack("root", TestDSL.Style.NONE, () -> {
            var count = StateSlot.useState(0);
            TestDSL.Text("txt", "count=" + count.get(), TestDSL.Style.NONE);
            TestDSL.Button("inc", "inc", () -> count.set(count.get() + 1), TestDSL.Style.NONE);
        });

        Reconciler.mount((Blueprint) tree, (WidgetGroup) root, context);

        TestTreeDiff.Snapshot before = TestTreeDiff.snapshot(root);

        TestDSL.TestStackWidget stack = requireOnlyChildStack(root);
        assertEquals(0, stack.onStateChangedCount);

        TestDSL.TestTextWidget text = requireByKey(root, "txt", TestDSL.TestTextWidget.class);
        TestDSL.TestButtonWidget inc = requireByKey(root, "inc", TestDSL.TestButtonWidget.class);

        assertEquals("count=0", text.text);
        assertEquals(1, text.updateCount, "Text should have been updated once during initial mount");
        assertEquals(1, inc.updateCount, "Button should have been updated once during initial mount");

        inc.click();

        TestTreeDiff.Snapshot after = TestTreeDiff.snapshot(root);
        printDiff("useState increment", before, after);

        // Dirty should trigger the group's onDirty callback which re-runs children() under the same StateContext.
        assertEquals(1, stack.onStateChangedCount, "Container should observe exactly one onStateChanged after a single set()");
        assertSame(inc, requireByKey(root, "inc", TestDSL.TestButtonWidget.class), "Keyed button should be reused");
        assertSame(text, requireByKey(root, "txt", TestDSL.TestTextWidget.class), "Keyed text should be reused");

        assertEquals("count=1", text.text);
        assertEquals(2, text.updateCount, "Text should be updated again during reconcile");
        assertEquals(2, inc.updateCount, "Button should also be updated during reconcile");

        // Marker assertion is handled by printDiff().
    }

    @Test
    void conditional_branch_changes_widget_type_replaces_node_and_unmounts_old_one() {
        UIContext context = null;
        RootWidget root = new RootWidget();

        Blueprint<?> tree = TestDSL.VStack("root", TestDSL.Style.NONE, () -> {
            var showText = StateSlot.useState(false);

            TestDSL.Button("toggle", "toggle", () -> showText.set(!showText.get()), TestDSL.Style.NONE);

            if (showText.get()) {
                TestDSL.Text("swap", "now-text", TestDSL.Style.NONE);
            } else {
                TestDSL.Button("swap", "now-button", () -> {}, TestDSL.Style.NONE);
            }
        });

        Reconciler.mount((Blueprint) tree, (WidgetGroup) root, context);

        TestTreeDiff.Snapshot before = TestTreeDiff.snapshot(root);

        TestDSL.TestButtonWidget toggle = requireByKey(root, "toggle", TestDSL.TestButtonWidget.class);
        TestDSL.TestButtonWidget swapButton = requireByKey(root, "swap", TestDSL.TestButtonWidget.class);
        assertEquals(0, swapButton.unmountCount);

        toggle.click();

        TestTreeDiff.Snapshot after = TestTreeDiff.snapshot(root);
        printDiff("conditional replace swap(Button->Text)", before, after);

        // Same key, different blueprint class -> canUpdate == false -> old unmount + new mount
        assertEquals(1, swapButton.unmountCount, "Old widget should be unmounted when replaced by a different widget type");

        TestDSL.TestTextWidget swapText = requireByKey(root, "swap", TestDSL.TestTextWidget.class);
        assertNotSame(swapButton, swapText, "Replaced widget must be a different instance");
        assertEquals("now-text", swapText.text);
    }

    @Test
    void nested_groups_have_independent_state_contexts_and_only_the_dirty_subtree_reconciles() {
        UIContext context = null;
        RootWidget root = new RootWidget();

        Blueprint<?> tree = TestDSL.HStack("root", TestDSL.Style.NONE, () -> {
            TestDSL.Group("left", "left", TestDSL.Style.NONE, () -> {
                var count = StateSlot.useState(0);
                TestDSL.Text("leftTxt", "L=" + count.get(), TestDSL.Style.NONE);
                TestDSL.Button("leftInc", "L+", () -> count.set(count.get() + 1), TestDSL.Style.NONE);
            });

            TestDSL.Group("right", "right", TestDSL.Style.NONE, () -> {
                var count = StateSlot.useState(0);
                TestDSL.Text("rightTxt", "R=" + count.get(), TestDSL.Style.NONE);
                TestDSL.Button("rightInc", "R+", () -> count.set(count.get() + 1), TestDSL.Style.NONE);
            });
        });

        Reconciler.mount((Blueprint) tree, (WidgetGroup) root, context);

        TestTreeDiff.Snapshot before = TestTreeDiff.snapshot(root);

        TestDSL.TestStackWidget rootStack = requireOnlyChildStack(root);
        TestDSL.TestStackWidget leftGroup = requireByKey(root, "left", TestDSL.TestStackWidget.class);
        TestDSL.TestStackWidget rightGroup = requireByKey(root, "right", TestDSL.TestStackWidget.class);

        assertEquals(0, rootStack.onStateChangedCount);
        assertEquals(0, leftGroup.onStateChangedCount);
        assertEquals(0, rightGroup.onStateChangedCount);

        TestDSL.TestTextWidget leftTxt = requireByKey(root, "leftTxt", TestDSL.TestTextWidget.class);
        TestDSL.TestTextWidget rightTxt = requireByKey(root, "rightTxt", TestDSL.TestTextWidget.class);
        TestDSL.TestButtonWidget leftInc = requireByKey(root, "leftInc", TestDSL.TestButtonWidget.class);

        assertEquals("L=0", leftTxt.text);
        assertEquals("R=0", rightTxt.text);

        leftInc.click();

        TestTreeDiff.Snapshot after = TestTreeDiff.snapshot(root);
        printDiff("nested groups: leftInc", before, after);

        assertEquals("L=1", leftTxt.text);
        assertEquals("R=0", rightTxt.text, "Sibling subtree should not change when left subtree state updates");

        assertEquals(0, rootStack.onStateChangedCount, "Parent stack has no state; it should not be marked dirty by child state");
        assertEquals(1, leftGroup.onStateChangedCount, "Only the left group should reconcile");
        assertEquals(0, rightGroup.onStateChangedCount, "Right group should not reconcile");
    }

    @Test
    void deeply_nested_control_flow_reorder_and_mode_switch_exercises_reuse_replace_and_unmount() {
        UIContext context = null;
        RootWidget root = new RootWidget();

        Blueprint<?> tree = TestDSL.VStack("root", TestDSL.Style.NONE, () -> {
            var mode = StateSlot.useState(0);      // 0=Group, 1=Button
            var count = StateSlot.useState(0);
            var reverse = StateSlot.useState(false);

            TestDSL.Text("header", "m=" + mode.get() + ",c=" + count.get(), TestDSL.Style.NONE);

            TestDSL.HStack("controls", TestDSL.Style.NONE, () -> {
                TestDSL.Button("inc", "+", () -> count.set(count.get() + 1), TestDSL.Style.NONE);
                TestDSL.Button("noop", "=", () -> count.set(count.get()), TestDSL.Style.NONE);
                TestDSL.Button("rev", "rev", () -> reverse.set(!reverse.get()), TestDSL.Style.NONE);
                TestDSL.Button("cycle", "mode", () -> mode.set((mode.get() + 1) % 2), TestDSL.Style.NONE);
            });

            TestDSL.Group("content", "content", TestDSL.Style.NONE, () -> {
                int m = mode.get();
                int base = count.get();
                int n = 4 + (base % 3);
                boolean rev = reverse.get();

                for (int pos = 0; pos < n; pos++) {
                    int idx = rev ? (n - 1 - pos) : pos;
                    String itemKey = "item-" + idx;

                    if (m == 0) {
                        // Deep nesting: item (keyed) -> Group -> ZStack -> Group -> HStack -> Text
                        TestDSL.Group(itemKey, "item", TestDSL.Style.NONE, () -> {
                            TestDSL.Group("lvl1-" + idx, "lvl1", TestDSL.Style.NONE, () -> {
                                TestDSL.ZStack("lvl2-" + idx, TestDSL.Style.NONE, () -> {
                                    TestDSL.Group("lvl3-" + idx, "lvl3", TestDSL.Style.NONE, () -> {
                                        TestDSL.HStack("lvl4-" + idx, TestDSL.Style.NONE, () -> {
                                            TestDSL.Text("leaf-" + idx, "T" + idx + "@" + base, TestDSL.Style.NONE);
                                        });
                                    });
                                });
                            });
                        });
                    } else {
                        // Same key, different blueprint class => forces replacement + old unmount.
                        TestDSL.Button(itemKey, "B" + idx + "@" + base, () -> count.set(count.get() + 10 + idx), TestDSL.Style.NONE);
                    }

                    // Optional insertion point (keyed) whose position depends on iteration order.
                    if (idx == 1 && (base % 2 == 0)) {
                        TestDSL.Text("injected", "I@" + base, TestDSL.Style.NONE);
                    }
                }
            });
        });

        Reconciler.mount((Blueprint) tree, root, context);

        TestTreeDiff.Snapshot s0 = TestTreeDiff.snapshot(root);

        TestDSL.TestStackWidget rootStack = requireOnlyChildStack(root);
        TestDSL.TestStackWidget content = requireByKey(root, "content", TestDSL.TestStackWidget.class);

        // Initial structure (count=0 => n=4, injected present, reverse=false)
        assertEquals(expectedKeysForItems(4, false, true), directChildKeys(content));

        // Capture keyed item instances in mode=0 (they are groups)
        TestDSL.TestStackWidget item0 = requireByKey(root, "item-0", TestDSL.TestStackWidget.class);
        TestDSL.TestStackWidget item1 = requireByKey(root, "item-1", TestDSL.TestStackWidget.class);
        TestDSL.TestStackWidget item3 = requireByKey(root, "item-3", TestDSL.TestStackWidget.class);
        assertEquals(0, item0.unmountCount);
        assertEquals(0, item1.unmountCount);
        assertEquals(0, item3.unmountCount);

        // No-op set: should NOT mark dirty (StateAccessor.set checks equality)
        int beforeNoop = rootStack.onStateChangedCount;
        requireByKey(root, "noop", TestDSL.TestButtonWidget.class).click();
        assertEquals(beforeNoop, rootStack.onStateChangedCount, "Setting state to the same value should not trigger dirty/reconcile");

        TestTreeDiff.Snapshot sNoop = TestTreeDiff.snapshot(root);
        printDiff("deep: noop(set same)", s0, sNoop);

        // Reorder: keyed children should be reused (same instances), order changes
        requireByKey(root, "rev", TestDSL.TestButtonWidget.class).click();

        TestTreeDiff.Snapshot sRev = TestTreeDiff.snapshot(root);
        printDiff("deep: reorder(reverse)", sNoop, sRev);
        assertEquals(expectedKeysForItems(4, true, true), directChildKeys(content));
        assertSame(item0, requireByKey(root, "item-0", TestDSL.TestStackWidget.class));
        assertSame(item1, requireByKey(root, "item-1", TestDSL.TestStackWidget.class));
        assertSame(item3, requireByKey(root, "item-3", TestDSL.TestStackWidget.class));
        assertEquals(0, item0.unmountCount);
        assertEquals(0, item1.unmountCount);
        assertEquals(0, item3.unmountCount);

        // Mode switch: same key but different blueprint class => replacement + unmount old groups
        requireByKey(root, "cycle", TestDSL.TestButtonWidget.class).click();

        TestTreeDiff.Snapshot sCycle = TestTreeDiff.snapshot(root);
        printDiff("deep: mode switch(Group->Button)", sRev, sCycle);
        assertEquals(1, item0.unmountCount);
        assertEquals(1, item1.unmountCount);
        assertEquals(1, item3.unmountCount);

        TestDSL.TestButtonWidget item0Btn = requireByKey(root, "item-0", TestDSL.TestButtonWidget.class);
        assertNotNull(item0Btn.label);
        assertTrue(item0Btn.label.startsWith("B0@"));
    }

    @Test
    void per_item_local_state_is_captured_and_preserved_across_keyed_reorder() {
        UIContext context = null;
        RootWidget root = new RootWidget();

        Blueprint<?> tree = TestDSL.VStack("root", TestDSL.Style.NONE, () -> {
            var reverse = StateSlot.useState(false);
            TestDSL.Button("rev", "rev", () -> reverse.set(!reverse.get()), TestDSL.Style.NONE);

            TestDSL.Group("list", "list", TestDSL.Style.NONE, () -> {
                int n = 5;
                boolean rev = reverse.get();
                for (int pos = 0; pos < n; pos++) {
                    int idx = rev ? (n - 1 - pos) : pos;
                    String itemKey = "item-" + idx;

                    TestDSL.Group(itemKey, "item", TestDSL.Style.NONE, () -> {
                        var selected = StateSlot.useState(false);
                        TestDSL.Button("toggle-" + idx, "toggle", () -> selected.set(!selected.get()), TestDSL.Style.NONE);
                        TestDSL.Text("value-" + idx, selected.get() ? "ON" : "OFF", TestDSL.Style.NONE);
                    });
                }
            });
        });

        Reconciler.mount((Blueprint) tree, (WidgetGroup) root, context);

        TestTreeDiff.Snapshot s0 = TestTreeDiff.snapshot(root);

        TestDSL.TestStackWidget rootStack = requireOnlyChildStack(root);
        TestDSL.TestStackWidget list = requireByKey(root, "list", TestDSL.TestStackWidget.class);

        TestDSL.TestStackWidget item1 = requireByKey(root, "item-1", TestDSL.TestStackWidget.class);
        TestDSL.TestTextWidget value1 = requireByKey(root, "value-1", TestDSL.TestTextWidget.class);
        assertEquals("OFF", value1.text);

        // Toggle local state inside item-1: should only dirty item-1, not list/root.
        requireByKey(root, "toggle-1", TestDSL.TestButtonWidget.class).click();

        TestTreeDiff.Snapshot sToggle = TestTreeDiff.snapshot(root);
        printDiff("per-item: toggle item-1", s0, sToggle);
        assertEquals(1, item1.onStateChangedCount, "Item should reconcile when its own state changes");
        assertEquals("ON", requireByKey(root, "value-1", TestDSL.TestTextWidget.class).text);
        assertEquals(0, list.onStateChangedCount, "Parent list should not be marked dirty by child local state");

        // Now reorder at parent level: item-1 should be reused and keep its local state value.
        requireByKey(root, "rev", TestDSL.TestButtonWidget.class).click();

        TestTreeDiff.Snapshot sRev = TestTreeDiff.snapshot(root);
        printDiff("per-item: parent reorder(reverse)", sToggle, sRev);
        assertEquals(1, rootStack.onStateChangedCount, "Root owns the reorder state, so it should reconcile once");

        TestDSL.TestStackWidget item1After = requireByKey(root, "item-1", TestDSL.TestStackWidget.class);
        assertSame(item1, item1After, "Keyed item widget should be reused across reorder");
        assertEquals("ON", requireByKey(root, "value-1", TestDSL.TestTextWidget.class).text, "Local state should be preserved across reorder");
        assertEquals(0, item1.unmountCount, "Reorder should not unmount keyed items");

        // Sanity: list child order is reversed (keys move), but instance is stable.
        assertEquals(expectedKeysForItems(5, true, false), directChildKeys(list));
    }
}
