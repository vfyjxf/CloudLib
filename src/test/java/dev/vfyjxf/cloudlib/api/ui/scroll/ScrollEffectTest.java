package dev.vfyjxf.cloudlib.api.ui.scroll;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollEffectTest {

    @Test
    void middleMouseAutoScrollAdvancesFromAnchorDistance() {
        ScrollState state = ScrollState.create(ScrollDirection.vertical)
            .middleMouseAutoScroll(true)
            .smooth(false)
            .autoScrollDeadZone(5.0f)
            .autoScrollSpeed(2.0f)
            .autoScrollMaxSpeed(12.0f);
        state.updateViewport(40, 40);
        state.updateContentSize(40, 120);

        ScrollEffect effect = ScrollEffect.of(state);

        assertTrue(effect.beginAutoScroll(10, 10));
        assertTrue(effect.autoScrolling());

        effect.advanceAutoScroll(10, 17);
        assertEquals(4.0f, state.targetScrollY(), 0.001f);

        effect.advanceAutoScroll(10, 40);
        assertEquals(16.0f, state.targetScrollY(), 0.001f);
    }

    @Test
    void middleMouseAutoScrollDoesNotStartWithoutScrollableContent() {
        ScrollState state = ScrollState.create(ScrollDirection.vertical)
            .middleMouseAutoScroll(true);
        state.updateViewport(40, 40);
        state.updateContentSize(40, 40);

        ScrollEffect effect = ScrollEffect.of(state);

        assertFalse(effect.beginAutoScroll(10, 10));
        assertFalse(effect.autoScrolling());
    }

    @Test
    void disabledMiddleMouseAutoScrollStopsRunningMode() {
        ScrollState state = ScrollState.create(ScrollDirection.vertical)
            .middleMouseAutoScroll(true);
        state.updateViewport(40, 40);
        state.updateContentSize(40, 120);

        ScrollEffect effect = ScrollEffect.of(state);

        assertTrue(effect.beginAutoScroll(10, 10));

        state.middleMouseAutoScroll(false);
        effect.advanceAutoScroll(10, 40);

        assertFalse(effect.autoScrolling());
        assertEquals(0.0f, state.targetScrollY(), 0.001f);
    }
}
