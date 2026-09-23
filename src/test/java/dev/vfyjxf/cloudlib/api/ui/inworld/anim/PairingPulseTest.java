package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PairingPulseTest {

    private static final double frame = 1.0 / 60.0;

    private static final PairingPulse.Config config = PairingPulse.Config.ofDefaults();

    @Test
    void anUnfiredPulseStaysSilent() {
        PairingPulse pulse = new PairingPulse(config);

        for (int i = 0; i < 120; i++) {
            assertEquals(0.0, pulse.advance(frame), 1.0e-9, "frame " + i);
        }
        assertFalse(pulse.running());
    }

    @Test
    void thePulseSoundsOnTheFireFrameAndPeaksAtItsMidpoint() {
        PairingPulse pulse = new PairingPulse(config);
        pulse.fire();

        // the fire frame itself answers above zero — the onset is that
        // frame, both ends of the pairing brighten in it
        double onset = pulse.advance(frame);
        assertTrue(onset > 0.0 && onset < 0.5, "the pulse is already sounding, below its peak: " + onset);

        // step to just before the midpoint, then across it
        double steps = config.durationSeconds() / frame;
        double weight = onset;
        double peak = 0.0;
        for (double t = frame; t < config.durationSeconds(); t += frame) {
            double next = pulse.advance(frame);
            assertTrue(next >= 0.0 && next <= 1.0, "the weight stays a curve value: " + next);
            peak = Math.max(peak, next);
            if (t < config.durationSeconds() * 0.5 - frame) {
                assertTrue(next > weight, "rising through the first half at t=" + t + ": " + next + " vs " + weight);
            }
            weight = next;
        }
        assertTrue(peak > 0.99, "the curve reaches its top around the midpoint: " + peak);
        assertTrue(steps > 1, "the duration spans frames");
    }

    @Test
    void thePulsePlaysOnceAndNeverLoops() {
        PairingPulse pulse = new PairingPulse(config);
        pulse.fire();

        List<Double> weights = new ArrayList<>();
        for (double t = 0; t < config.durationSeconds() * 3; t += frame) {
            weights.add(pulse.advance(frame));
        }

        long sounding = weights.stream().filter(w -> w > 0.0).count();
        assertTrue(sounding > 0, "the pulse sounds");
        assertTrue(
            sounding * frame <= config.durationSeconds() + frame,
            "it sounds for one duration only: " + sounding + " frames"
        );
        // and everything past the first duration is stone silent
        int firstSilent = (int) Math.ceil(config.durationSeconds() / frame) + 1;
        for (int i = firstSilent; i < weights.size(); i++) {
            assertEquals(0.0, weights.get(i), 1.0e-9, "no loop back around at frame " + i);
        }
        assertFalse(pulse.running());
    }

    @Test
    void aFireWhileRunningDoesNotRestartTheCurve() {
        PairingPulse pulse = new PairingPulse(config);
        pulse.fire();
        for (double t = 0; t < config.durationSeconds() * 0.5 - frame; t += frame) {
            pulse.advance(frame);
        }

        pulse.fire(); // mid-pulse — ignored: one pulse per entry
        double weight = pulse.advance(frame);
        assertTrue(weight > 0.7, "still on the original curve near its peak: " + weight);
        // the pulse ends one duration after the ORIGINAL fire, not later
        double t = config.durationSeconds() * 0.5;
        for (; t < config.durationSeconds() + frame; t += frame) {
            weight = pulse.advance(frame);
        }
        assertEquals(0.0, weight, 1.0e-9, "the original curve has run out");
        assertFalse(pulse.running());
    }

    @Test
    void aFreshFireAfterSilenceSoundsAgain() {
        PairingPulse pulse = new PairingPulse(config);
        pulse.fire();
        for (double t = 0; t < config.durationSeconds(); t += frame) {
            pulse.advance(frame);
        }
        assertEquals(0.0, pulse.advance(frame), 1.0e-9);

        pulse.fire(); // a re-entry into the attach tier is a new pairing event
        assertTrue(pulse.advance(frame) > 0.0, "the next entry pulses again");
    }

    @Test
    void resetReturnsToTheNeverPulsedState() {
        PairingPulse pulse = new PairingPulse(config);
        pulse.fire();
        pulse.advance(frame);
        pulse.reset();

        assertFalse(pulse.running());
        assertEquals(0.0, pulse.advance(frame), 1.0e-9);
    }

    @Test
    void rejectsInvalidUse() {
        assertThrows(IllegalArgumentException.class, () -> new PairingPulse.Config(0, 0.3));
        assertThrows(IllegalArgumentException.class, () -> new PairingPulse.Config(-0.2, 0.3));
        assertThrows(IllegalArgumentException.class, () -> new PairingPulse.Config(0.2, 0));
        assertThrows(IllegalArgumentException.class, () -> new PairingPulse.Config(0.2, -0.3));
        assertThrows(IllegalArgumentException.class, () -> new PairingPulse.Config(0.2, 0.36));
        PairingPulse pulse = new PairingPulse(config);
        assertThrows(IllegalArgumentException.class, () -> pulse.advance(-0.1));
        assertThrows(IllegalArgumentException.class, () -> pulse.advance(Double.NaN));
        // the survey delta stays inside the pairing-cue budget
        assertTrue(config.opacityDelta() <= 0.35);
        assertTrue(
            config.durationSeconds() >= 0.15 && config.durationSeconds() <= 0.25,
            "the default duration is in the 150–250 ms window"
        );
    }
}
