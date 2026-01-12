package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.UIContext;
import dev.vfyjxf.cloudlib.api.ui.widget.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the SwiftUI-inspired DSL system.
 * <p>
 * Tests focus on visibility and basic component structure.
 * No MinecraftServer annotation is used - this is a pure client-side test.
 */
class DSLVisibilityTest {

    // UIContext is a class, not interface - no mock needed

    private static class TestWidget extends Widget {
        public TestWidget() {
            super();
        }
    }

    private static class TestGroup extends WidgetGroup<TestWidget> {
        public TestGroup() {
            super();
        }
    }

    @BeforeEach
    void setUp() {
        // Nothing to setup
    }

    /**
     * Test that a basic widget is visible by default.
     */
    @Test
    void testWidgetVisibleByDefault() {
        TestWidget widget = new TestWidget();
        assertTrue(widget.visible(), "Widget should be visible by default");
        assertEquals(Visibility.VISIBLE, widget.visibility(), "Default visibility should be VISIBLE");
    }

    /**
     * Test that widget visibility can be changed to INVISIBLE.
     */
    @Test
    void testSetWidgetInvisible() {
        TestWidget widget = new TestWidget();
        widget.setVisibility(Visibility.INVISIBLE);
        assertFalse(widget.visible(), "Widget should not be visible when set to INVISIBLE");
        assertEquals(Visibility.INVISIBLE, widget.visibility(), "Visibility should be INVISIBLE");
    }

    /**
     * Test that widget visibility can be changed to GONE.
     */
    @Test
    void testSetWidgetGone() {
        TestWidget widget = new TestWidget();
        widget.setVisibility(Visibility.GONE);
        assertFalse(widget.visible(), "Widget should not be visible when set to GONE");
        assertEquals(Visibility.GONE, widget.visibility(), "Visibility should be GONE");
    }

    /**
     * Test that widget visibility can be toggled back to VISIBLE.
     */
    @Test
    void testToggleWidgetVisibility() {
        TestWidget widget = new TestWidget();
        widget.setVisibility(Visibility.INVISIBLE);
        assertFalse(widget.visible());

        widget.setVisibility(Visibility.VISIBLE);
        assertTrue(widget.visible(), "Widget should be visible after toggling back");
    }

    /**
     * Test group children visibility inheritance.
     */
    @Test
    void testGroupWithVisibleChildren() {
        TestGroup group = new TestGroup();
        TestWidget child1 = new TestWidget();
        TestWidget child2 = new TestWidget();

        group.addWidget(child1);
        group.addWidget(child2);

        assertEquals(2, group.size(), "Group should contain 2 children");
        assertTrue(child1.visible(), "Child should be visible by default");
        assertTrue(child2.visible(), "Child should be visible by default");
    }

    /**
     * Test making children invisible in a group.
     */
    @Test
    void testGroupWithInvisibleChildren() {
        TestGroup group = new TestGroup();
        TestWidget child1 = new TestWidget();
        TestWidget child2 = new TestWidget();

        group.addWidget(child1);
        group.addWidget(child2);

        child1.setVisibility(Visibility.GONE);

        assertTrue(child2.visible(), "Second child should still be visible");
        assertFalse(child1.visible(), "First child should be invisible");
    }

    /**
     * Test that group is visible even with invisible children.
     */
    @Test
    void testGroupVisibilityIndependentOfChildren() {
        TestGroup group = new TestGroup();
        TestWidget child = new TestWidget();
        group.addWidget(child);

        child.setVisibility(Visibility.INVISIBLE);

        assertTrue(group.visible(), "Group should still be visible even if children are invisible");
    }

    /**
     * Test widget positioning.
     */
    @Test
    void testWidgetPositioning() {
        TestWidget widget = new TestWidget();
        widget.setPos(new Pos(100, 200));

        assertEquals(100, widget.posX(), "X position should be 100");
        assertEquals(200, widget.posY(), "Y position should be 200");
    }

    /**
     * Test widget sizing.
     */
    @Test
    void testWidgetSizing() {
        TestWidget widget = new TestWidget();
        widget.setSize(new Size(150, 75));

        assertEquals(150, widget.getWidth(), "Width should be 150");
        assertEquals(75, widget.getHeight(), "Height should be 75");
    }

