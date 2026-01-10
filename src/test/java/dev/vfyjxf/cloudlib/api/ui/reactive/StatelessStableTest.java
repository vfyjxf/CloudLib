package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.TextBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Stateless and Stable Blueprint/Element types.
 */
@DisplayName("Stateless and Stable Element Tests")
class StatelessStableTest {

    @Nested
    @DisplayName("StatelessBlueprint/Element")
    class StatelessTests {

        @Test
        @DisplayName("Stateless component builds correctly")
        void statelessBuildWorks() {
            // Arrange
            StatelessBlueprint blueprint = StatelessBlueprint.of(() -> Text("Hello"));

            // Act
            UIElement<?> element = blueprint.createElement();
            BuildOwner owner = new BuildOwner();
            element.mount(null, owner);

            // Assert
            assertInstanceOf(StatelessElement.class, element);
            StatelessElement<?> statelessElement = (StatelessElement<?>) element;
            assertNotNull(statelessElement.getChild());
        }

        @Test
        @DisplayName("Stateless component does NOT track reactive state")
        void statelessDoesNotTrackState() {
            // Arrange
            Signal<String> text = Signal.of("Initial");
            int[] buildCount = {0};

            StatelessBlueprint blueprint = StatelessBlueprint.of(() -> {
                buildCount[0]++;
                return Text(text.get()); // Read signal but should NOT subscribe
            });

            // Act
            BuildOwner owner = new BuildOwner();
            UIElement<?> element = blueprint.createElement();
            element.mount(null, owner);

            assertEquals(1, buildCount[0], "Should build once");

            // Change the signal - should NOT trigger rebuild
            text.set("Changed");
            owner.buildScope(); // Run any scheduled builds

            // Assert - no rebuild should have happened
            assertEquals(1, buildCount[0], "Stateless should NOT rebuild on signal change");
        }

        @Test
        @DisplayName("Stateless component rebuilds when parent updates it")
        void statelessRebuildsOnParentUpdate() {
            // Arrange
            int[] buildCount = {0};

            StatelessBlueprint blueprint = StatelessBlueprint.of(() -> {
                buildCount[0]++;
                return Text("Content");
            });

            // Act
            BuildOwner owner = new BuildOwner();
            StatelessElement<StatelessBlueprint> element = 
                    (StatelessElement<StatelessBlueprint>) blueprint.createElement();
            element.mount(null, owner);

            assertEquals(1, buildCount[0], "Initial build");

            // Explicitly update (as parent would)
            element.update(blueprint);
            element.performRebuild();

            // Assert - rebuild happened because parent triggered it
            assertEquals(2, buildCount[0], "Should rebuild on explicit update");
        }

        @Test
        @DisplayName("DSL Stateless() works in scope")
        void dslStatelessInScope() {
            // Arrange & Act
            Blueprint column = Column(() -> {
                Stateless(() -> Text("Static content"));
                Text("Dynamic content");
            });

            // Assert
            UIElement<?> element = column.createElement();
            element.mount(null, new BuildOwner());

            // Should have both children
            int[] childCount = {0};
            element.visitChildren(child -> childCount[0]++);
            assertEquals(2, childCount[0]);
        }
    }

    @Nested
    @DisplayName("StableBlueprint/Element")
    class StableTests {

        @Test
        @DisplayName("Stable component builds correctly")
        void stableBuildWorks() {
            // Arrange
            StableBlueprint blueprint = StableBlueprint.of(() -> Text("Forever"));

            // Act
            UIElement<?> element = blueprint.createElement();
            BuildOwner owner = new BuildOwner();
            element.mount(null, owner);

            // Assert
            assertInstanceOf(StableElement.class, element);
            StableElement<?> stableElement = (StableElement<?>) element;
            assertNotNull(stableElement.getChild());
            assertTrue(stableElement.isStable(), "Should be stable after first build");
        }

        @Test
        @DisplayName("Stable component NEVER rebuilds after first build")
        void stableNeverRebuilds() {
            // Arrange
            int[] buildCount = {0};

            StableBlueprint blueprint = StableBlueprint.of(() -> {
                buildCount[0]++;
                return Text("Constant");
            });

            // Act
            BuildOwner owner = new BuildOwner();
            StableElement<StableBlueprint> element = 
                    (StableElement<StableBlueprint>) blueprint.createElement();
            element.mount(null, owner);

            assertEquals(1, buildCount[0], "Initial build");

            // Try various ways to trigger rebuild - all should be ignored

            // 1. markNeedsBuild() should be ignored
            element.markNeedsBuild();
            owner.buildScope();
            assertEquals(1, buildCount[0], "markNeedsBuild should be ignored");

            // 2. Direct performRebuild() should be ignored
            element.performRebuild();
            assertEquals(1, buildCount[0], "performRebuild should be ignored");

            // 3. update() should be ignored
            element.update(blueprint);
            element.performRebuild();
            assertEquals(1, buildCount[0], "update should be ignored");
        }

