package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementModeTest {

    @Test
    void declarationOrderIsTheLadderFromWorldToScreen() {
        assertEquals(0, PlacementMode.worldAttached.ordinal());
        assertEquals(1, PlacementMode.worldFloating.ordinal());
        assertEquals(2, PlacementMode.screenAnchored.ordinal());
        assertEquals(3, PlacementMode.screenDisplaced.ordinal());
        assertEquals(4, PlacementMode.screenEdge.ordinal());
    }

    @Test
    void nextTowardsScreenWalksTheLadder() {
        assertEquals(PlacementMode.worldFloating, PlacementMode.worldAttached.nextTowardsScreen());
        assertEquals(PlacementMode.screenAnchored, PlacementMode.worldFloating.nextTowardsScreen());
        assertEquals(PlacementMode.screenDisplaced, PlacementMode.screenAnchored.nextTowardsScreen());
        assertEquals(PlacementMode.screenEdge, PlacementMode.screenDisplaced.nextTowardsScreen());
    }

    @Test
    void screenEdgeIsTerminal() {
        assertEquals(PlacementMode.screenEdge, PlacementMode.screenEdge.nextTowardsScreen());
    }

    @Test
    void walkingTheFullLadderEndsAtTheScreenEdge() {
        PlacementMode mode = PlacementMode.worldAttached;
        for (int i = 0; i < 10; i++) {
            mode = mode.nextTowardsScreen();
        }
        assertEquals(PlacementMode.screenEdge, mode);
    }

    @Test
    void worldAndScreenHalvesPartitionTheLadder() {
        assertTrue(PlacementMode.worldAttached.isWorldSpace());
        assertTrue(PlacementMode.worldFloating.isWorldSpace());
        assertFalse(PlacementMode.screenAnchored.isWorldSpace());
        assertFalse(PlacementMode.screenDisplaced.isWorldSpace());
        assertFalse(PlacementMode.screenEdge.isWorldSpace());

        assertFalse(PlacementMode.worldAttached.isScreenSpace());
        assertTrue(PlacementMode.screenAnchored.isScreenSpace());
        assertTrue(PlacementMode.screenDisplaced.isScreenSpace());
        assertTrue(PlacementMode.screenEdge.isScreenSpace());
    }
}
