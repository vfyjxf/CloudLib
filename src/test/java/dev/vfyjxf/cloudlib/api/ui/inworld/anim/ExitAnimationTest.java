package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link ExitAnimation} — the exit envelope: kinds, easings, boundaries, normalization. */
class ExitAnimationTest {

    private static final double ms = 0.001;

    @Test
    void endpointsAreExactForEveryKind() {
        for (ExitAnimation.Kind kind : ExitAnimation.Kind.values()) {
            if (kind == ExitAnimation.Kind.none) continue;
            ExitAnimation a = new ExitAnimation(kind, 200, ExitAnimation.Easing.easeInOut);
            assertEquals(1f, a.alphaAt(0), 1.0e-6, kind + " starts opaque");
            assertEquals(0f, a.alphaAt(0.2), 1.0e-6, kind + " ends transparent");
            assertEquals(0f, a.riseAt(0), 1.0e-6);
            assertEquals(1f, a.scaleAt(0), 1.0e-6);
            assertTrue(a.finished(0.2) && !a.finished(0.199), kind + " finishes at the duration");
            if (kind == ExitAnimation.Kind.fadeRise) {
                assertEquals(1f, a.riseAt(0.2), 1.0e-6, kind + " completes the rise");
            } else {
                assertEquals(0f, a.riseAt(0.2), 1.0e-6, kind + " does not rise");
            }
            if (kind == ExitAnimation.Kind.fadeScale) {
                assertEquals(ExitAnimation.endScale, a.scaleAt(0.2), 1.0e-6, kind + " completes the shrink");
            } else {
                assertEquals(1f, a.scaleAt(0.2), 1.0e-6, kind + " does not scale");
            }
        }
    }

    @Test
    void fadeIsAlphaOnly() {
        ExitAnimation a = ExitAnimation.fade(180);
        assertEquals(ExitAnimation.Kind.fade, a.kind());
        assertEquals(ExitAnimation.Easing.easeOut, a.easing());
        assertEquals(0.18, a.durationSeconds(), 1.0e-9);
        assertEquals(0f, a.riseAt(0.09), "no rise");
        assertEquals(1f, a.scaleAt(0.09), "no scale");
        assertTrue(a.alphaAt(0.09) > 0f && a.alphaAt(0.09) < 1f, "mid fade");
    }

    @Test
    void fadeRiseRisesMonotonically() {
        ExitAnimation a = ExitAnimation.fadeRise(200, ExitAnimation.Easing.linear);
        float prev = 0f;
        for (double t = 0; t <= 0.2; t += 0.02) {
            float rise = a.riseAt(t);
            assertTrue(rise >= prev - 1.0e-6, "rise monotonic at t=" + t);
            prev = rise;
        }
        assertEquals(0.5f, a.riseAt(0.1), 1.0e-6, "linear midpoint");
        assertEquals(1f, a.scaleAt(0.1), "rise does not scale");
    }

    @Test
    void fadeScaleShrinksTowardEndScale() {
        ExitAnimation a = ExitAnimation.fadeScale(200, ExitAnimation.Easing.linear);
        assertEquals(1f, a.scaleAt(0), 1.0e-6);
        assertEquals(ExitAnimation.endScale, a.scaleAt(0.2), 1.0e-6, "ends at endScale");
        assertEquals(0.5f * (1f + ExitAnimation.endScale), a.scaleAt(0.1), 1.0e-6, "linear midpoint");
        assertEquals(0f, a.riseAt(0.1), "scale does not rise");
    }

