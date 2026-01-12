package dev.vfyjxf.cloudlib.api.ui.base;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Test-only tree snapshot + diff helper.
 * <p>
 * Goal: produce a readable diff similar to the TreePrinter/TreeDiff output you shared:
 * - show BEFORE tree
 * - show AFTER tree
 * - show a diff tree with REUSED/UPDATED/CREATED/REMOVED/REPLACED
 * - show a concise summary
 */
public final class TestTreeDiff {

    private TestTreeDiff() {}

    public static Snapshot snapshot(Widget root) {
        return new Snapshot(snapshotNode(root));
    }

    /**
     * Human-friendly report that includes:
     * - Before snapshot
     * - After snapshot
     * - Diff tree + summary
     */
    public static String report(String title, Snapshot before, Snapshot after) {
        String beforeTree = formatTree(before.root);
        String afterTree = formatTree(after.root);

        DiffAccumulator acc = new DiffAccumulator();
        String diffTree = formatDiffTree(acc, before.root, after.root);

        List<String> lines = new ArrayList<>();
        lines.add("=== Before Update ===");
        lines.add(beforeTree);
        lines.add("");
        lines.add("=== After Update ===");
        lines.add(afterTree);
        lines.add("");

        lines.addAll(box(
            title,
            "DIFF:",
            indentLines(diffTree, 2),
            "SUMMARY:",
            List.of(
                "● Reused:  " + acc.reused,
                "↻ Updated: " + acc.updated + " widgets (same instance, new content)",
                "◆ Created: " + acc.created,
                "✖ Removed: " + acc.removed
            )
        ));

        return String.join("\n", lines) + "\n";
    }

    // ==================== Snapshot model ====================

    public record Snapshot(Node root) {}

    public record Node(
        String type,
        @Nullable Object key,
        int identity,
        @Nullable String primary,
        List<Node> children
    ) {
        public boolean isGroup() {
            return !children.isEmpty();
        }
    }

    private static Node snapshotNode(Widget widget) {
        String type = widget.getClass().getSimpleName();
        Object key = widget.key();
        int identity = System.identityHashCode(widget);
        String primary = primaryOf(widget);

        List<Node> children = List.of();
        if (widget instanceof WidgetGroup<?> group) {
            List<Node> tmp = new ArrayList<>(group.children().size());
            for (Widget child : group.children()) {
                tmp.add(snapshotNode(child));
            }
            children = List.copyOf(tmp);
        }
        return new Node(type, key, identity, primary, children);
    }

    private static @Nullable String primaryOf(Widget widget) {
        if (widget instanceof TestDSL.TestTextWidget text) {
            return text.text;
        }
        if (widget instanceof TestDSL.TestButtonWidget btn) {
            return btn.label;
        }
        if (widget instanceof TestDSL.TestStackWidget stack) {
            // Prefer name if present to reduce noise.
            return stack.name != null ? stack.name : stack.kind;
        }
        return null;
    }

    // ==================== Tree formatting ====================

    private static String formatTree(Node root) {
        StringBuilder sb = new StringBuilder();
        // Always print as a single rooted tree.
        sb.append("└── ");
        sb.append(nodeLine(root));
        sb.append('\n');
        formatChildren(sb, root.children, "    ");
        // trim last newline
        return trimLastNewline(sb);
    }

