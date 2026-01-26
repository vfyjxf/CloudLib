package dev.vfyjxf.cloudlib.api.ui.base;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Reconciler - diff algorithm for updating widgets.
 * Tests the reconciliation logic that determines when to create, update, or delete widgets.
 */
class ReconcilerTest {

    // ==================== canUpdate Basic Tests ====================

    @Test
    void testCanUpdateSameTypeNoKeys() {
        TestBlueprint bp1 = new TestBlueprint("a");
        TestBlueprint bp2 = new TestBlueprint("b");

        assertTrue(Reconciler.canUpdate(bp1, bp2));
    }

    @Test
    void testCannotUpdateDifferentTypes() {
        TestBlueprint simple = new TestBlueprint("test");
        TestContainerBlueprint container = new TestContainerBlueprint("test");

        assertFalse(Reconciler.canUpdate(simple, container));
        assertFalse(Reconciler.canUpdate(container, simple));
    }

    @Test
    void testCanUpdateMatchingKeys() {
        TestBlueprint bp1 = new TestBlueprint("a");
        bp1.setKey("shared-key");

        TestBlueprint bp2 = new TestBlueprint("b");
        bp2.setKey("shared-key");

        assertTrue(Reconciler.canUpdate(bp1, bp2));
    }

    @Test
    void testCannotUpdateDifferentKeys() {
        TestBlueprint bp1 = new TestBlueprint("a");
        bp1.setKey("key-1");

        TestBlueprint bp2 = new TestBlueprint("b");
        bp2.setKey("key-2");

        assertFalse(Reconciler.canUpdate(bp1, bp2));
    }

    @Test
    void testCannotUpdateKeyedWithUnkeyed() {
        TestBlueprint keyed = new TestBlueprint("keyed");
        keyed.setKey("some-key");

        TestBlueprint unkeyed = new TestBlueprint("unkeyed");

        assertFalse(Reconciler.canUpdate(keyed, unkeyed));
        assertFalse(Reconciler.canUpdate(unkeyed, keyed));
    }

    @Test
    void testCannotUpdateWithNull() {
        TestBlueprint bp = new TestBlueprint("test");

        assertFalse(Reconciler.canUpdate(null, bp));
        assertFalse(Reconciler.canUpdate(bp, null));
        assertFalse(Reconciler.canUpdate(null, null));
    }

    // ==================== Key Types ====================

    @Test
    void testStringKeys() {
        TestBlueprint bp1 = new TestBlueprint("a");
        bp1.setKey("string-key");

        TestBlueprint bp2 = new TestBlueprint("b");
        bp2.setKey("string-key");

        assertTrue(Reconciler.canUpdate(bp1, bp2));
    }

    @Test
    void testIntegerKeys() {
        TestBlueprint bp1 = new TestBlueprint("a");
        bp1.setKey(42);

        TestBlueprint bp2 = new TestBlueprint("b");
        bp2.setKey(42);

        assertTrue(Reconciler.canUpdate(bp1, bp2));
    }

    @Test
    void testEnumKeys() {
        enum ItemType { HEADER, CONTENT, FOOTER }

        TestBlueprint bp1 = new TestBlueprint("a");
        bp1.setKey(ItemType.HEADER);

        TestBlueprint bp2 = new TestBlueprint("b");
        bp2.setKey(ItemType.HEADER);

        assertTrue(Reconciler.canUpdate(bp1, bp2));
    }

    @Test
    void testMixedKeyTypes() {
        TestBlueprint bp1 = new TestBlueprint("a");
        bp1.setKey("42");

        TestBlueprint bp2 = new TestBlueprint("b");
        bp2.setKey(42); // Integer, not String

        assertFalse(Reconciler.canUpdate(bp1, bp2));
    }

    @Test
    void testCompositeKey() {
        record CompositeKey(String type, int id) {}

        TestBlueprint bp1 = new TestBlueprint("a");
        bp1.setKey(new CompositeKey("item", 1));

        TestBlueprint bp2 = new TestBlueprint("b");
        bp2.setKey(new CompositeKey("item", 1));

        assertTrue(Reconciler.canUpdate(bp1, bp2));

        TestBlueprint bp3 = new TestBlueprint("c");
        bp3.setKey(new CompositeKey("item", 2));

        assertFalse(Reconciler.canUpdate(bp1, bp3));
    }

