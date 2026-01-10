package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.debug.DebugBuildOwner;
import dev.vfyjxf.cloudlib.api.ui.reactive.debug.TreeChangeVisualizer;
import dev.vfyjxf.cloudlib.api.ui.reactive.debug.TreeDiffer;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.*;

/**
 * Tests demonstrating the tree change visualization debugging tools.
 * <p>
 * These tests show how to:
 * <ol>
 *   <li>Take snapshots of the element tree before/after state changes</li>
 *   <li>Compare snapshots to detect structural changes</li>
 *   <li>Visualize what changed in a human-readable format</li>
 * </ol>
 */
@ExtendWith(EphemeralTestServerProvider.class)
public class TreeChangeVisualizerTest {

    private DebugBuildOwner debugBuildOwner;
    private TreeDiffer.TreeSnapshot lastSnapshot;

    private void mountWithDebug(UIElement<?> element) {
        debugBuildOwner = new DebugBuildOwner();
        element.mount(null, debugBuildOwner);
        // Take initial snapshot for debugging
        lastSnapshot = TreeDiffer.snapshot(element);
    }

    /**
     * Takes a snapshot and compares with the last one, printing changes.
     */
    private TreeDiffer.TreeSnapshot snapshotAndCompare(UIElement<?> element, String description) {
        TreeDiffer.TreeSnapshot newSnapshot = TreeDiffer.snapshot(element);
        if (lastSnapshot != null) {
            TreeDiffer.SnapshotDiff diff = lastSnapshot.compareTo(newSnapshot);
            System.out.println("\n=== " + description + " ===");
            System.out.println(diff.getSummary());
            if (!diff.matches()) {
                System.out.println("Previous tree:");
                System.out.println(lastSnapshot);
                System.out.println("Current tree:");
                System.out.println(newSnapshot);
            }
        }
        lastSnapshot = newSnapshot;
        return newSnapshot;
    }

    /**
     * Asserts the tree changed after state update, with detailed debug info.
     */
    private void assertTreeChanged(UIElement<?> element, String context) {
        TreeDiffer.TreeSnapshot newSnapshot = TreeDiffer.snapshot(element);
        TreeDiffer.SnapshotDiff diff = lastSnapshot.compareTo(newSnapshot);
        if (diff.matches()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(context).append(": Expected tree to change but it didn't\n");
            errorMsg.append("Tree structure:\n");
            errorMsg.append(newSnapshot);
            errorMsg.append("\nDebugBuildOwner summary:\n");
            errorMsg.append(debugBuildOwner.getSummary());
            Assertions.fail(errorMsg.toString());
        }
        lastSnapshot = newSnapshot;
    }

    /**
     * Asserts the tree did NOT change, with detailed debug info on failure.
     */
    private void assertTreeUnchanged(UIElement<?> element, String context) {
        TreeDiffer.TreeSnapshot newSnapshot = TreeDiffer.snapshot(element);
        TreeDiffer.SnapshotDiff diff = lastSnapshot.compareTo(newSnapshot);
        if (!diff.matches()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(context).append(": Expected tree to stay the same but it changed\n");
            errorMsg.append(diff.getSummary());
            errorMsg.append("\nPrevious tree:\n");
            errorMsg.append(lastSnapshot);
            errorMsg.append("\nCurrent tree:\n");
            errorMsg.append(newSnapshot);
            Assertions.fail(errorMsg.toString());
        }
    }

    // ==================== Basic Change Detection ====================

