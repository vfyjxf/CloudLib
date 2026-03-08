package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Immutable snapshot of a widget tree for diff comparisons.
 */
public final class TreeSnapshot {

    private final Node root;

    private TreeSnapshot(Node root) {
        this.root = root;
    }

    public static TreeSnapshot of(Widget root) {
        return new TreeSnapshot(snapshotNode(root));
    }

    public Node root() {
        return root;
    }

    public @Nullable Node findByKey(Object key) {
        return findByKey(root, key);
    }

    public List<Node> findByType(String typeName) {
        List<Node> results = new ArrayList<>();
        collectByType(root, typeName, results);
        return results;
    }

    /**
     * Prints a human-readable tree representation.
     */
    public String print() {
        StringBuilder sb = new StringBuilder();
        printNode(sb, root, "", true);
        return sb.toString();
    }

    // ==================== Node ====================

    public record Node(
            String type,
            @Nullable Object key,
            int identity,
            @Nullable String label,
            Map<String, Object> properties,
            List<Node> children
    ) {
        public boolean isGroup() {
            return !children.isEmpty();
        }
    }

    // ==================== Snapshot building ====================

    private static Node snapshotNode(Widget widget) {
        String type = widget.getClass().getSimpleName();
        Object key = widget.key();
        int identity = System.identityHashCode(widget);
        String label = WidgetInspector.textOf(widget);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("visible", widget.visible());
        props.put("x", widget.posX());
        props.put("y", widget.posY());
        props.put("width", widget.width());
        props.put("height", widget.height());

        List<Node> children = List.of();
        if (widget instanceof CompositeWidget<?> group) {
            List<Node> tmp = new ArrayList<>(group.children().size());
            for (Widget child : group.children()) {
                tmp.add(snapshotNode(child));
            }
            children = List.copyOf(tmp);
        }
        return new Node(type, key, identity, label, props, children);
    }

    // ==================== Search ====================

    private static @Nullable Node findByKey(Node node, Object key) {
        if (key.equals(node.key)) return node;
        for (Node child : node.children) {
            Node found = findByKey(child, key);
            if (found != null) return found;
        }
        return null;
    }

    private static void collectByType(Node node, String typeName, List<Node> results) {
        if (typeName.equals(node.type)) results.add(node);
        for (Node child : node.children) {
            collectByType(child, typeName, results);
        }
    }

    // ==================== Pretty print ====================

    private static void printNode(StringBuilder sb, Node node, String prefix, boolean last) {
        sb.append(prefix);
        sb.append(last ? "└── " : "├── ");
        sb.append(node.type);
        if (node.key != null) {
            sb.append("[key=").append(node.key).append("]");
        }
        if (node.label != null) {
            sb.append(" \"").append(node.label).append("\"");
        }
        sb.append(" @").append(Integer.toHexString(node.identity));
        if (node.isGroup()) {
            sb.append(" (").append(node.children.size()).append(" children)");
        }
        sb.append("\n");

        String childPrefix = prefix + (last ? "    " : "│   ");
        for (int i = 0; i < node.children.size(); i++) {
            printNode(sb, node.children.get(i), childPrefix, i == node.children.size() - 1);
        }
    }
}
