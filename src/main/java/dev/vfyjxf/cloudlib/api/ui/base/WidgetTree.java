package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Utility class for traversing and querying widget trees.
 * <p>
 * Provides hit testing, DFS/BFS traversal, and ancestry operations
 * for the {@link Widget} hierarchy.
 *
 * @see Widget
 * @see CompositeWidget
 * @see WidgetPath
 */
public final class WidgetTree {

    private WidgetTree() {
    }

    /**
     * Maximum tree depth to prevent stack overflow from cycles.
     */
    private static final int MAX_DEPTH_GUARD = 1_000_000;

    //region visitor & view

    /**
     * Controls the traversal flow during tree walks.
     */
    public enum TraversalControl {
        /**
         * Continue traversal normally, visiting children and siblings.
         */
        proceed,

        /**
         * Skip the subtree of the current node but continue with siblings.
         */
        skipChildren,

        /**
         * Stop the entire traversal immediately.
         */
        terminate
    }

    /**
     * Visitor for tree traversal with depth information.
     */
    @FunctionalInterface
    public interface Visitor {
        /**
         * @param widget the current widget being visited
         * @param depth  the depth of the widget (0 = root of traversal)
         * @return control signal for traversal flow
         */
        TraversalControl visit(Widget widget, int depth);
    }

    /**
     * Visitor that receives the full path from root to the current widget.
     * <p>
     * The {@link AncestryView} is only valid during the callback. Do not store references to it.
     */
    @FunctionalInterface
    public interface PathVisitor {
        /**
         * @param widget   the current widget being visited
         * @param depth    the depth of the widget (0 = root of traversal)
         * @param ancestry mutable view of path from root to current widget
         * @return control signal for traversal flow
         */
        TraversalControl visit(Widget widget, int depth, AncestryView ancestry);
    }

    /**
     * Predicate for hit testing that can short-circuit traversal.
     */
    @FunctionalInterface
    public interface HitTestPredicate {
        /**
         * @param widget the widget to test
         * @param localX x coordinate in widget's local space (after viewport transform)
         * @param localY y coordinate in widget's local space (after viewport transform)
         * @return {@link HitTestResult} indicating whether this is a hit and if children should be tested
         */
        HitTestResult test(Widget widget, double localX, double localY);
    }

    /**
     * A read-only view of the ancestry path during traversal.
     * <p>
     * The path is ordered from root (index 0) to the current widget (index size-1).
     * This view is only valid during the visitor callback and should not be stored.
     */
    public interface AncestryView {
        int size();

        /**
         * @param indexFromRoot 0-based index where 0 is the root
         */
        Widget get(int indexFromRoot);

        default int depth() {
            return size() - 1;
        }

        default Widget root() {
            return get(0);
        }

        default Widget current() {
            return get(size() - 1);
        }

        /**
         * Copies this ancestry to a new {@link WidgetPath}.
         * Unlike this view, the returned path is safe to store.
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
         */
        descend,

        /**
         * Widget is hit but children should NOT be tested.
         */
        stop,

        /**
         * Widget is not hit, skip this branch entirely.
         */
        miss
    }

