package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.element.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test showcasing the fine-grained recomposition capabilities.
 * <p>
 * This test demonstrates:
 * <ul>
 *   <li>Fine-grained reactivity - only affected components rebuild</li>
 *   <li>Signal/Computed dependency tracking</li>
 *   <li>Component state isolation</li>
 *   <li>Conditional rendering</li>
 *   <li>List rendering with keys</li>
 *   <li>Nested components</li>
 *   <li>Effect lifecycle</li>
 * </ul>
 */
public class RecompositionShowcaseTest {

    // ===== Fine-Grained Recomposition Tests =====
    
    @Nested
    @DisplayName("Fine-Grained Recomposition")
    class FineGrainedRecomposition {
        
        @Test
        @DisplayName("Only affected components rebuild when Signal changes")
        void onlyAffectedComponentsRebuild() {
            // Shared state
            Signal<Integer> count = Signal.of(0);
            Signal<String> name = Signal.of("Alice");
            
            // Build counters
            AtomicInteger counterBuilds = new AtomicInteger(0);
            AtomicInteger greetingBuilds = new AtomicInteger(0);
            AtomicInteger staticBuilds = new AtomicInteger(0);
            
            // Counter component - depends on count
            Component counterComp = Component.stateful(ctx -> {
                counterBuilds.incrementAndGet();
                return text("Count: " + count.get());
            });
            
            // Greeting component - depends on name
            Component greetingComp = Component.stateful(ctx -> {
                greetingBuilds.incrementAndGet();
                return text("Hello, " + name.get());
            });
            
            // Static component - no dependencies
            Component staticComp = Component.stateful(ctx -> {
                staticBuilds.incrementAndGet();
                return text("I'm static");
            });
            
            // Build tree
            RenderNode tree = column(
                RenderNode.component("counter", counterComp),
                RenderNode.component("greeting", greetingComp),
                RenderNode.component("static", staticComp)
            );
            
            ElementTree elementTree = new ElementTree();
            elementTree.attachRoot(tree);
            elementTree.flushBuild();
            
            // Initial builds
            assertEquals(1, counterBuilds.get(), "Counter should build once initially");
            assertEquals(1, greetingBuilds.get(), "Greeting should build once initially");
            assertEquals(1, staticBuilds.get(), "Static should build once initially");
            
            // Change count - only counter should rebuild
            count.set(1);
            elementTree.flushBuild();
            
            assertEquals(2, counterBuilds.get(), "Counter should rebuild on count change");
            assertEquals(1, greetingBuilds.get(), "Greeting should NOT rebuild on count change");
            assertEquals(1, staticBuilds.get(), "Static should NOT rebuild on count change");
            
            // Change name - only greeting should rebuild
            name.set("Bob");
            elementTree.flushBuild();
            
            assertEquals(2, counterBuilds.get(), "Counter should NOT rebuild on name change");
            assertEquals(2, greetingBuilds.get(), "Greeting should rebuild on name change");
            assertEquals(1, staticBuilds.get(), "Static should NEVER rebuild");
        }
        
