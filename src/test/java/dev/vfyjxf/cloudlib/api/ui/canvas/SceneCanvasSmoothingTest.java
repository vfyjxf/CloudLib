package dev.vfyjxf.cloudlib.api.ui.canvas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The SDF smoothing scale for both target kinds: the main gui framebuffer
 * rasterizes at {@code guiScale} pixels per logical pixel (AA half-width
 * {@code 1/guiScale}), an offscreen surface rasterizes at its own
 * {@code ss} texels per logical pixel with the window's guiScale irrelevant
 * inside the FBO (AA half-width {@code 1/ss}). The two densities are never
 * multiplied — a surface at {@code ss} on a guiScale-3 window smooths at
 * {@code 1/ss}, not {@code 1/(3·ss)}.
 */
class SceneCanvasSmoothingTest {

    @Test
    void mainTargetUsesGuiScaleOnly() {
        assertEquals(1.0f, SceneCanvas.smoothing(1f));
        assertEquals(0.5f, SceneCanvas.smoothing(2f));
        assertEquals(1f / 3f, SceneCanvas.smoothing(3f), 1.0e-6f);
        assertEquals(0.25f, SceneCanvas.smoothing(4f));
    }

    @Test
    void offscreenTargetUsesItsOwnDensity() {
        // an FBO rasterizing at ss texels per logical pixel: one texel of AA,
        // whatever the window's gui scale happens to be
        assertEquals(1.0f, SceneCanvas.offscreenSmoothing(1f));
        assertEquals(0.5f, SceneCanvas.offscreenSmoothing(2f));
        assertEquals(0.25f, SceneCanvas.offscreenSmoothing(4f));
        assertEquals(0.125f, SceneCanvas.offscreenSmoothing(8f));
    }

    @Test
    void degenerateInputsClampToScaleOne() {
        assertEquals(1.0f, SceneCanvas.smoothing(0f));
        assertEquals(0.5f, SceneCanvas.smoothing(2f));
        assertEquals(1.0f, SceneCanvas.offscreenSmoothing(0f));
        assertEquals(1.0f, SceneCanvas.offscreenSmoothing(-2f));
    }
}
