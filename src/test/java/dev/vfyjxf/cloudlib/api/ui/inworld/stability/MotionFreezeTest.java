package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The moving-target freeze of {@link MotionFreeze}: the Schmitt trigger on
 * the tracked speed — enter above 10 px/s, release only after the speed has
 * stayed below 2 px/s for the settle dwell — with the low-passed speed
 * estimate keeping single-frame blips from tripping the gate.
 */
class MotionFreezeTest {

    private static final double dt = 1.0 / 20.0;

    private final MotionFreeze freeze = new MotionFreeze(MotionFreeze.Config.of(10.0, 2.0, 0.25, 0.1));

    /** Drives frames at a constant speed along x; returns the last frozen flag. */
    private boolean drive(double pxPerSec, int frames) {
        boolean frozen = false;
        FloatPos p = new FloatPos(0, 0);
        for (int i = 0; i < frames; i++) {
            p = new FloatPos(p.x() + pxPerSec * dt, 0);
            frozen = freeze.observe(p, dt);
        }
        return frozen;
    }

    @Test
    void fastMotionEntersTheFreeze() {
        assertTrue(drive(40.0, 20), "40 px/s must freeze");
    }

    @Test
    void slowMotionNeverFreezes() {
        assertFalse(drive(5.0, 40), "5 px/s — inside the Schmitt band, below enter — never freezes");
    }

    @Test
    void sustainedJustAboveEnterFreezesEventually() {
        // 12 px/s sustained: the low-pass converges to 12 > 10 — enters
        assertTrue(drive(12.0, 40), "sustained 12 px/s must enter the freeze");
    }

    @Test
    void freezeHoldsThroughTheBandAndReleasesAfterTheDwell() {
        drive(40.0, 20); // frozen
        assertTrue(freeze.frozen());
        // into the band (5 px/s: above release, below enter) — holds frozen
        // even once the low-passed speed has settled at 5
        drive(5.0, 20);
        assertTrue(freeze.frozen(), "the band between release and enter holds the freeze");
        // below release — the dwell accumulates (0.25 s at settle speed after
        // the low-pass has decayed under it); 1.5 s is far past the dwell
        drive(1.0, 30);
        assertFalse(freeze.frozen(), "a sustained sub-release speed releases after the dwell");
    }

    @Test
    void aSpikeAboveReleaseInterruptsTheDwell() {
        drive(40.0, 20);
        // settle almost fully, then spike and resettle: the release must have
        // restarted its dwell after the spike, not kept the accumulated one
        drive(1.0, 30);
        assertFalse(freeze.frozen());
        drive(40.0, 20); // re-frozen
        drive(1.0, 30);
        assertFalse(freeze.frozen());
        // now the precise restart: re-freeze, decay the filter, spike mid-dwell
        drive(40.0, 20);
        drive(1.0, 14); // speed decayed under release, dwell partially built
        assertTrue(freeze.frozen(), "still inside the post-settle release window");
        drive(30.0, 4); // a spike — the dwell restarts
        assertTrue(freeze.frozen());
        drive(1.0, 25); // decay + a fresh full dwell
        assertFalse(freeze.frozen(), "the spike restarted the dwell and it completed again");
    }

    @Test
    void aSingleFrameBlipDoesNotTripTheEnterGate() {
        // 12 px/s for one frame from rest: the low-pass (half-life 0.1 s)
        // keeps the smoothed speed at ~4 px/s — under the enter threshold
        drive(1.0, 10);
        drive(12.0, 1);
        assertFalse(freeze.frozen(), "a single-frame blip must not enter the freeze");
        drive(1.0, 1);
        assertFalse(freeze.frozen());
    }

    @Test
    void nullPointResetsTheGate() {
        drive(40.0, 20);
        assertTrue(freeze.frozen());
        assertFalse(freeze.observe(null, dt), "a null point thaws and clears the gate");
        assertFalse(freeze.frozen());
        assertTrue(drive(40.0, 20), "the gate re-arms from scratch after the reset");
    }

    @Test
    void zeroDtHoldsState() {
        drive(40.0, 20);
        assertTrue(freeze.observe(new FloatPos(1000, 0), 0.0));
        assertTrue(freeze.frozen());
    }

    @Test
    void nonFiniteDtIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> freeze.observe(new FloatPos(0, 0), Double.NaN));
    }

    @Test
    void configRejectsInvertedBandAndNonPositiveKnobs() {
        assertThrows(IllegalArgumentException.class, () -> MotionFreeze.Config.of(2.0, 10.0, 0.25, 0.1));
        assertThrows(IllegalArgumentException.class, () -> MotionFreeze.Config.of(10.0, 10.0, 0.25, 0.1));
        assertThrows(IllegalArgumentException.class, () -> MotionFreeze.Config.of(0.0, 2.0, 0.25, 0.1));
        assertThrows(IllegalArgumentException.class, () -> MotionFreeze.Config.of(10.0, 0.0, 0.25, 0.1));
        assertThrows(IllegalArgumentException.class, () -> MotionFreeze.Config.of(10.0, 2.0, 0.0, 0.1));
        assertThrows(IllegalArgumentException.class, () -> MotionFreeze.Config.of(10.0, 2.0, 0.25, 0.0));
        assertThrows(IllegalArgumentException.class, () -> MotionFreeze.Config.of(10.0, 2.0, 0.25, Double.NaN));
        assertThrows(NullPointerException.class, () -> new MotionFreeze(null));
    }
}