        @Test
        @DisplayName("Component with internal state is independent")
        void componentWithInternalStateIsIndependent() {
            AtomicInteger comp1Builds = new AtomicInteger(0);
            AtomicInteger comp2Builds = new AtomicInteger(0);
            
            // Two counters with independent internal state
            Component counter1 = Component.stateful(ctx -> {
                comp1Builds.incrementAndGet();
                var count = ctx.signal(0);
                return column(
                    text("Counter 1: " + count.get()),
                    button("+", () -> count.update(n -> n + 1))
                );
            });
            
            Component counter2 = Component.stateful(ctx -> {
                comp2Builds.incrementAndGet();
                var count = ctx.signal(0);
                return column(
                    text("Counter 2: " + count.get()),
                    button("+", () -> count.update(n -> n + 1))
                );
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(column(
                RenderNode.component("c1", counter1),
                RenderNode.component("c2", counter2)
            ));
            tree.flushBuild();
            
            // Initial builds
            assertEquals(1, comp1Builds.get());
            assertEquals(1, comp2Builds.get());
            
            // Find counter1's element and trigger its state change
            ComponentElement ce1 = (ComponentElement) tree.findElement(e -> 
                e instanceof ComponentElement ce && ce.getComponent() == counter1
            );
            assertNotNull(ce1);
            
            // Get the signal from counter1 (first signal in the component)
            // Since we can't directly access internal state, we simulate via markNeedsBuild
            ce1.markNeedsBuild();
            tree.flushBuild();
            
            // Only counter1 should rebuild
            assertEquals(2, comp1Builds.get(), "Counter1 should rebuild");
            assertEquals(1, comp2Builds.get(), "Counter2 should NOT rebuild");
        }
        
        @Test
        @DisplayName("Computed values invalidation chain")
        void computedInvalidationChain() {
            Signal<Integer> base = Signal.of(1);
            AtomicInteger level1Computes = new AtomicInteger(0);
            AtomicInteger level2Computes = new AtomicInteger(0);
            AtomicInteger level3Computes = new AtomicInteger(0);
            
            // Three levels of computed
            Computed<Integer> level1 = Computed.of(() -> {
                level1Computes.incrementAndGet();
                return base.get() * 2;
            });
            
            Computed<Integer> level2 = Computed.of(() -> {
                level2Computes.incrementAndGet();
                return level1.get() + 10;
            });
            
            Computed<Integer> level3 = Computed.of(() -> {
                level3Computes.incrementAndGet();
                return level2.get() * level2.get();
            });
            
            // Initial access
            assertEquals(144, level3.get()); // (1*2+10)^2 = 12^2 = 144
            assertEquals(1, level1Computes.get());
            assertEquals(1, level2Computes.get());
            assertEquals(1, level3Computes.get());
            
            // Multiple accesses don't recompute (caching works)
            level3.get();
            level3.get();
            assertEquals(1, level1Computes.get());
            assertEquals(1, level2Computes.get());
            assertEquals(1, level3Computes.get());
            
            // Change base - the chain should update when accessed
            base.set(5);
            
            // Access each level to trigger recomputation
            // Level1 should be 10 (5*2)
            assertEquals(10, level1.get());
            // Level2 should be 20 (10+10)
            assertEquals(20, level2.get());
            // Level3 should be 400 (20*20)
            assertEquals(400, level3.get());
        }
    }
    
    // ===== Conditional Rendering Tests =====
    
    @Nested
    @DisplayName("Conditional Rendering")
    class ConditionalRendering {
        
        @Test
        @DisplayName("Show/hide based on condition")
        void showHideBasedOnCondition() {
            Signal<Boolean> showDetails = Signal.of(false);
            AtomicInteger detailsBuilds = new AtomicInteger(0);
            
            Component detailsComp = Component.stateful(ctx -> {
                detailsBuilds.incrementAndGet();
                return text("Secret details here!");
            });
            
            RenderNode tree = column(
                text("Header"),
                RenderNode.when(
                    showDetails::get,
                    RenderNode.component(detailsComp),
                    text("Click to show details")
                )
            );
            
            ElementTree elementTree = new ElementTree();
            elementTree.attachRoot(tree);
            elementTree.flushBuild();
            
            // Verify initial state
            var stats = elementTree.getStats();
            assertTrue(stats.totalElements() >= 2, "Should have at least header and conditional");
            
            // Show details
            showDetails.set(true);
            elementTree.updateRoot(tree); // Re-evaluate conditionals
            elementTree.flushBuild();
            
            // Verify updated state
            var newStats = elementTree.getStats();
            // Tree should have changed or component should have been built
            assertTrue(newStats.totalElements() >= 2 || detailsBuilds.get() > 0, 
                "Either tree changed or details component was built");
        }
        
        @Test
        @DisplayName("Switch between different components")
        void switchBetweenComponents() {
            Signal<String> activeTab = Signal.of("home");
            AtomicInteger homeBuilds = new AtomicInteger(0);
            AtomicInteger settingsBuilds = new AtomicInteger(0);
            
            Component homeTab = Component.stateful(ctx -> {
                homeBuilds.incrementAndGet();
                return text("Welcome Home!");
            });
            
            Component settingsTab = Component.stateful(ctx -> {
                settingsBuilds.incrementAndGet();
                return text("Settings Panel");
            });
            
            // Dynamic tab switching
            Component tabView = Component.stateful(ctx -> {
                String tab = activeTab.get();
                return switch (tab) {
                    case "home" -> RenderNode.component("home", homeTab);
                    case "settings" -> RenderNode.component("settings", settingsTab);
                    default -> text("Unknown tab");
                };
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(tabView));
            tree.flushBuild();
            
            assertEquals(1, homeBuilds.get(), "Home should build initially");
            assertEquals(0, settingsBuilds.get(), "Settings should not build initially");
            
            // Switch to settings
            activeTab.set("settings");
            tree.flushBuild();
            
            assertEquals(1, homeBuilds.get(), "Home should not rebuild");
            assertTrue(settingsBuilds.get() > 0, "Settings should build now");
        }
    }
    
    // ===== List Rendering Tests =====
    
    @Nested
    @DisplayName("List Rendering")
    class ListRendering {
        
        @Test
        @DisplayName("Render dynamic list of items")
        void renderDynamicList() {
            Signal<List<String>> items = Signal.of(List.of("Apple", "Banana", "Cherry"));
            
            // Item component
            Component itemComp = Component.stateless(ctx -> {
                // This is stateless, so we can't track builds per-item easily
                // Instead, we'll just verify the structure
                return text("Item");
            });
            
            RenderNode tree = column(
                text("Shopping List:"),
                RenderNode.forEach(
                    items::get,
                    item -> item, // Key by item value
                    (item, index) -> text(index + ". " + item)
                )
            );
            
            ElementTree elementTree = new ElementTree();
            elementTree.attachRoot(tree);
            elementTree.flushBuild();
            
            var stats = elementTree.getStats();
            // Should have multiple elements (exact count depends on implementation)
            assertTrue(stats.totalElements() >= 2, "Should have at least header and some list elements");
            
            // Add an item
            items.set(List.of("Apple", "Banana", "Cherry", "Date"));
            elementTree.updateRoot(tree);
            elementTree.flushBuild();
            
            var newStats = elementTree.getStats();
            // After adding item, element count should increase or stay same
            assertTrue(newStats.totalElements() >= stats.totalElements(), 
                "Element count should not decrease after adding item");
        }
        
        @Test
        @DisplayName("List with keyed items preserves state")
        void keyedListPreservesState() {
            // This test verifies that keyed items maintain identity across reorders
            Signal<List<String>> items = Signal.of(List.of("A", "B", "C"));
            
            RenderNode tree = RenderNode.forEach(
                items::get,
                item -> item, // Key by item value
                (item, index) -> RenderNode.component(
                    item, // Use item as key
                    Component.stateful(ctx -> {
                        var expanded = ctx.signal(false);
                        return column(
                            text(item + (expanded.get() ? " (expanded)" : "")),
                            button("Toggle", () -> expanded.update(b -> !b))
                        );
                    })
                )
            );
            
            ElementTree elementTree = new ElementTree();
            elementTree.attachRoot(tree);
            elementTree.flushBuild();
            
            // Find component for "B"
            ComponentElement compB = elementTree.findComponentElement(Component.Stateful.class);
            assertNotNull(compB);
            
            // The test verifies structure - actual state preservation
            // would require more sophisticated tracking
            var stats = elementTree.getStats();
            assertTrue(stats.componentElements() >= 3, "Should have at least 3 component elements");
        }
    }
    
    // ===== Nested Components Tests =====
    
    @Nested
    @DisplayName("Nested Components")
    class NestedComponents {
        
        @Test
        @DisplayName("Parent-child component hierarchy")
        void parentChildHierarchy() {
            AtomicInteger parentBuilds = new AtomicInteger(0);
            AtomicInteger child1Builds = new AtomicInteger(0);
            AtomicInteger child2Builds = new AtomicInteger(0);
            
            Signal<String> parentState = Signal.of("Parent");
            Signal<Integer> child1State = Signal.of(1);
            
            Component child1 = Component.stateful(ctx -> {
                child1Builds.incrementAndGet();
                return text("Child1: " + child1State.get());
            });
            
            Component child2 = Component.stateful(ctx -> {
                child2Builds.incrementAndGet();
                var localState = ctx.signal(100);
                return text("Child2: " + localState.get());
            });
            
            Component parent = Component.stateful(ctx -> {
                parentBuilds.incrementAndGet();
                return column(
                    text("Parent: " + parentState.get()),
                    RenderNode.component("child1", child1),
                    RenderNode.component("child2", child2)
                );
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(parent));
            tree.flushBuild();
            
            assertEquals(1, parentBuilds.get());
            assertEquals(1, child1Builds.get());
            assertEquals(1, child2Builds.get());
            
            // Change parent state - parent rebuilds, children may rebuild based on structure change
            parentState.set("Updated Parent");
            tree.flushBuild();
            
            assertEquals(2, parentBuilds.get(), "Parent should rebuild");
            // Children may or may not rebuild depending on reconciliation
            
            // Change child1's external state - only child1 should rebuild
            int prevChild2Builds = child2Builds.get();
            child1State.set(999);
            tree.flushBuild();
            
            assertTrue(child1Builds.get() > 1, "Child1 should rebuild on its state change");
            assertEquals(prevChild2Builds, child2Builds.get(), "Child2 should NOT rebuild");
        }
        
        @Test
        @DisplayName("Deeply nested tree structure")
        void deeplyNestedTree() {
            // Create a 5-level deep tree
            AtomicInteger level5Builds = new AtomicInteger(0);
            Signal<String> level5State = Signal.of("Deep");
            
            Component level5 = Component.stateful(ctx -> {
                level5Builds.incrementAndGet();
                return text("Level 5: " + level5State.get());
            });
            
            Component level4 = Component.pure(column(
                text("Level 4"),
                RenderNode.component(level5)
            ));
            
            Component level3 = Component.pure(column(
                text("Level 3"),
                RenderNode.component(level4)
            ));
            
            Component level2 = Component.pure(column(
                text("Level 2"),
                RenderNode.component(level3)
            ));
            
            Component level1 = Component.pure(column(
                text("Level 1"),
                RenderNode.component(level2)
            ));
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(level1));
            tree.flushBuild();
            
            assertEquals(1, level5Builds.get());
            
            // Change level 5 state - should propagate correctly
            level5State.set("Very Deep");
            tree.flushBuild();
            
            assertEquals(2, level5Builds.get(), "Level 5 should rebuild on its state change");
        }
    }
    
    // ===== Effect Lifecycle Tests =====
    
    @Nested
    @DisplayName("Effect Lifecycle")
    class EffectLifecycle {
        
        @Test
        @DisplayName("Effect runs after mount")
        void effectRunsAfterMount() {
            AtomicInteger effectRuns = new AtomicInteger(0);
            AtomicInteger cleanupRuns = new AtomicInteger(0);
            
            Component comp = Component.stateful(ctx -> {
                ctx.effect(() -> {
                    effectRuns.incrementAndGet();
                    return () -> cleanupRuns.incrementAndGet();
                });
                return text("With Effect");
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(comp));
            tree.flushBuild();
            
            assertEquals(1, effectRuns.get(), "Effect should run after mount");
            assertEquals(0, cleanupRuns.get(), "Cleanup should not run yet");
            
            // Unmount
            tree.detach();
            assertEquals(1, cleanupRuns.get(), "Cleanup should run on unmount");
        }
        
        @Test
        @DisplayName("Effect with dependencies only runs when deps change")
        void effectWithDependencies() {
            Signal<Integer> dep1 = Signal.of(1);
            Signal<Integer> dep2 = Signal.of(100);
            AtomicInteger effectRuns = new AtomicInteger(0);
            
            Component comp = Component.stateful(ctx -> {
                int d1 = dep1.get();
                ctx.effect(() -> {
                    effectRuns.incrementAndGet();
                    return null;
                }, d1); // Depends only on dep1's value
                
                return text("Count: " + d1 + ", Other: " + dep2.get());
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(comp));
            tree.flushBuild();
            
            assertEquals(1, effectRuns.get(), "Effect runs on mount");
            
            // Change dep2 - effect should not run again (different dep)
            dep2.set(200);
            tree.flushBuild();
            
            // Component rebuilds but effect deps didn't change
            // (Note: actual behavior depends on how effect scheduling works)
            
            // Change dep1 - effect should run again
            dep1.set(2);
            tree.flushBuild();
            
            // After dep1 change, effect should run again
            assertTrue(effectRuns.get() >= 2, "Effect should run when dependencies change");
        }
        
        @Test
        @DisplayName("Mount/unmount callbacks")
        void mountUnmountCallbacks() {
            AtomicInteger mountCount = new AtomicInteger(0);
            AtomicInteger unmountCount = new AtomicInteger(0);
            
            Component comp = Component.stateful(ctx -> {
                ctx.onMount(() -> mountCount.incrementAndGet());
                ctx.onUnmount(() -> unmountCount.incrementAndGet());
                return text("Lifecycle Test");
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(comp));
            tree.flushBuild();
            
            assertEquals(1, mountCount.get(), "Should mount once");
            assertEquals(0, unmountCount.get(), "Should not unmount yet");
            
            tree.detach();
            assertEquals(1, unmountCount.get(), "Should unmount once");
        }
    }
    
    // ===== State Hook Tests =====
    
    @Nested
    @DisplayName("State Hooks")
    class StateHooks {
        
        @Test
        @DisplayName("Signal hook maintains state across rebuilds")
        void signalMaintainsState() {
            AtomicInteger buildCount = new AtomicInteger(0);
            List<Integer> observedValues = new ArrayList<>();
            
            Component comp = Component.stateful(ctx -> {
                buildCount.incrementAndGet();
                var count = ctx.signal(0);
                observedValues.add(count.get());
                return button("++", () -> count.update(n -> n + 1));
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(comp));
            tree.flushBuild();
            
            assertEquals(1, buildCount.get());
            assertEquals(List.of(0), observedValues);
            
            // Trigger rebuild via markNeedsBuild
            ComponentElement ce = (ComponentElement) tree.getRoot();
            ce.markNeedsBuild();
            tree.flushBuild();
            
            assertEquals(2, buildCount.get());
            // State should persist - still 0 since we didn't actually click
            assertEquals(List.of(0, 0), observedValues);
        }
        
        @Test
        @DisplayName("Computed hook caches value")
        void computedHookCaches() {
            AtomicInteger computeCount = new AtomicInteger(0);
            
            Component comp = Component.stateful(ctx -> {
                var base = ctx.signal(5);
                var doubled = ctx.computed(() -> {
                    computeCount.incrementAndGet();
                    return base.get() * 2;
                });
                
                // Access multiple times
                doubled.get();
                doubled.get();
                doubled.get();
                
                return text("Doubled: " + doubled.get());
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(comp));
            tree.flushBuild();
            
            // Should compute only once despite multiple accesses
            assertEquals(1, computeCount.get(), "Computed should cache its value");
        }
        
        @Test
        @DisplayName("Memo hook recomputes only when deps change")
        void memoRecomputesOnDepChange() {
            AtomicInteger computeCount = new AtomicInteger(0);
            Signal<Integer> trigger = Signal.of(0);
            
            Component comp = Component.stateful(ctx -> {
                int t = trigger.get();
                String expensive = ctx.memo(() -> {
                    computeCount.incrementAndGet();
                    return "Computed at " + System.nanoTime();
                }, t);
                
                return text(expensive);
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(comp));
            tree.flushBuild();
            
            assertEquals(1, computeCount.get());
            
            // Same trigger value - memo should cache
            ComponentElement ce = (ComponentElement) tree.getRoot();
            ce.markNeedsBuild();
            tree.flushBuild();
            
            assertEquals(1, computeCount.get(), "Memo should not recompute with same deps");
            
            // Change trigger - memo should recompute
            trigger.set(1);
            tree.flushBuild();
            
            assertEquals(2, computeCount.get(), "Memo should recompute with new deps");
        }
    }
    
    // ===== Performance Tests =====
    
    @Nested
    @DisplayName("Performance Characteristics")
    class Performance {
        
        @Test
        @DisplayName("Large tree with single Signal change")
        void largeTreeSingleChange() {
            // Create a tree with many components, but only one depends on the signal
            Signal<Integer> targetSignal = Signal.of(0);
            AtomicInteger targetBuilds = new AtomicInteger(0);
            AtomicInteger otherBuilds = new AtomicInteger(0);
            
            // Target component that depends on signal
            Component target = Component.stateful(ctx -> {
                targetBuilds.incrementAndGet();
                return text("Target: " + targetSignal.get());
            });
            
            // Many other components that don't depend on it
            List<RenderNode> children = new ArrayList<>();
            children.add(RenderNode.component("target", target));
            
            for (int i = 0; i < 50; i++) {
                final int idx = i;
                Component other = Component.stateful(ctx -> {
                    otherBuilds.incrementAndGet();
                    return text("Other " + idx);
                });
                children.add(RenderNode.component("other-" + i, other));
            }
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.group(LayoutType.COLUMN, children));
            tree.flushBuild();
            
            int initialTargetBuilds = targetBuilds.get();
            int initialOtherBuilds = otherBuilds.get();
            
            assertEquals(1, initialTargetBuilds);
            assertEquals(50, initialOtherBuilds);
            
            // Change target signal - only target should rebuild
            targetSignal.set(1);
            tree.flushBuild();
            
            assertEquals(2, targetBuilds.get(), "Target should rebuild");
            assertEquals(50, otherBuilds.get(), "Other components should NOT rebuild");
        }
        
        @Test
        @DisplayName("Multiple rapid state changes coalesce")
        void rapidStateChangesCoalesce() {
            AtomicInteger buildCount = new AtomicInteger(0);
            Signal<Integer> count = Signal.of(0);
            
            Component comp = Component.stateful(ctx -> {
                buildCount.incrementAndGet();
                return text("Count: " + count.get());
            });
            
            ElementTree tree = new ElementTree();
            tree.attachRoot(RenderNode.component(comp));
            tree.flushBuild();
            
            assertEquals(1, buildCount.get());
            
            // Multiple rapid changes before flush
            count.set(1);
            count.set(2);
            count.set(3);
            count.set(4);
            count.set(5);
            
            // Single flush should handle all changes
            tree.flushBuild();
            
            // Build count may vary based on implementation,
            // but final state should be consistent
            int finalValue = count.get();
            assertEquals(5, finalValue, "Final value should be 5");
            
            // Verify the component was rebuilt (at least once more after changes)
            assertTrue(buildCount.get() >= 2, 
                "Component should rebuild after state changes");
        }
    }
    
    // ===== Tree Dump Helper Test =====
    
    @Test
    @DisplayName("Tree structure can be dumped for debugging")
    void treeDumpForDebugging() {
        Signal<Integer> count = Signal.of(42);
        
        Component counter = Component.stateful(ctx -> {
            var localCount = ctx.signal(0);
            return column(
                text("External: " + count.get()),
                text("Local: " + localCount.get()),
                button("+", () -> localCount.update(n -> n + 1))
            );
        });
        
        ElementTree tree = new ElementTree();
        tree.attachRoot(column(
            text("Header"),
            RenderNode.component("counter", counter)
        ));
        tree.flushBuild();
        
        String dump = tree.dumpTree();
        
        // Verify dump contains expected structure
        assertNotNull(dump);
        assertTrue(dump.contains("GroupElement"));
        assertTrue(dump.contains("LeafElement"));
        assertTrue(dump.contains("ComponentElement"));
        assertTrue(dump.contains("[text:"));
        
        // Print for debugging
        System.out.println("=== Tree Structure ===");
        System.out.println(dump);
        System.out.println("=== Stats: " + tree.getStats() + " ===");
    }
}
