package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.debug.DebugBuildOwner;
import dev.vfyjxf.cloudlib.api.ui.reactive.debug.TreeDiffer;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.Button;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.Column;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.ForEachScoped;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.If;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.Row;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.Stateful;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.Stateless;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.Text;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.container;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.key;
import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.signal;

/**
 * Comprehensive tests for the reactive UI system.
 * <p>
 * Tests are organized into three main categories:
 * <ol>
 *   <li>DSL Construction - Tests the ability to build UI tree blueprints</li>
 *   <li>State Tracking - Tests that components update when state changes</li>
 *   <li>Debug Tools - Tests the debugging capabilities (DebugBuildOwner, TreeDiffer)</li>
 * </ol>
 */
@ExtendWith(EphemeralTestServerProvider.class)
public class ReactiveSystemTest {

    // ==================== Test Infrastructure ====================

    private DebugBuildOwner debugBuildOwner;
    private TreeDiffer.TreeSnapshot lastSnapshot;

    private void mountWithDebug(UIElement<?> element) {
        debugBuildOwner = new DebugBuildOwner();
        element.mount(null, debugBuildOwner);
        // Take initial snapshot for debugging
        lastSnapshot = TreeDiffer.snapshot(element);
    }

    /**
     * Verifies that the element tree matches the expected blueprint structure.
     * Provides detailed diff information on failure.
     */
    private void assertTreeMatches(Blueprint expectedBlueprint, UIElement<?> actualElement, String context) {
        TreeDiffer.DiffResult result = TreeDiffer.compare(expectedBlueprint, actualElement);
        if (!result.matches()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Tree structure mismatch in: ").append(context).append("\n");
            errorMsg.append(result.getSummary());
            errorMsg.append("\nActual tree structure:\n");
            errorMsg.append(TreeDiffer.snapshot(actualElement).toString());
            Assertions.fail(errorMsg.toString());
        }
    }