    @Test
    @DisplayName("Should detect added children")
    void testDetectAddedChildren(MinecraftServer server) {
        Signal<List<String>> items = signal(new ArrayList<>(List.of("A", "B")));

        var ui = Stateful(() -> Column(() -> {
            for (String item : items.get()) {
                Text(item);
            }
        }));

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        // Take snapshot before change (use lastSnapshot which is set by mountWithDebug)
        TreeDiffer.TreeSnapshot before = lastSnapshot;
        System.out.println("=== BEFORE STATE ===");
        System.out.println(before);

        // Add item and rebuild
        var newItems = new ArrayList<>(items.get());
        newItems.add("C");
        items.set(newItems);
        debugBuildOwner.buildScope();

        // Verify tree changed with detailed info
        assertTreeChanged(element, "Adding item 'C' to list");

        // Take snapshot after change (use lastSnapshot which is updated by assertTreeChanged)
        TreeDiffer.TreeSnapshot after = lastSnapshot;
        System.out.println("\n=== AFTER STATE ===");
        System.out.println(after);

        // Compare and visualize
        TreeChangeVisualizer visualizer = new TreeChangeVisualizer();
        TreeChangeVisualizer.ChangeResult result = visualizer.compare(before, after);

        System.out.println("\n=== CHANGE VISUALIZATION ===");
        System.out.println(visualizer.render(result));

        // Verify changes detected
        Assertions.assertTrue(result.hasChanges(), 
            "Should detect changes when adding item. Before: " + before + "\nAfter: " + after);
        Assertions.assertTrue(result.addedCount() > 0,
            "Should detect added nodes. Result: " + result);
    }

    @Test
    @DisplayName("Should detect removed children")
    void testDetectRemovedChildren(MinecraftServer server) {
        Signal<List<String>> items = signal(new ArrayList<>(List.of("A", "B", "C")));

        var ui = Stateful(() -> Column(() -> {
            for (String item : items.get()) {
                Text(item);
            }
        }));

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        TreeDiffer.TreeSnapshot before = lastSnapshot;

        // Remove item
        items.set(List.of("A", "B"));
        debugBuildOwner.buildScope();

        // Verify tree changed with detailed info
        assertTreeChanged(element, "Removing item 'C' from list");

        TreeDiffer.TreeSnapshot after = lastSnapshot;

        // Visualize change
        System.out.println(TreeChangeVisualizer.createChangeReport(
                element, before, "Removed item 'C' from list"));

        TreeChangeVisualizer visualizer = new TreeChangeVisualizer();
        var result = visualizer.compare(before, after);
        Assertions.assertTrue(result.removedCount() > 0,
            "Should detect removed nodes. Before had " + before.getChildren().size() + 
            " children, after has " + after.getChildren().size());
    }

    @Test
    @DisplayName("Should detect structural modifications")
    void testDetectStructuralModification(MinecraftServer server) {
        Signal<Boolean> showDetails = signal(false);

        var ui = Stateful(() -> Column(() -> {
            Text("Header");
            If(showDetails::get, () -> {
                Text("Detail 1");
                Text("Detail 2");
                Text("Detail 3");
            });
            Text("Footer");
        }));

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        TreeDiffer.TreeSnapshot before = lastSnapshot;
        System.out.println("=== Before (details hidden) ===");
        System.out.println(before);

        // Show details
        showDetails.set(true);
        debugBuildOwner.buildScope();

        // Verify tree changed with detailed info
        assertTreeChanged(element, "Toggling showDetails to true");

        TreeDiffer.TreeSnapshot after = lastSnapshot;
        System.out.println("\n=== After (details shown) ===");
        System.out.println(after);

        // Print change report with verification
        System.out.println("\n" + "=".repeat(60));
        System.out.println(TreeChangeVisualizer.createChangeReport(
                element,
                before,
                "Toggling showDetails to true"
        ));

        // Additional verification with detailed info
        var visualizer = new TreeChangeVisualizer();
        var result = visualizer.compare(before, after);
        Assertions.assertTrue(result.hasChanges(),
            "Should detect structural changes. Added: " + result.addedCount() + 
            ", Removed: " + result.removedCount() + ", Modified: " + result.modifiedCount());
    }

    // ==================== Side-by-Side Comparison ====================

