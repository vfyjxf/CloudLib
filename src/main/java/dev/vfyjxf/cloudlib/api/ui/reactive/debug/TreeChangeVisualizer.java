package dev.vfyjxf.cloudlib.api.ui.reactive.debug;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.StatefulBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A comprehensive tree comparison and visualization tool.
 * <p>
 * TreeChangeVisualizer provides detailed visual diff output showing:
 * <ul>
 *   <li>Which nodes were added (marked with +)</li>
 *   <li>Which nodes were removed (marked with -)</li>
 *   <li>Which nodes were modified (marked with ~)</li>
 *   <li>Which nodes are unchanged (no marker)</li>
 * </ul>
 * <p>
 * Example output:
 * <pre>
 * === Tree Change Visualization ===
 * 
 *   CompositeElement&lt;ColumnBlueprint&gt;
 *     LeafElement&lt;TextBlueprint&gt; "Header"
 * ~   CompositeElement&lt;RowBlueprint&gt;                    [MODIFIED: child count 2 → 3]
 *       LeafElement&lt;TextBlueprint&gt; "Left"
 *       LeafElement&lt;TextBlueprint&gt; "Center"
 * +     LeafElement&lt;TextBlueprint&gt; "Right"              [ADDED]
 * -   LeafElement&lt;TextBlueprint&gt; "Old Footer"          [REMOVED]
 * +   LeafElement&lt;TextBlueprint&gt; "New Footer"          [ADDED]
 * </pre>
 */
@ApiStatus.Experimental
public class TreeChangeVisualizer {

    /**
     * Type of change for a node.
     */
    public enum ChangeType {
        /** Node is unchanged */
        UNCHANGED(" "),
        /** Node was added */
        ADDED("+"),
        /** Node was removed */
        REMOVED("-"),
        /** Node was modified (same position, different content) */
        MODIFIED("~"),
        /** Node was moved to different position */
        MOVED("→");

        private final String marker;

        ChangeType(String marker) {
            this.marker = marker;
        }

        public String getMarker() {
            return marker;
        }
    }

    /**
     * Represents a node in the diff tree.
     */
    public record DiffNode(
            ChangeType changeType,
            String nodeType,
            String blueprintType,
            @Nullable Key key,
            @Nullable String description,
            @Nullable String changeDetail,
            List<DiffNode> children
    ) {
        public static DiffNode unchanged(String nodeType, String blueprintType, @Nullable Key key,
                                         @Nullable String description, List<DiffNode> children) {
            return new DiffNode(ChangeType.UNCHANGED, nodeType, blueprintType, key, description, null, children);
        }

        public static DiffNode added(String nodeType, String blueprintType, @Nullable Key key,
                                     @Nullable String description, List<DiffNode> children) {
            return new DiffNode(ChangeType.ADDED, nodeType, blueprintType, key, description, "ADDED", children);
        }

        public static DiffNode removed(String nodeType, String blueprintType, @Nullable Key key,
                                       @Nullable String description, List<DiffNode> children) {
            return new DiffNode(ChangeType.REMOVED, nodeType, blueprintType, key, description, "REMOVED", children);
        }

        public static DiffNode modified(String nodeType, String blueprintType, @Nullable Key key,
                                        @Nullable String description, String detail, List<DiffNode> children) {
            return new DiffNode(ChangeType.MODIFIED, nodeType, blueprintType, key, description, detail, children);
        }
    }

    /**
     * Result of tree comparison with visualization.
     */
    public record ChangeResult(
            DiffNode root,
            int addedCount,
            int removedCount,
            int modifiedCount,
            int unchangedCount
    ) {
        public boolean hasChanges() {
            return addedCount > 0 || removedCount > 0 || modifiedCount > 0;
        }

        public String getSummary() {
            if (!hasChanges()) {
                return "No changes detected.";
            }
            return String.format("Changes: +%d added, -%d removed, ~%d modified, %d unchanged",
                    addedCount, removedCount, modifiedCount, unchangedCount);
        }
    }

    // ANSI color codes for terminal output
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GRAY = "\u001B[90m";

    private final boolean useColors;

    public TreeChangeVisualizer() {
        this(false);
    }

    public TreeChangeVisualizer(boolean useColors) {
        this.useColors = useColors;
    }

