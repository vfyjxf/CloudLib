package dev.vfyjxf.cloudlib.api.ui.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for WidgetTree traversal and hit testing.
 */
class WidgetTreeTest {

    // ==================== Test Widgets ====================

    /**
     * Simple test widget with configurable bounds.
     */
    static class TestWidget extends Widget {
        private final String name;

        TestWidget(String name, int x, int y, int width, int height) {
            this.name = name;
            this.setPos(x, y);
            this.setSize(width, height);
        }

        public TestWidget setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Composite test widget that can have children.
     */
    static class TestContainer extends CompositeWidget<Widget> {
        private final String name;

        TestContainer(String name, int x, int y, int width, int height) {
            this.name = name;
            this.setPos(x, y);
            this.setSize(width, height);
        }

        public TestContainer addChild(Widget child) {
            addWidget(child);  // Use parent class method which handles parent assignment and position update
            return this;
        }

        public TestContainer setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // ==================== Test Tree Structure ====================
    //
    // All positions are RELATIVE to parent.
    // Absolute positions are calculated as parent.absolutePos + child.relativePos
    //
    //  root (0,0 - 400x400)                     -- absolute: (0,0) to (400,400)
    //  ├── panel1 (10,10 - 180x180)             -- absolute: (10,10) to (190,190)
    //  │   ├── button1 (10,10 - 60x30)          -- absolute: (20,20) to (80,50)
    //  │   └── button2 (10,50 - 60x30)          -- absolute: (20,60) to (80,90)
    //  └── panel2 (200,10 - 180x180)            -- absolute: (200,10) to (380,190)
    //      ├── label1 (10,10 - 80x20)           -- absolute: (210,20) to (290,40)
    //      └── nested (10,40 - 100x100)         -- absolute: (210,50) to (310,150)
    //          └── deep (10,10 - 50x50)         -- absolute: (220,60) to (270,110)

    TestContainer root;
    TestContainer panel1, panel2, nested;
    TestWidget button1, button2, label1, deep;

    @BeforeEach
    void setup() {
        // Build tree from root to leaves to ensure absolute positions are calculated correctly.
        // When a child is added, its absolutePos is computed from parent's current absolutePos.
        // So we must add children AFTER their parent is added to its parent.

        root = new TestContainer("root", 0, 0, 400, 400);

        // First level: add panels to root
        panel1 = new TestContainer("panel1", 10, 10, 180, 180);
        panel2 = new TestContainer("panel2", 200, 10, 180, 180);
        root.addChild(panel1).addChild(panel2);

        // Second level: add children to panels
        button1 = new TestWidget("button1", 10, 10, 60, 30);
        button2 = new TestWidget("button2", 10, 50, 60, 30);
        panel1.addChild(button1).addChild(button2);

        label1 = new TestWidget("label1", 10, 10, 80, 20);
        nested = new TestContainer("nested", 10, 40, 100, 100);
        panel2.addChild(label1).addChild(nested);

        // Third level: add deep to nested
        deep = new TestWidget("deep", 10, 10, 50, 50);
        nested.addChild(deep);
    }

    // ==================== Hit Testing ====================

    @Nested
    class HitTesting {

        @Test
        void hitTest_findsDeepestWidget() {
            // Click on button1 - absolute position is (20,20) to (80,50)
            // Click at (30, 30) which is inside button1
            Widget hit = WidgetTree.hitTest(root, 30, 30);
            assertEquals(button1, hit, "Should find button1 at (30,30)");
        }

        @Test
        void hitTest_findsNestedWidget() {
            // Click on deep widget - absolute position is (220,60) to (270,110)
            // Click at (230, 70) which is inside deep
            Widget hit = WidgetTree.hitTest(root, 230, 70);
            assertEquals(deep, hit, "Should find deep at (230,70)");
        }

        @Test
        void hitTest_findsContainerWhenNotOnChild() {
            // Click on panel1 but not on any button
            // panel1 absolute: (10,10) to (190,190)
            // button1 absolute: (20,20) to (80,50)
            // button2 absolute: (20,60) to (80,90)
            // Click at (100, 100) - inside panel1 but outside both buttons
            Widget hit = WidgetTree.hitTest(root, 100, 100);
            assertEquals(panel1, hit, "Should find panel1 at (100,100)");
        }

