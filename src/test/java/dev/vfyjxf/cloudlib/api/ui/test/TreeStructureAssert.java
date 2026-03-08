package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structural assertion for verifying the widget tree shape.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * TreeStructureAssert.assertTree(root, tree -> tree
 *     .group(WidgetGroup.class, "toolbar", toolbar -> toolbar
 *         .widget(ButtonWidget.class, "add")
 *         .widget(ButtonWidget.class, "delete")
 *     )
 *     .widget(LabelWidget.class, "status")
 * );
 * }</pre>
 */
public final class TreeStructureAssert {

    private TreeStructureAssert() {}

    public static void assertTree(Widget root, Consumer<NodeExpectation> expected) {
        NodeExpectation rootExpectation = new NodeExpectation();
        expected.accept(rootExpectation);
        assertChildren(root, rootExpectation.childExpectations, "root");
    }

    private static void assertChildren(Widget parent, List<ChildExpectation> expectations, String path) {
        if (!(parent instanceof CompositeWidget<?> group)) {
            if (!expectations.isEmpty()) {
                fail(path + ": expected " + expectations.size() + " children but widget is not a CompositeWidget");
            }
            return;
        }

        var children = group.children();
        assertEquals(expectations.size(), children.size(),
                path + ": child count mismatch. Expected " + expectations.size() + " but found " + children.size()
                        + ". Children: " + childrenSummary(children));

        for (int i = 0; i < expectations.size(); i++) {
            ChildExpectation exp = expectations.get(i);
            Widget child = children.get(i);
            String childPath = path + " / " + child.getClass().getSimpleName() + "[" + i + "]";

            // Type check
            if (exp.expectedType != null) {
                assertTrue(exp.expectedType.isInstance(child),
                        childPath + ": type mismatch, expected " + exp.expectedType.getSimpleName()
                                + " but was " + child.getClass().getSimpleName());
            }

            // Key check
            if (exp.expectedKey != null) {
                assertEquals(exp.expectedKey, child.key(),
                        childPath + ": key mismatch");
            }

            // Label check
            if (exp.expectedLabel != null) {
                String actual = WidgetInspector.textOf(child);
                assertEquals(exp.expectedLabel, actual,
                        childPath + ": label/text mismatch");
            }

            // Visibility check
            if (exp.expectVisible != null) {
                assertEquals(exp.expectVisible, child.visible(),
                        childPath + ": visibility mismatch");
            }

            // Recurse into children
            if (exp.childExpectations != null && !exp.childExpectations.isEmpty()) {
                assertChildren(child, exp.childExpectations, childPath);
            }
        }
    }

    private static String childrenSummary(List<? extends Widget> children) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) sb.append(", ");
            Widget w = children.get(i);
            sb.append(w.getClass().getSimpleName());
            if (w.key() != null) sb.append("[key=").append(w.key()).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    // ==================== Expectation DSL ====================

    public static final class NodeExpectation {

        final List<ChildExpectation> childExpectations = new ArrayList<>();

        /**
         * Expect a leaf widget of the given type.
         */
        public NodeExpectation widget(Class<? extends Widget> type) {
            childExpectations.add(new ChildExpectation(type, null, null, null, null));
            return this;
        }

        /**
         * Expect a leaf widget with type and key.
         */
        public NodeExpectation widget(Class<? extends Widget> type, Object key) {
            childExpectations.add(new ChildExpectation(type, key, null, null, null));
            return this;
        }

        /**
         * Expect a container widget with children.
         */
        public NodeExpectation group(Class<? extends Widget> type, Consumer<NodeExpectation> children) {
            NodeExpectation inner = new NodeExpectation();
            children.accept(inner);
            childExpectations.add(new ChildExpectation(type, null, null, null, inner.childExpectations));
            return this;
        }

        /**
         * Expect a container widget with key and children.
         */
        public NodeExpectation group(Class<? extends Widget> type, Object key, Consumer<NodeExpectation> children) {
            NodeExpectation inner = new NodeExpectation();
            children.accept(inner);
            childExpectations.add(new ChildExpectation(type, key, null, null, inner.childExpectations));
            return this;
        }

        /**
         * Expect any widget (no type constraint).
         */
        public NodeExpectation any() {
            childExpectations.add(new ChildExpectation(null, null, null, null, null));
            return this;
        }

        /**
         * Expect any container with children.
         */
        public NodeExpectation anyGroup(Consumer<NodeExpectation> children) {
            NodeExpectation inner = new NodeExpectation();
            children.accept(inner);
            childExpectations.add(new ChildExpectation(null, null, null, null, inner.childExpectations));
            return this;
        }

        /**
         * Last added child expectation: add label constraint.
         */
        public NodeExpectation withLabel(String text) {
            if (childExpectations.isEmpty()) throw new IllegalStateException("No child expectation to modify");
            ChildExpectation last = childExpectations.removeLast();
            childExpectations.add(new ChildExpectation(last.expectedType, last.expectedKey, text, last.expectVisible, last.childExpectations));
            return this;
        }

        /**
         * Last added child expectation: add visibility constraint.
         */
        public NodeExpectation visible() {
            return withVisibility(true);
        }

        /**
         * Last added child expectation: hidden.
         */
        public NodeExpectation hidden() {
            return withVisibility(false);
        }

        private NodeExpectation withVisibility(boolean visible) {
            if (childExpectations.isEmpty()) throw new IllegalStateException("No child expectation to modify");
            ChildExpectation last = childExpectations.removeLast();
            childExpectations.add(new ChildExpectation(last.expectedType, last.expectedKey, last.expectedLabel, visible, last.childExpectations));
            return this;
        }
    }

    record ChildExpectation(
            Class<? extends Widget> expectedType,
            Object expectedKey,
            String expectedLabel,
            Boolean expectVisible,
            List<ChildExpectation> childExpectations
    ) {}
}
