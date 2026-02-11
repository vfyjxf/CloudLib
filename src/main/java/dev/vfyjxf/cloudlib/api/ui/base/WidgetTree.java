package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Utility class for traversing and querying widget trees.
 *
 * <h2>Overview</h2>
 * <p>This class provides various tree traversal algorithms and hit testing capabilities
 * for the widget hierarchy. It supports:</p>
 * <ul>
 *     <li><b>Hit Testing</b> - Finding widgets at a specific screen coordinate with
 *         support for capture/bubble event propagation models</li>
 *     <li><b>Tree Traversal</b> - DFS (pre/post order), BFS, and path-based traversals</li>
 *     <li><b>Ancestry Operations</b> - Building paths from any widget to root</li>
 * </ul>
 *
 * @see Widget
 * @see CompositeWidget
 * @see WidgetPath
 */
public final class WidgetTree {

    private WidgetTree() {}


    /**
     * Maximum tree depth to prevent stack overflow from cycles.
     */
    private static final int MAX_DEPTH_GUARD = 1_000_000;

    //region visitor & view

    /**
     * Controls the traversal flow during tree walks.
     *
     * <p>Used by {@link Visitor} and {@link PathVisitor} to indicate
     * how traversal should proceed after visiting a node.</p>
     */
    public enum TraversalControl {
        /**
         * Continue traversal normally, visiting children and siblings.
         */
        CONTINUE,

        /**
         * Skip the subtree of the current node but continue with siblings.
         * <p>Useful for pruning branches that don't need to be visited.</p>
         */
        SKIP_CHILDREN,

        /**
         * Stop the entire traversal immediately.
         * <p>Used when the desired widget has been found or a condition is met.</p>
         */
        TERMINATE
    }

    /**
     * Visitor for tree traversal with depth information.
     *
     * @see #walkPreOrder(Widget, boolean, int, Visitor)
     * @see #walkPostOrder(Widget, boolean, int, Visitor)
     */
    @FunctionalInterface
    public interface Visitor {
        /**
         * Called when visiting a widget during traversal.
         *
         * @param widget the current widget being visited
         * @param depth  the depth of the widget (0 = root of traversal)
         * @return control signal for traversal flow
         */
        TraversalControl visit(Widget widget, int depth);
    }

    /**
     * Visitor that receives the full path from root to the current widget.
     *
     * <p><b>Important:</b> The {@link AncestryView} is only valid during the
     * callback. Do not store references to it.</p>
     *
     * @see #walkPreOrderWithPath(Widget, boolean, int, PathVisitor)
     */
    @FunctionalInterface
    public interface PathVisitor {
        /**
         * Called when visiting a widget with its ancestry path.
         *
         * @param widget   the current widget being visited
         * @param depth    the depth of the widget (0 = root of traversal)
         * @param ancestry mutable view of path from root to current widget
         * @return control signal for traversal flow
         */
        TraversalControl visit(Widget widget, int depth, AncestryView ancestry);
    }

    /**
     * Predicate for hit testing that can short-circuit traversal.
     *
     * @see #hitTest(Widget, double, double, HitTestPredicate)
     */
    @FunctionalInterface
    public interface HitTestPredicate {
        /**
         * Tests if a widget should be considered a hit and if traversal should continue.
         *
         * @param widget the widget to test
         * @param localX x coordinate in widget's local space (after viewport transform)
         * @param localY y coordinate in widget's local space (after viewport transform)
         * @return {@link HitTestResult} indicating whether this is a hit and if children should be tested
         */
        HitTestResult test(Widget widget, double localX, double localY);
    }

    /**
     * A read-only view of the ancestry path during traversal.
     *
     * <p>The path is ordered from root (index 0) to the current widget (index size-1).
     * This view is only valid during the visitor callback and should not be stored.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // For a widget hierarchy: Root -> Panel -> Button
     * // When visiting Button:
     * ancestry.size()       // = 3
     * ancestry.root()       // = Root
     * ancestry.current()    // = Button
     * ancestry.get(1)       // = Panel
     * ancestry.depth()      // = 2
     * }</pre>
     */
    public interface AncestryView {
        /**
         * @return the number of widgets in the path (always >= 1)
         */
        int size();

