package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FocusEnvelopeTest {

    private static final double frame = 1.0 / 60.0;

    private static final FocusEnvelope.Config config = FocusEnvelope.Config.ofDefaults();

    @Test
    void anUnfocusedEnvelopeStaysInvisible() {
        FocusEnvelope envelope = new FocusEnvelope(config);

        for (int i = 0; i < 120; i++) {
            FocusEnvelope.Sample s = envelope.advance(false, frame);
            assertEquals(0f, s.alpha(), 1.0e-9f, "frame " + i);
            assertEquals(0f, s.brighten(), 1.0e-9f);
            assertFalse(s.visible(), "an unfocused panel draws no frame");
        }
    }

    @Test
    void acquireFiresOnePulseAndNeverLoops() {
        FocusEnvelope envelope = new FocusEnvelope(config);
        FocusEnvelope.Sample onset = envelope.advance(true, frame);

        assertEquals(1f, onset.alpha(), 1.0e-9f, "the frame is at full ink the frame it is gained");
        assertTrue(onset.brighten() > 0f, "the acquire pulse sounds on its onset frame: " + onset.brighten());

        List<Float> brightens = new ArrayList<>();
        brightens.add(onset.brighten());
        for (double t = frame; t < config.pulseSeconds() * 3; t += frame) {
            FocusEnvelope.Sample s = envelope.advance(true, frame);
            assertEquals(1f, s.alpha(), 1.0e-9f, "held focus never dims the frame at t=" + t);
            brightens.add(s.brighten());
        }

        float peak = brightens.stream().max(Float::compare).orElse(0f);
        assertTrue(peak > 0.9f * (float) config.opacityDelta(), "the pulse climbs near its budget: " + peak);
        assertTrue(peak <= (float) config.opacityDelta() + 1.0e-6f, "the bonus never exceeds the delta");

        long sounding = brightens.stream().filter(b -> b > 0f).count();
        assertTrue(sounding > 0, "the pulse sounds");
        assertTrue(
            sounding * frame <= config.pulseSeconds() + frame,
            "it sounds for one pulse only, then holds silent: " + sounding + " frames"
        );
        int firstSilent = (int) Math.ceil(config.pulseSeconds() / frame) + 1;
        for (int i = firstSilent; i < brightens.size(); i++) {
            assertEquals(0f, brightens.get(i), 1.0e-9f, "no loop back around at frame " + i);
        }
    }

    @Test
    void lossDimsEaseInAndEndsSilent() {
        FocusEnvelope envelope = new FocusEnvelope(config);
        envelope.advance(true, frame);
        for (double t = frame; t < config.pulseSeconds() + frame; t += frame) {
            envelope.advance(true, frame); // the pulse is over before the loss begins
        }

        List<Float> alphas = new ArrayList<>();
        for (double t = 0; t < config.lossSeconds() * 2; t += frame) {
            FocusEnvelope.Sample s = envelope.advance(false, frame);
            assertEquals(0f, s.brighten(), 1.0e-9f, "the loss never pulses");
            alphas.add(s.alpha());
        }

        assertTrue(alphas.get(0) < 1f && alphas.get(0) > 0.9f, "the dim starts from full ink: " + alphas.get(0));
        int gone = alphas.indexOf(0f);
        assertTrue(gone > 0, "the frame reaches zero");
        assertTrue(
            gone * frame <= config.lossSeconds() + frame,
            "it is gone within one loss window: " + gone + " frames"
        );
        for (int i = gone; i < alphas.size(); i++) {
            assertEquals(0f, alphas.get(i), 1.0e-9f, "and stays gone at frame " + i);
        }
        for (int i = 1; i < gone; i++) {
            assertTrue(alphas.get(i) < alphas.get(i - 1), "the dim is monotone at frame " + i);
        }

        // ease-in: the dim accelerates — the first quarter of the window loses
        // less ink than the last quarter does
        int quarter = Math.max(1, gone / 4);
        float earlyLoss = alphas.get(0) - alphas.get(quarter);
        float lateLoss = alphas.get(gone - 1 - quarter) - alphas.get(gone - 1);
        assertTrue(earlyLoss < lateLoss, "the fade-in of the dim is ease-in: " + earlyLoss + " vs " + lateLoss);
    }

    @Test
    void regainSnapsBackToFullInkAndPulsesAgain() {
        FocusEnvelope envelope = new FocusEnvelope(config);
        envelope.advance(true, frame);
        for (double t = frame; t < config.pulseSeconds() + frame; t += frame) {
            envelope.advance(true, frame);
        }
        for (double t = 0; t < config.lossSeconds() * 0.5; t += frame) {
            envelope.advance(false, frame); // halfway into the loss
        }

        FocusEnvelope.Sample regained = envelope.advance(true, frame);
        assertEquals(1f, regained.alpha(), 1.0e-9f, "the frame returns to full ink, no bounce");
        assertTrue(regained.brighten() > 0f, "each acquisition pulses from its onset: " + regained.brighten());
    }

    @Test
    void phaseAdvancesOnlyWhileAnimating() {
        FocusEnvelope envelope = new FocusEnvelope(config);
        assertEquals(0.0, envelope.phase(), 1.0e-9, "an unfocused frame is idle");

        double idle = envelope.phase();
        envelope.advance(true, frame);
        List<Double> pulsePhases = new ArrayList<>();
        pulsePhases.add(envelope.phase());
        for (double t = frame; t < config.pulseSeconds(); t += frame) {
            envelope.advance(true, frame);
            pulsePhases.add(envelope.phase());
        }
        for (int i = 1; i < pulsePhases.size(); i++) {
            assertTrue(pulsePhases.get(i) > pulsePhases.get(i - 1), "the phase moves while the pulse runs");
        }

        double settled = 0.0;
        for (int i = 0; i < 30; i++) {
            envelope.advance(true, frame); // held focus past the pulse
            settled = envelope.phase();
        }
        assertEquals(settled, envelope.phase(), 0.0, "the phase is constant while idle");

        envelope.advance(false, frame);
        double lossPhase = envelope.phase();
        assertTrue(lossPhase > 0.0 && lossPhase != settled, "the phase moves again while the loss runs");
        envelope.advance(false, config.lossSeconds()); // finish the loss
        assertEquals(0.0, envelope.phase(), 1.0e-9, "the finished loss settles back to idle");
        assertFalse(envelope.advance(false, frame).visible());
    }

    @Test
    void resetReturnsToTheNeverFocusedState() {
        FocusEnvelope envelope = new FocusEnvelope(config);
        envelope.advance(true, frame);
        envelope.reset();

        assertEquals(0f, envelope.advance(false, frame).alpha(), 1.0e-9f);
        assertTrue(envelope.advance(true, frame).brighten() > 0f, "the next acquisition pulses again");
    }

    @Test
    void rejectsInvalidUse() {
        assertThrows(IllegalArgumentException.class, () -> new FocusEnvelope.Config(0, 0.3, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new FocusEnvelope.Config(0.16, 0, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new FocusEnvelope.Config(0.16, 0.36, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new FocusEnvelope.Config(0.16, 0.3, 0));
        FocusEnvelope envelope = new FocusEnvelope(config);
        assertThrows(IllegalArgumentException.class, () -> envelope.advance(true, -0.1));
        assertThrows(IllegalArgumentException.class, () -> envelope.advance(true, Double.NaN));
        // the survey windows the defaults stand in for
        assertTrue(
            config.pulseSeconds() >= 0.12 && config.pulseSeconds() <= 0.20,
            "the default pulse is in the 120–200 ms window"
        );
        assertTrue(
            config.lossSeconds() >= 0.08 && config.lossSeconds() <= 0.15,
            "the default loss is in the 80–150 ms window"
        );
        assertTrue(config.opacityDelta() <= 0.35, "the default delta stays inside the cue budget");
    }
}
