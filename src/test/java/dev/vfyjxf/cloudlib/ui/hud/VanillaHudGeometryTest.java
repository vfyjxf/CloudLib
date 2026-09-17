package dev.vfyjxf.cloudlib.ui.hud;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.ExclusionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaHudGeometryTest {

    private static final int width = 200;
    private static final int height = 100;

    @Test
    void hotbarSitsBottomCenter() {
        List<Rect> rects =
                VanillaHudGeometry.compute(HudInputs.builder(width, height).build());

        assertEquals(List.of(new Rect(9, 78, 182, 22)), rects);
    }

    @Test
    void offhandSlotFollowsTheArmSide() {
        List<Rect> left = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .offhandSlot(true)
                .offhandLeft(true)
                .build());
        List<Rect> right = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .offhandSlot(true)
                .offhandLeft(false)
                .build());

        assertEquals(new Rect(width / 2 - 91 - 29, 77, 29, 24), left.get(1));
        assertEquals(new Rect(width / 2 + 91, 77, 29, 24), right.get(1));
    }

    @Test
    void experienceBarAndJumpMeterShareTheSameRect() {
        Rect bar = VanillaHudGeometry.experienceBar(width, height);

        assertEquals(new Rect(9, 71, 182, 5), bar);
        List<Rect> withJumpMeter = VanillaHudGeometry.compute(
                HudInputs.builder(width, height).jumpMeter(true).build());
        List<Rect> withExperience = VanillaHudGeometry.compute(
                HudInputs.builder(width, height).experienceBar(true).build());

        assertEquals(bar, withJumpMeter.get(1));
        assertEquals(bar, withExperience.get(1));
    }

    @Test
    void experienceLevelSitsAboveTheBar() {
        assertEquals(new Rect(9, 65, 182, 9), VanillaHudGeometry.experienceLevel(width, height));

        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .experienceBar(true)
                .experienceLevel(true)
                .build());

        assertEquals(new Rect(9, 65, 182, 9), rects.get(2));
    }

    @Test
    void healthColumnsUseTheSampledHeights() {
        assertEquals(new Rect(9, 61, 81, 39), VanillaHudGeometry.leftColumn(width, height, 39));
        assertEquals(new Rect(110, 61, 81, 39), VanillaHudGeometry.rightColumn(width, height, 39));
        assertEquals(new Rect(9, 49, 81, 51), VanillaHudGeometry.leftColumn(width, height, 51));

        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .healthColumns(true)
                .leftHeight(49)
                .rightHeight(39)
                .build());

        assertEquals(new Rect(9, 51, 81, 49), rects.get(1));
        assertEquals(new Rect(110, 61, 81, 39), rects.get(2));
    }

    @Test
    void effectIconsStackFromTheRightEdge() {
        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .effects(true)
                .beneficialEffects(2)
                .otherEffects(1)
                .build());

        assertEquals(
                List.of(
                        new Rect(9, 78, 182, 22),
                        new Rect(175, 1, 24, 24),
                        new Rect(150, 1, 24, 24),
                        new Rect(175, 27, 24, 24)),
                rects);
    }

    @Test
    void demoShiftsEffectRowsDown() {
        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .effects(true)
                .beneficialEffects(1)
                .demo(true)
                .build());

        assertEquals(new Rect(175, 16, 24, 24), rects.get(1));
    }

    @Test
    void effectFlagOffHidesIconsEvenWithCounts() {
        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .beneficialEffects(3)
                .otherEffects(2)
                .build());

        assertEquals(List.of(new Rect(9, 78, 182, 22)), rects);
    }

    @Test
    void bossBarsIncludeTheNameRowAndStepByIncrement() {
        Rect first = VanillaHudGeometry.bossBar(width, new HudInputs.BossBar(12, 19));

        assertEquals(new Rect(9, 3, 182, 19), first);

        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .bossBars(List.of(new HudInputs.BossBar(12, 19), new HudInputs.BossBar(31, 24)))
                .build());

        assertEquals(new Rect(9, 3, 182, 19), rects.get(1));
        assertEquals(new Rect(9, 22, 182, 24), rects.get(2));
    }

    @Test
    void chatBottomMarginIsForty() {
        Rect chat = VanillaHudGeometry.chat(width, height, 0, 60, 20);

        assertEquals(new Rect(0, 40, 60, 20), chat);

        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .chat(true)
                .chatX(4)
                .chatSize(60, 20)
                .build());

        assertEquals(new Rect(4, 40, 60, 20), rects.get(1));
    }

    @Test
    void toastsAnchorTopRightBySlotIndex() {
        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .toasts(List.of(new HudInputs.Toast(0, 1, 160), new HudInputs.Toast(2, 2, 160)))
                .build());

        assertEquals(new Rect(40, 0, 160, 32), rects.get(1));
        assertEquals(new Rect(40, 64, 160, 64), rects.get(2));
    }

    @Test
    void subtitlesStackUpwardFromBottomCenterRight() {
        Rect firstRow = VanillaHudGeometry.subtitle(width, height, 10, 0);
        Rect secondRow = VanillaHudGeometry.subtitle(width, height, 10, 1);

        assertEquals(new Rect(177, 60, 22, 10), firstRow);
        assertEquals(new Rect(177, 50, 22, 10), secondRow);

        List<Rect> rects = VanillaHudGeometry.compute(
                HudInputs.builder(width, height).subtitles(10, 2).build());

        assertEquals(new Rect(177, 60, 22, 10), rects.get(1));
        assertEquals(new Rect(177, 50, 22, 10), rects.get(2));
    }

    @Test
    void zeroHalfWidthHidesSubtitles() {
        List<Rect> rects = VanillaHudGeometry.compute(
                HudInputs.builder(width, height).subtitles(0, 3).build());

        assertEquals(List.of(new Rect(9, 78, 182, 22)), rects);
    }

    @Test
    void computeEmitsEveryFamilyInOrderWhenEverythingIsOn() {
        List<Rect> rects = VanillaHudGeometry.compute(HudInputs.builder(width, height)
                .leftHeight(49)
                .rightHeight(39)
                .healthColumns(true)
                .experienceBar(true)
                .experienceLevel(true)
                .offhandSlot(true)
                .offhandLeft(false)
                .effects(true)
                .beneficialEffects(1)
                .bossBars(List.of(new HudInputs.BossBar(12, 19)))
                .chat(true)
                .chatSize(60, 20)
                .toasts(List.of(new HudInputs.Toast(0, 1, 160)))
                .subtitles(10, 1)
                .build());

        assertEquals(
                List.of(
                        new Rect(9, 78, 182, 22), // hotbar
                        new Rect(191, 77, 29, 24), // offhand slot, right side
                        new Rect(9, 71, 182, 5), // experience bar
                        new Rect(9, 65, 182, 9), // experience level
                        new Rect(9, 51, 81, 49), // hearts + armor column
                        new Rect(110, 61, 81, 39), // food + air column
                        new Rect(175, 1, 24, 24), // effect icon
                        new Rect(9, 3, 182, 19), // boss bar
                        new Rect(0, 40, 60, 20), // chat
                        new Rect(40, 0, 160, 32), // toast
                        new Rect(177, 60, 22, 10)), // subtitle row
                rects);
    }

    @Test
    void providerYieldsNothingWhenGuiIsHidden() {
        HudInputs inputs = HudInputs.builder(width, height)
                .healthColumns(true)
                .bossBars(List.of(new HudInputs.BossBar(12, 19)))
                .build();
        VanillaHudExclusions hidden = new VanillaHudExclusions(() -> true, () -> inputs);
        VanillaHudExclusions visible = new VanillaHudExclusions(() -> false, () -> inputs);

        assertTrue(hidden.exclusionAreas(new ExclusionContext(width, height, 0)).isEmpty());
        assertEquals(
                VanillaHudGeometry.compute(inputs), visible.exclusionAreas(new ExclusionContext(width, height, 0)));
    }

    @Test
    void hudInputsDefensivelyCopiesItsLists() {
        List<HudInputs.BossBar> bossBars = new ArrayList<>();
        bossBars.add(new HudInputs.BossBar(12, 19));
        HudInputs inputs = HudInputs.builder(width, height).bossBars(bossBars).build();

        bossBars.clear();

        assertEquals(List.of(new HudInputs.BossBar(12, 19)), inputs.bossBars());
    }

    @Test
    void builderDefaultsMatchVanillaColumnHeights() {
        HudInputs inputs = HudInputs.builder(width, height).build();

        assertEquals(39, inputs.leftHeight());
        assertEquals(39, inputs.rightHeight());
        assertEquals(1, VanillaHudGeometry.compute(inputs).size());
    }
}