    private static void formatChildren(StringBuilder sb, List<Node> children, String prefix) {
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            boolean last = i == children.size() - 1;

            sb.append(prefix);
            sb.append(last ? "└── " : "├── ");
            sb.append(nodeLine(child));
            sb.append('\n');

            String childPrefix = prefix + (last ? "    " : "│   ");
            formatChildren(sb, child.children, childPrefix);
        }
    }

    private static String nodeLine(Node node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.type);
        sb.append('@').append(Integer.toHexString(node.identity));
        if (node.isGroup()) {
            sb.append(" [").append(node.children.size()).append(" children]");
        }
        if (node.primary != null && !node.primary.isBlank()) {
            sb.append(" [\"").append(node.primary).append("\"]");
        }
        if (node.key != null) {
            sb.append(" key=").append(node.key);
        }
        return sb.toString();
    }

    // ==================== Diff ====================

    private static String formatDiffTree(DiffAccumulator acc, Node before, Node after) {
        StringBuilder sb = new StringBuilder();
        sb.append("└── ");
        sb.append(diffNodeLine(acc, before, after));
        sb.append('\n');
        formatDiffChildren(sb, acc, before.children, after.children, "    ");
        return trimLastNewline(sb);
    }

    private static void formatDiffChildren(StringBuilder sb, DiffAccumulator acc, List<Node> beforeChildren, List<Node> afterChildren, String prefix) {
        List<Pair> pairs = pairChildren(beforeChildren, afterChildren);
        for (int i = 0; i < pairs.size(); i++) {
            Pair p = pairs.get(i);
            boolean last = i == pairs.size() - 1;

            sb.append(prefix);
            sb.append(last ? "└── " : "├── ");
            sb.append(diffNodeLine(acc, p.before, p.after));
            sb.append('\n');

            String childPrefix = prefix + (last ? "    " : "│   ");
            formatDiffChildren(sb, acc,
                p.before == null ? List.of() : p.before.children,
                p.after == null ? List.of() : p.after.children,
                childPrefix
            );
        }
    }

    private static String diffNodeLine(DiffAccumulator acc, @Nullable Node before, @Nullable Node after) {
        if (before == null && after == null) {
            return "";
        }
        if (before == null) {
            acc.created++;
            return "◆ " + nodeLine(after) + " (CREATED)";
        }
        if (after == null) {
            acc.removed++;
            return "✖ " + nodeLine(before) + " (REMOVED)";
        }

        boolean sameIdentity = before.identity == after.identity;
        if (sameIdentity) {
            boolean samePrimary = Objects.equals(before.primary, after.primary);
            if (samePrimary) {
                acc.reused++;
                return "● " + nodeLine(after) + " (REUSED)";
            }
            acc.updated++;
            if (before.primary != null && after.primary != null) {
                return "● " + nodeLine(after) + " (UPDATED from: \"" + before.primary + "\")";
            }
            return "● " + nodeLine(after) + " (UPDATED)";
        }

        // Not the same instance. If key+type match, treat as replacement.
        boolean sameType = Objects.equals(before.type, after.type);
        boolean sameKey = Objects.equals(before.key, after.key);
        if (sameType && sameKey) {
            acc.created++;
            acc.removed++;
            return "◆ " + nodeLine(after) + " (REPLACED from: " + nodeLine(before) + ")";
        }

        // Generic replacement.
        acc.created++;
        acc.removed++;
        return "◆ " + nodeLine(after) + " (REPLACED from: " + nodeLine(before) + ")";
    }

    private static class DiffAccumulator {
        int reused;
        int updated;
        int created;
        int removed;
    }

    private record Pair(@Nullable Node before, @Nullable Node after) {}

    private static List<Pair> pairChildren(List<Node> before, List<Node> after) {
        Map<Object, Node> beforeKeyed = new HashMap<>();
        List<Node> beforeUnkeyed = new ArrayList<>();
        for (Node n : before) {
            if (n.key != null) {
                beforeKeyed.put(n.key, n);
            } else {
                beforeUnkeyed.add(n);
            }
        }

        Set<Object> usedBeforeKeys = new HashSet<>();
        List<Pair> out = new ArrayList<>();
        int unkeyedIndex = 0;

        for (Node a : after) {
            if (a.key != null) {
                Node b = beforeKeyed.get(a.key);
                if (b != null) {
                    usedBeforeKeys.add(a.key);
                    out.add(new Pair(b, a));
                } else {
                    out.add(new Pair(null, a));
                }
            } else {
                Node b = unkeyedIndex < beforeUnkeyed.size() ? beforeUnkeyed.get(unkeyedIndex++) : null;
                out.add(new Pair(b, a));
            }
        }

        // Anything left in BEFORE becomes REMOVED.
        for (Node b : before) {
            if (b.key != null && !usedBeforeKeys.contains(b.key)) {
                out.add(new Pair(b, null));
            }
        }
        while (unkeyedIndex < beforeUnkeyed.size()) {
            out.add(new Pair(beforeUnkeyed.get(unkeyedIndex++), null));
        }

        return out;
    }

    // ==================== Box / helpers ====================

    private static List<String> box(String title, String section1Label, List<String> section1Lines, String section2Label, List<String> section2Lines) {
        List<String> body = new ArrayList<>();
        body.add(section1Label);
        body.addAll(section1Lines);
        body.add("");
        body.add(section2Label);
        body.addAll(section2Lines);

        int width = title.length();
        for (String line : body) {
            width = Math.max(width, line.length());
        }
        width = Math.max(width, 50);

        String top = "┌" + "─".repeat(width + 2) + "┐";
        String mid = "├" + "─".repeat(width + 2) + "┤";
        String bot = "└" + "─".repeat(width + 2) + "┘";

        List<String> out = new ArrayList<>();
        out.add(top);
        out.add("│ " + padRight(title, width) + " │");
        out.add(mid);

        boolean pendingSeparator = false;
        for (String line : body) {
            if (line.isEmpty()) {
                // Add a section separator, but avoid stacking multiple ones.
                if (!pendingSeparator) {
                    out.add(mid);
                    pendingSeparator = true;
                }
                continue;
            }
            pendingSeparator = false;
            out.add("│ " + padRight(line, width) + " │");
        }

        out.add(bot);
        return out;
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    private static List<String> indentLines(String block, int spaces) {
        String indent = " ".repeat(spaces);
        String[] lines = block.split("\\R", -1);
        List<String> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            out.add(indent + line);
        }
        return out;
    }

    private static String trimLastNewline(StringBuilder sb) {
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == '\n') {
            sb.setLength(len - 1);
        }
        return sb.toString();
    }
}