        /**
         * Gets the widget at the specified index from root.
         *
         * @param indexFromRoot 0-based index where 0 is the root
         * @return the widget at the specified position
         * @throws IndexOutOfBoundsException if index is out of range
         */
        Widget get(int indexFromRoot);

        /**
         * @return the depth of the current widget (size - 1)
         */
        default int depth() {
            return size() - 1;
        }

        /**
         * @return the root widget (first in path)
         */
        default Widget root() {
            return get(0);
        }

        /**
         * @return the current/deepest widget (last in path)
         */
        default Widget current() {
            return get(size() - 1);
        }

        /**
         * Copies this ancestry to a new {@link WidgetPath}.
         *
         * <p>Unlike this view, the returned path is safe to store.</p>
         *
         * @return a new WidgetPath containing this ancestry (root→leaf order)
         */
        default WidgetPath toPath() {
            return WidgetPath.fromAncestry(this);
        }
    }

    //endregion

    //region hit testing

    /**
     * Result of a hit test operation.
     */
    public enum HitTestResult {
        /**
         * Widget is hit and children should also be tested.
         * <p>Use this for container widgets that want to allow
         * their children to receive events.</p>
         */
        HIT_CONTINUE,

        /**
         * Widget is hit but children should NOT be tested.
         * <p>Use this for widgets that "consume" the hit and
         * don't want children to receive events.</p>
         */
        HIT_STOP,

        /**
         * Widget is not hit, skip this branch entirely.
         */
        MISS
    }

    /**
     * Performs hit testing to find the deepest widget at the given coordinates.
     *
     * <p>This method traverses the widget tree in depth-first order, transforming
     * mouse coordinates through each widget's viewport as it descends.
     * It returns the deepest (most specific) widget that contains
     * the coordinate, similar to how DOM event targeting works.</p>
     *
     * <h3>Algorithm</h3>
     * <ol>
     *     <li>Start at the root widget with scene-space coordinates</li>
     *     <li>For each widget, inverse-transform coords via viewport to get local coords</li>
     *     <li>Check if local coords are within bounds (0,0 → w×h)</li>
     *     <li>If hit, recursively check children using the local coords</li>
     *     <li>Return the deepest hit widget</li>
     * </ol>
     *
     * @param root   the root widget to start hit testing from
     * @param mouseX x coordinate in the root's parent space (scene space)
     * @param mouseY y coordinate in the root's parent space (scene space)
     * @return the deepest widget containing the coordinate, or null if none
     */
    public static @Nullable Widget hitTest(Widget root, double mouseX, double mouseY) {
        return hitTestViewport(root, mouseX, mouseY);
    }

    /**
     * Viewport-aware hit testing. Coordinates are transformed through each widget's
     * viewport as we descend the tree.
     *
     * @param root     the root widget to test
     * @param parentX  x in the root's parent coordinate space
     * @param parentY  y in the root's parent coordinate space
     * @return the deepest hit widget, or null
     */
    private static @Nullable Widget hitTestViewport(Widget root, double parentX, double parentY) {
        Deque<ViewportHitFrame> stack = new ArrayDeque<>();
        stack.push(new ViewportHitFrame(root, parentX, parentY));

        while (!stack.isEmpty()) {
            ViewportHitFrame frame = stack.peek();

            // First visit: transform coords through viewport, check bounds
            if (!frame.evaluated) {
                frame.evaluated = true;

                if (!frame.widget.visible()) {
                    stack.pop();
                    continue;
                }

                // Transform parent-space coords to this widget's local space
                FloatPos local = frame.widget.viewport.parentToLocal(frame.parentX, frame.parentY);
                frame.localX = local.x;
                frame.localY = local.y;

                // Check if local coords are within widget bounds
                if (local.x < 0 || local.x > frame.widget.width()
                    || local.y < 0 || local.y > frame.widget.height()) {
                    stack.pop();
                    continue;
                }

                frame.hit = true;
                // Will check children using local coords
            }

            // Try next child (reverse order: later children have priority)
            if (frame.children != null && frame.childIndex >= 0) {
                Widget child = frame.children.get(frame.childIndex--);
                // Child's "parentX/Y" = this widget's local coords + content offset
                double contentX = frame.localX + frame.widget.viewport.contentOffsetX;
                double contentY = frame.localY + frame.widget.viewport.contentOffsetY;
                stack.push(new ViewportHitFrame(child, contentX, contentY));
            } else {
                stack.pop();
                if (frame.hit) {
                    return frame.widget;
                }
            }
        }
        return null;
    }