    @Test
    @DisplayName("Should display side-by-side comparison")
    void testSideBySideComparison(MinecraftServer server) {
        Signal<Integer> count = signal(2);

        var ui = Stateful(() -> Column(() -> {
            Text("Items:");
            for (int idx = 0; idx < count.get(); idx++) {
                final int itemIndex = idx;
                Row(() -> {
                    Text("Item " + itemIndex);
                    Button("Delete", () -> {});
                });
            }
        }));

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        TreeDiffer.TreeSnapshot before = TreeDiffer.snapshot(element);

        // Add more items
        count.set(4);
        debugBuildOwner.buildScope();

        TreeDiffer.TreeSnapshot after = TreeDiffer.snapshot(element);

        // Print side-by-side
        System.out.println("\n=== SIDE-BY-SIDE COMPARISON ===");
        TreeChangeVisualizer.printSideBySide(before, after);
    }

    // ==================== Complex Scenarios ====================

    @Test
    @DisplayName("Should track changes through multiple state updates")
    void testMultipleStateUpdates(MinecraftServer server) {
        Signal<String> title = signal("Initial");
        Signal<List<String>> items = signal(new ArrayList<>(List.of("A")));
        Signal<Boolean> showFooter = signal(false);

        var ui = Stateful(() -> Column(() -> {
            Text(() -> "Title: " + title.get());
            ForEachScoped(items::get, item -> Text(item));
            If(showFooter::get, () -> Text("Footer"));
        }));

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        TreeDiffer.TreeSnapshot state0 = TreeDiffer.snapshot(element);
        System.out.println("=== State 0 (Initial) ===");
        System.out.println(state0);

        // Change 1: Update title
        title.set("Updated Title");
        debugBuildOwner.buildScope();
        TreeDiffer.TreeSnapshot state1 = TreeDiffer.snapshot(element);
        System.out.println("\n--- Change 1: Title updated ---");
        TreeChangeVisualizer.printDiff(state0, state1);

        // Change 2: Add items
        items.set(new ArrayList<>(List.of("A", "B", "C")));
        debugBuildOwner.buildScope();
        TreeDiffer.TreeSnapshot state2 = TreeDiffer.snapshot(element);
        System.out.println("\n--- Change 2: Items added ---");
        TreeChangeVisualizer.printDiff(state1, state2);

        // Change 3: Show footer
        showFooter.set(true);
        debugBuildOwner.buildScope();
        TreeDiffer.TreeSnapshot state3 = TreeDiffer.snapshot(element);
        System.out.println("\n--- Change 3: Footer shown ---");
        TreeChangeVisualizer.printDiff(state2, state3);

        // Overall change
        System.out.println("\n=== TOTAL CHANGES (State 0 → State 3) ===");
        TreeChangeVisualizer.printDiff(state0, state3);
    }

    @Test
    @DisplayName("Should generate comprehensive change report")
    void testChangeReport(MinecraftServer server) {
        Signal<List<String>> todos = signal(new ArrayList<>(List.of("Task 1", "Task 2")));

        var ui = Stateful(() -> Column(10, () -> {
            container()
                    .padding(8)
                    .background(0xFF2196F3)
                    .build(() -> {
                        Text(() -> "Todo List (" + todos.get().size() + " items)", 0xFFFFFFFF);
                    });

            ForEachScoped(todos::get, todo -> {
                Row(5, () -> {
                    Text("• " + todo);
                    Button("Delete", () -> {});
                });
            });

            Row(() -> {
                Button("Add", () -> {});
                Button("Clear All", () -> {});
            });
        }));

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        TreeDiffer.TreeSnapshot before = TreeDiffer.snapshot(element);

        // Simulate adding and removing tasks
        todos.set(new ArrayList<>(List.of("Task 1", "Task 3", "Task 4")));
        debugBuildOwner.buildScope();

        // Generate comprehensive report
        String report = TreeChangeVisualizer.createChangeReport(
                element,
                before,
                "Updated todo list: removed Task 2, added Task 3 and Task 4"
        );

        System.out.println(report);
    }

