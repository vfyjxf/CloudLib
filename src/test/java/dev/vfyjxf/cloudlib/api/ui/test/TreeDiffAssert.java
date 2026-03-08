package dev.vfyjxf.cloudlib.api.ui.test;

import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assertions over tree snapshot diffs.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * var before = scene.snapshot();
 * scene.tap("add");
 * TreeDiffAssert.create(before, scene.snapshot())
 *     .reused("toolbar")
 *     .created("new-item")
 *     .noRemovals();
 * }</pre>
 */
public final class TreeDiffAssert {

    private final TreeSnapshot before;
    private final TreeSnapshot after;
    private final Map<Object, DiffStatus> keyStatuses;
    private final DiffSummary summary;

    private TreeDiffAssert(TreeSnapshot before, TreeSnapshot after,
                           Map<Object, DiffStatus> keyStatuses, DiffSummary summary) {
        this.before = before;
        this.after = after;
        this.keyStatuses = keyStatuses;
        this.summary = summary;
    }

    public static TreeDiffAssert create(TreeSnapshot before, TreeSnapshot after) {
        Map<Object, DiffStatus> statuses = new LinkedHashMap<>();
        DiffSummary summary = new DiffSummary();
        diffNode(before.root(), after.root(), statuses, summary);
        return new TreeDiffAssert(before, after, statuses, summary);
    }

    // ==================== Per-key assertions ====================

    public TreeDiffAssert reused(Object key) {
        assertKeyStatus(key, DiffStatus.REUSED);
        return this;
    }

    public TreeDiffAssert updated(Object key) {
        assertKeyStatus(key, DiffStatus.UPDATED);
        return this;
    }

    public TreeDiffAssert created(Object key) {
        assertKeyStatus(key, DiffStatus.CREATED);
        return this;
    }

    public TreeDiffAssert removed(Object key) {
        assertKeyStatus(key, DiffStatus.REMOVED);
        return this;
    }

    public TreeDiffAssert replaced(Object key) {
        assertKeyStatus(key, DiffStatus.REPLACED);
        return this;
    }

    // ==================== Bulk assertions ====================

    public TreeDiffAssert noRemovals() {
        assertEquals(0, summary.removed, "Expected no removals but found " + summary.removed + "\n" + report());
        return this;
    }

    public TreeDiffAssert noCreations() {
        assertEquals(0, summary.created, "Expected no creations but found " + summary.created + "\n" + report());
        return this;
    }

    public TreeDiffAssert unchanged() {
        assertEquals(0, summary.updated, "Expected no updates but found " + summary.updated + "\n" + report());
        assertEquals(0, summary.created, "Expected no creations but found " + summary.created + "\n" + report());
        assertEquals(0, summary.removed, "Expected no removals but found " + summary.removed + "\n" + report());
        return this;
    }

    // ==================== Count assertions ====================

    public TreeDiffAssert reusedCount(int expected) {
        assertEquals(expected, summary.reused, "Reused count mismatch\n" + report());
        return this;
    }

    public TreeDiffAssert updatedCount(int expected) {
        assertEquals(expected, summary.updated, "Updated count mismatch\n" + report());
        return this;
    }

    public TreeDiffAssert createdCount(int expected) {
        assertEquals(expected, summary.created, "Created count mismatch\n" + report());
        return this;
    }

    public TreeDiffAssert removedCount(int expected) {
        assertEquals(expected, summary.removed, "Removed count mismatch\n" + report());
        return this;
    }