    /**
     * Stack frame for viewport-aware hit testing.
     */
    private static final class ViewportHitFrame {
        final Widget widget;
        final List<? extends Widget> children;
        final double parentX;
        final double parentY;
        double localX;
        double localY;
        int childIndex;
        boolean evaluated;
        boolean hit;

        ViewportHitFrame(Widget widget, double parentX, double parentY) {
            this.widget = widget;
            this.parentX = parentX;
            this.parentY = parentY;
            this.children = widget instanceof CompositeWidget<?> g ? g.children() : null;
            this.childIndex = children != null ? children.size() - 1 : -1;
        }
    }

    /**
     * Performs hit testing with a custom predicate for fine-grained control.
     * Coordinates are transformed through each widget's viewport as the tree is descended.
     *
     * <p>The predicate receives <b>local</b> coordinates (after viewport transform)
     * and can control:</p>
     * <ul>
     *     <li>Whether a widget counts as a "hit"</li>
     *     <li>Whether to continue testing children</li>
     *     <li>Custom hit detection logic (e.g., non-rectangular shapes)</li>
     * </ul>
     *
     * @param root      the root widget to start hit testing from
     * @param mouseX    x coordinate in root's parent space (scene space)
     * @param mouseY    y coordinate in root's parent space (scene space)
     * @param predicate custom hit test logic — receives local coordinates
     * @return the deepest widget that was hit, or null if none
     */
    public static @Nullable Widget hitTest(Widget root, double mouseX, double mouseY, HitTestPredicate predicate) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(predicate, "predicate");

        Deque<PredicateHitFrame> stack = new ArrayDeque<>();
        stack.push(new PredicateHitFrame(root, mouseX, mouseY));

