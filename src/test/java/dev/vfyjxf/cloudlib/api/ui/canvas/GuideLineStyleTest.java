package dev.vfyjxf.cloudlib.api.ui.canvas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GuideLineStyle}'s named derivations: the per-frame values a caller has and the theme cannot know.
 * {@link GuideLineStyle#withDashDuty(float)} is the one a pattern fade rides — the gaps close while the pattern's
 * rhythm and its motion stay put.
 */
class GuideLineStyleTest {

    private static GuideLineStyle sample() {
        return new GuideLineStyle(
            2f,
            1f,
            0xFF112233,
            0x44112233,
            0.22f,
            0.15f,
            8f,
            0.5f,
            3f,
            GuideLineStyle.Marker.dot,
            4f,
            1.5f,
            0.25f,
            0.75f
        );
    }

    @Test
    void withDashDutyChangesTheDutyAlone() {
        GuideLineStyle base = sample();

        GuideLineStyle closed = base.withDashDuty(1f);

        assertEquals(1f, closed.dashDuty(), "the duty is the one value that moves");
        assertEquals(base.dashPeriodPx(), closed.dashPeriodPx(), "the period is the pattern's rhythm and stays");
        assertEquals(base.dashPhasePx(), closed.dashPhasePx(), "and the phase the ants march on stays with it");
        assertEquals(base.lineWidth(), closed.lineWidth());
        assertEquals(base.edgeWidth(), closed.edgeWidth());
        assertEquals(base.lineColor(), closed.lineColor());
        assertEquals(base.edgeColor(), closed.edgeColor());
        assertEquals(base.fadeFraction(), closed.fadeFraction());
        assertEquals(base.fadeAlpha(), closed.fadeAlpha());
        assertEquals(base.marker(), closed.marker());
        assertEquals(base.markerSize(), closed.markerSize());
        assertEquals(base.portTick(), closed.portTick());
        assertEquals(base.arcStart(), closed.arcStart());
        assertEquals(base.arcEnd(), closed.arcEnd());
    }

    @Test
    void withDashDutyComposesWithTheOtherDerivations() {
        GuideLineStyle base = sample();

        GuideLineStyle derived = base.at(4f, 0xFF445566, 0x22445566, 8f, 6f, GuideLineStyle.Marker.none, 0f)
                .withArc(0.1f, 0.9f).withDashDuty(0.75f);

        assertEquals(0.75f, derived.dashDuty());
        assertEquals(6f, derived.dashPhasePx(), "the phase the derivation was given survives the duty override");
        assertEquals(0.1f, derived.arcStart());
        assertEquals(0.9f, derived.arcEnd());
        assertEquals(4f, derived.lineWidth());
        assertEquals(GuideLineStyle.Marker.none, derived.marker());
    }
}