    @Test
    void easingsCurveTheEnvelope() {
        // ease-out drops faster than linear in the first half, slower at the end
        ExitAnimation out = ExitAnimation.fade(200, ExitAnimation.Easing.easeOut);
        ExitAnimation linear = ExitAnimation.fade(200, ExitAnimation.Easing.linear);
        float a = 1 - out.alphaAt(0.05);
        float b = 1 - linear.alphaAt(0.05);
        assertTrue(a > b, "ease-out leads early: " + a + " vs " + b);
        // ease-in lags linear early and catches up by the end
        ExitAnimation in = ExitAnimation.fade(200, ExitAnimation.Easing.easeIn);
        assertTrue((1 - in.alphaAt(0.05)) < b, "ease-in lags early");
        // ease-in-out is symmetric about the midpoint
        ExitAnimation io = ExitAnimation.fadeRise(200, ExitAnimation.Easing.easeInOut);
        assertEquals(io.riseAt(0.05), 1f - io.riseAt(0.15), 1.0e-6, "easeInOut symmetry");
    }

    @Test
    void durationIsClampedToTheHardCap() {
        ExitAnimation a = ExitAnimation.fade(5000);
        assertEquals(ExitAnimation.maxDurationMs, a.durationMs());
        assertTrue(a.finished(ExitAnimation.maxDurationMs * ms + 0.001), "done just past the cap");
        assertFalse(a.finished(ExitAnimation.maxDurationMs * ms - 0.001), "not done before the cap");
        assertEquals(ExitAnimation.maxDurationMs, ExitAnimation.fadeScale(9999).durationMs());
    }

    @Test
    void noneIsTheCanonicalInstantExit() {
        ExitAnimation explicit = new ExitAnimation(ExitAnimation.Kind.none, 400, ExitAnimation.Easing.easeIn);
        assertEquals(ExitAnimation.none, explicit, "none normalizes away duration and easing");
        assertTrue(explicit.instant());
        assertEquals(0, explicit.durationMs());
        assertTrue(explicit.finished(0), "instant exit is always finished");
        assertEquals(0f, explicit.alphaAt(0));
        assertEquals(0f, explicit.alphaAt(-1));
        assertEquals(1f, explicit.scaleAt(5));
        assertEquals(0f, explicit.riseAt(5));
    }

    @Test
    void zeroOrNegativeDurationNormalizesToNone() {
        assertEquals(ExitAnimation.none, ExitAnimation.fade(0));
        assertEquals(ExitAnimation.none, ExitAnimation.fadeRise(-50));
        assertTrue(new ExitAnimation(ExitAnimation.Kind.fadeScale, 0, ExitAnimation.Easing.linear).instant());
    }

    @Test
    void elapsedOutsideTheWindowClampsToTheEndpoints() {
        ExitAnimation a = ExitAnimation.fadeRise(150, ExitAnimation.Easing.easeIn);
        assertEquals(1f, a.alphaAt(-10), "before the window: fully visible");
        assertEquals(0f, a.alphaAt(10), "after the window: fully gone");
        assertEquals(0f, a.riseAt(-10));
        assertEquals(1f, a.riseAt(10));
    }

    @Test
    void spentEnvelopeAndFinishedAgreeAtFloatPrecision() {
        // the last sliver of a frame rounds the eased progress to exactly 1
        // (alpha 0) a hair before the raw double comparison crosses the
        // duration — finished must agree with the envelope, never one frame
        // later, or a spent corpse lingers a frame at alpha 0
        for (int ms : new int[]{150, 180, 200, 300, 500}) {
            ExitAnimation a = ExitAnimation.fade(ms);
            for (double t = 0; t < a.durationSeconds() + 0.005; t += 0.0005) {
                boolean spent = a.alphaAt(t) <= 0f;
                assertEquals(spent, a.finished(t), ms + "ms at t=" + t);
            }
            assertTrue(a.finished(a.durationSeconds() + 1.0e-15));
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void nullKindIsRejectedAndNullEasingDefaultsToLinear() {
        assertThrows(IllegalArgumentException.class, () -> new ExitAnimation(null, 100, null));
        ExitAnimation a = new ExitAnimation(ExitAnimation.Kind.fade, 100, null);
        assertEquals(ExitAnimation.Easing.linear, a.easing());
    }
}
