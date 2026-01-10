package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.*;

/**
 * Tests for the reactive UI Blueprint system.
 */
@ExtendWith(EphemeralTestServerProvider.class)
public class BlueprintTest {

    /**
     * Helper method to mount an element with a fresh BuildOwner.
     */
    private void mountRoot(UIElement<?> element) {
        BuildOwner buildOwner = new BuildOwner();
        element.mount(null, buildOwner);
    }

    @Test
    void testBasicColumnLayout(MinecraftServer server) {
        // Build a simple column layout
        var column = Column(() -> {
            Text("Hello");
            Text("World");
        });

        // Verify structure
        Assertions.assertNotNull(column);
        Assertions.assertEquals(2, column.children().size());

        // Print tree for debugging
        TreePrinter.print(column);
    }

    @Test
    void testNestedLayout(MinecraftServer server) {
        // Build nested layout
        var ui = Column(() -> {
            Text("Header");
            Row(() -> {
                Button("Left", () -> {});
                Text("Center");
                Button("Right", () -> {});
            });
            Text("Footer");
        });

        // Verify structure
        Assertions.assertEquals(3, ui.children().size());
        var row = ui.children().get(1);
        Assertions.assertInstanceOf(CompositeBlueprint.class, row);
        Assertions.assertEquals(3, ((CompositeBlueprint) row).getChildren().size());

        TreePrinter.print(ui);
    }

    @Test
    void testReactiveText(MinecraftServer server) {
        Signal<Integer> count = signal(0);

        var text = Text(() -> "Count: " + count.get());
        
        Assertions.assertEquals("Count: 0", text.getText());
        
        count.set(5);
        Assertions.assertEquals("Count: 5", text.getText());
    }

    @Test
    void testConditionalRendering(MinecraftServer server) {
        Signal<Boolean> showDetails = signal(false);

        var ui = Column(() -> {
            Text("Title");
            If(showDetails::get,
                () -> {
                    Text("Detail 1");
                    Text("Detail 2");
                }
            );
        });

        // Initially, should have Title + empty conditional
        Assertions.assertEquals(2, ui.children().size());

        TreePrinter.print(ui);
    }

    @Test
    void testForEachRendering(MinecraftServer server) {
        Signal<List<String>> items = signal(List.of("Apple", "Banana", "Cherry"));

        var ui = Column(() -> {
            Text("Shopping List");
            ForEachScoped(items::get, item -> {
                Text("- " + item);
            });
        });

        TreePrinter.print(ui);
    }

    @Test
    void testElementMounting(MinecraftServer server) {
        var ui = Column(() -> {
            Text("Hello");
            Text("World");
        });

        // Create and mount element
        UIElement<?> element = ui.createElement();
        mountRoot(element);

        // Verify element state
        Assertions.assertTrue(element.isMounted());
        Assertions.assertEquals(UIElement.ElementLifecycle.MOUNTED, element.getLifecycle());

        // Print element tree
        TreePrinter.print(element);
    }

    @Test
    void testStatefulElement(MinecraftServer server) {
        Signal<Integer> count = signal(0);

        var ui = Stateful(() -> {
            // Access signal during build - this will be tracked
            int value = count.get();
            return Column(() -> {
                Text("Count: " + value);
                Button("Increment", () -> count.update(n -> n + 1));
            });
        });

        UIElement<?> element = ui.createElement();
        mountRoot(element);

        // Element should track the count signal
        Assertions.assertFalse(element.getDependencies().isEmpty(),
            "StatefulElement should have dependencies when Signal is accessed in build()");

        TreePrinter.print(element);
    }

    @Test
    void testContainerBuilder(MinecraftServer server) {
        var ui = container()
                .padding(10)
                .background(0xFF333333)
                .size(200, 100)
                .build(() -> {
                    Text("Styled content");
                });

        Assertions.assertEquals(10, ui.paddingTop());
        Assertions.assertEquals(10, ui.paddingRight());
        Assertions.assertEquals(10, ui.paddingBottom());
        Assertions.assertEquals(10, ui.paddingLeft());
        Assertions.assertEquals(0xFF333333, ui.backgroundColor());
        Assertions.assertEquals(200, ui.width());
        Assertions.assertEquals(100, ui.height());

        TreePrinter.print(ui);
    }