        while (!stack.isEmpty()) {
            PredicateHitFrame frame = stack.peek();

            // First visit: transform coords and evaluate predicate
            if (!frame.evaluated) {
                frame.evaluated = true;

                // Transform parent-space coords to local space
                FloatPos local = frame.widget.viewport.parentToLocal(frame.parentX, frame.parentY);
                frame.localX = local.x;
                frame.localY = local.y;

                frame.result = predicate.test(frame.widget, local.x, local.y);

                if (frame.result == HitTestResult.MISS) {
                    stack.pop();
                    continue;
                }
                if (frame.result == HitTestResult.HIT_STOP) {
                    return frame.widget;
                }
                // HIT_CONTINUE: will check children
            }

            // Try next child (reverse order: later children have priority)
            if (frame.children != null && frame.childIndex >= 0) {
                Widget child = frame.children.get(frame.childIndex--);
                double contentX = frame.localX + frame.widget.viewport.contentOffsetX;
                double contentY = frame.localY + frame.widget.viewport.contentOffsetY;
                stack.push(new PredicateHitFrame(child, contentX, contentY));
            } else {
                // All children processed, none deeper hit found
                stack.pop();
                if (frame.result == HitTestResult.HIT_CONTINUE) {
                    return frame.widget;
                }
            }
        }
        return null;
    }

    /**
     * Stack frame for predicate-based viewport-aware hit testing.
     */
    private static final class PredicateHitFrame {
        final Widget widget;
        final List<? extends Widget> children;
        final double parentX;
        final double parentY;
        double localX;
        double localY;
        int childIndex;
        boolean evaluated;
        HitTestResult result;

        PredicateHitFrame(Widget widget, double parentX, double parentY) {
            this.widget = widget;
            this.parentX = parentX;
            this.parentY = parentY;
            this.children = widget instanceof CompositeWidget<?> g ? g.children() : null;
            this.childIndex = children != null ? children.size() - 1 : -1;
        }
    }

    /**
     * Performs hit testing and returns the full path from root to the hit widget.
     * Uses viewport-aware coordinate transformation.
     *
     * @param root   the root widget to start hit testing from
     * @param mouseX x coordinate in root's parent space
     * @param mouseY y coordinate in root's parent space
     * @return path from root to hit widget (root→leaf order), or empty path if no hit
     */
    public static WidgetPath hitTestPath(Widget root, double mouseX, double mouseY) {
        Widget hit = hitTest(root, mouseX, mouseY);
        return hit == null ? WidgetPath.empty() : pathToRoot(hit);
    }

    /**
     * Performs hit testing with a custom predicate and returns the full path.
     *
     * @param root      the root widget to start hit testing from
     * @param mouseX    absolute x coordinate
     * @param mouseY    absolute y coordinate
     * @param predicate custom hit test logic
     * @return path from root to hit widget (root→leaf order), or empty path if no hit
     */
    public static WidgetPath hitTestPath(Widget root, double mouseX, double mouseY, HitTestPredicate predicate) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(predicate, "predicate");

        // Find deepest hit widget, then build ancestry using parent links.
        Widget hit = hitTest(root, mouseX, mouseY, predicate);
        return hit == null ? WidgetPath.empty() : pathToRoot(hit);
    }

    /**
     * Collects all widgets at the given coordinates, from deepest to shallowest.
     * Uses viewport-aware coordinate transformation.
     *
     * @param root   the root widget to start from
     * @param mouseX x coordinate in root's parent space
     * @param mouseY y coordinate in root's parent space
     * @param out    list to collect hit widgets into (deepest first)
     * @return number of widgets collected
     */
    public static int hitTestAll(Widget root, double mouseX, double mouseY, List<? super Widget> out) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(out, "out");

        Deque<ViewportHitAllFrame> stack = new ArrayDeque<>();
        stack.push(new ViewportHitAllFrame(root, mouseX, mouseY));
        int count = 0;

        while (!stack.isEmpty()) {
            ViewportHitAllFrame frame = stack.peek();

            // First visit: transform and check bounds
            if (!frame.entered) {
                frame.entered = true;
                if (!frame.widget.visible()) {
                    stack.pop();
                    continue;
                }
                FloatPos local = frame.widget.viewport.parentToLocal(frame.parentX, frame.parentY);
                frame.localX = local.x;
                frame.localY = local.y;
                if (local.x < 0 || local.x > frame.widget.width()
                    || local.y < 0 || local.y > frame.widget.height()) {
                    stack.pop();
                    continue;
                }
            }

            // Try next child (reverse order)
            if (frame.children != null && frame.childIndex >= 0) {
                Widget child = frame.children.get(frame.childIndex--);
                double contentX = frame.localX + frame.widget.viewport.contentOffsetX;
                double contentY = frame.localY + frame.widget.viewport.contentOffsetY;
                stack.push(new ViewportHitAllFrame(child, contentX, contentY));
            } else {
                // Post-order: collect after children
                out.add(frame.widget);
                count++;
                stack.pop();
            }
        }
        return count;
    }

    /**
     * Stack frame for viewport-aware hitTestAll.
     */
    private static final class ViewportHitAllFrame {
        final Widget widget;
        final List<? extends Widget> children;
        final double parentX;
        final double parentY;
        double localX;
        double localY;
        int childIndex;
        boolean entered;

        ViewportHitAllFrame(Widget widget, double parentX, double parentY) {
            this.widget = widget;
            this.parentX = parentX;
            this.parentY = parentY;
            this.children = widget instanceof CompositeWidget<?> g ? g.children() : null;
            this.childIndex = children != null ? children.size() - 1 : -1;
        }
    }

    //endregion

    //region ancestry & path operations

    /**
     * Builds the ancestry path from a widget up to the root.
     *
     * <p>The returned {@link WidgetPath} is in root→leaf order, meaning
     * index 0 is the root and the last index is the starting widget.</p>
     *
     * @param start the widget to start from
     * @return path from root to start (root→leaf order)
     * @throws IllegalStateException if a parent cycle is detected
     */
    public static WidgetPath pathToRoot(Widget start) {
        Objects.requireNonNull(start, "start");
        return WidgetPath.fromLeafToRoot(start, MAX_DEPTH_GUARD);
    }

    /**
     * Computes the common ancestor of two widgets.
     *
     * @param a first widget
     * @param b second widget
     * @return the lowest common ancestor, or null if they have no common ancestor
     */
    public static @Nullable Widget commonAncestor(Widget a, Widget b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        // Build both paths and compute the shared root-side suffix.
        // This yields the LCA in O(min(depth(a), depth(b))).
        WidgetPath pathA = pathToRoot(a);
        WidgetPath pathB = pathToRoot(b);
        return pathA.commonAncestor(pathB);
    }

    /**
     * Checks if {@code ancestor} is an ancestor of {@code widget}.
     *
     * @param widget   the potential descendant
     * @param ancestor the potential ancestor
     * @return true if ancestor is in widget's ancestry chain
     */
    public static boolean isAncestorOf(Widget widget, Widget ancestor) {
        Objects.requireNonNull(widget, "widget");
        Objects.requireNonNull(ancestor, "ancestor");

        Widget current = widget.parent();
        int guard = 0;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.parent();

            if (++guard > MAX_DEPTH_GUARD) {
                throw new IllegalStateException("Possible widget parent-cycle detected");
            }
        }

        return false;
    }

    /**
     * Gets the depth of a widget in its tree (distance from root).
     *
     * @param widget the widget to check
     * @return depth (0 = root, 1 = direct child of root, etc.)
     */
    public static int depthOf(Widget widget) {
        Objects.requireNonNull(widget, "widget");

        int depth = 0;
        Widget current = widget.parent();
        int guard = 0;
        while (current != null) {
            depth++;
            current = current.parent();

            if (++guard > MAX_DEPTH_GUARD) {
                throw new IllegalStateException("Possible widget parent-cycle detected");
            }
        }

        return depth;
    }

    //endregion

    //region tree traversal

    /**
     * Depth-first pre-order traversal (visit node before children).
     *
     * <p>This is the most common traversal order, visiting nodes in the order
     * they would be rendered (parent before children).</p>
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param maxDepth    maximum depth to traverse (-1 for unlimited)
     * @param visitor     callback for each visited widget
     * @return the final traversal control signal
     */
    public static TraversalControl walkPreOrder(Widget root, boolean includeRoot, int maxDepth, Visitor visitor) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(visitor, "visitor");

        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root, 0));

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();

            // First visit: process this node
            if (frame.childIndex == -1) {
                frame.childIndex = 0;

                if (includeRoot || frame.depth > 0) {
                    TraversalControl control = visitor.visit(frame.widget, frame.depth);
                    if (control == TraversalControl.TERMINATE) return TraversalControl.TERMINATE;
                    if (control == TraversalControl.SKIP_CHILDREN) {
                        stack.pop();
                        continue;
                    }
                }
            }

            // Try to push next child
            if (frame.children != null && frame.childIndex < frame.children.size()) {
                Widget child = frame.children.get(frame.childIndex++);
                int childDepth = frame.depth + 1;
                if (maxDepth < 0 || childDepth <= maxDepth) {
                    stack.push(new Frame(child, childDepth));
                }
            } else {
                stack.pop();
            }
        }
        return TraversalControl.CONTINUE;
    }

    /**
     * Stack frame for iterative traversal.
     */
    private static final class Frame {
        final Widget widget;
        final int depth;
        final List<? extends Widget> children;
        int childIndex = -1; // -1 = not yet visited

        Frame(Widget widget, int depth) {
            this.widget = widget;
            this.depth = depth;
            this.children = widget instanceof CompositeWidget<?> g ? g.children() : null;
        }
    }

    /**
     * Depth-first pre-order traversal with ancestry path information.
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param maxDepth    maximum depth to traverse (-1 for unlimited)
     * @param visitor     callback for each visited widget with path
     * @return the final traversal control signal
     */
    public static TraversalControl walkPreOrderWithPath(Widget root, boolean includeRoot, int maxDepth, PathVisitor visitor) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(visitor, "visitor");

        Deque<Frame> stack = new ArrayDeque<>();
        List<Widget> ancestry = new ArrayList<>();
        AncestryViewList view = new AncestryViewList(ancestry);

        stack.push(new Frame(root, 0));
        ancestry.add(root);

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();

            // First visit: process this node
            if (frame.childIndex == -1) {
                frame.childIndex = 0;

                if (includeRoot || frame.depth > 0) {
                    TraversalControl control = visitor.visit(frame.widget, frame.depth, view);
                    if (control == TraversalControl.TERMINATE) return TraversalControl.TERMINATE;
                    if (control == TraversalControl.SKIP_CHILDREN) {
                        stack.pop();
                        ancestry.removeLast();
                        continue;
                    }
                }
            }

            // Try to push next child
            if (frame.children != null && frame.childIndex < frame.children.size()) {
                Widget child = frame.children.get(frame.childIndex++);
                int childDepth = frame.depth + 1;
                if (maxDepth < 0 || childDepth <= maxDepth) {
                    stack.push(new Frame(child, childDepth));
                    ancestry.add(child);
                }
            } else {
                stack.pop();
                ancestry.removeLast();
            }
        }
        return TraversalControl.CONTINUE;
    }

    /**
     * Depth-first post-order traversal (visit children before node).
     *
     * <p>This order ensures children are visited before their parent,
     * useful for operations like deletion or size computation.</p>
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param maxDepth    maximum depth to traverse (-1 for unlimited)
     * @param visitor     callback for each visited widget
     * @return the final traversal control signal
     */
    public static TraversalControl walkPostOrder(Widget root, boolean includeRoot, int maxDepth, Visitor visitor) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(visitor, "visitor");

        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root, 0));

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();

            // First visit: just mark as entered
            if (frame.childIndex == -1) {
                frame.childIndex = 0;
            }

            // Try to push next child (depth-first into children)
            boolean pushed = false;
            while (frame.children != null && frame.childIndex < frame.children.size()) {
                Widget child = frame.children.get(frame.childIndex++);
                int childDepth = frame.depth + 1;
                if (maxDepth < 0 || childDepth <= maxDepth) {
                    stack.push(new Frame(child, childDepth));
                    pushed = true;
                    break;
                }
            }

            if (!pushed) {
                // Post-order: visit after all children
                stack.pop();
                if (includeRoot || frame.depth > 0) {
                    TraversalControl control = visitor.visit(frame.widget, frame.depth);
                    if (control == TraversalControl.TERMINATE) return TraversalControl.TERMINATE;
                }
            }
        }
        return TraversalControl.CONTINUE;
    }

    /**
     * Breadth-first (level-order) traversal.
     *
     * <p>Visits all widgets at depth N before any widget at depth N+1.
     * Useful for operations that need to process the tree level by level.</p>
     *
     * <p>Uses double-buffering (two lists alternating) for optimal memory usage:
     * only the current level and next level are kept in memory at any time,
     * giving O(w) space complexity where w is the maximum tree width.</p>
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param maxDepth    maximum depth to traverse (-1 for unlimited)
     * @param visitor     callback for each visited widget
     * @return the final traversal control signal
     */
    public static TraversalControl walkBreadthFirst(Widget root, boolean includeRoot, int maxDepth, Visitor visitor) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(visitor, "visitor");

        // Double-buffering: swap between current and next level
        List<Widget> current = new ArrayList<>();
        List<Widget> next = new ArrayList<>();
        current.add(root);
        int depth = 0;

        while (!current.isEmpty()) {
            for (int i = 0, n = current.size(); i < n; i++) {
                Widget w = current.get(i);

                TraversalControl control = TraversalControl.CONTINUE;
                if (includeRoot || depth > 0) {
                    control = visitor.visit(w, depth);
                    if (control == TraversalControl.TERMINATE) return TraversalControl.TERMINATE;
                }

                // Collect children for next level
                if (control != TraversalControl.SKIP_CHILDREN && (maxDepth < 0 || depth < maxDepth)) {
                    if (w instanceof CompositeWidget<?> g) {
                        next.addAll(g.children());
                    }
                }
            }

            // Swap and clear: reuse the old 'current' list as the new 'next'
            List<Widget> tmp = current;
            current = next;
            next = tmp;
            next.clear();
            depth++;
        }
        return TraversalControl.CONTINUE;
    }

    /**
     * Traverses children of a widget in reverse order (last to first).
     *
     * <p>This is useful for hit testing and rendering where later children
     * should have visual priority over earlier ones.</p>
     *
     * @param parent  the parent widget
     * @param visitor callback for each child
     * @return the final traversal control signal
     */
    public static TraversalControl walkChildrenReverse(Widget parent, Visitor visitor) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(visitor, "visitor");

        if (!(parent instanceof CompositeWidget<?> group)) {
            return TraversalControl.CONTINUE;
        }

        List<? extends Widget> children = group.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            TraversalControl control = visitor.visit(children.get(i), 0);
            if (control == TraversalControl.TERMINATE) {
                return TraversalControl.TERMINATE;
            }
        }

        return TraversalControl.CONTINUE;
    }

    //endregion

    //region query operations

    /**
     * Finds the first widget matching a predicate in pre-order.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth to search (-1 for unlimited)
     * @param predicate   condition to match
     * @return the first matching widget, or null if none found
     */
    public static @Nullable Widget findFirst(Widget root, boolean includeRoot, int maxDepth, Predicate<? super Widget> predicate) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(predicate, "predicate");

        Widget[] result = new Widget[1];
        walkPreOrder(root, includeRoot, maxDepth, (widget, depth) -> {
            if (predicate.test(widget)) {
                result[0] = widget;
                return TraversalControl.TERMINATE;
            }
            return TraversalControl.CONTINUE;
        });

        return result[0];
    }

    /**
     * Finds the first widget matching a predicate, with depth information.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth to search (-1 for unlimited)
     * @param predicate   condition to match (widget, depth) -> boolean
     * @return the first matching widget, or null if none found
     */
    public static @Nullable Widget findFirst(Widget root, boolean includeRoot, int maxDepth, BiPredicate<Widget, Integer> predicate) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(predicate, "predicate");

        Widget[] result = new Widget[1];
        walkPreOrder(root, includeRoot, maxDepth, (widget, depth) -> {
            if (predicate.test(widget, depth)) {
                result[0] = widget;
                return TraversalControl.TERMINATE;
            }
            return TraversalControl.CONTINUE;
        });

        return result[0];
    }

    /**
     * Collects all widgets matching a predicate into a list.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth to search (-1 for unlimited)
     * @param predicate   condition to match
     * @param out         list to collect results into
     * @return number of widgets collected
     */
    public static int collectInto(Widget root, boolean includeRoot, int maxDepth,
                                  Predicate<? super Widget> predicate, List<? super Widget> out) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(out, "out");

        int[] count = {0};
        walkPreOrder(root, includeRoot, maxDepth, (widget, depth) -> {
            if (predicate.test(widget)) {
                out.add(widget);
                count[0]++;
            }
            return TraversalControl.CONTINUE;
        });

        return count[0];
    }

    /**
     * Collects all widgets matching a predicate into a new list.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth to search (-1 for unlimited)
     * @param predicate   condition to match
     * @return list of matching widgets
     */
    public static List<Widget> collect(Widget root, boolean includeRoot, int maxDepth, Predicate<? super Widget> predicate) {
        List<Widget> result = new ArrayList<>();
        collectInto(root, includeRoot, maxDepth, predicate, result);
        return result;
    }

    /**
     * Counts widgets matching a predicate.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth to search (-1 for unlimited)
     * @param predicate   condition to match
     * @return number of matching widgets
     */
    public static int count(Widget root, boolean includeRoot, int maxDepth, Predicate<? super Widget> predicate) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(predicate, "predicate");

        int[] count = {0};
        walkPreOrder(root, includeRoot, maxDepth, (widget, depth) -> {
            if (predicate.test(widget)) {
                count[0]++;
            }
            return TraversalControl.CONTINUE;
        });

        return count[0];
    }

    /**
     * Counts all widgets in a subtree.
     *
     * @param root        the root widget to count from
     * @param includeRoot whether to count the root
     * @return total number of widgets
     */
    public static int countAll(Widget root, boolean includeRoot) {
        return count(root, includeRoot, -1, w -> true);
    }

    //endregion

    //region leaf-first traversal

    /**
     * Traverses the widget tree from leaves to root (bottom-up).
     *
     * <p>This traversal visits all leaf widgets first, then their parents,
     * continuing upward until the root is visited. Widgets at the same level
     * are visited in the order determined by the tree structure.</p>
     *
     * <p>This is ideal for destruction/cleanup operations where children
     * must be destroyed before their parents to avoid dangling references.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // For a tree: Root -> [Panel1 -> [A, B], Panel2 -> [C]]
     * // Visit order: A, B, Panel1, C, Panel2, Root
     * // (all leaves first within each subtree, then parents)
     * }</pre>
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param consumer    action to perform on each widget
     */
    public static void bottomUp(Widget root, boolean includeRoot, Consumer<? super Widget> consumer) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(consumer, "consumer");

        walkPostOrder(root, includeRoot, -1, (widget, depth) -> {
            consumer.accept(widget);
            return TraversalControl.CONTINUE;
        });
    }

    /**
     * Traverses the widget tree from leaves to root with traversal control.
     *
     * <p>Similar to {@link #bottomUp(Widget, boolean, Consumer)} but
     * allows early termination or skipping of subtrees.</p>
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param maxDepth    maximum depth to traverse (-1 for unlimited)
     * @param visitor     callback for each visited widget
     * @return the final traversal control signal
     * @see #walkPostOrder(Widget, boolean, int, Visitor)
     */
    public static TraversalControl walkBottomUp(Widget root, boolean includeRoot, int maxDepth, Visitor visitor) {
        return walkPostOrder(root, includeRoot, maxDepth, visitor);
    }

    /**
     * Traverses the widget tree level by level from deepest to shallowest (reverse BFS).
     *
     * <p>This traversal visits all widgets at the maximum depth first, then
     * all widgets at depth-1, and so on until the root level. Within each level,
     * widgets are visited in their natural order.</p>
     *
     * <p>This is useful for batch destruction operations where you want to
     * process all widgets at the same depth together before moving to parents.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // For a tree: Root -> [Panel1 -> [A, B], Panel2 -> [C]]
     * // Visit order by level: [A, B, C] (depth 2), [Panel1, Panel2] (depth 1), [Root] (depth 0)
     * }</pre>
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param consumer    action to perform on each widget
     */
    public static void deepestFirst(Widget root, boolean includeRoot, Consumer<? super Widget> consumer) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(consumer, "consumer");

        // First, collect all widgets level by level
        List<List<Widget>> levels = new ArrayList<>();
        List<Widget> current = new ArrayList<>();
        List<Widget> next = new ArrayList<>();
        current.add(root);
        int depth = 0;

        while (!current.isEmpty()) {
            List<Widget> levelWidgets = new ArrayList<>(current);
            levels.add(levelWidgets);

            for (Widget w : current) {
                if (w instanceof CompositeWidget<?> g) {
                    next.addAll(g.children());
                }
            }

            List<Widget> tmp = current;
            current = next;
            next = tmp;
            next.clear();
            depth++;
        }

        // Visit from deepest level to shallowest
        for (int i = levels.size() - 1; i >= 0; i--) {
            if (i == 0 && !includeRoot) {
                continue;
            }
            for (Widget widget : levels.get(i)) {
                consumer.accept(widget);
            }
        }
    }

    /**
     * Traverses the widget tree level by level from deepest to shallowest with traversal control.
     *
     * <p>Similar to {@link #deepestFirst(Widget, boolean, Consumer)} but allows
     * early termination. Note that {@link TraversalControl#SKIP_CHILDREN} has no effect
     * in this traversal mode since children are always visited before parents.</p>
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param visitor     callback for each visited widget
     * @return the final traversal control signal
     */
    public static TraversalControl walkDeepestFirst(Widget root, boolean includeRoot, Visitor visitor) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(visitor, "visitor");

        // First, collect all widgets level by level with depth info
        List<List<Widget>> levels = new ArrayList<>();
        List<Widget> current = new ArrayList<>();
        List<Widget> next = new ArrayList<>();
        current.add(root);

        while (!current.isEmpty()) {
            levels.add(new ArrayList<>(current));

            for (Widget w : current) {
                if (w instanceof CompositeWidget<?> g) {
                    next.addAll(g.children());
                }
            }

            List<Widget> tmp = current;
            current = next;
            next = tmp;
            next.clear();
        }

        // Visit from deepest level to shallowest
        for (int i = levels.size() - 1; i >= 0; i--) {
            int depth = i;
            if (depth == 0 && !includeRoot) {
                continue;
            }
            for (Widget widget : levels.get(i)) {
                TraversalControl control = visitor.visit(widget, depth);
                if (control == TraversalControl.TERMINATE) {
                    return TraversalControl.TERMINATE;
                }
            }
        }
        return TraversalControl.CONTINUE;
    }

    //endregion

    /**
     * Internal implementation of AncestryView backed by a List.
     */
    private record AncestryViewList(List<Widget> ancestry) implements AncestryView {

        @Override
        public int size() {
            return ancestry.size();
        }

        @Override
        public Widget get(int indexFromRoot) {
            return ancestry.get(indexFromRoot);
        }
    }
}
