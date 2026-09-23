package dev.vfyjxf.cloudlib.api.ui.inworld.group;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Identity semantics and record validation of the grouping value types. */
class InworldGroupTest {

    @Test
    void equalTuplesAreTheSameGroup() {
        assertEquals(new InworldGroup("nimbus", "riders", "cart-1"), new InworldGroup("nimbus", "riders", "cart-1"));
        assertNotEquals(new InworldGroup("nimbus", "riders", "cart-1"), new InworldGroup("nimbus", "riders", "cart-2"));
        assertNotEquals(new InworldGroup("nimbus", "riders", "cart-1"), new InworldGroup("other", "riders", "cart-1"));
    }

    @Test
    void emptyPartsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new InworldGroup("", "riders", "cart-1"));
        assertThrows(IllegalArgumentException.class, () -> new InworldGroup("nimbus", "", "cart-1"));
        assertThrows(IllegalArgumentException.class, () -> new InworldGroup("nimbus", "riders", ""));
    }

    @Test
    void strategyRecordsValidateTheirParameters() {
        assertThrows(IllegalArgumentException.class, () -> new OrbitAroundAnchor(0, 14, 44, 4, 2, 20, 0.15, 8));
        assertThrows(IllegalArgumentException.class, () -> new OrbitAroundAnchor(28, 14, 44, 0, 2, 20, 0.15, 8));
        assertThrows(IllegalArgumentException.class, () -> new OrbitAroundAnchor(28, 14, 44, 4, -1, 20, 0.15, 8));
        assertThrows(IllegalArgumentException.class, () -> new OrbitAroundAnchor(28, 14, 44, 4, 2, 20, 1.0, 8));
        assertThrows(IllegalArgumentException.class, () -> new ClusterToRepresentative(1.2, 120, 8, 16, true));
        assertThrows(IllegalArgumentException.class, () -> new ClusterToRepresentative(0.6, 0, 8, 16, true));
        assertThrows(IllegalArgumentException.class, () -> new ClusterToRepresentative(0.6, 120, 0, 16, true));
        assertThrows(IllegalArgumentException.class, () -> new StackInColumn(StackInColumn.Direction.up, -1, 4, 9));
        assertThrows(IllegalArgumentException.class, () -> new StackInColumn(StackInColumn.Direction.up, 4, 0, 9));
    }

    @Test
    void presetsAreValid() {
        assertNotNull(OrbitAroundAnchor.of());
        assertNotNull(ClusterToRepresentative.of());
        assertNotNull(StackInColumn.of());
        assertNotNull(NoGrouping.instance);
        assertTrue(NoGrouping.instance instanceof GroupStrategy);
    }

    @Test
    void duplicateMemberIdsAreRejected() {
        GroupLayoutEngine engine = GroupLayoutEngine
                .of(new InworldGroup("nimbus", "riders", "cart-1"), NoGrouping.instance);
        GroupLayoutEngine.GroupFrame frame = new GroupLayoutEngine.GroupFrame(
            100,
            100,
            List.of(
                new GroupLayoutEngine.GroupMember("a", 100, 100, 20, 20),
                new GroupLayoutEngine.GroupMember("a", 110, 100, 20, 20)
            ),
            List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> engine.arrange(frame));
    }
}
