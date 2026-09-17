package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpacePolicyTest {

    @Test
    void activePushesAndIsPushed() {
        assertTrue(SpacePolicy.active.pushesOthers());
        assertTrue(SpacePolicy.active.pushable());
        assertTrue(SpacePolicy.active.participates());
        assertFalse(SpacePolicy.active.occlusionExempt());
    }

    @Test
    void passiveIsOnlyPushed() {
        assertFalse(SpacePolicy.passive.pushesOthers());
        assertTrue(SpacePolicy.passive.pushable());
        assertTrue(SpacePolicy.passive.participates());
        assertFalse(SpacePolicy.passive.occlusionExempt());
    }

    @Test
    void fixedIsExemptFromOcclusionByDefault() {
        assertFalse(SpacePolicy.fixed.pushesOthers());
        assertFalse(SpacePolicy.fixed.pushable());
        assertTrue(SpacePolicy.fixed.participates());
        assertTrue(SpacePolicy.fixed.occlusionExempt());
    }

    @Test
    void ghostParticipatesInNothing() {
        assertFalse(SpacePolicy.ghost.pushesOthers());
        assertFalse(SpacePolicy.ghost.pushable());
        assertFalse(SpacePolicy.ghost.participates());
        assertTrue(SpacePolicy.ghost.occlusionExempt());
    }
}