    /**
     * Test widget bounds calculation.
     */
    @Test
    void testWidgetBounds() {
        TestWidget widget = new TestWidget();
        widget.setBound(50, 100, 200, 300);

        assertEquals(50, widget.posX());
        assertEquals(100, widget.posY());
        assertEquals(200, widget.getWidth());
        assertEquals(300, widget.getHeight());
    }

    /**
     * Test that DSL scope tracks nested blueprints correctly.
     */
    @Test
    void testDSLScopeNesting() {
        List<Blueprint<?>> outerBlueprints = ScopedReceiver.buildChildren(() -> {
            // Simulate building a container blueprint
            ScopedReceiver.add(createMockBlueprint("outer1"));

            // Nested scope
            List<Blueprint<?>> innerBlueprints = ScopedReceiver.buildChildren(() -> {
                ScopedReceiver.add(createMockBlueprint("inner1"));
                ScopedReceiver.add(createMockBlueprint("inner2"));
            });

            assertEquals(2, innerBlueprints.size(), "Inner scope should have 2 blueprints");

            ScopedReceiver.add(createMockBlueprint("outer2"));
        });

        assertEquals(2, outerBlueprints.size(), "Outer scope should have 2 blueprints");
    }

    /**
     * Test DSL scope isolation.
     */
    @Test
    void testDSLScopeIsolation() {
        List<Blueprint<?>> blueprints1 = ScopedReceiver.buildChildren(() -> {
            ScopedReceiver.add(createMockBlueprint("scope1-item1"));
        });

        List<Blueprint<?>> blueprints2 = ScopedReceiver.buildChildren(() -> {
            ScopedReceiver.add(createMockBlueprint("scope2-item1"));
            ScopedReceiver.add(createMockBlueprint("scope2-item2"));
        });

        assertEquals(1, blueprints1.size(), "First scope should have 1 blueprint");
        assertEquals(2, blueprints2.size(), "Second scope should have 2 blueprints");
    }

    /**
     * Test that adding to DSL scope outside of context returns the blueprint.
     */
    @Test
    void testDSLAddOutsideScope() {
        MockBlueprint blueprint = createMockBlueprint("test");
        MockBlueprint result = ScopedReceiver.add(blueprint);

        assertSame(blueprint, result, "DSL.add should return the same blueprint");
    }

    /**
     * Test widget tree parent-child relationships.
     */
    @Test
    void testWidgetTreeParentChild() {
        TestGroup parent = new TestGroup();
        TestWidget child = new TestWidget();

        parent.addWidget(child);

        assertSame(parent, child.parent(), "Child's parent should be the group");
        assertTrue(parent.children().contains(child), "Parent should contain the child");
    }

    /**
     * Test widget active state.
     */
    @Test
    void testWidgetActiveState() {
        TestWidget widget = new TestWidget();
        assertTrue(widget.active(), "Widget should be active by default");

        widget.setActive(false);
        assertFalse(widget.active(), "Widget should be inactive after setActive(false)");
    }

    /**
     * Test widget interactable state (visibility and active combined).
     */
    @Test
    void testWidgetInteractable() {
        TestWidget widget = new TestWidget();
        assertTrue(widget.interactable(), "Widget should be interactable when visible and active");

        widget.setVisibility(Visibility.INVISIBLE);
        assertFalse(widget.interactable(), "Widget should not be interactable when invisible");

        widget.setVisibility(Visibility.VISIBLE);
        widget.setActive(false);
        assertFalse(widget.interactable(), "Widget should not be interactable when inactive");
    }

    /**
     * Test widget key for reconciliation.
     * Note: Widget keys are set via blueprint, not directly on widget.
     */
    @Test
    void testWidgetKey() {
        TestWidget widget = new TestWidget();
        assertNull(widget.key(), "Widget key should be null by default");
        // Note: Widget does not have setKey method - keys are managed by Blueprint
    }

