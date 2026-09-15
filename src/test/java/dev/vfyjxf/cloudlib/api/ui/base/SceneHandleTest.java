package dev.vfyjxf.cloudlib.api.ui.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SceneHandle functionality.
 */
@DisplayName("SceneHandle Tests")
class SceneHandleTest {

    private Scene scene;

    @BeforeEach
    void setUp() {
        // Create a minimal scene for testing
        WidgetGroup<Widget> root = new WidgetGroup<>();
        scene = new Scene(root);
    }

    private SceneHandle createHandle() {
        return scene.globalHandle;
    }

    @Nested
    @DisplayName("Basic Cleanup Operations")
    class BasicCleanupTests {

        @Test
        @DisplayName("onCleanup registers and executes cleanup action")
        void onCleanupExecutes() {
            SceneHandle handle = createHandle();
            AtomicBoolean cleaned = new AtomicBoolean(false);

            handle.onCleanup(() -> cleaned.set(true));
            assertFalse(cleaned.get(), "Cleanup should not run before cleanup() is called");

            handle.cleanup();
            assertTrue(cleaned.get(), "Cleanup action should have executed");
        }

        @Test
        @DisplayName("Multiple cleanup actions execute in LIFO order")
        void cleanupInLifoOrder() {
            SceneHandle handle = createHandle();
            List<Integer> order = new ArrayList<>();

            handle.onCleanup(() -> order.add(1));
            handle.onCleanup(() -> order.add(2));
            handle.onCleanup(() -> order.add(3));

            handle.cleanup();

            assertEquals(List.of(3, 2, 1), order, "Cleanup should execute in LIFO order");
        }

        @Test
        @DisplayName("cleanup() is idempotent")
        void cleanupIsIdempotent() {
            SceneHandle handle = createHandle();
            AtomicInteger counter = new AtomicInteger(0);

            handle.onCleanup(counter::incrementAndGet);

            handle.cleanup();
            handle.cleanup();
            handle.cleanup();

            assertEquals(1, counter.get(), "Cleanup should only execute once");
        }

        @Test
        @DisplayName("hasCleanupActions returns correct state")
        void hasCleanupActionsState() {
            SceneHandle handle = createHandle();
            assertFalse(handle.hasCleanupActions());

            handle.onCleanup(() -> {});
            assertTrue(handle.hasCleanupActions());

            handle.cleanup();
            assertFalse(handle.hasCleanupActions());
        }

        @Test
        @DisplayName("Cannot register cleanup after cleaned")
        void cannotRegisterAfterCleaned() {
            SceneHandle handle = createHandle();
            handle.cleanup();

            assertThrows(IllegalStateException.class, () -> handle.onCleanup(() -> {}));
        }
    }

    @Nested
    @DisplayName("Widget Integration Scenarios")
    class WidgetIntegrationTests {

        /**
         * Simulates a child widget attaching data to parent's handle
         * during mount, and cleanup happening when parent unmounts.
         */
        @Test
        @DisplayName("Child attaches to parent handle, cleaned on parent unmount")
        void childAttachesToParentHandle() {
            // Simulate parent widget's scene handle
            Widget parent = new Widget();
            SceneHandle parentHandle = scene.handleOf(parent);

            // Simulate parent's global registry
            List<String> parentRegistry = new ArrayList<>();

            // Child mounts and registers to parent's registry via handle
            parentRegistry.add("child-data");
            parentHandle.onCleanup(() -> parentRegistry.remove("child-data"));

            assertTrue(parentRegistry.contains("child-data"),
                "Child's data should be in parent's registry");

            // Parent unmounts -> cleanup
            parentHandle.cleanup();

            assertFalse(parentRegistry.contains("child-data"),
                "Child's data should be removed when parent unmounts");
        }

        /**
         * Simulates multiple children attaching to same parent handle.
         */
        @Test
        @DisplayName("Multiple children attach to same parent handle")
        void multipleChildrenAttachToParent() {
            Widget parent = new Widget();
            SceneHandle parentHandle = scene.handleOf(parent);
            List<String> registry = new ArrayList<>();

            // Child 1 attaches
            registry.add("child1");
            parentHandle.onCleanup(() -> registry.remove("child1"));

            // Child 2 attaches
            registry.add("child2");
            parentHandle.onCleanup(() -> registry.remove("child2"));

            // Child 3 attaches
            registry.add("child3");
            parentHandle.onCleanup(() -> registry.remove("child3"));

            assertEquals(3, registry.size());

            parentHandle.cleanup();

            assertTrue(registry.isEmpty(), "All children's data should be cleaned");
        }

        /**
         * Simulates nested parent-child cleanup chain.
         */
        @Test
        @DisplayName("Nested cleanup chain: grandparent -> parent -> child")
        void nestedCleanupChain() {
            List<String> cleanupOrder = new ArrayList<>();

            // Grandparent's handle
            Widget grandparent = new Widget();
            SceneHandle grandparentHandle = scene.handleOf(grandparent);
            grandparentHandle.onCleanup(() -> cleanupOrder.add("grandparent"));

            // Parent attaches to grandparent
            Widget parentWidget = new Widget();
            SceneHandle parentHandle = scene.handleOf(parentWidget);
            grandparentHandle.onCleanup(() -> {
                cleanupOrder.add("parent-detach-from-grandparent");
                parentHandle.cleanup(); // Parent cleanup when grandparent unmounts
            });
            parentHandle.onCleanup(() -> cleanupOrder.add("parent"));

            // Child attaches to parent
            parentHandle.onCleanup(() -> cleanupOrder.add("child-detach-from-parent"));

            // Grandparent unmounts -> cascading cleanup
            grandparentHandle.cleanup();

            // LIFO order: grandparent's last-registered runs first (which triggers parent cleanup),
            // then grandparent's first-registered runs
            assertEquals(List.of(
                "parent-detach-from-grandparent",
                "child-detach-from-parent",
                "parent",
                "grandparent"
            ), cleanupOrder, "Cleanup should cascade properly in LIFO order");
        }
    }

    @Nested
    @DisplayName("Scope Navigation")
    class ScopeNavigationTests {

        @Test
        @DisplayName("scene() returns scene-level handle")
        void sceneReturnsSceneHandle() {
            Widget widget = new Widget();
            SceneHandle widgetHandle = scene.handleOf(widget);

            SceneHandle sceneHandle = widgetHandle.scene();
            assertNotNull(sceneHandle);
            assertSame(scene.globalHandle, sceneHandle, "scene() should return the scene's handle");
        }

        @Test
        @DisplayName("Scope navigation allows cross-scope cleanup registration")
        void crossScopeCleanupRegistration() {
            Widget widget = new Widget();
            AtomicBoolean sceneCleanedUp = new AtomicBoolean(false);
            AtomicBoolean widgetCleanedUp = new AtomicBoolean(false);

            // Start from widget handle
            SceneHandle widgetHandle = scene.handleOf(widget);
            widgetHandle.onCleanup(() -> widgetCleanedUp.set(true));

            // Navigate to scene and register there
            widgetHandle.scene().onCleanup(() -> sceneCleanedUp.set(true));

            // Widget cleanup doesn't affect scene
            widgetHandle.cleanup();
            assertTrue(widgetCleanedUp.get());
            assertFalse(sceneCleanedUp.get());

            // Scene cleanup runs separately
            scene.globalHandle.cleanup();
            assertTrue(sceneCleanedUp.get());
        }
    }
}