        @Test
        @DisplayName("Stable component ignores reactive state changes")
        void stableIgnoresStateChanges() {
            // Arrange
            Signal<String> text = Signal.of("Before");
            int[] buildCount = {0};

            // Even though we read a signal, stable element ignores everything
            StableBlueprint blueprint = StableBlueprint.of(() -> {
                buildCount[0]++;
                return Text(text.get());
            });

            // Act
            BuildOwner owner = new BuildOwner();
            UIElement<?> element = blueprint.createElement();
            element.mount(null, owner);

            assertEquals(1, buildCount[0], "Initial build");

            // Change signal
            text.set("After");
            owner.buildScope();

            // Assert - still only one build
            assertEquals(1, buildCount[0], "Stable should ignore signal changes");
        }

        @Test
        @DisplayName("DSL Stable() works in scope")
        void dslStableInScope() {
            // Arrange & Act
            Blueprint column = Column(() -> {
                Stable(() -> Text("Static header"));
                Text("Dynamic body");
            });

            // Assert
            UIElement<?> element = column.createElement();
            element.mount(null, new BuildOwner());

            int[] childCount = {0};
            element.visitChildren(child -> childCount[0]++);
            assertEquals(2, childCount[0]);
        }

        @Test
        @DisplayName("Stable element can be unmounted normally")
        void stableUnmountWorks() {
            // Arrange
            StableBlueprint blueprint = StableBlueprint.of(() -> Text("Content"));
            StableElement<?> element = (StableElement<?>) blueprint.createElement();
            element.mount(null, new BuildOwner());

            // Act
            element.unmount();

            // Assert
            assertFalse(element.isMounted());
            assertNull(element.getChild());
        }
    }

    @Nested
    @DisplayName("Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("Compare build counts: Stateful vs Stateless vs Stable")
        void compareBuildCounts() {
            // Arrange
            Signal<Integer> counter = Signal.of(0);

            int[] statefulBuildCount = {0};
            int[] statelessBuildCount = {0};
            int[] stableBuildCount = {0};

            StatefulBlueprint stateful = StatefulBlueprint.of(() -> {
                statefulBuildCount[0]++;
                return Text("Count: " + counter.get());
            });

            StatelessBlueprint stateless = StatelessBlueprint.of(() -> {
                statelessBuildCount[0]++;
                return Text("Count: " + counter.get());
            });

            StableBlueprint stable = StableBlueprint.of(() -> {
                stableBuildCount[0]++;
                return Text("Count: " + counter.get());
            });

            // Act - mount all
            BuildOwner owner = new BuildOwner();
            UIElement<?> statefulElement = stateful.createElement();
            UIElement<?> statelessElement = stateless.createElement();
            UIElement<?> stableElement = stable.createElement();

            statefulElement.mount(null, owner);
            statelessElement.mount(null, owner);
            stableElement.mount(null, owner);

            // All should have built once
            assertEquals(1, statefulBuildCount[0], "Stateful initial");
            assertEquals(1, statelessBuildCount[0], "Stateless initial");
            assertEquals(1, stableBuildCount[0], "Stable initial");

            // Change signal
            counter.set(1);
            owner.buildScope();

            // Only Stateful should rebuild
            assertEquals(2, statefulBuildCount[0], "Stateful rebuilds on signal change");
            assertEquals(1, statelessBuildCount[0], "Stateless does NOT rebuild on signal change");
            assertEquals(1, stableBuildCount[0], "Stable NEVER rebuilds");

            // Change signal again
            counter.set(2);
            owner.buildScope();

            assertEquals(3, statefulBuildCount[0], "Stateful rebuilds again");
            assertEquals(1, statelessBuildCount[0], "Stateless still unchanged");
            assertEquals(1, stableBuildCount[0], "Stable still unchanged");

            // Manually update all elements
            ((UIElement<StatefulBlueprint>) statefulElement).update(stateful);
            ((UIElement<StatelessBlueprint>) statelessElement).update(stateless);
            ((UIElement<StableBlueprint>) stableElement).update(stable);

            statefulElement.performRebuild();
            statelessElement.performRebuild();
            stableElement.performRebuild();

            // Stateful and Stateless rebuild, Stable ignores
            assertEquals(4, statefulBuildCount[0], "Stateful rebuilds on update");
            assertEquals(2, statelessBuildCount[0], "Stateless rebuilds on explicit update");
            assertEquals(1, stableBuildCount[0], "Stable ignores even explicit updates");
        }

        @Test
        @DisplayName("All types create correct element types")
        void correctElementTypes() {
            StatefulBlueprint stateful = StatefulBlueprint.of(() -> Text(""));
            StatelessBlueprint stateless = StatelessBlueprint.of(() -> Text(""));
            StableBlueprint stable = StableBlueprint.of(() -> Text(""));

            assertInstanceOf(StatefulElement.class, stateful.createElement());
            assertInstanceOf(StatelessElement.class, stateless.createElement());
            assertInstanceOf(StableElement.class, stable.createElement());
        }
    }
}