    /**
     * Asserts that element has expected child count and prints tree on failure.
     */
    private void assertChildCount(UIElement<?> element, int expectedCount, String context) {
        TreeDiffer.TreeSnapshot snapshot = TreeDiffer.snapshot(element);
        int actualCount = snapshot.getChildren().size();
        if (actualCount != expectedCount) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(context).append(": Expected ").append(expectedCount)
                    .append(" children but got ").append(actualCount).append("\n");
            errorMsg.append("Current tree structure:\n");
            errorMsg.append(snapshot.toString());
            Assertions.fail(errorMsg.toString());
        }
    }

    /**
     * Prints detailed debug information about an element and its tree.
     */
    private void debugPrintTree(UIElement<?> element, String label) {
        System.out.println("\n=== " + label + " ===");
        System.out.println("Element type: " + element.getClass().getSimpleName());
        System.out.println("Blueprint type: " + element.getBlueprint().getClass().getSimpleName());
        System.out.println("Lifecycle: " + element.getLifecycle());
        System.out.println("Dependencies: " + element.getDependencies());
        System.out.println("\nTree structure:");
        System.out.println(TreeDiffer.snapshot(element));
    }

    // ==================== Part 1: DSL Construction Tests ====================

    @Nested
    @DisplayName("DSL Construction Tests")
    class DSLConstructionTests {

        @Test
        @DisplayName("Should build simple column layout")
        void testSimpleColumn(MinecraftServer server) {
            var column = Column(() -> {
                Text("Item 1");
                Text("Item 2");
                Text("Item 3");
            });

            Assertions.assertNotNull(column);
            Assertions.assertEquals(3, column.children().size());

            // Verify children types
            for (Blueprint child : column.children()) {
                Assertions.assertInstanceOf(Blueprint.class, child);
            }
        }

        @Test
        @DisplayName("Should build nested row and column layout")
        void testNestedLayout(MinecraftServer server) {
            var ui = Column(() -> {
                Text("Header");
                Row(() -> {
                    Text("Left");
                    Text("Center");
                    Text("Right");
                });
                Text("Footer");
            });

            Assertions.assertEquals(3, ui.children().size());

            // Verify middle child is a row with 3 children
            var row = ui.children().get(1);
            Assertions.assertInstanceOf(CompositeBlueprint.class, row);
            Assertions.assertEquals(3, ((CompositeBlueprint) row).getChildren().size());
        }

        @Test
        @DisplayName("Should build deeply nested structures")
        void testDeeplyNested(MinecraftServer server) {
            var ui = Column(() -> {
                Row(() -> {
                    Column(() -> {
                        Row(() -> {
                            Text("Deep Nested");
                        });
                    });
                });
            });

            // Navigate to the deepest text
            var row1 = (CompositeBlueprint) ui.children().get(0);
            var col = (CompositeBlueprint) row1.getChildren().get(0);
            var row2 = (CompositeBlueprint) col.getChildren().get(0);

            Assertions.assertEquals(1, row2.getChildren().size());
        }

        @Test
        @DisplayName("Should support conditional rendering with If")
        void testConditionalRendering(MinecraftServer server) {
            Signal<Boolean> condition = signal(true);

            var ui = Column(() -> {
                Text("Always");
                If(condition::get,
                    () -> Text("Visible"),
                    () -> Text("Hidden")
                );
            });

            Assertions.assertEquals(2, ui.children().size());
        }

        @Test
        @DisplayName("Should support list rendering with ForEach")
        void testListRendering(MinecraftServer server) {
            var items = List.of("Apple", "Banana", "Cherry");

            var ui = Column(() -> {
                ForEachScoped(() -> items, item -> {
                    Text(item);
                });
            });

            Assertions.assertEquals(1, ui.children().size()); // ForEach creates one child
        }

        @Test
        @DisplayName("Should support keyed elements")
        void testKeyedElements(MinecraftServer server) {
            var ui = Column(() -> {
                // Use Text(Key, String) instead of Text(String).withKey(Key)
                // because Text(String) already adds to scope before withKey is called
                Text(key("item-1"), "Item 1");
                Text(key("item-2"), "Item 2");
                Text(key("item-3"), "Item 3");
            });

            var children = ui.children();
            Assertions.assertNotNull(children.get(0).key(), "First element should have a key");
            Assertions.assertNotNull(children.get(1).key(), "Second element should have a key");
            Assertions.assertNotNull(children.get(2).key(), "Third element should have a key");
            Assertions.assertNotEquals(children.get(0).key(), children.get(1).key(),
                "Different elements should have different keys");
        }

        @Test
        @DisplayName("Should support container builder pattern")
        void testContainerBuilder(MinecraftServer server) {
            var ui = container()
                .padding(10, 20, 30, 40)
                .background(0xFF123456)
                .size(200, 100)
                .build(() -> {
                    Text("Content");
                });

            Assertions.assertEquals(10, ui.paddingTop());
            Assertions.assertEquals(20, ui.paddingRight());
            Assertions.assertEquals(30, ui.paddingBottom());
            Assertions.assertEquals(40, ui.paddingLeft());
            Assertions.assertEquals(0xFF123456, ui.backgroundColor());
            Assertions.assertEquals(200, ui.width());
            Assertions.assertEquals(100, ui.height());
        }

        @Test
        @DisplayName("Should properly isolate nested scopes")
        void testNestedScopes(MinecraftServer server) {
            var outer = Column(() -> {
                Text("Outer 1");
                var inner = Row(() -> {
                    Text("Inner 1");
                    Text("Inner 2");
                });
                // Inner scope should not leak into outer
                Assertions.assertEquals(2, inner.children().size());
                Text("Outer 2");
            });

            // Outer should have exactly 3 children
            Assertions.assertEquals(3, outer.children().size());
        }

        @Test
        @DisplayName("Should support Stateful blueprints")
        void testStatefulBlueprint(MinecraftServer server) {
            Signal<String> title = signal("Hello");

            var ui = Stateful(() -> Column(() -> {
                Text(() -> title.get());
            }));

            Assertions.assertInstanceOf(StatefulBlueprint.class, ui);

            // The builder should produce a Column
            Blueprint child = ui.getBuilder().get();
            Assertions.assertInstanceOf(CompositeBlueprint.class, child);
        }

        @Test
        @DisplayName("Should support complex real-world UI")
        void testComplexUI(MinecraftServer server) {
            Signal<List<String>> todos = signal(List.of("Task 1", "Task 2"));
            Signal<String> filter = signal("all");

            var ui = Column(10, () -> {
                // Header
                container()
                    .padding(8)
                    .background(0xFF2196F3)
                    .build(() -> {
                        Text("Todo App", 0xFFFFFFFF);
                    });

                // Filter buttons
                Row(5, () -> {
                    Button("All", () -> filter.set("all"));
                    Button("Active", () -> filter.set("active"));
                    Button("Done", () -> filter.set("done"));
                });

                // Todo list
                ForEachScoped(todos::get, todo -> {
                    Row(5, () -> {
                        Text("• " + todo);
                        Button("Delete", () -> {
                        });
                    });
                });

                // Footer
                Row(() -> {
                    Button("Add", () -> {
                    });
                    Button("Clear", () -> todos.set(List.of()));
                });
            });

            // Header + Filter Row + ForEach + Footer Row = 4
            Assertions.assertEquals(4, ui.children().size());
        }
    }

    // ==================== Part 2: State Tracking Tests ====================

    @Nested
    @DisplayName("State Tracking Tests")
    class StateTrackingTests {

        @Test
        @DisplayName("Should track Signal dependencies during build")
        void testSignalTracking(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            // Signal must be accessed during build(), not just wrapped in a Supplier
            var ui = Stateful(() -> {
                // Access signal during build - this will be tracked
                int value = count.get();
                return Column(() -> {
                    Text("Count: " + value);
                });
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Take snapshot for debugging
            TreeDiffer.TreeSnapshot snapshot = TreeDiffer.snapshot(element);

            // Element should have captured the count signal as dependency
            if (element.getDependencies().isEmpty()) {
                System.err.println("=== DEPENDENCY TRACKING FAILURE ===");
                System.err.println("Element tree snapshot:\n" + snapshot);
                System.err.println("Build owner summary:\n" + debugBuildOwner.getSummary());
                Assertions.fail("StatefulElement should have dependencies when Signal is accessed in build()");
            }
            Assertions.assertTrue(element.getDependencies().contains(count),
                "StatefulElement should contain the accessed Signal as dependency. " +
                "Dependencies: " + element.getDependencies());
        }

        @Test
        @DisplayName("Should mark element dirty when Signal changes")
        void testDirtyOnSignalChange(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            var ui = Stateful(() -> {
                // Access signal during build
                int value = count.get();
                return Column(() -> {
                    Text("Count: " + value);
                });
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Take initial snapshot
            TreeDiffer.TreeSnapshot beforeSnapshot = TreeDiffer.snapshot(element);

            // Initially no dirty elements
            if (debugBuildOwner.hasDirtyElements()) {
                System.err.println("=== UNEXPECTED DIRTY STATE ===");
                System.err.println("Tree snapshot:\n" + beforeSnapshot);
                System.err.println("Build owner summary:\n" + debugBuildOwner.getSummary());
                Assertions.fail("Should have no dirty elements after initial mount");
            }

            // Change signal
            count.set(1);

            // Element should be marked dirty
            if (!debugBuildOwner.hasDirtyElements()) {
                System.err.println("=== DIRTY STATE NOT SET ===");
                System.err.println("Tree snapshot (before change):\n" + beforeSnapshot);
                System.err.println("Build owner summary:\n" + debugBuildOwner.getSummary());
                Assertions.fail("Element should be marked dirty when Signal changes");
            }
        }

        @Test
        @DisplayName("Should rebuild element when buildScope is called")
        void testRebuildOnBuildScope(MinecraftServer server) {
            Signal<Integer> count = signal(0);
            AtomicInteger buildCount = new AtomicInteger(0);

            var ui = Stateful(() -> {
                buildCount.incrementAndGet();
                // Access signal during build
                int value = count.get();
                return Column(() -> {
                    Text("Count: " + value);
                });
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            int initialBuildCount = buildCount.get();
            Assertions.assertEquals(1, initialBuildCount,
                "Build should have been called once during mount");

            // Take snapshot before change
            TreeDiffer.TreeSnapshot beforeChange = TreeDiffer.snapshot(element);

            // Change signal and rebuild
            count.set(1);
            debugBuildOwner.buildScope();

            // Take snapshot after change
            TreeDiffer.TreeSnapshot afterChange = TreeDiffer.snapshot(element);

            // Build should have been called again
            if (buildCount.get() != 2) {
                System.err.println("Build count mismatch!");
                System.err.println("Before change tree:\n" + beforeChange);
                System.err.println("After change tree:\n" + afterChange);
                System.err.println("DebugBuildOwner summary:\n" + debugBuildOwner.getSummary());
                Assertions.fail("Build should have been called again after Signal change and buildScope, " +
                                "but was called " + buildCount.get() + " times");
            }

            // No more dirty elements
            Assertions.assertFalse(debugBuildOwner.hasDirtyElements(),
                "Should have no dirty elements after buildScope");
        }

        @Test
        @DisplayName("Should track rebuild count correctly")
        void testRebuildCount(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            var ui = Stateful(() -> {
                int value = count.get();
                return Column(() -> {
                    Text("Count: " + value);
                });
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Take initial snapshot
            TreeDiffer.TreeSnapshot initialSnapshot = TreeDiffer.snapshot(element);

            // Initial rebuild count for this specific element
            int initialCount = debugBuildOwner.getRebuildCount(element);

            // Trigger multiple rebuilds
            List<TreeDiffer.TreeSnapshot> snapshots = new ArrayList<>();
            snapshots.add(initialSnapshot);

            for (int i = 1; i <= 5; i++) {
                count.set(i);
                debugBuildOwner.buildScope();
                snapshots.add(TreeDiffer.snapshot(element));
            }

            // Should have recorded 5 rebuild events for this specific element
            int finalCount = debugBuildOwner.getRebuildCount(element);
            if (finalCount != initialCount + 5) {
                System.err.println("=== REBUILD COUNT MISMATCH ===");
                System.err.println("Expected: " + (initialCount + 5) + ", Actual: " + finalCount);
                System.err.println("Build owner summary:\n" + debugBuildOwner.getSummary());
                System.err.println("\n=== TREE EVOLUTION ===");
                for (int i = 0; i < snapshots.size(); i++) {
                    System.err.println("State " + i + ":\n" + snapshots.get(i));
                }
                Assertions.fail("Should have recorded 5 rebuild events for the Stateful element");
            }
        }

        @Test
        @DisplayName("Should only rebuild affected elements")
        void testSelectiveRebuild(MinecraftServer server) {
            Signal<Integer> signal1 = signal(0);
            Signal<Integer> signal2 = signal(0);

            AtomicInteger element1Builds = new AtomicInteger(0);
            AtomicInteger element2Builds = new AtomicInteger(0);

            var ui = Column(() -> {
                Stateful(() -> {
                    element1Builds.incrementAndGet();
                    int value = signal1.get();
                    return Text("Signal1: " + value);
                });
                Stateful(() -> {
                    element2Builds.incrementAndGet();
                    int value = signal2.get();
                    return Text("Signal2: " + value);
                });
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Take initial snapshot
            TreeDiffer.TreeSnapshot beforeChange = TreeDiffer.snapshot(element);

            int initial1 = element1Builds.get();
            int initial2 = element2Builds.get();

            // Change only signal1
            signal1.set(1);
            debugBuildOwner.buildScope();

            // Take snapshot after change
            TreeDiffer.TreeSnapshot afterChange = TreeDiffer.snapshot(element);

            // Only element1 should have rebuilt
            if (element1Builds.get() != initial1 + 1) {
                System.err.println("=== ELEMENT1 REBUILD FAILURE ===");
                System.err.println("Before change:\n" + beforeChange);
                System.err.println("After change:\n" + afterChange);
                System.err.println("Build owner:\n" + debugBuildOwner.getSummary());
                Assertions.fail("Only element1 should have rebuilt when signal1 changes");
            }

            if (element2Builds.get() != initial2) {
                System.err.println("=== UNEXPECTED ELEMENT2 REBUILD ===");
                System.err.println("Before change:\n" + beforeChange);
                System.err.println("After change:\n" + afterChange);
                System.err.println("Build owner:\n" + debugBuildOwner.getSummary());
                Assertions.fail("Element2 should not rebuild when only signal1 changes");
            }
        }

        @Test
        @DisplayName("Should handle multiple Signal dependencies")
        void testMultipleDependencies(MinecraftServer server) {
            Signal<String> firstName = signal("John");
            Signal<String> lastName = signal("Doe");
            AtomicInteger buildCount = new AtomicInteger(0);

            var ui = Stateful(() -> {
                buildCount.incrementAndGet();
                // Access both signals during build
                String first = firstName.get();
                String last = lastName.get();
                return Text(first + " " + last);
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Should depend on both signals
            Assertions.assertTrue(element.getDependencies().contains(firstName),
                "Should depend on firstName Signal");
            Assertions.assertTrue(element.getDependencies().contains(lastName),
                "Should depend on lastName Signal");

            int initialBuilds = buildCount.get();

            // Change firstName
            firstName.set("Jane");
            debugBuildOwner.buildScope();
            Assertions.assertEquals(initialBuilds + 1, buildCount.get(),
                "Should rebuild when firstName changes");

            // Change lastName
            lastName.set("Smith");
            debugBuildOwner.buildScope();
            Assertions.assertEquals(initialBuilds + 2, buildCount.get(),
                "Should rebuild when lastName changes");
        }

        @Test
        @DisplayName("Should update subscriptions on rebuild")
        void testSubscriptionUpdate(MinecraftServer server) {
            Signal<Boolean> useSignal1 = signal(true);
            Signal<Integer> signal1 = signal(0);
            Signal<Integer> signal2 = signal(0);

            var ui = Stateful(() -> {
                // Conditionally depend on different signals
                boolean useFirst = useSignal1.get();
                if (useFirst) {
                    int value = signal1.get();
                    return Text("Signal1: " + value);
                } else {
                    int value = signal2.get();
                    return Text("Signal2: " + value);
                }
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Initially depends on useSignal1 and signal1
            Assertions.assertTrue(element.getDependencies().contains(useSignal1),
                "Should depend on useSignal1");
            Assertions.assertTrue(element.getDependencies().contains(signal1),
                "Should depend on signal1 when useSignal1 is true");

            // Switch to signal2
            useSignal1.set(false);
            debugBuildOwner.buildScope();

            // Now should depend on useSignal1 and signal2
            // (signal1 dependency should be removed after rebuild)
            Assertions.assertTrue(element.getDependencies().contains(useSignal1),
                "Should still depend on useSignal1 after rebuild");
        }

        @Test
        @DisplayName("Should handle rapid consecutive updates")
        void testRapidUpdates(MinecraftServer server) {
            Signal<Integer> count = signal(0);
            AtomicInteger buildCount = new AtomicInteger(0);

            var ui = Stateful(() -> {
                buildCount.incrementAndGet();
                int value = count.get();
                return Text("Count: " + value);
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            int initialBuilds = buildCount.get();

            // Rapid updates without buildScope
            for (int i = 1; i <= 10; i++) {
                count.set(i);
            }

            // Only one buildScope call
            debugBuildOwner.buildScope();

            // Should have only one additional build (coalesced)
            Assertions.assertEquals(initialBuilds + 1, buildCount.get());
        }

        @Test
        @DisplayName("Should not rebuild unmounted elements")
        void testNoRebuildAfterUnmount(MinecraftServer server) {
            Signal<Integer> count = signal(0);
            AtomicInteger buildCount = new AtomicInteger(0);

            var ui = Stateful(() -> {
                buildCount.incrementAndGet();
                int value = count.get();
                return Text("Count: " + value);
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            int initialBuilds = buildCount.get();

            // Unmount the element
            element.unmount();

            // Change signal
            count.set(1);

            // Try to rebuild - should not actually rebuild
            debugBuildOwner.buildScope();

            // Build count should not have increased
            Assertions.assertEquals(initialBuilds, buildCount.get(),
                "Should not rebuild unmounted elements");
        }
    }

    // ==================== Part 3: Debug Tools Tests ====================

    @Nested
    @DisplayName("Debug Tools Tests")
    class DebugToolsTests {

        @Test
        @DisplayName("DebugBuildOwner should track scheduled elements")
        void testScheduledElementTracking(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            var ui = Stateful(() -> {
                int value = count.get();
                return Text("Count: " + value);
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Change signal
            count.set(1);

            // Should have pending elements before buildScope
            Assertions.assertTrue(debugBuildOwner.hasDirtyElements(),
                "Should have dirty elements after Signal change");
        }

        @Test
        @DisplayName("DebugBuildOwner should record rebuild history")
        void testRebuildHistory(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            var ui = Stateful(() -> {
                int value = count.get();
                return Text("Count: " + value);
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Take initial snapshot
            TreeDiffer.TreeSnapshot beforeSnapshot = TreeDiffer.snapshot(element);

            // Trigger rebuilds
            count.set(1);
            debugBuildOwner.buildScope();

            TreeDiffer.TreeSnapshot midSnapshot = TreeDiffer.snapshot(element);

            count.set(2);
            debugBuildOwner.buildScope();

            TreeDiffer.TreeSnapshot afterSnapshot = TreeDiffer.snapshot(element);

            var history = debugBuildOwner.getRebuildHistory();

            if (history.isEmpty()) {
                System.err.println("=== REBUILD HISTORY EMPTY ===");
                System.err.println("Before snapshot:\n" + beforeSnapshot);
                System.err.println("Mid snapshot:\n" + midSnapshot);
                System.err.println("After snapshot:\n" + afterSnapshot);
                System.err.println("Build owner summary:\n" + debugBuildOwner.getSummary());
                Assertions.fail("Rebuild history should not be empty after multiple rebuilds");
            }

            // Check history entries have correct data
            System.out.println("=== REBUILD HISTORY (" + history.size() + " events) ===");
            for (var event : history) {
                System.out.println("  - " + event.elementType() + " (blueprint: " + event.blueprintType() +
                                   ", rebuild #" + event.rebuildCount() + ")");
                Assertions.assertNotNull(event.element(), "Event element should not be null");
                Assertions.assertNotNull(event.elementType(), "Event elementType should not be null");
                Assertions.assertNotNull(event.blueprintType(), "Event blueprintType should not be null");
                Assertions.assertTrue(event.rebuildCount() > 0, "Rebuild count should be positive");
            }
        }

        @Test
        @DisplayName("DebugBuildOwner should provide accurate summary")
        void testDebugSummary(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            var ui = Stateful(() -> {
                int value = count.get();
                return Text("Count: " + value);
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            count.set(1);
            debugBuildOwner.buildScope();

            String summary = debugBuildOwner.getSummary();
            Assertions.assertNotNull(summary, "Summary should not be null");
            Assertions.assertTrue(summary.contains("DebugBuildOwner Summary"),
                "Summary should contain header");
            Assertions.assertTrue(summary.contains("Total rebuild events"),
                "Summary should contain rebuild events info");
        }

        @Test
        @DisplayName("TreeDiffer should detect matching trees")
        void testTreeDifferMatch(MinecraftServer server) {
            var blueprint = Column(() -> {
                Text("Item 1");
                Text("Item 2");
            });

            UIElement<?> element = blueprint.createElement();
            mountWithDebug(element);

            var result = TreeDiffer.compare(blueprint, element);

            Assertions.assertTrue(result.matches());
            Assertions.assertTrue(result.differences().isEmpty());
        }

        @Test
        @DisplayName("TreeDiffer should detect child count mismatch")
        void testTreeDifferChildCountMismatch(MinecraftServer server) {
            // Build element with 2 children
            var originalBlueprint = Column(() -> {
                Text("Item 1");
                Text("Item 2");
            });
            UIElement<?> element = originalBlueprint.createElement();
            mountWithDebug(element);

            // Compare against blueprint with 3 children
            var differentBlueprint = Column(() -> {
                Text("Item 1");
                Text("Item 2");
                Text("Item 3");
            });

            var result = TreeDiffer.compare(differentBlueprint, element);

            Assertions.assertFalse(result.matches());
            Assertions.assertTrue(result.differences().stream()
                                        .anyMatch(d -> d.type() == TreeDiffer.DiffType.CHILD_COUNT_MISMATCH));
        }

        @Test
        @DisplayName("TreeSnapshot should capture tree structure")
        void testTreeSnapshot(MinecraftServer server) {
            var blueprint = Column(() -> {
                Text("Item 1");
                Row(() -> {
                    Text("Nested");
                });
            });

            UIElement<?> element = blueprint.createElement();
            mountWithDebug(element);

            var snapshot = TreeDiffer.snapshot(element);
            String snapshotString = snapshot.toString();

            Assertions.assertNotNull(snapshotString);
            Assertions.assertTrue(snapshotString.contains("CompositeElement"));
        }

        @Test
        @DisplayName("TreeSnapshot should detect changes after rebuild")
        void testSnapshotComparison(MinecraftServer server) {
            Signal<Integer> itemCount = signal(2);

            var ui = Stateful(() -> {
                int count = itemCount.get();
                return Column(() -> {
                    for (int i = 0; i < count; i++) {
                        final int itemIndex = i;
                        Text("Item " + itemIndex);
                    }
                });
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Take initial snapshot
            var snapshot1 = TreeDiffer.snapshot(element);
            System.out.println("=== SNAPSHOT 1 (2 items) ===");
            System.out.println(snapshot1);

            // Change state and rebuild
            itemCount.set(3);
            debugBuildOwner.buildScope();

            // Take new snapshot
            var snapshot2 = TreeDiffer.snapshot(element);
            System.out.println("\n=== SNAPSHOT 2 (3 items) ===");
            System.out.println(snapshot2);

            // Snapshots should differ
            var diff = snapshot1.compareTo(snapshot2);

            if (diff.matches()) {
                System.err.println("=== SNAPSHOT COMPARISON FAILURE ===");
                System.err.println("Expected snapshots to differ, but they match!");
                System.err.println("Snapshot 1:\n" + snapshot1);
                System.err.println("Snapshot 2:\n" + snapshot2);
                System.err.println("Diff details:\n" + diff);
                Assertions.fail("Snapshots should differ after item count change");
            }

            System.out.println("\n=== DIFF RESULT ===");
            System.out.println("Matches: " + diff.matches());
            System.out.println("Details: " + diff);
        }

        @Test
        @DisplayName("Should clear debug history")
        void testClearHistory(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            var ui = Stateful(() -> {
                int value = count.get();
                return Text("Count: " + value);
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            count.set(1);
            debugBuildOwner.buildScope();

            Assertions.assertFalse(debugBuildOwner.getRebuildHistory().isEmpty(),
                "Rebuild history should not be empty before clear");

            debugBuildOwner.clearHistory();

            Assertions.assertTrue(debugBuildOwner.getRebuildHistory().isEmpty(),
                "Rebuild history should be empty after clear");
            Assertions.assertEquals(0, debugBuildOwner.getTotalRebuildCount(),
                "Total rebuild count should be 0 after clear");
        }

        @Test
        @DisplayName("Should track per-element rebuild counts")
        void testPerElementRebuildCount(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            var ui = Stateful(() -> {
                int value = count.get();
                return Text("Count: " + value);
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Rebuild 3 times
            for (int i = 1; i <= 3; i++) {
                count.set(i);
                debugBuildOwner.buildScope();
            }

            Assertions.assertEquals(3, debugBuildOwner.getRebuildCount(element),
                "Element should have been rebuilt 3 times");
        }

        @Test
        @DisplayName("DebugBuildOwner should support enabling/disabling tracking")
        void testTrackingToggle(MinecraftServer server) {
            Signal<Integer> count = signal(0);

            var ui = Stateful(() -> {
                int value = count.get();
                return Text("Count: " + value);
            });

            Stateless(() -> Co);

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Disable tracking
            debugBuildOwner.setTrackingEnabled(false);

            count.set(1);
            debugBuildOwner.buildScope();

            // Should not have recorded the rebuild
            Assertions.assertEquals(0, debugBuildOwner.getTotalRebuildCount(),
                "Should not record rebuilds when tracking is disabled");

            // Re-enable tracking
            debugBuildOwner.setTrackingEnabled(true);

            count.set(2);
            debugBuildOwner.buildScope();

            // Should have recorded this rebuild
            Assertions.assertEquals(1, debugBuildOwner.getTotalRebuildCount(),
                "Should record rebuilds when tracking is re-enabled");
        }
    }

    // ==================== Integration Tests ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Full lifecycle: build -> update -> rebuild -> verify")
        void testFullLifecycle(MinecraftServer server) {
            Signal<List<String>> items = signal(new ArrayList<>(List.of("A", "B", "C")));

            var ui = Stateful(() -> {
                List<String> currentItems = items.get();
                return Column(() -> {
                    Text("Count: " + currentItems.size());
                    ForEachScoped(() -> currentItems, item -> {
                        Text(item);
                    });
                });
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Verify initial structure
            var initialSnapshot = TreeDiffer.snapshot(element);
            System.out.println("Initial tree:");
            System.out.println(initialSnapshot);

            // Modify state
            var newItems = new ArrayList<>(items.get());
            newItems.add("D");
            items.set(newItems);

            // Rebuild
            debugBuildOwner.buildScope();

            // Verify structure changed
            var afterSnapshot = TreeDiffer.snapshot(element);
            System.out.println("After adding item:");
            System.out.println(afterSnapshot);

            var diff = initialSnapshot.compareTo(afterSnapshot);
            Assertions.assertFalse(diff.matches(),
                "Tree structure should change after adding item");

            // Print debug info
            System.out.println(debugBuildOwner.getSummary());
        }

        @Test
        @DisplayName("Complex state interactions")
        void testComplexStateInteractions(MinecraftServer server) {
            Signal<Boolean> showDetails = signal(false);
            Signal<Integer> selectedIndex = signal(-1);
            Signal<List<String>> items = signal(List.of("Item 1", "Item 2", "Item 3"));

            var ui = Stateful(() -> {
                // Access signals in build
                boolean show = showDetails.get();
                int selected = selectedIndex.get();
                List<String> currentItems = items.get();

                return Column(() -> {
                    // Header with toggle
                    Row(() -> {
                        Text("Items: " + currentItems.size());
                        Button(show ? "Hide" : "Show", () -> {
                            showDetails.update(v -> !v);
                        });
                    });

                    // Conditional details
                    If(() -> show, () -> {
                        Text("Selected: " + selected);
                    });

                    // Item list
                    ForEachScoped(() -> currentItems, item -> {
                        Row(() -> {
                            Text(item);
                            Button("Select", () -> {
                                selectedIndex.set(currentItems.indexOf(item));
                            });
                        });
                    });
                });
            });

            UIElement<?> element = ui.createElement();
            mountWithDebug(element);

            // Toggle details
            showDetails.set(true);
            debugBuildOwner.buildScope();

            // Select an item
            selectedIndex.set(1);
            debugBuildOwner.buildScope();

            // Verify rebuild tracking
            Assertions.assertTrue(debugBuildOwner.getTotalRebuildCount() > 0);

            System.out.println("After state changes:");
            System.out.println(TreeDiffer.snapshot(element));
            System.out.println(debugBuildOwner.getSummary());
        }
    }


    @Nested
    @DisplayName("Scoped State Capture Tests")
    class ScopedStateCapture {


        @Test
        void captureScopedState(MinecraftServer server) {

        }

    }

}