    /**
     * Finds the deepest widget at the given coordinates.
     * <p>
     * Traverses the widget tree in depth-first order, transforming
     * mouse coordinates through each widget's viewport as it descends.
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
     * Viewport-aware hit testing implementation.
     */
    private static @Nullable Widget hitTestViewport(Widget root, double parentX, double parentY) {
        Deque<ViewportHitFrame> stack = new ArrayDeque<>();
        stack.push(new ViewportHitFrame(root, parentX, parentY));

        while (!stack.isEmpty()) {
            ViewportHitFrame frame = stack.peek();

            // First visit: transform coords through viewport, check bounds
            if (!frame.evaluated) {
                frame.evaluated = true;

                if (!frame.widget.visible() || !frame.widget.interactive()) {
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
                if (child.coordinateSpace != CoordinateSpace.parent) continue;
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
     * Finds the deepest widget at the given coordinates with a custom predicate.
     * <p>
     * The predicate receives local coordinates (after viewport transform)
     * and controls whether a widget counts as a hit and whether to continue testing children.
     *
     * @param root      the root widget to start hit testing from
     * @param mouseX    x coordinate in root's parent space (scene space)
     * @param mouseY    y coordinate in root's parent space (scene space)
     * @param predicate custom hit test logic
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

                if (frame.result == HitTestResult.miss) {
                    stack.pop();
                    continue;
                }
                if (frame.result == HitTestResult.stop) {
                    return frame.widget;
                }
                // HIT_CONTINUE: will check children
            }

            // Try next child (reverse order: later children have priority)
            if (frame.children != null && frame.childIndex >= 0) {
                Widget child = frame.children.get(frame.childIndex--);
                if (child.coordinateSpace != CoordinateSpace.parent) continue;
                double contentX = frame.localX + frame.widget.viewport.contentOffsetX;
                double contentY = frame.localY + frame.widget.viewport.contentOffsetY;
                stack.push(new PredicateHitFrame(child, contentX, contentY));
            } else {
                // All children processed, none deeper hit found
                stack.pop();
                if (frame.result == HitTestResult.descend) {
                    return frame.widget;
                }
            }
        }
        return null;
    }

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
     * Finds the hit widget and returns the full path from root to it.
     *
     * @param root   the root widget to start hit testing from
     * @param mouseX x coordinate in root's parent space
     * @param mouseY y coordinate in root's parent space
     * @return path from root to hit widget, or empty path if no hit
     */
    public static WidgetPath hitTestPath(Widget root, double mouseX, double mouseY) {
        Widget hit = hitTest(root, mouseX, mouseY);
        return hit == null ? WidgetPath.empty() : pathToRoot(hit);
    }

    /**
     * Finds the hit widget with a custom predicate and returns the full path.
     *
     * @param root      the root widget to start hit testing from
     * @param mouseX    x coordinate in root's parent space
     * @param mouseY    y coordinate in root's parent space
     * @param predicate custom hit test logic
     * @return path from root to hit widget, or empty path if no hit
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
                if (child.coordinateSpace != CoordinateSpace.parent) continue;
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
     * <p>
     * The returned path is in root→leaf order.
     *
     * @param start the widget to start from
     * @return path from root to start
     */
    public static WidgetPath pathToRoot(Widget start) {
        Objects.requireNonNull(start, "start");
        return WidgetPath.fromLeafToRoot(start, MAX_DEPTH_GUARD);
    }

    /**
     * Computes the lowest common ancestor of two widgets.
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
     * @param widget   the potential descendant
     * @param ancestor the potential ancestor
     * @return true if {@code ancestor} is in widget's ancestry chain
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
                    if (control == TraversalControl.terminate) return TraversalControl.terminate;
                    if (control == TraversalControl.skipChildren) {
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
        return TraversalControl.proceed;
    }

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
                    if (control == TraversalControl.terminate) return TraversalControl.terminate;
                    if (control == TraversalControl.skipChildren) {
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
        return TraversalControl.proceed;
    }

    /**
     * Depth-first post-order traversal (visit children before node).
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
                    if (control == TraversalControl.terminate) return TraversalControl.terminate;
                }
            }
        }
        return TraversalControl.proceed;
    }

    /**
     * Breadth-first (level-order) traversal.
     * <p>
     * Visits all widgets at depth N before any widget at depth N+1.
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

                TraversalControl control = TraversalControl.proceed;
                if (includeRoot || depth > 0) {
                    control = visitor.visit(w, depth);
                    if (control == TraversalControl.terminate) return TraversalControl.terminate;
                }

                // Collect children for next level
                if (control != TraversalControl.skipChildren && (maxDepth < 0 || depth < maxDepth)) {
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
        return TraversalControl.proceed;
    }

    /**
     * Traverses children of a widget in reverse order (last to first).
     *
     * @param parent  the parent widget
     * @param visitor callback for each child
     * @return the final traversal control signal
     */
    public static TraversalControl walkChildrenReverse(Widget parent, Visitor visitor) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(visitor, "visitor");

        if (!(parent instanceof CompositeWidget<?> group)) {
            return TraversalControl.proceed;
        }

        List<? extends Widget> children = group.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            TraversalControl control = visitor.visit(children.get(i), 0);
            if (control == TraversalControl.terminate) {
                return TraversalControl.terminate;
            }
        }

