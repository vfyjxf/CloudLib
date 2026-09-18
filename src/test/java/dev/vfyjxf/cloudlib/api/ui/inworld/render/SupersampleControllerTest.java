package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupersampleControllerTest {

    @Test
    void firstObservationIsAdoptedImmediately() {
        SupersampleController controller = new SupersampleController();
        assertEquals(-1, controller.current());
        assertEquals(5, controller.observe(5));
        assertEquals(5, controller.current());
    }

    @Test
    void switchRequiresStableFrames() {
        SupersampleController controller = new SupersampleController(3);
        assertEquals(2, controller.observe(2));
        assertEquals(2, controller.observe(5)); // candidate seen once
        assertEquals(2, controller.observe(5)); // twice
        assertEquals(5, controller.observe(5)); // third stable frame applies
        assertEquals(5, controller.current());
    }

    @Test
    void defaultWindowIsTenFrames() {
        assertEquals(10, SupersampleController.defaultStableFrames);
        SupersampleController controller = new SupersampleController();
        assertEquals(2, controller.observe(2));
        for (int i = 0; i < 9; i++) assertEquals(2, controller.observe(6));
        assertEquals(6, controller.observe(6));
    }

    @Test
    void jitterNeverSwitches() {
        SupersampleController controller = new SupersampleController();
        assertEquals(3, controller.observe(3));
        for (int frame = 0; frame < 200; frame++) {
            assertEquals(3, controller.observe(frame % 2 == 0 ? 4 : 3));
        }
        assertEquals(3, controller.current());
    }

    @Test
    void interruptedRunDoesNotSwitch() {
        SupersampleController controller = new SupersampleController(4);
        assertEquals(2, controller.observe(2));
        // three frames toward 5 — one short of the window
        for (int i = 0; i < 3; i++) assertEquals(2, controller.observe(5));
        // a single-frame blip to 6 resets the candidate
        assertEquals(2, controller.observe(6));
        for (int i = 0; i < 3; i++) assertEquals(2, controller.observe(5));
        assertEquals(2, controller.current());
        // only a full uninterrupted run switches (three stable frames above)
        assertEquals(2, controller.observe(6));
        for (int i = 0; i < 3; i++) assertEquals(2, controller.observe(5));
        assertEquals(5, controller.observe(5));
        assertEquals(5, controller.current());
    }

    @Test
    void noReboundAfterASwitch() {
        SupersampleController controller = new SupersampleController(2);
        assertEquals(2, controller.observe(2));
        assertEquals(2, controller.observe(5));
        assertEquals(5, controller.observe(5));
        // single-frame blips back toward the old value never reapply it
        assertEquals(5, controller.observe(2));
        assertEquals(5, controller.observe(5));
        assertEquals(5, controller.current());
        // but a genuinely sustained return does switch back
        assertEquals(5, controller.observe(2));
        assertEquals(2, controller.observe(2));
        assertEquals(2, controller.current());
    }

    @Test
    void missingObservationHoldsTheCurrentFactor() {
        SupersampleController controller = new SupersampleController(2);
        assertEquals(4, controller.observe(4));
        // hidden panel / degenerate projection: desired ≤ 0 is "no data"
        assertEquals(4, controller.observe(0));
        assertEquals(4, controller.observe(-1));
        assertEquals(4, controller.current());
        // pending candidate state survives the gap
        assertEquals(4, controller.observe(6));
        assertEquals(4, controller.observe(0));
        assertEquals(6, controller.observe(6));
    }

    @Test
    void stepwiseRatchetFollowsASustainedTrend() {
        SupersampleController controller = new SupersampleController(3);
        assertEquals(2, controller.observe(2));
        for (int i = 0; i < 3; i++) controller.observe(3);
        assertEquals(3, controller.current());
        for (int i = 0; i < 3; i++) controller.observe(4);
        assertEquals(4, controller.current());
        for (int i = 0; i < 3; i++) controller.observe(3);
        assertEquals(3, controller.current());
    }
}