    // ==================== Report ====================

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== BEFORE ===\n");
        sb.append(before.print());
        sb.append("\n=== AFTER ===\n");
        sb.append(after.print());
        sb.append("\n=== DIFF SUMMARY ===\n");
        sb.append("Reused: ").append(summary.reused).append("\n");
        sb.append("Updated: ").append(summary.updated).append("\n");
        sb.append("Created: ").append(summary.created).append("\n");
        sb.append("Removed: ").append(summary.removed).append("\n");
        if (!keyStatuses.isEmpty()) {
            sb.append("\nPer-key status:\n");
            for (var entry : keyStatuses.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(" → ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    // ==================== Internals ====================

    enum DiffStatus {
        REUSED, UPDATED, CREATED, REMOVED, REPLACED
    }

    private static class DiffSummary {
        int reused, updated, created, removed;
    }

    private void assertKeyStatus(Object key, DiffStatus expected) {
        DiffStatus actual = keyStatuses.get(key);
        assertNotNull(actual, "Key '" + key + "' not found in diff. Available keys: " + keyStatuses.keySet() + "\n" + report());
        assertEquals(expected, actual, "Status mismatch for key '" + key + "'\n" + report());
    }

    private static void diffNode(@Nullable TreeSnapshot.Node before, @Nullable TreeSnapshot.Node after,
                                 Map<Object, DiffStatus> statuses, DiffSummary summary) {
        if (before == null && after == null) return;

        if (before == null) {
            // Created
            summary.created++;
            if (after.key() != null) {
                statuses.put(after.key(), DiffStatus.CREATED);
            }
            for (var child : after.children()) {
                diffNode(null, child, statuses, summary);
            }
            return;
        }

        if (after == null) {
            // Removed
            summary.removed++;
            if (before.key() != null) {
                statuses.put(before.key(), DiffStatus.REMOVED);
            }
            for (var child : before.children()) {
                diffNode(child, null, statuses, summary);
            }
            return;
        }

        // Both exist
        boolean sameIdentity = before.identity() == after.identity();
        if (sameIdentity) {
            boolean sameLabel = Objects.equals(before.label(), after.label());
            boolean sameProps = Objects.equals(before.properties(), after.properties());
            if (sameLabel && sameProps) {
                summary.reused++;
                if (after.key() != null) statuses.put(after.key(), DiffStatus.REUSED);
            } else {
                summary.updated++;
                if (after.key() != null) statuses.put(after.key(), DiffStatus.UPDATED);
            }
        } else {
            summary.created++;
            summary.removed++;
            if (after.key() != null) statuses.put(after.key(), DiffStatus.REPLACED);
        }

        // Diff children
        diffChildren(before.children(), after.children(), statuses, summary);
    }

    private static void diffChildren(List<TreeSnapshot.Node> beforeChildren, List<TreeSnapshot.Node> afterChildren,
                                     Map<Object, DiffStatus> statuses, DiffSummary summary) {
        // Pair by key first, then positionally for unkeyed
        Map<Object, TreeSnapshot.Node> beforeKeyed = new LinkedHashMap<>();
        List<TreeSnapshot.Node> beforeUnkeyed = new ArrayList<>();
        for (var n : beforeChildren) {
            if (n.key() != null) beforeKeyed.put(n.key(), n);
            else beforeUnkeyed.add(n);
        }

        Set<Object> matchedKeys = new HashSet<>();
        int unkeyedIdx = 0;

        for (var afterNode : afterChildren) {
            if (afterNode.key() != null) {
                var beforeNode = beforeKeyed.get(afterNode.key());
                if (beforeNode != null) {
                    matchedKeys.add(afterNode.key());
                    diffNode(beforeNode, afterNode, statuses, summary);
                } else {
                    diffNode(null, afterNode, statuses, summary);
                }
            } else {
                var beforeNode = unkeyedIdx < beforeUnkeyed.size() ? beforeUnkeyed.get(unkeyedIdx++) : null;
                diffNode(beforeNode, afterNode, statuses, summary);
            }
        }

        // Remaining before-only nodes are removed
        for (var n : beforeChildren) {
            if (n.key() != null && !matchedKeys.contains(n.key())) {
                diffNode(n, null, statuses, summary);
            }
        }
        while (unkeyedIdx < beforeUnkeyed.size()) {
            diffNode(beforeUnkeyed.get(unkeyedIdx++), null, statuses, summary);
        }
    }
}