    /**
     * Test reconciliation can update widget when blueprint matches.
     */
    @Test
    void testReconcilerCanUpdate() {
        MockBlueprint blueprint1 = createMockBlueprint("test");
        MockBlueprint blueprint2 = createMockBlueprint("test");

        assertTrue(
            Reconciler.canUpdate(blueprint1, blueprint2),
            "Reconciler should allow update for matching blueprint classes"
        );
    }

    /**
     * Test reconciliation requires type match.
     */
    @Test
    void testReconcilerRequiresTypeMatch() {
        MockBlueprint blueprint1 = createMockBlueprint("test");
        AnotherMockBlueprint blueprint2 = new AnotherMockBlueprint();

        assertFalse(
            Reconciler.canUpdate(blueprint1, blueprint2),
            "Reconciler should reject update for different blueprint types"
        );
    }

    /**
     * Test reconciliation respects blueprint keys.
     */
    @Test
    void testReconcilerKeysMatch() {
        MockBlueprint blueprint1 = createMockBlueprint("test");
        blueprint1.setKey("key1");

        MockBlueprint blueprint2 = createMockBlueprint("test");
        blueprint2.setKey("key1");

        assertTrue(
            Reconciler.canUpdate(blueprint1, blueprint2),
            "Reconciler should allow update when keys match"
        );
    }

    /**
     * Test reconciliation rejects mismatched keys.
     */
    @Test
    void testReconcilerKeysMismatch() {
        MockBlueprint blueprint1 = createMockBlueprint("test");
        blueprint1.setKey("key1");

        MockBlueprint blueprint2 = createMockBlueprint("test");
        blueprint2.setKey("key2");

        assertFalse(
            Reconciler.canUpdate(blueprint1, blueprint2),
            "Reconciler should reject update when keys don't match"
        );
    }

    /**
     * Test multiple widgets in a group maintain independent visibility.
     */
    @Test
    void testGroupMultipleVisibilityStates() {
        TestGroup group = new TestGroup();
        TestWidget widget1 = new TestWidget();
        TestWidget widget2 = new TestWidget();
        TestWidget widget3 = new TestWidget();

        group.addWidget(widget1);
        group.addWidget(widget2);
        group.addWidget(widget3);

        widget1.setVisibility(Visibility.VISIBLE);
        widget2.setVisibility(Visibility.INVISIBLE);
        widget3.setVisibility(Visibility.GONE);

        assertTrue(widget1.visible());
        assertFalse(widget2.visible());
        assertFalse(widget3.visible());
        assertEquals(Visibility.INVISIBLE, widget2.visibility());
        assertEquals(Visibility.GONE, widget3.visibility());
    }

    /**
     * Test DSL scope maintains separate contexts.
     */
    @Test
    void testMultipleDSLScopes() {
        List<Blueprint<?>> scope1 = ScopedReceiver.buildChildren(() -> {
            for (int i = 0; i < 3; i++) {
                ScopedReceiver.add(createMockBlueprint("scope1-" + i));
            }
        });

        List<Blueprint<?>> scope2 = ScopedReceiver.buildChildren(() -> {
            for (int i = 0; i < 5; i++) {
                ScopedReceiver.add(createMockBlueprint("scope2-" + i));
            }
        });

        assertEquals(3, scope1.size());
        assertEquals(5, scope2.size());
    }

    // ==================== Helper Classes ====================

    private static class MockBlueprint implements Blueprint<TestWidget> {
        private final String name;
        private Object key;

        MockBlueprint(String name) {
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
        public TestWidget createWidget(UIContext context) {
            return new TestWidget();
        }

        @Override
        public void updateWidget(TestWidget widget, UIContext context) {
            // No-op for testing
        }

        @Override
        public String toString() {
            return "MockBlueprint{" + name + "}";
        }
    }

    private static class AnotherMockBlueprint implements Blueprint<TestWidget> {
        @Override
        public Object key() {
            return null;
        }

        @Override
        public TestWidget createWidget(UIContext context) {
            return new TestWidget();
        }

        @Override
        public void updateWidget(TestWidget widget, UIContext context) {
            // No-op for testing
        }
    }

    private MockBlueprint createMockBlueprint(String name) {
        return new MockBlueprint(name);
    }
}