    @Test
    void testKeyMatching(MinecraftServer server) {
        // Keys should be equal for same value
        Key key1 = key("item-1");
        Key key2 = key("item-1");
        Key key3 = key("item-2");

        Assertions.assertEquals(key1, key2);
        Assertions.assertNotEquals(key1, key3);

        // Unique keys should never match
        Key unique1 = uniqueKey();
        Key unique2 = uniqueKey();
        Assertions.assertNotEquals(unique1, unique2);
    }

    @Test
    void testScopedReceiver(MinecraftServer server) {
        // Test that scoped receiver properly collects children
        List<Blueprint> children = ScopedReceiver.buildScope(() -> {
            Text("Child 1");
            Text("Child 2");
            Row(() -> {
                Text("Nested");
            });
        });

        Assertions.assertEquals(3, children.size());
    }

    @Test
    void testNestedScopes(MinecraftServer server) {
        // Nested scopes should not interfere with each other
        var outer = Column(() -> {
            Text("Outer 1");
            var inner = Row(() -> {
                Text("Inner 1");
                Text("Inner 2");
            });
            Assertions.assertEquals(2, inner.children().size());
            Text("Outer 2");
        });

        // Outer should have 3 children (Outer 1, Row, Outer 2)
        Assertions.assertEquals(3, outer.children().size());
    }

    @Test
    void testElementReconciliation(MinecraftServer server) {
        Signal<String> text = signal("Initial");

        var ui = Column(() -> {
            Text(() -> text.get());
        });

        UIElement<?> element = ui.createElement();
        mountRoot(element);

        // Change the text
        text.set("Updated");

        // The element should be marked dirty
        // (In a real scenario, rebuild would happen on next frame)
    }

    @Test
    void testComplexExample(MinecraftServer server) {
        // A more complex example demonstrating various features
        Signal<List<String>> todos = signal(List.of("Task 1", "Task 2"));
        Signal<Boolean> showCompleted = signal(true);
        Signal<Integer> selectedIndex = signal(-1);

        var ui = Column(10, () -> {
            // Header
            container()
                    .padding(8)
                    .background(0xFF2196F3)
                    .build(() -> {
                        Text("Todo App", 0xFFFFFFFF);
                    });

            // Toggle
            Row(5, () -> {
                Text("Show completed:");
                Button(showCompleted.peek() ? "ON" : "OFF", () -> {
                    showCompleted.update(b -> !b);
                });
            });

            // Todo list
            If(() -> !todos.get().isEmpty(),
                () -> {
                    ForEachScoped(todos::get, todo -> {
                        Row(5, () -> {
                            Text("• " + todo);
                            Button("Delete", () -> {
                                // Delete logic
                            });
                        });
                    });
                },
                () -> {
                    Text("No todos yet!");
                }
            );

            // Footer
            Row(() -> {
                Button("Add Todo", () -> {
                    // Add logic
                });
                Button("Clear All", () -> {
                    todos.set(List.of());
                });
            });
        });

        UIElement<?> element = ui.createElement();
        mountRoot(element);

        System.out.println("=== Complex Example Tree ===");
        TreePrinter.print(element);
    }

    @Test
    void testBuildOwnerScheduling(MinecraftServer server) {
        Signal<Integer> count = signal(0);
        BuildOwner buildOwner = new BuildOwner();

        var ui = Stateful(() -> {
            // Access signal during build - this will be tracked
            int value = count.get();
            return Column(() -> {
                Text("Count: " + value);
            });
        });

        UIElement<?> element = ui.createElement();
        element.mount(null, buildOwner);

        // Initially no dirty elements after mount
        Assertions.assertFalse(buildOwner.hasDirtyElements(),
            "Should have no dirty elements after initial mount");

        // Change state - element should be scheduled
        count.set(1);
        Assertions.assertTrue(buildOwner.hasDirtyElements(),
            "Element should be scheduled as dirty when Signal changes");

        // Process the build scope
        buildOwner.buildScope();

        // After processing, no more dirty elements
        Assertions.assertFalse(buildOwner.hasDirtyElements(),
            "Should have no dirty elements after buildScope");
    }
}