        return TraversalControl.proceed;
    }

    //endregion

    //region query operations

    /**
     * Finds the first widget matching a predicate in pre-order.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth (-1 for unlimited)
     * @param predicate   condition to match
     * @return the first matching widget, or null
     */
    public static @Nullable Widget findFirst(Widget root, boolean includeRoot, int maxDepth, Predicate<? super Widget> predicate) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(predicate, "predicate");

        Widget[] result = new Widget[1];
        walkPreOrder(root, includeRoot, maxDepth, (widget, depth) -> {
            if (predicate.test(widget)) {
                result[0] = widget;
                return TraversalControl.terminate;
            }
            return TraversalControl.proceed;
        });

        return result[0];
    }

    /**
     * Finds the first widget matching a predicate, with depth information.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth (-1 for unlimited)
     * @param predicate   condition to match (widget, depth) -&gt; boolean
     * @return the first matching widget, or null
     */
    public static @Nullable Widget findFirst(Widget root, boolean includeRoot, int maxDepth, BiPredicate<Widget, Integer> predicate) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(predicate, "predicate");

        Widget[] result = new Widget[1];
        walkPreOrder(root, includeRoot, maxDepth, (widget, depth) -> {
            if (predicate.test(widget, depth)) {
                result[0] = widget;
                return TraversalControl.terminate;
            }
            return TraversalControl.proceed;
        });

        return result[0];
    }

    /**
     * Collects all widgets matching a predicate into a list.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth (-1 for unlimited)
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
            return TraversalControl.proceed;
        });

        return count[0];
    }

    /**
     * Collects all widgets matching a predicate into a new list.
     *
     * @param root        the root widget to search from
     * @param includeRoot whether to consider the root
     * @param maxDepth    maximum depth (-1 for unlimited)
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
     * @param maxDepth    maximum depth (-1 for unlimited)
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
            return TraversalControl.proceed;
        });

        return count[0];
    }

    /**
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
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param consumer    action to perform on each widget
     */
    public static void bottomUp(Widget root, boolean includeRoot, Consumer<? super Widget> consumer) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(consumer, "consumer");

        walkPostOrder(root, includeRoot, -1, (widget, depth) -> {
            consumer.accept(widget);
            return TraversalControl.proceed;
        });
    }

    /**
     * Traverses the widget tree from leaves to root with traversal control.
     *
     * @param root        the root widget to start from
     * @param includeRoot whether to visit the root itself
     * @param maxDepth    maximum depth to traverse (-1 for unlimited)
     * @param visitor     callback for each visited widget
     * @return the final traversal control signal
     */
    public static TraversalControl walkBottomUp(Widget root, boolean includeRoot, int maxDepth, Visitor visitor) {
        return walkPostOrder(root, includeRoot, maxDepth, visitor);
    }

    /**
     * Traverses the widget tree level by level from deepest to shallowest (reverse BFS).
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
        for (int depth = levels.size() - 1; depth >= 0; depth--) {
            if (depth == 0 && !includeRoot) {
                continue;
            }
            for (Widget widget : levels.get(depth)) {
                TraversalControl control = visitor.visit(widget, depth);
                if (control == TraversalControl.terminate) {
                    return TraversalControl.terminate;
                }
            }
        }
        return TraversalControl.proceed;
    }

    //endregion

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
