package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariantLadderTest {

    private static final VariantLadder twoRungs = VariantLadder
            .of(List.of(rung(0, 100, 40, ContentTier.full), rung(1, 60, 24, ContentTier.compact)));

    private static InworldVariant rung(int level, int w, int h, ContentTier tier) {
        return new InworldVariant(level, new Size(w, h), tier, SpacePolicy.active, true, true, 100);
    }

    @Test
    void strongestIsIndexZeroAndWeakestIsLast() {
        VariantLadder ladder = VariantLadder
                .of(List.of(rung(0, 100, 40, ContentTier.full), rung(1, 60, 24, ContentTier.compact)));

        assertEquals(0, ladder.strongest().level());
        assertEquals(1, ladder.weakest().level());
        assertEquals(2, ladder.size());
        assertEquals(rung(1, 60, 24, ContentTier.compact), ladder.variant(1));
        assertTrue(ladder.isStrongest(ladder.strongest()));
        assertTrue(ladder.isWeakest(ladder.weakest()));
    }

    @Test
    void degradeDropsOneLevelByDefault() {
        VariantLadder ladder = VariantLadder.of(
            List.of(
                rung(0, 100, 40, ContentTier.full),
                rung(1, 60, 24, ContentTier.compact),
                rung(2, 20, 12, ContentTier.iconOnly)
            )
        );

        for (RejectionReason reason : RejectionReason.values()) {
            assertEquals(1, ladder.degradeSteps(reason), "default steps for " + reason);
        }
        assertEquals(1, Objects.requireNonNull(ladder.degrade(ladder.strongest(), RejectionReason.overlap)).level());
        assertEquals(2, Objects.requireNonNull(ladder.degrade(ladder.variant(1), RejectionReason.exclusion)).level());
        assertNull(ladder.degrade(ladder.weakest(), RejectionReason.overlap));
    }

    @Test
    void degradeSkipsLevelsByReason() {
        VariantLadder ladder = VariantLadder.of(
            List.of(
                rung(0, 100, 40, ContentTier.full),
                rung(1, 60, 24, ContentTier.compact),
                rung(2, 40, 16, ContentTier.labelOnly),
                rung(3, 20, 12, ContentTier.iconOnly)
            ),
            Map.of(RejectionReason.insufficientArea, 2, RejectionReason.outOfBounds, 3)
        );

        assertEquals(2, ladder.degradeSteps(RejectionReason.insufficientArea));
        assertEquals(3, ladder.degradeSteps(RejectionReason.outOfBounds));
        assertEquals(
            2,
            Objects.requireNonNull(ladder.degrade(ladder.strongest(), RejectionReason.insufficientArea)).level()
        );
        assertEquals(
            3,
            Objects.requireNonNull(ladder.degrade(ladder.strongest(), RejectionReason.outOfBounds)).level()
        );
        // skips clamp at the weakest rung
        assertEquals(3, Objects.requireNonNull(ladder.degrade(ladder.variant(2), RejectionReason.outOfBounds)).level());
        assertEquals(
            3,
            Objects.requireNonNull(ladder.degrade(ladder.variant(2), RejectionReason.insufficientArea)).level()
        );
        assertNull(ladder.degrade(ladder.variant(3), RejectionReason.insufficientArea));
    }

    @Test
    void upgradeStepsTowardStrongestOneRungAtATime() {
        VariantLadder ladder = VariantLadder.of(
            List.of(
                rung(0, 100, 40, ContentTier.full),
                rung(1, 60, 24, ContentTier.compact),
                rung(2, 20, 12, ContentTier.iconOnly)
            )
        );

        assertEquals(1, Objects.requireNonNull(ladder.upgrade(ladder.variant(2))).level());
        assertEquals(0, Objects.requireNonNull(ladder.upgrade(ladder.variant(1))).level());
        assertNull(ladder.upgrade(ladder.strongest()));
    }

    @Test
    void degradeThenUpgradeRoundTrips() {
        VariantLadder ladder = VariantLadder.of(
            List.of(
                rung(0, 100, 40, ContentTier.full),
                rung(1, 60, 24, ContentTier.compact),
                rung(2, 20, 12, ContentTier.iconOnly)
            )
        );

        InworldVariant current = ladder.strongest();
        for (int i = 0; i < ladder.size() - 1; i++) {
            current = ladder.degrade(current, RejectionReason.overlap);
            assertNotNull(current);
        }
        for (int i = 0; i < ladder.size() - 1; i++) {
            current = ladder.upgrade(Objects.requireNonNull(current));
        }
        assertEquals(ladder.strongest(), current);
    }

    @Test
    void monotonicityInvariantsHoldForStdLadders() {
        VariantLadder ladder = TestElement.ladder(new Size(100, 40), new Size(60, 24), new Size(20, 12));

        for (int i = 1; i < ladder.size(); i++) {
            assertTrue(
                ladder.variant(i).requestedArea() <= ladder.variant(i - 1).requestedArea(),
                "area must not grow down the ladder at rung " + i
            );
            assertTrue(
                ladder.variant(i).contentTier().ordinal() >= ladder.variant(i - 1).contentTier().ordinal(),
                "tier must not regress down the ladder at rung " + i
            );
        }
    }

    @Test
    void rejectsMalformedLadders() {
        assertThrows(IllegalArgumentException.class, () -> VariantLadder.of(List.of()));
        assertThrows(
            IllegalArgumentException.class,
            () -> VariantLadder.of(List.of(rung(1, 100, 40, ContentTier.full)))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> VariantLadder.of(List.of(rung(0, 100, 40, ContentTier.full), rung(1, 120, 40, ContentTier.compact)))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> VariantLadder.of(List.of(rung(0, 100, 40, ContentTier.compact), rung(1, 60, 24, ContentTier.full)))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> VariantLadder.of(
                List.of(rung(0, 100, 40, ContentTier.full), rung(1, 60, 24, ContentTier.compact)),
                Map.of(RejectionReason.overlap, 0)
            )
        );
        assertThrows(IllegalArgumentException.class, () -> twoRungs.variant(-1));
        assertThrows(IllegalArgumentException.class, () -> twoRungs.variant(2));
    }
}
