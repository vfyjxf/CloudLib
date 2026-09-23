package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Size;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The total arbitration order is a specification, not an implementation
 * detail: {@code spaceKind (world → tracked → panel) → priority (desc) →
 * sticky-first → registration order}, and the coordinator's outputs must
 * follow it exactly — no hash order, no randomness.
 */
class ArbitrationOrderTest {

    private static final int width = 400;
    private static final int height = 300;

    private static CoordinationResult firstFrame(InworldCoordinator coordinator) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, 0, 1.0 / 60.0));
    }

    @Test
    void worldAnchoredClaimsSpaceBeforeHigherPriorityPanels() {
        TestElement world = TestElement.arbitrated("world", 200, 150, new Size(100, 40));
        TestElement panel = TestElement.arbitrated("panel", 200, 150, new Size(100, 50)).withKind(SpaceKind.panel);
        panel = panel.withPriority(100); // registered first AND higher priority — kind still wins
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        coordinator.register(panel);
        coordinator.register(world);

        assertEquals(List.of("world", "panel"), ids(coordinator.arbitrationOrderSnapshot()));

        CoordinationResult result = firstFrame(coordinator);
        assertEquals(List.of("world", "panel"), placementIds(result));

        FloatRect worldRect = Objects.requireNonNull(result.placementOf("world")).screenRect();
        FloatRect panelRect = Objects.requireNonNull(result.placementOf("panel")).screenRect();
        // the world element keeps the contested primary candidate...
        assertEquals(150, worldRect.x(), 0.01);
        assertEquals(130, worldRect.y(), 0.01);
        // ...and the panel, registered first with priority 100, still moves over
        assertTrue(!worldRect.intersects(panelRect), "panel must not overlap the world element");
        assertEquals(worldRect.right() + 8, panelRect.x(), 0.01);
    }

    @Test
    void trackedSortsBetweenWorldAndPanel() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement panel = TestElement.arbitrated("panel", 200, 150, new Size(80, 30)).withKind(SpaceKind.panel);
        TestElement tracked = TestElement.arbitrated("tracked", 200, 150, new Size(80, 30)).withKind(SpaceKind.tracked);
        TestElement world = TestElement.arbitrated("world", 200, 150, new Size(80, 30));
        coordinator.register(panel);
        coordinator.register(tracked);
        coordinator.register(world);

        assertEquals(List.of("world", "tracked", "panel"), ids(coordinator.arbitrationOrderSnapshot()));
    }

    @Test
    void priorityBreaksTiesDescending() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement low = TestElement.arbitrated("low", 200, 150, new Size(100, 40));
        TestElement high = TestElement.arbitrated("high", 200, 150, new Size(100, 40)).withPriority(10);
        coordinator.register(low);
        coordinator.register(high);

        assertEquals(List.of("high", "low"), ids(coordinator.arbitrationOrderSnapshot()));
        CoordinationResult result = firstFrame(coordinator);
        assertEquals(150, Objects.requireNonNull(result.placementOf("high")).screenRect().x(), 0.01);
        assertTrue(
            Objects.requireNonNull(result.placementOf("low")).screenRect().x() > 150,
            "low priority must yield the spot"
        );
    }

    @Test
    void stickyBreaksPriorityTiesEvenWhenRegisteredLater() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement plain = TestElement.arbitrated("plain", 200, 150, new Size(100, 40));
        TestElement sticky = TestElement.arbitrated("sticky", 200, 150, new Size(100, 40)).withSticky();
        coordinator.register(plain);
        coordinator.register(sticky);

        assertEquals(List.of("sticky", "plain"), ids(coordinator.arbitrationOrderSnapshot()));
        CoordinationResult result = firstFrame(coordinator);
        assertEquals(150, Objects.requireNonNull(result.placementOf("sticky")).screenRect().x(), 0.01);
        assertTrue(Objects.requireNonNull(result.placementOf("plain")).screenRect().x() > 150);
    }

    @Test
    void registrationOrderIsTheFinalTieBreak() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement first = TestElement.arbitrated("first", 200, 150, new Size(100, 40));
        TestElement second = TestElement.arbitrated("second", 200, 150, new Size(100, 40));
        coordinator.register(second);
        coordinator.register(first);

        assertEquals(List.of("second", "first"), ids(coordinator.arbitrationOrderSnapshot()));
        CoordinationResult result = firstFrame(coordinator);
        assertEquals(150, Objects.requireNonNull(result.placementOf("second")).screenRect().x(), 0.01);
        assertTrue(Objects.requireNonNull(result.placementOf("first")).screenRect().x() > 150);
    }

    @Test
    void orderStaysStableAcrossFrames() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        coordinator.register(TestElement.arbitrated("a", 100, 100, new Size(60, 20)).withKind(SpaceKind.panel));
        coordinator.register(TestElement.arbitrated("b", 120, 210, new Size(60, 20)).withSticky());
        coordinator.register(
            TestElement.arbitrated("c", 300, 80, new Size(60, 20)).withKind(SpaceKind.tracked).withPriority(3)
        );
        coordinator.register(TestElement.arbitrated("d", 300, 200, new Size(60, 20)).withPriority(1));

        double now = 0;
        List<String> first = ids(coordinator.arbitrationOrderSnapshot());
        for (int i = 0; i < 40; i++) {
            now += 1.0 / 60.0;
            coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, 1.0 / 60.0));
            assertEquals(first, ids(coordinator.arbitrationOrderSnapshot()), "frame " + (i + 2));
        }
    }

    private static List<String> ids(List<InworldElement> elements) {
        return elements.stream().map(InworldElement::id).toList();
    }

    private static List<String> placementIds(CoordinationResult result) {
        return result.placements().stream().map(InworldPlacement::elementId).toList();
    }
}
