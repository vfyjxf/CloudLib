package dev.vfyjxf.cloudlib.api.ui.base;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An immutable path of widgets from the root to a leaf widget.
 *
 * <h2>Storage Order</h2>
 * <p>Widgets are stored from root → leaf (index 0 is the root, index size-1 is the leaf/widget).
 * This matches the natural order of capture-phase event handling.</p>
 *
 * <h2>Iteration</h2>
 * <p>The path is iterable in root→leaf order.</p>
 *
 * <h2>Null Elements</h2>
 * <p>Null elements are not allowed. Adding a null widget will throw {@link NullPointerException}.</p>
 *
 * @see WidgetTree#pathToRoot(Widget)
 */
public final class WidgetPath implements Iterable<Widget> {

    private static final WidgetPath EMPTY = new WidgetPath(new Widget[0], 0, false);

    private final Widget[] nodes;
    private final int size;

    /**
     * Private constructor for internal use.
     *
     * @param nodes      array of widgets (will NOT be cloned if trusted)
     * @param size       number of elements
     * @param cloneArray whether to clone the array
     */
    private WidgetPath(Widget[] nodes, int size, boolean cloneArray) {
        this.nodes = cloneArray ? Arrays.copyOf(nodes, size) : nodes;
        this.size = size;
    }

    /**
     * Returns an empty path.
     *
     * @return the singleton empty path
     */
    public static WidgetPath empty() {
        return EMPTY;
    }

    /**
     * Creates a path containing a single widget.
     *
     * @param widget the widget (must not be null)
     * @return a path containing just this widget
     */
    public static WidgetPath of(Widget widget) {
        Objects.requireNonNull(widget, "widget");
        return new WidgetPath(new Widget[]{widget}, 1, false);
    }

    /**
     * Creates a path from an array of widgets in root→leaf order.
     *
     * @param widgets widgets from root to leaf (must not contain nulls)
     * @return a new path
     * @throws NullPointerException if any widget is null
     */
    public static WidgetPath of(Widget... widgets) {
        if (widgets.length == 0) return EMPTY;
        for (Widget w : widgets) {
            Objects.requireNonNull(w, "widget must not be null");
        }
        return new WidgetPath(widgets, widgets.length, true);
    }

    /**
     * Creates a path by traversing from the given widget up to the root.
     *
     * <p>The returned path is in root→leaf order.</p>
     *
     * @param leaf the starting widget
     * @return path from root to leaf
     * @throws IllegalStateException if a parent cycle is detected
     */
    static WidgetPath fromLeafToRoot(Widget leaf, int maxDepth) {
        Objects.requireNonNull(leaf, "leaf");

        // First pass: count depth
        int depth = 0;
        Widget current = leaf;
        while (current != null) {
            depth++;
            current = current.parent();
            if (depth > maxDepth) {
                throw new IllegalStateException("Possible widget parent-cycle detected starting from: " + leaf);
            }
        }

        if (depth == 0) return EMPTY;

        // Second pass: fill array in root→leaf order
        Widget[] arr = new Widget[depth];
        current = leaf;
        for (int i = depth - 1; i >= 0; i--) {
            arr[i] = current;
            current = current.parent();
        }

        return new WidgetPath(arr, depth, false);
    }