    // ==================== List Reconciliation Scenarios ====================

    @Test
    void testListReconciliationAppend() {
        // Old: [A, B]
        // New: [A, B, C]
        // Result: Update A, Update B, Create C

        List<TestBlueprint> oldList = List.of(
            createBlueprint("A", "key-a"),
            createBlueprint("B", "key-b")
        );

        List<TestBlueprint> newList = List.of(
            createBlueprint("A-new", "key-a"),
            createBlueprint("B-new", "key-b"),
            createBlueprint("C", "key-c")
        );

        // First two should match by key
        assertTrue(Reconciler.canUpdate(oldList.get(0), newList.get(0)));
        assertTrue(Reconciler.canUpdate(oldList.get(1), newList.get(1)));
    }

    @Test
    void testListReconciliationPrepend() {
        // Old: [B, C]
        // New: [A, B, C]
        // Result: Create A, Update B, Update C

        List<TestBlueprint> oldList = List.of(
            createBlueprint("B", "key-b"),
            createBlueprint("C", "key-c")
        );

        List<TestBlueprint> newList = List.of(
            createBlueprint("A", "key-a"),
            createBlueprint("B-new", "key-b"),
            createBlueprint("C-new", "key-c")
        );

        // key-a is new
        assertFalse(oldList.stream().anyMatch(bp -> "key-a".equals(bp.key())));

        // key-b and key-c exist
        assertTrue(Reconciler.canUpdate(oldList.get(0), newList.get(1)));
        assertTrue(Reconciler.canUpdate(oldList.get(1), newList.get(2)));
    }

    @Test
    void testListReconciliationReorder() {
        // Old: [A, B, C]
        // New: [C, A, B]
        // All items should still match by key

        TestBlueprint oldA = createBlueprint("A", "key-a");
        TestBlueprint oldB = createBlueprint("B", "key-b");
        TestBlueprint oldC = createBlueprint("C", "key-c");

        TestBlueprint newC = createBlueprint("C-new", "key-c");
        TestBlueprint newA = createBlueprint("A-new", "key-a");
        TestBlueprint newB = createBlueprint("B-new", "key-b");

        assertTrue(Reconciler.canUpdate(oldA, newA));
        assertTrue(Reconciler.canUpdate(oldB, newB));
        assertTrue(Reconciler.canUpdate(oldC, newC));
    }

    @Test
    void testListReconciliationRemoveMiddle() {
        // Old: [A, B, C]
        // New: [A, C]
        // Result: Update A, Delete B, Update C

        TestBlueprint oldA = createBlueprint("A", "key-a");
        TestBlueprint oldB = createBlueprint("B", "key-b");
        TestBlueprint oldC = createBlueprint("C", "key-c");

        TestBlueprint newA = createBlueprint("A-new", "key-a");
        TestBlueprint newC = createBlueprint("C-new", "key-c");

        assertTrue(Reconciler.canUpdate(oldA, newA));
        assertTrue(Reconciler.canUpdate(oldC, newC));
        // B has no match
    }

    // ==================== Container Blueprint Tests ====================

    @Test
    void testContainerBlueprintCanUpdate() {
        TestContainerBlueprint old = new TestContainerBlueprint("container");
        TestContainerBlueprint updated = new TestContainerBlueprint("container-updated");

        assertTrue(Reconciler.canUpdate(old, updated));
    }

    @Test
    void testContainerBlueprintWithKeys() {
        TestContainerBlueprint old = new TestContainerBlueprint("container");
        old.setKey("container-key");

        TestContainerBlueprint updated = new TestContainerBlueprint("container-updated");
        updated.setKey("container-key");

        assertTrue(Reconciler.canUpdate(old, updated));
    }

    @Test
    void testNestedContainerReconciliation() {
        TestContainerBlueprint oldRoot = new TestContainerBlueprint("root");
        oldRoot.addChild(createBlueprint("child-1", "c1"));
        oldRoot.addChild(createBlueprint("child-2", "c2"));

        TestContainerBlueprint newRoot = new TestContainerBlueprint("root");
        newRoot.addChild(createBlueprint("child-1-updated", "c1"));
        newRoot.addChild(createBlueprint("child-2-updated", "c2"));

        assertTrue(Reconciler.canUpdate(oldRoot, newRoot));

        // Children should also be able to update
        assertTrue(Reconciler.canUpdate(
            oldRoot.children().get(0),
            newRoot.children().get(0)
        ));
    }

