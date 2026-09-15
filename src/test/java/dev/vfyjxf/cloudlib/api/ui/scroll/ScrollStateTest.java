package dev.vfyjxf.cloudlib.api.ui.scroll;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollStateTest {

    @Test
    void viewportInsetClampsNegativeValuesAndSupportsUniformConfiguration() {
        ScrollState state = ScrollState.create()
            .viewportInset(4)
            .viewportInset(-1, 5, -2, 6);

        assertEquals(0, state.viewportInsetTop());
        assertEquals(5, state.viewportInsetRight());
        assertEquals(0, state.viewportInsetBottom());
        assertEquals(6, state.viewportInsetLeft());
    }

    @Test
    void disabledStateStopsScrollCapabilityAndResetsPosition() {
        ScrollState state = ScrollState.create(ScrollDirection.vertical);
        state.updateViewport(40, 40);
        state.updateContentSize(40, 120);
        state.jumpTo(0, 32);

        assertTrue(state.canScrollVertically());

        state.enabled(false);

        assertFalse(state.enabled());
        assertFalse(state.canScrollVertically());
        assertEquals(0f, state.targetScrollY());
    }

    @Test
    void wheelAccelerationScalesRepeatedWheelInputUntilCapped() {
        ScrollState state = ScrollState.create()
            .scrollSpeed(10)
            .wheelAcceleration(true)
            .wheelAccelerationStep(0.5f)
            .wheelAccelerationMaxMultiplier(2.0f);

        assertEquals(-10.0f, state.wheelScrollDelta(0, 1, 1_000).y(), 0.001f);
        assertEquals(-15.0f, state.wheelScrollDelta(0, 1, 1_030).y(), 0.001f);
        assertEquals(-20.0f, state.wheelScrollDelta(0, 1, 1_060).y(), 0.001f);
        assertEquals(-20.0f, state.wheelScrollDelta(0, 1, 1_090).y(), 0.001f);
        assertEquals(2.0f, state.currentWheelAccelerationMultiplier(), 0.001f);
    }

    @Test
    void wheelAccelerationResetsAfterIdleOrDirectionChange() {
        ScrollState state = ScrollState.create()
            .scrollSpeed(10)
            .wheelAcceleration(true)
            .wheelAccelerationStep(0.5f)
            .wheelAccelerationResetMillis(100);

        state.wheelScrollDelta(0, 1, 1_000);
        assertEquals(-15.0f, state.wheelScrollDelta(0, 1, 1_050).y(), 0.001f);

        assertEquals(-10.0f, state.wheelScrollDelta(0, 1, 1_200).y(), 0.001f);
        assertEquals(10.0f, state.wheelScrollDelta(0, -1, 1_220).y(), 0.001f);
        assertEquals(1.0f, state.currentWheelAccelerationMultiplier(), 0.001f);
    }

    @Test
    void scrollConfigurationClampsInvalidAccelerationValues() {
        ScrollState state = ScrollState.create()
            .wheelAccelerationStep(-1.0f)
            .wheelAccelerationMaxMultiplier(0.25f)
            .wheelAccelerationResetMillis(-1L);

        assertEquals(0.0f, state.wheelAccelerationStep(), 0.001f);
        assertEquals(1.0f, state.wheelAccelerationMaxMultiplier(), 0.001f);
        assertEquals(0L, state.wheelAccelerationResetMillis());
    }

    @Test
    void longContentWheelAccelerationUsesHigherButCappedMultiplier() {
        ScrollState state = ScrollState.create()
            .scrollSpeed(10)
            .longContentWheelAcceleration();

        assertTrue(state.wheelAcceleration());
        assertTrue(state.wheelAccelerationMaxMultiplier() > 4.0f);
        assertEquals(-10.0f, state.wheelDelta(0, 1, 1_000).y(), 0.001f);

        for (int i = 1; i < 20; i++) {
            state.wheelDelta(0, 1, 1_000 + i * 20L);
        }

        assertEquals(state.wheelAccelerationMaxMultiplier(), state.currentWheelAccelerationMultiplier(), 0.001f);
        assertEquals(-10.0f, state.wheelDelta(0, 1, 2_000).y(), 0.001f);
    }

    @Test
    void autoScrollDeltaUsesDeadZoneSpeedAndMaxSpeed() {
        ScrollState state = ScrollState.create()
            .middleMouseAutoScroll(true)
            .autoScrollDeadZone(5.0f)
            .autoScrollSpeed(2.0f)
            .autoScrollMaxSpeed(12.0f);

        assertTrue(state.middleMouseAutoScroll());
        assertEquals(0.0f, state.autoScrollDelta(5), 0.001f);
        assertEquals(4.0f, state.autoScrollDelta(7), 0.001f);
        assertEquals(12.0f, state.autoScrollDelta(20), 0.001f);
        assertEquals(-12.0f, state.autoScrollDelta(-20), 0.001f);
    }

    @Test
    void autoScrollConfigurationClampsNegativeValues() {
        ScrollState state = ScrollState.create()
            .autoScrollDeadZone(-1.0f)
            .autoScrollSpeed(-2.0f)
            .autoScrollMaxSpeed(-3.0f);

        assertEquals(0.0f, state.autoScrollDeadZone(), 0.001f);
        assertEquals(0.0f, state.autoScrollSpeed(), 0.001f);
        assertEquals(0.0f, state.autoScrollMaxSpeed(), 0.001f);
    }
}