    /**
     * Creates a path from an AncestryView (root→current order).
     *
     * @param ancestry the ancestry view
     * @return a new path
     */
    static WidgetPath fromAncestry(WidgetTree.AncestryView ancestry) {
        int size = ancestry.size();
        if (size == 0) return EMPTY;

        Widget[] arr = new Widget[size];
        for (int i = 0; i < size; i++) {
            arr[i] = ancestry.get(i);
        }
        return new WidgetPath(arr, size, false);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public @Nullable Widget leaf() {
        return size == 0 ? null : nodes[size - 1];
    }

    public @Nullable Widget root() {
        return size == 0 ? null : nodes[0];
    }

    /**
     * Gets widget at {@code indexFromRoot} where 0 is the root and {@code size-1} is the leaf.
     *
     * @param indexFromRoot 0-based index from root (0 = root, size-1 = leaf)
     * @return the widget at the specified position
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Widget get(int indexFromRoot) {
        if (indexFromRoot < 0 || indexFromRoot >= size) {
            throw new IndexOutOfBoundsException(indexFromRoot + " (size=" + size + ")");
        }
        return nodes[indexFromRoot];
    }

    /**
     * Returns an iterator over the widgets in root→leaf order.
     *
     * @return an iterator from root to leaf
     */
    @Override
    public Iterator<Widget> iterator() {
        return new PathIterator(0, size, 1);
    }

    /**
     * Returns an iterator over the widgets in leaf→root order (reverse).
     *
     * <p>This is useful for bubble-phase event handling where you want
     * to process descendants before ancestors.</p>
     *
     * @return an iterator from leaf to root
     */
    public Iterator<Widget> reverseIterator() {
        return new PathIterator(size - 1, -1, -1);
    }

    /**
     * Returns a sequential stream of widgets in root→leaf order.
     *
     * @return a stream of widgets
     */
    public Stream<Widget> stream() {
        return StreamSupport.stream(spliterator(), false);
    }


    @Override
    public Spliterator<Widget> spliterator() {
        return Spliterators.spliterator(nodes, 0, size,
            Spliterator.ORDERED | Spliterator.SIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE);
    }

    /**
     * Checks if this path contains the specified widget.
     *
     * @param widget the widget to check
     * @return true if the widget is in this path
     */
    public boolean contains(Widget widget) {
        Objects.requireNonNull(widget, "widget");
        for (int i = 0; i < size; i++) {
            if (nodes[i] == widget) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the index of the specified widget in this path.
     *
     * @param widget the widget to find
     * @return the index from root (0-based), or -1 if not found
     */
    public int indexOf(Widget widget) {
        Objects.requireNonNull(widget, "widget");
        for (int i = 0; i < size; i++) {
            if (nodes[i] == widget) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if any widget in this path matches the given predicate.
     *
     * @param predicate the predicate to test widgets with
     * @return true if any widget matches
     */
    public boolean anyMatch(Predicate<? super Widget> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        for (int i = 0; i < size; i++) {
            if (predicate.test(nodes[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if all widgets in this path match the given predicate.
     *
     * @param predicate the predicate to test widgets with
     * @return true if all widgets match (or path is empty)
     */
    public boolean allMatch(Predicate<? super Widget> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        for (int i = 0; i < size; i++) {
            if (!predicate.test(nodes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the first widget in this path that matches the predicate.
     *
     * @param predicate the predicate to test widgets with
     * @return the first matching widget, or null if none match
     */
    public @Nullable Widget findFirst(Predicate<? super Widget> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        for (int i = 0; i < size; i++) {
            if (predicate.test(nodes[i])) {
                return nodes[i];
            }
        }
        return null;
    }

    /**
     * Creates a subpath from the root down to (but not including) the specified descendant.
     *
     * <p>For example, if the path is [Root, Container, Panel, Button] and you call
     * {@code subPath(panel)}, you get [Root, Container].</p>
     *
     * @param descendant the descendant widget to stop at (exclusive)
     * @return a new path containing widgets from root to descendant (exclusive)
     * @throws IllegalArgumentException if descendant is not in this path
     */
    public WidgetPath subPath(Widget descendant) {
        Objects.requireNonNull(descendant, "descendant");
        int descendantIndex = indexOf(descendant);
        if (descendantIndex < 0) {
            throw new IllegalArgumentException("Descendant not in path: " + descendant);
        }
        return subPath(0, descendantIndex);
    }

    /**
     * Creates a subpath with the specified range [fromIndex, toIndex).
     *
     * @param fromIndex starting index from root (inclusive)
     * @param toIndex   ending index from root (exclusive)
     * @return a new path containing the specified range
     * @throws IndexOutOfBoundsException if indices are out of range
     */
    public WidgetPath subPath(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException(
                "fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", size: " + size);
        }

        int newSize = toIndex - fromIndex;
        if (newSize == 0) {
            return EMPTY;
        }

        Widget[] newNodes = Arrays.copyOfRange(nodes, fromIndex, toIndex);
        return new WidgetPath(newNodes, newSize, false);
    }

    /**
     * Computes the length of the common head (root-side prefix) shared by this path and {@code other}.
     *
     * <p>Because {@link WidgetPath} is stored in root→leaf order, the shared ancestor chain
     * (from the root down to the lowest common ancestor) appears as a prefix at the beginning of the array.
     * This method compares from the root side to find the shared prefix length in O(min(n, m)).</p>
     *
     * <p>Identity comparison ({@code ==}) is used, consistent with {@link #contains(Widget)} and {@link #equals(Object)}.</p>
     *
     * @param other the other path
     * @return number of shared widgets at the root side (0 if none or either path is empty)
     */
    public int commonHeadLength(WidgetPath other) {
        Objects.requireNonNull(other, "other");
        if (this.size == 0 || other.size == 0) return 0;

        int count = 0;
        int minLen = Math.min(this.size, other.size);
        while (count < minLen && this.nodes[count] == other.nodes[count]) {
            count++;
        }
        return count;
    }

    /**
     * Finds the lowest common ancestor (nearest common ancestor) of two paths.
     *
     * <p>If both paths come from {@link WidgetTree#pathToRoot(Widget)} for two widgets in the same tree,
     * this returns their LCA. If they share no ancestor (different trees), returns {@code null}.</p>
     *
     * @param other the other path
     * @return the lowest common ancestor widget, or {@code null} if none
     */
    public @Nullable Widget commonAncestor(WidgetPath other) {
        Objects.requireNonNull(other, "other");
        int head = commonHeadLength(other);
        if (head == 0) return null;
        return this.nodes[head - 1];
    }

    /**
     * Returns the index-from-root of the lowest common ancestor between this path and {@code other}.
     *
     * @param other the other path
     * @return index from root of the LCA, or -1 if none
     */
    public int commonAncestorIndex(WidgetPath other) {
        Objects.requireNonNull(other, "other");
        int head = commonHeadLength(other);
        return head == 0 ? -1 : (head - 1);
    }

    /**
     * Returns the shared ancestor chain of two paths as a new {@link WidgetPath}.
     *
     * <p>The returned path is in root→leaf order and its leaf is the lowest common ancestor.
     * If there is no common ancestor, returns an empty path.</p>
     *
     * @param other the other path
     * @return a path representing [Root .. LCA] (root→leaf), or empty if none
     */
    public WidgetPath commonHead(WidgetPath other) {
        Objects.requireNonNull(other, "other");
        int head = commonHeadLength(other);
        if (head == 0) return EMPTY;
        return subPath(0, head);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WidgetPath other)) return false;
        if (size != other.size) return false;
        for (int i = 0; i < size; i++) {
            if (nodes[i] != other.nodes[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < size; i++) {
            result = 31 * result + System.identityHashCode(nodes[i]);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WidgetPath[");
        for (int i = 0; i < size; i++) {
            if (i != 0) sb.append(" -> ");
            Widget w = nodes[i];
            sb.append(w == null ? "null" : w.key());
        }
        return sb.append(']').toString();
    }

    /**
     * Iterator implementation for WidgetPath.
     */
    private final class PathIterator implements Iterator<Widget> {
        private int current;
        private final int end;
        private final int step;

        PathIterator(int start, int end, int step) {
            this.current = start;
            this.end = end;
            this.step = step;
        }

        @Override
        public boolean hasNext() {
            return step > 0 ? current < end : current > end;
        }

        @Override
        public Widget next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Widget widget = nodes[current];
            current += step;
            return widget;
        }

        @Override
        public void forEachRemaining(Consumer<? super Widget> action) {
            Objects.requireNonNull(action, "action");
            if (step > 0) {
                while (current < end) {
                    action.accept(nodes[current]);
                    current += step;
                }
            } else {
                while (current > end) {
                    action.accept(nodes[current]);
                    current += step;
                }
            }
        }
    }
}