        @Test
        void hitTest_findsRootWhenNotOnAnyChild() {
            // Click on root but not on any panel
            // root absolute: (0,0) to (400,400)
            // panel1 absolute: (10,10) to (190,190)
            // panel2 absolute: (200,10) to (380,190)
            // Click at (5, 300) - inside root but outside both panels
            Widget hit = WidgetTree.hitTest(root, 5, 300);
            assertEquals(root, hit, "Should find root at (5,300)");
        }

        @Test
        void hitTest_returnsNullWhenMiss() {
            // Click outside root (-10, -10)
            Widget hit = WidgetTree.hitTest(root, -10, -10);
            assertNull(hit, "Should return null when clicking outside root");
        }

        @Test
        void hitTest_laterChildrenHavePriority() {
            // Create overlapping widgets
            TestContainer parent = new TestContainer("parent", 0, 0, 200, 200);
            TestWidget first = new TestWidget("first", 10, 10, 100, 100);
            TestWidget second = new TestWidget("second", 50, 50, 100, 100);  // overlaps with first
            parent.addChild(first).addChild(second);

            // Click in overlap area (60, 60) - second should win (added later)
            // first absolute: (10,10) to (110,110)
            // second absolute: (50,50) to (150,150)
            // overlap: (50,50) to (110,110)
            Widget hit = WidgetTree.hitTest(parent, 60, 60);
            assertEquals(second, hit, "Later child should have priority in overlap");
        }

        @Test
        void hitTest_respectsVisibility() {
            button1.setVisible(false);

            // Click where button1 would be (30,30) - should hit panel1 instead
            Widget hit = WidgetTree.hitTest(root, 30, 30);
            assertEquals(panel1, hit, "Should skip invisible widget");
        }

        @Test
        void hitTestPath_returnsCorrectPath() {
            // Click on deep widget at (230, 70)
            WidgetPath path = WidgetTree.hitTestPath(root, 230, 70);

            assertFalse(path.isEmpty());
            assertEquals(root, path.root());
            assertEquals(deep, path.leaf());
            assertEquals(4, path.size()); // root -> panel2 -> nested -> deep
        }

        @Test
        void hitTestPath_returnsEmptyOnMiss() {
            WidgetPath path = WidgetTree.hitTestPath(root, -10, -10);
            assertTrue(path.isEmpty());
        }

        @Test
        void hitTestAll_collectsAllHitWidgets() {
            // Click at (230, 70) which hits: deep, nested, panel2, root
            List<Widget> hits = new ArrayList<>();
            int count = WidgetTree.hitTestAll(root, 230, 70, hits);

            assertEquals(4, count);
            // Order: deepest first
            assertEquals(deep, hits.get(0));
            assertEquals(nested, hits.get(1));
            assertEquals(panel2, hits.get(2));
            assertEquals(root, hits.get(3));
        }

        @Test
        void hitTest_withCustomPredicate_HIT_STOP() {
            // Use HIT_STOP to stop at panel2 without checking children
            // The predicate receives LOCAL coordinates (after viewport transform)
            Widget hit = WidgetTree.hitTest(root, 230, 70, (w, localX, localY) -> {
                if (!w.visible()) return WidgetTree.HitTestResult.miss;
                // Check bounds in local space: (0,0) to (w,h)
                if (localX < 0 || localX > w.width() || localY < 0 || localY > w.height()) {
                    return WidgetTree.HitTestResult.miss;
                }
                if (w == panel2) return WidgetTree.HitTestResult.stop;
                return WidgetTree.HitTestResult.descend;
            });

            assertEquals(panel2, hit, "HIT_STOP should prevent checking children");
        }
    }

    // ==================== Tree Traversal ====================

    @Nested
    class Traversal {