    // ==================== Snapshot-based comparison ====================

    /**
     * Compares two element tree snapshots and produces a visual diff.
     */
    public ChangeResult compare(TreeDiffer.TreeSnapshot before, TreeDiffer.TreeSnapshot after) {
        int[] counts = new int[4]; // added, removed, modified, unchanged
        DiffNode root = compareSnapshots(before, after, counts);
        return new ChangeResult(root, counts[0], counts[1], counts[2], counts[3]);
    }

    private DiffNode compareSnapshots(TreeDiffer.TreeSnapshot before, TreeDiffer.TreeSnapshot after, int[] counts) {
        if (before == null && after == null) {
            return null;
        }

        if (before == null) {
            counts[0]++; // added
            return createAddedNode(after, counts);
        }

        if (after == null) {
            counts[1]++; // removed
            return createRemovedNode(before, counts);
        }

        // Both exist - compare them
        String beforeType = before.getBlueprintType();
        String afterType = after.getBlueprintType();
        String beforeNodeType = before.getElementType();

        List<DiffNode> childDiffs = new ArrayList<>();
        String changeDetail = null;
        ChangeType changeType = ChangeType.UNCHANGED;

        // Check if type changed
        if (!beforeType.equals(afterType)) {
            changeType = ChangeType.MODIFIED;
            changeDetail = "TYPE: " + beforeType + " → " + afterType;
            counts[2]++; // modified
        } else {
            // Compare children
            List<TreeDiffer.TreeSnapshot> beforeChildren = before.getChildren();
            List<TreeDiffer.TreeSnapshot> afterChildren = after.getChildren();

            if (beforeChildren.size() != afterChildren.size()) {
                changeType = ChangeType.MODIFIED;
                changeDetail = "CHILDREN: " + beforeChildren.size() + " → " + afterChildren.size();
                counts[2]++; // modified
            } else {
                counts[3]++; // unchanged
            }

            // Reconcile children
            childDiffs = reconcileChildren(beforeChildren, afterChildren, counts);
        }

        return new DiffNode(
                changeType,
                beforeNodeType,
                afterType,
                after.getKey(),
                null,
                changeDetail,
                childDiffs
        );
    }

    private List<DiffNode> reconcileChildren(
            List<TreeDiffer.TreeSnapshot> before,
            List<TreeDiffer.TreeSnapshot> after,
            int[] counts
    ) {
        List<DiffNode> result = new ArrayList<>();

        // Build key maps for efficient matching
        Map<Key, TreeDiffer.TreeSnapshot> beforeKeyed = new HashMap<>();
        List<TreeDiffer.TreeSnapshot> beforeUnkeyed = new ArrayList<>();
        Map<Key, TreeDiffer.TreeSnapshot> afterKeyed = new HashMap<>();
        List<TreeDiffer.TreeSnapshot> afterUnkeyed = new ArrayList<>();

        for (TreeDiffer.TreeSnapshot snapshot : before) {
            Key key = snapshot.getKey();
            if (key != null) {
                beforeKeyed.put(key, snapshot);
            } else {
                beforeUnkeyed.add(snapshot);
            }
        }

        for (TreeDiffer.TreeSnapshot snapshot : after) {
            Key key = snapshot.getKey();
            if (key != null) {
                afterKeyed.put(key, snapshot);
            } else {
                afterUnkeyed.add(snapshot);
            }
        }

        // Process keyed elements first
        for (Map.Entry<Key, TreeDiffer.TreeSnapshot> entry : afterKeyed.entrySet()) {
            Key key = entry.getKey();
            TreeDiffer.TreeSnapshot afterSnapshot = entry.getValue();
            TreeDiffer.TreeSnapshot beforeSnapshot = beforeKeyed.remove(key);

            if (beforeSnapshot != null) {
                result.add(compareSnapshots(beforeSnapshot, afterSnapshot, counts));
            } else {
                result.add(createAddedNode(afterSnapshot, counts));
                counts[0]++;
            }
        }

        // Removed keyed elements
        for (TreeDiffer.TreeSnapshot snapshot : beforeKeyed.values()) {
            result.add(createRemovedNode(snapshot, counts));
            counts[1]++;
        }

        // Process unkeyed elements by position
        int minSize = Math.min(beforeUnkeyed.size(), afterUnkeyed.size());
        for (int i = 0; i < minSize; i++) {
            result.add(compareSnapshots(beforeUnkeyed.get(i), afterUnkeyed.get(i), counts));
        }

        // Extra unkeyed elements in after
        for (int i = minSize; i < afterUnkeyed.size(); i++) {
            result.add(createAddedNode(afterUnkeyed.get(i), counts));
            counts[0]++;
        }

        // Missing unkeyed elements from before
        for (int i = minSize; i < beforeUnkeyed.size(); i++) {
            result.add(createRemovedNode(beforeUnkeyed.get(i), counts));
            counts[1]++;
        }

        return result;
    }

