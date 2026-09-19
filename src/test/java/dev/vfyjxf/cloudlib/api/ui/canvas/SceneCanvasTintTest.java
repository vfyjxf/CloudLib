package dev.vfyjxf.cloudlib.api.ui.canvas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The tint arithmetic behind {@link SceneCanvas#color(int)}: every
 * color-carrying primitive (fill, text, gradient, line, freeform quad, shader
 * shape) multiplies its own color by the current tint per channel — the white
 * default is the identity. This is the contract that makes a subtree-wide
 * fade (an alpha-only tint set around a subtree's render) reach the real draw
 * calls: text and fill widgets keep setting their own colors, and the tint
 * scales all of them.
 */
class SceneCanvasTintTest {

    @Test
    void whiteTintIsTheIdentity() {
        assertEquals(0xFF2E4057, SceneCanvas.multiplyColor(0xFF2E4057, 0xFFFFFFFF));
        assertEquals(0x80F0E0D0, SceneCanvas.multiplyColor(0x80F0E0D0, 0xFFFFFFFF));
    }

    @Test
    void alphaOnlyTintScalesOnlyAlpha() {
        // the fade shape: white channels, scaled alpha
        assertEquals(0x80FFFFFF, SceneCanvas.multiplyColor(0xFFFFFFFF, 0x80FFFFFF));
        assertEquals(0x402E4057, SceneCanvas.multiplyColor(0xFF2E4057, 0x40FFFFFF));
        assertEquals(0xC0F0E0D0, SceneCanvas.multiplyColor(0xFFF0E0D0, 0xC0FFFFFF));
    }

    @Test
    void tintScalesEveryChannel() {
        // a gray tint halves every channel including alpha
        assertEquals(0x80808080, SceneCanvas.multiplyColor(0xFFFFFFFF, 0x80808080));
        // per-channel: r 0x20·0x80→0x10, g 0x40·0xC0→0x30, b 0x60·0xFF→0x60
        assertEquals(0xFF103060, SceneCanvas.multiplyColor(0xFF204060, 0xFF80C0FF));
    }

    @Test
    void zeroTintBlacksEverythingOut() {
        assertEquals(0x00000000, SceneCanvas.multiplyColor(0xFF2E4057, 0x00000000));
        // a fully transparent but colored tint zeroes alpha, keeps its channels
        assertEquals(0x00FFFFFF, SceneCanvas.multiplyColor(0xFFFFFFFF, 0x00FFFFFF));
    }

    @Test
    void doubleFadingComposes() {
        // a fade of a fade (parent and child each halve): 0.5 × 0.5 = 0.25
        int half = SceneCanvas.multiplyColor(0xFFFFFFFF, 0x80FFFFFF);
        int quarter = SceneCanvas.multiplyColor(half, 0x80FFFFFF);
        assertEquals(0x40, (quarter >>> 24) & 0xFF);
        assertEquals(0x40FFFFFF, quarter);
    }
}