    // ==================== Keyed Element Tracking ====================

    @Test
    @DisplayName("Should track keyed elements correctly")
    void testKeyedElementTracking(MinecraftServer server) {
        Signal<List<String>> items = signal(new ArrayList<>(List.of("A", "B", "C")));

        var ui = Stateful(() -> Column(() -> {
            for (String item : items.get()) {
                // Use keyed text (assuming Text with key is supported)
                Text(key(item), item);
            }
        }));

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        TreeDiffer.TreeSnapshot before = TreeDiffer.snapshot(element);
        System.out.println("=== Before (A, B, C) ===");
        System.out.println(before);

        // Reorder items
        items.set(new ArrayList<>(List.of("C", "A", "B")));
        debugBuildOwner.buildScope();

        TreeDiffer.TreeSnapshot after = TreeDiffer.snapshot(element);
        System.out.println("\n=== After (C, A, B) - Reordered ===");
        System.out.println(after);

        TreeChangeVisualizer.printDiff(before, after);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle empty tree")
    void testEmptyTree(MinecraftServer server) {
        Signal<Boolean> show = signal(false);

        var ui = Stateful(() -> {
            if (show.get()) {
                return Column(() -> Text("Content"));
            } else {
                return Column(() -> {}); // Empty column
            }
        });

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        TreeDiffer.TreeSnapshot before = lastSnapshot;
        System.out.println("=== BEFORE (empty) ===");
        System.out.println(before);

        show.set(true);
        debugBuildOwner.buildScope();

        // Verify tree changed with detailed info
        assertTreeChanged(element, "Showing content in previously empty tree");

        TreeDiffer.TreeSnapshot after = lastSnapshot;
        System.out.println("\n=== AFTER (with content) ===");
        System.out.println(after);

        System.out.println("\n=== Empty to Non-Empty ===");
        TreeChangeVisualizer.printDiff(before, after);

        // Additional verification
        var visualizer = new TreeChangeVisualizer();
        var result = visualizer.compare(before, after);
        Assertions.assertTrue(result.hasChanges(), 
            "Should detect changes from empty to non-empty tree");
    }

    @Test
    @DisplayName("Should handle deeply nested changes")
    void testDeeplyNestedChanges(MinecraftServer server) {
        Signal<Boolean> showDeep = signal(false);

        var ui = Stateful(() -> {
            // Access signal during build - this will be tracked
            boolean show = showDeep.get();
            return Column(() -> {
                Row(() -> {
                    Column(() -> {
                        Row(() -> {
                            Text("Outer");
                            // Conditionally show deep content that changes structure
                            if (show) {
                                Text("Deep Content");
                                Button("Deep Action", () -> {});
                            }
                        });
                    });
                });
            });
        });

        UIElement<?> element = ui.createElement();
        mountWithDebug(element);

        TreeDiffer.TreeSnapshot before = lastSnapshot;
        System.out.println("=== BEFORE (showDeep=false) ===");
        System.out.println(before);

        showDeep.set(true);
        debugBuildOwner.buildScope();

        // Verify tree changed with detailed info
        assertTreeChanged(element, "Showing deeply nested content");

        TreeDiffer.TreeSnapshot after = lastSnapshot;
        System.out.println("\n=== AFTER (showDeep=true) ===");
        System.out.println(after);

        System.out.println("\n=== Deeply Nested Change ===");
        TreeChangeVisualizer.printDiff(before, after);
        TreeChangeVisualizer.printSideBySide(before, after);

        // Additional verification
        var visualizer = new TreeChangeVisualizer();
        var result = visualizer.compare(before, after);
        System.out.println("\nChange summary: added=" + result.addedCount() + 
            ", removed=" + result.removedCount() + ", modified=" + result.modifiedCount());
        
        Assertions.assertTrue(result.hasChanges(),
            "Should detect structural changes in deeply nested tree");
    }
}