    private DiffNode createAddedNode(TreeDiffer.TreeSnapshot snapshot, int[] counts) {
        List<DiffNode> children = new ArrayList<>();
        for (TreeDiffer.TreeSnapshot child : snapshot.getChildren()) {
            children.add(createAddedNode(child, counts));
        }
        return DiffNode.added(
                snapshot.getElementType(),
                snapshot.getBlueprintType(),
                snapshot.getKey(),
                null,
                children
        );
    }

    private DiffNode createRemovedNode(TreeDiffer.TreeSnapshot snapshot, int[] counts) {
        List<DiffNode> children = new ArrayList<>();
        for (TreeDiffer.TreeSnapshot child : snapshot.getChildren()) {
            children.add(createRemovedNode(child, counts));
        }
        return DiffNode.removed(
                snapshot.getElementType(),
                snapshot.getBlueprintType(),
                snapshot.getKey(),
                null,
                children
        );
    }

    // ==================== Visualization ====================

    /**
     * Renders the diff tree to a string.
     */
    public String render(ChangeResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Tree Change Visualization ===\n");
        sb.append(result.getSummary()).append("\n\n");

        if (result.root() != null) {
            renderNode(sb, result.root(), "", true);
        }

        return sb.toString();
    }

    /**
     * Renders the diff tree with side-by-side comparison.
     */
    public String renderSideBySide(TreeDiffer.TreeSnapshot before, TreeDiffer.TreeSnapshot after) {
        return renderSideBySide(before, after, 80); // Default width
    }

    /**
     * Renders the diff tree with side-by-side comparison with custom column width.
     *
     * @param before the before snapshot
     * @param after  the after snapshot
     * @param columnWidth the width of each column
     */
    public String renderSideBySide(TreeDiffer.TreeSnapshot before, TreeDiffer.TreeSnapshot after, int columnWidth) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Side-by-Side Tree Comparison ===\n\n");

        String beforeHeader = "BEFORE:";
        String afterHeader = "AFTER:";
        sb.append(String.format("%-" + columnWidth + "s  %s%n", beforeHeader, afterHeader));
        sb.append("─".repeat(columnWidth)).append("  ").append("─".repeat(columnWidth)).append("\n");

        String beforeStr = before != null ? before.toString() : "(empty)";
        String afterStr = after != null ? after.toString() : "(empty)";

        String[] beforeLines = beforeStr.split("\n");
        String[] afterLines = afterStr.split("\n");

        int maxLines = Math.max(beforeLines.length, afterLines.length);
        for (int i = 0; i < maxLines; i++) {
            String beforeLine = i < beforeLines.length ? beforeLines[i] : "";
            String afterLine = i < afterLines.length ? afterLines[i] : "";

            // Truncate with ellipsis if too long, but use larger width
            String beforeDisplay = truncateWithEllipsis(beforeLine, columnWidth);
            String afterDisplay = truncateWithEllipsis(afterLine, columnWidth);

            sb.append(String.format("%-" + columnWidth + "s  %s%n", beforeDisplay, afterDisplay));
        }