    // ==================== Subclass Handling ====================

    @Test
    void testSubclassCannotUpdateParent() {
        TestBlueprint parent = new TestBlueprint("parent");
        ExtendedBlueprint child = new ExtendedBlueprint("child");

        assertFalse(Reconciler.canUpdate(parent, child));
        assertFalse(Reconciler.canUpdate(child, parent));
    }

    @Test
    void testSameSubclassCanUpdate() {
        ExtendedBlueprint bp1 = new ExtendedBlueprint("a");
        ExtendedBlueprint bp2 = new ExtendedBlueprint("b");

        assertTrue(Reconciler.canUpdate(bp1, bp2));
    }

    // ==================== Edge Cases ====================

    @Test
    void testEmptyKey() {
        TestBlueprint bp1 = new TestBlueprint("a");
        bp1.setKey("");

        TestBlueprint bp2 = new TestBlueprint("b");
        bp2.setKey("");

        assertTrue(Reconciler.canUpdate(bp1, bp2));
    }

    @Test
    void testKeyWithNullToString() {
        // Edge case where key object might have unusual toString
        Object strangeKey = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        TestBlueprint bp = new TestBlueprint("test");
        bp.setKey(strangeKey);

        // Should not throw
        assertNotNull(bp.key());
    }

    @Test
    void testSameBlueprintInstance() {
        TestBlueprint bp = new TestBlueprint("test");

        assertTrue(Reconciler.canUpdate(bp, bp));
    }

    // ==================== Batch Reconciliation Simulation ====================

    @Test
    void testBatchReconciliationSimulation() {
        // Simulate reconciling a list of blueprints
        List<TestBlueprint> oldBlueprints = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            oldBlueprints.add(createBlueprint("item-" + i, i));
        }

        List<TestBlueprint> newBlueprints = new ArrayList<>();
        // Reverse order
        for (int i = 4; i >= 0; i--) {
            newBlueprints.add(createBlueprint("item-" + i + "-updated", i));
        }

        // All items should have a match by key
        for (TestBlueprint newBp : newBlueprints) {
            boolean found = false;
            for (TestBlueprint oldBp : oldBlueprints) {
                if (Reconciler.canUpdate(oldBp, newBp)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Should find match for " + newBp.key());
        }
    }

    // ==================== Helper Methods ====================

    private TestBlueprint createBlueprint(String name, Object key) {
        TestBlueprint bp = new TestBlueprint(name);
        bp.setKey(key);
        return bp;
    }

    // ==================== Helper Classes ====================

    private static class TestBlueprint implements Blueprint<Widget> {
        private final String name;
        private Object key;

        TestBlueprint(String name) {
            this.name = name;
        }

        void setKey(Object key) {
            this.key = key;
        }

        @Override
        public Object key() {
            return key;
        }

        @Override
        public Widget createWidget(Scene scene, SceneContext context) {
            return new Widget();
        }

        @Override
        public void updateWidget(Widget widget, Scene scene, SceneContext context) {
        }

        @Override
        public String toString() {
            return "TestBlueprint{" + name + ", key=" + key + "}";
        }
    }

    private static class ExtendedBlueprint extends TestBlueprint {
        ExtendedBlueprint(String name) {
            super(name);
        }
    }

    private static class TestContainerBlueprint implements Blueprint.Group<CompositeWidget<Widget>, Widget> {
        private final String name;
        private final MutableList<Blueprint<Widget>> children = Lists.mutable.empty();
        private Object key;

        TestContainerBlueprint(String name) {
            this.name = name;
        }

        void addChild(Blueprint<?> child) {
            children.add((Blueprint<Widget>) child);
        }

        void setKey(Object key) {
            this.key = key;
        }

        @Override
        public Object key() {
            return key;
        }

        @Override
        public CompositeWidget<Widget> createWidget(Scene scene, SceneContext context) {
            return new CompositeWidget<>();
        }

        @Override
        public void updateWidget(CompositeWidget<Widget> widget, Scene scene, SceneContext context) {
        }

        @Override
        public MutableList<Blueprint<Widget>> children() {
            return children;
        }

        @Override
        public String toString() {
            return "TestContainerBlueprint{" + name + "}";
        }
    }
}
