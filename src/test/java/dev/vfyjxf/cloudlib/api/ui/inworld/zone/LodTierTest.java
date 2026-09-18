package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LodTierTest {

    @Test
    void ordinalIsTheDegradeOrder() {
        assertEquals(0, LodTier.full.ordinal());
        assertEquals(1, LodTier.compact.ordinal());
        assertEquals(2, LodTier.icon.ordinal());
        assertEquals(3, LodTier.clustered.ordinal());
        assertEquals(4, LodTier.hidden.ordinal());
        // every step up the ordinal ladder is a degradation
        assertTrue(LodTier.full.ordinal() < LodTier.compact.ordinal());
        assertTrue(LodTier.compact.ordinal() < LodTier.icon.ordinal());
        assertTrue(LodTier.icon.ordinal() < LodTier.clustered.ordinal());
        assertTrue(LodTier.clustered.ordinal() < LodTier.hidden.ordinal());
    }

    @Test
    void degradeWalksTheLadderOneRungAtATime() {
        assertEquals(LodTier.compact, LodTier.full.degrade());
        assertEquals(LodTier.icon, LodTier.compact.degrade());
        assertEquals(LodTier.clustered, LodTier.icon.degrade());
        assertEquals(LodTier.hidden, LodTier.clustered.degrade());
    }

    @Test
    void hiddenIsTerminal() {
        assertEquals(LodTier.hidden, LodTier.hidden.degrade());
        assertFalse(LodTier.hidden.canDegrade());
    }

    @Test
    void everyNonHiddenRungCanDegrade() {
        assertTrue(LodTier.full.canDegrade());
        assertTrue(LodTier.compact.canDegrade());
        assertTrue(LodTier.icon.canDegrade());
        assertTrue(LodTier.clustered.canDegrade());
    }

    @Test
    void degradeNeverSkipsRungsFromFullToHidden() {
        LodTier tier = LodTier.full;
        int steps = 0;
        while (tier.canDegrade()) {
            tier = tier.degrade();
            steps++;
        }
        assertEquals(LodTier.hidden, tier);
        assertEquals(4, steps);
    }
}