        return sb.toString();
    }

    /**
     * Truncates a string with ellipsis if it exceeds the max length.
     */
    private String truncateWithEllipsis(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private void renderNode(StringBuilder sb, DiffNode node, String prefix, boolean isLast) {
        String marker = node.changeType().getMarker();
        String connector = isLast ? "└── " : "├── ";

        // Apply colors if enabled
        String nodeText = formatNodeText(node);
        if (useColors) {
            nodeText = colorize(nodeText, node.changeType());
        }

        sb.append(marker).append(" ").append(prefix).append(connector).append(nodeText);

        // Add change detail if present
        if (node.changeDetail() != null) {
            String detail = " [" + node.changeDetail() + "]";
            if (useColors) {
                detail = colorize(detail, node.changeType());
            }
            sb.append(detail);
        }

        sb.append("\n");

        // Render children
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        List<DiffNode> children = node.children();
        for (int i = 0; i < children.size(); i++) {
            renderNode(sb, children.get(i), childPrefix, i == children.size() - 1);
        }
    }

    private String formatNodeText(DiffNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.nodeType()).append("<").append(node.blueprintType()).append(">");

        if (node.key() != null) {
            sb.append(" [key=").append(node.key()).append("]");
        }

        if (node.description() != null) {
            sb.append(" \"").append(node.description()).append("\"");
        }

        return sb.toString();
    }

    private String colorize(String text, ChangeType type) {
        return switch (type) {
            case ADDED -> ANSI_GREEN + text + ANSI_RESET;
            case REMOVED -> ANSI_RED + text + ANSI_RESET;
            case MODIFIED -> ANSI_YELLOW + text + ANSI_RESET;
            case UNCHANGED -> ANSI_GRAY + text + ANSI_RESET;
            case MOVED -> ANSI_YELLOW + text + ANSI_RESET;
        };
    }

    // ==================== Static convenience methods ====================

    /**
     * Compares two snapshots and prints the result.
     */
    public static void printDiff(TreeDiffer.TreeSnapshot before, TreeDiffer.TreeSnapshot after) {
        TreeChangeVisualizer visualizer = new TreeChangeVisualizer();
        ChangeResult result = visualizer.compare(before, after);
        System.out.println(visualizer.render(result));
    }

    /**
     * Compares two snapshots and prints with colors.
     */
    public static void printDiffColored(TreeDiffer.TreeSnapshot before, TreeDiffer.TreeSnapshot after) {
        TreeChangeVisualizer visualizer = new TreeChangeVisualizer(true);
        ChangeResult result = visualizer.compare(before, after);
        System.out.println(visualizer.render(result));
    }

    /**
     * Prints a side-by-side comparison with default width (80 chars per column).
     */
    public static void printSideBySide(TreeDiffer.TreeSnapshot before, TreeDiffer.TreeSnapshot after) {
        printSideBySide(before, after, 80);
    }

    /**
     * Prints a side-by-side comparison with custom column width.
     *
     * @param before      the before snapshot
     * @param after       the after snapshot
     * @param columnWidth the width of each column (default: 80)
     */
    public static void printSideBySide(TreeDiffer.TreeSnapshot before, TreeDiffer.TreeSnapshot after, int columnWidth) {
        TreeChangeVisualizer visualizer = new TreeChangeVisualizer();
        System.out.println(visualizer.renderSideBySide(before, after, columnWidth));
    }

    /**
     * Creates a detailed change report between two element states.
     */
    public static String createChangeReport(
            UIElement<?> element,
            TreeDiffer.TreeSnapshot beforeSnapshot,
            String operationDescription
    ) {
        TreeDiffer.TreeSnapshot afterSnapshot = TreeDiffer.snapshot(element);
        TreeChangeVisualizer visualizer = new TreeChangeVisualizer();
        ChangeResult result = visualizer.compare(beforeSnapshot, afterSnapshot);

        StringBuilder report = new StringBuilder();
        report.append("╔══════════════════════════════════════════════════════════════╗\n");
        report.append("║                    TREE CHANGE REPORT                        ║\n");
        report.append("╠══════════════════════════════════════════════════════════════╣\n");
        report.append("║ Operation: ").append(String.format("%-49s", operationDescription)).append("║\n");
        report.append("║ ").append(String.format("%-61s", result.getSummary())).append("║\n");
        report.append("╚══════════════════════════════════════════════════════════════╝\n\n");

        if (result.hasChanges()) {
            report.append(visualizer.render(result));
            report.append("\n");
            report.append(visualizer.renderSideBySide(beforeSnapshot, afterSnapshot,80));
        } else {
            report.append("No structural changes detected.\n");
        }

        return report.toString();
    }
}