        @Test
        void walkPreOrder_visitsNodesInPreOrder() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkPreOrder(root, true, -1, (widget, depth) -> {
                visited.add(widget.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            assertEquals(List.of("root", "panel1", "button1", "button2",
                    "panel2", "label1", "nested", "deep"), visited);
        }

        @Test
        void walkPreOrder_excludesRootWhenRequested() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkPreOrder(root, false, -1, (widget, depth) -> {
                visited.add(widget.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            assertFalse(visited.contains("root"));
            assertEquals(7, visited.size());
        }

        @Test
        void walkPreOrder_respectsMaxDepth() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkPreOrder(root, true, 1, (widget, depth) -> {
                visited.add(widget.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            // depth 0: root, depth 1: panel1, panel2
            assertEquals(List.of("root", "panel1", "panel2"), visited);
        }

        @Test
        void walkPreOrder_skipChildren() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkPreOrder(root, true, -1, (widget, depth) -> {
                visited.add(widget.toString());
                if (widget.toString().equals("panel1")) {
                    return WidgetTree.TraversalControl.skipChildren;
                }
                return WidgetTree.TraversalControl.proceed;
            });

            // panel1's children (button1, button2) should be skipped
            assertFalse(visited.contains("button1"));
            assertFalse(visited.contains("button2"));
            assertTrue(visited.contains("panel2"));
        }

        @Test
        void walkPreOrder_terminate() {
            List<String> visited = new ArrayList<>();
            WidgetTree.TraversalControl result = WidgetTree.walkPreOrder(root, true, -1, (widget, depth) -> {
                visited.add(widget.toString());
                if (widget.toString().equals("button1")) {
                    return WidgetTree.TraversalControl.terminate;
                }
                return WidgetTree.TraversalControl.proceed;
            });

            assertEquals(WidgetTree.TraversalControl.terminate, result);
            assertEquals(List.of("root", "panel1", "button1"), visited);
        }

        @Test
        void walkPreOrder_depthIsCorrect() {
            List<Integer> depths = new ArrayList<>();
            WidgetTree.walkPreOrder(root, true, -1, (widget, depth) -> {
                depths.add(depth);
                return WidgetTree.TraversalControl.proceed;
            });

            // root=0, panel1=1, button1=2, button2=2, panel2=1, label1=2, nested=2, deep=3
            assertEquals(List.of(0, 1, 2, 2, 1, 2, 2, 3), depths);
        }

        @Test
        void walkPostOrder_visitsNodesInPostOrder() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkPostOrder(root, true, -1, (widget, depth) -> {
                visited.add(widget.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            // Children before parents
            assertEquals(List.of("button1", "button2", "panel1",
                    "label1", "deep", "nested", "panel2", "root"), visited);
        }

        @Test
        void walkPostOrder_excludesRootWhenRequested() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkPostOrder(root, false, -1, (widget, depth) -> {
                visited.add(widget.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            assertFalse(visited.contains("root"));
        }

        @Test
        void walkBreadthFirst_visitsLevelByLevel() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
                visited.add(widget.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            // Level 0: root
            // Level 1: panel1, panel2
            // Level 2: button1, button2, label1, nested
            // Level 3: deep
            assertEquals(List.of("root", "panel1", "panel2",
                    "button1", "button2", "label1", "nested", "deep"), visited);
        }

        @Test
        void walkBreadthFirst_depthIsCorrect() {
            List<Integer> depths = new ArrayList<>();
            WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
                depths.add(depth);
                return WidgetTree.TraversalControl.proceed;
            });

            assertEquals(List.of(0, 1, 1, 2, 2, 2, 2, 3), depths);
        }

        @Test
        void walkBreadthFirst_respectsMaxDepth() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkBreadthFirst(root, true, 2, (widget, depth) -> {
                visited.add(widget.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            // depth 0, 1, 2 only - no "deep" (depth 3)
            assertFalse(visited.contains("deep"));
            assertEquals(7, visited.size());
        }

        @Test
        void walkPreOrderWithPath_providesCorrectAncestry() {
            List<String> paths = new ArrayList<>();
            WidgetTree.walkPreOrderWithPath(root, true, -1, (widget, depth, ancestry) -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ancestry.size(); i++) {
                    if (i > 0) sb.append(" > ");
                    sb.append(ancestry.get(i).toString());
                }
                paths.add(sb.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            assertTrue(paths.contains("root"));
            assertTrue(paths.contains("root > panel1"));
            assertTrue(paths.contains("root > panel1 > button1"));
            assertTrue(paths.contains("root > panel2 > nested > deep"));
        }

        @Test
        void walkChildrenReverse_iteratesInReverseOrder() {
            List<String> visited = new ArrayList<>();
            WidgetTree.walkChildrenReverse(panel1, (widget, depth) -> {
                visited.add(widget.toString());
                return WidgetTree.TraversalControl.proceed;
            });

            // button2 was added after button1, so should come first in reverse
            assertEquals(List.of("button2", "button1"), visited);
        }
    }

    // ==================== Path Operations ====================

    @Nested
    class PathOperations {

        @Test
        void pathToRoot_buildsCorrectPath() {
            WidgetPath path = WidgetTree.pathToRoot(deep);

            assertEquals(4, path.size());
            assertEquals(root, path.root());
            assertEquals(deep, path.leaf());
            assertEquals(root, path.get(0));
            assertEquals(panel2, path.get(1));
            assertEquals(nested, path.get(2));
            assertEquals(deep, path.get(3));
        }

        @Test
        void pathToRoot_singleWidget() {
            // Widget with no parent
            TestWidget orphan = new TestWidget("orphan", 0, 0, 10, 10);
            WidgetPath path = WidgetTree.pathToRoot(orphan);

            assertEquals(1, path.size());
            assertEquals(orphan, path.root());
            assertEquals(orphan, path.leaf());
        }
    }

    // ==================== Query Operations ====================

    @Nested
    class QueryOperations {

        @Test
        void findFirst_findsMatchingWidget() {
            Widget found = WidgetTree.findFirst(root, true, -1,
                    w -> w.toString().equals("button2"));

            assertEquals(button2, found);
        }

        @Test
        void findFirst_returnsNullWhenNotFound() {
            Widget found = WidgetTree.findFirst(root, true, -1,
                    w -> w.toString().equals("nonexistent"));

            assertNull(found);
        }

        @Test
        void collectInto_collectsAllMatching() {
            List<Widget> results = new ArrayList<>();
            WidgetTree.collectInto(root, true, -1,
                    w -> w.toString().startsWith("button"), results);

            assertEquals(2, results.size());
            assertTrue(results.contains(button1));
            assertTrue(results.contains(button2));
        }

        @Test
        void count_countsMatchingWidgets() {
            int count = WidgetTree.count(root, true, -1,
                    w -> w instanceof TestContainer);

            assertEquals(4, count); // root, panel1, panel2, nested
        }

        @Test
        void countAll_countsAllWidgets() {
            int count = WidgetTree.countAll(root, true);
            assertEquals(8, count);
        }
    }

    // ==================== WidgetPath Tests ====================

    @Nested
    class WidgetPathTests {

        @Test
        void empty_returnsSingleton() {
            WidgetPath empty1 = WidgetPath.empty();
            WidgetPath empty2 = WidgetPath.empty();

            assertSame(empty1, empty2);
            assertTrue(empty1.isEmpty());
            assertEquals(0, empty1.size());
        }

        @Test
        void of_singleWidget() {
            WidgetPath path = WidgetPath.of(button1);

            assertEquals(1, path.size());
            assertEquals(button1, path.root());
            assertEquals(button1, path.leaf());
        }

        @Test
        void of_multipleWidgets() {
            WidgetPath path = WidgetPath.of(root, panel1, button1);

            assertEquals(3, path.size());
            assertEquals(root, path.root());
            assertEquals(button1, path.leaf());
            assertEquals(panel1, path.get(1));
        }

        @Test
        void of_rejectsNull() {
            assertThrows(NullPointerException.class, () -> WidgetPath.of((Widget) null));
            assertThrows(NullPointerException.class, () -> WidgetPath.of(root, null, button1));
        }

        @Test
        void iterator_iteratesRootToLeaf() {
            WidgetPath path = WidgetTree.pathToRoot(deep);
            List<Widget> visited = new ArrayList<>();
            for (Widget w : path) {
                visited.add(w);
            }

            assertEquals(List.of(root, panel2, nested, deep), visited);
        }

        @Test
        void reverseIterator_iteratesLeafToRoot() {
            WidgetPath path = WidgetTree.pathToRoot(deep);
            List<Widget> visited = new ArrayList<>();
            var iter = path.reverseIterator();
            while (iter.hasNext()) {
                visited.add(iter.next());
            }

            assertEquals(List.of(deep, nested, panel2, root), visited);
        }

        @Test
        void contains_findsWidget() {
            WidgetPath path = WidgetTree.pathToRoot(deep);

            assertTrue(path.contains(root));
            assertTrue(path.contains(panel2));
            assertTrue(path.contains(nested));
            assertTrue(path.contains(deep));
            assertFalse(path.contains(panel1));
            assertFalse(path.contains(button1));
        }

        @Test
        void indexOf_returnsCorrectIndex() {
            WidgetPath path = WidgetTree.pathToRoot(deep);

            assertEquals(0, path.indexOf(root));
            assertEquals(1, path.indexOf(panel2));
            assertEquals(2, path.indexOf(nested));
            assertEquals(3, path.indexOf(deep));
            assertEquals(-1, path.indexOf(button1));
        }

        @Test
        void subPath_createsSubset() {
            WidgetPath path = WidgetTree.pathToRoot(deep);
            WidgetPath sub = path.subPath(0, 2);

            assertEquals(2, sub.size());
            assertEquals(root, sub.root());
            assertEquals(panel2, sub.leaf());
        }

        @Test
        void subPath_byWidget() {
            WidgetPath path = WidgetTree.pathToRoot(deep);
            WidgetPath sub = path.subPath(nested);  // up to but not including nested

            assertEquals(2, sub.size());
            assertEquals(root, sub.root());
            assertEquals(panel2, sub.leaf());
        }

        @Test
        void commonHeadLength_findsSharedPrefix() {
            WidgetPath path1 = WidgetTree.pathToRoot(button1);  // root -> panel1 -> button1
            WidgetPath path2 = WidgetTree.pathToRoot(button2);  // root -> panel1 -> button2

            int common = path1.commonHeadLength(path2);
            assertEquals(2, common);  // root, panel1
        }

        @Test
        void commonAncestor_findsLCA() {
            WidgetPath path1 = WidgetTree.pathToRoot(button1);  // root -> panel1 -> button1
            WidgetPath path2 = WidgetTree.pathToRoot(deep);     // root -> panel2 -> nested -> deep

            Widget lca = path1.commonAncestor(path2);
            assertEquals(root, lca);
        }

        @Test
        void commonAncestor_sameBranch() {
            WidgetPath path1 = WidgetTree.pathToRoot(button1);
            WidgetPath path2 = WidgetTree.pathToRoot(button2);

            Widget lca = path1.commonAncestor(path2);
            assertEquals(panel1, lca);
        }

        @Test
        void equals_comparesContent() {
            WidgetPath path1 = WidgetTree.pathToRoot(deep);
            WidgetPath path2 = WidgetTree.pathToRoot(deep);

            assertEquals(path1, path2);
            assertEquals(path1.hashCode(), path2.hashCode());
        }

        @Test
        void anyMatch_findsMatch() {
            WidgetPath path = WidgetTree.pathToRoot(deep);

            assertTrue(path.anyMatch(w -> w == nested));
            assertFalse(path.anyMatch(w -> w == button1));
        }

        @Test
        void allMatch_checksAll() {
            WidgetPath path = WidgetTree.pathToRoot(deep);

            assertTrue(path.allMatch(w -> w.visible()));
            assertFalse(path.allMatch(w -> w instanceof TestWidget));
        }

        @Test
        void findFirst_findsFirstMatch() {
            WidgetPath path = WidgetTree.pathToRoot(deep);

            Widget found = path.findFirst(w -> w instanceof TestContainer);
            assertEquals(root, found);  // root is first TestContainer in root->leaf order
        }
    }
}
