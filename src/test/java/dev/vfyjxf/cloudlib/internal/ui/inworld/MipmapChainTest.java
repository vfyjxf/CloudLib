package dev.vfyjxf.cloudlib.internal.ui.inworld;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MipmapChain} — the mip-validity state machine behind
 * {@code UiSurface}'s MIN filter. The contract under repair: an allocation
 * or resize leaves the texture with level 0 only, and a mipmap MIN filter
 * over a level-0-only texture is incomplete (samples opaque black), so the
 * filter must fall back to LINEAR while the chain is gone and the first
 * frame after any reallocation must regenerate the chain — even when the
 * quad magnifies the surface and would otherwise skip generation.
 */
class MipmapChainTest {

    @Test
    void freshSurfaceGeneratesOnItsFirstFrameWhateverTheMagnification() {
        // adaptive supersampling can sit the surface at ≈1 texel per
        // framebuffer pixel — needsMipmap answers false right after the
        // allocation, and the chain must be built anyway
        MipmapChain chain = new MipmapChain();
        assertFalse(chain.isValid());
        assertEquals(MipmapChain.MinFilter.linear, chain.minFilter(), "no chain yet — MIN must stay complete (LINEAR)");
        assertTrue(chain.shouldGenerate(false), "the allocation frame generates even while magnified");
        chain.generated();
        assertTrue(chain.isValid());
        assertEquals(MipmapChain.MinFilter.linearMipmapLinear, chain.minFilter());
    }

    @Test
    void resizeInvalidatesAndTheNextFrameRegenerates() {
        MipmapChain chain = new MipmapChain();
        chain.reallocated();
        chain.generated();
        assertTrue(chain.isValid());
        // the resize frame: chain gone, filter must drop to LINEAR before
        // anything could sample it, and this frame rebuilds the chain
        chain.reallocated();
        assertFalse(chain.isValid());
        assertEquals(MipmapChain.MinFilter.linear, chain.minFilter());
        assertTrue(chain.shouldGenerate(false), "first frame after a resize always regenerates");
        chain.generated();
        assertEquals(MipmapChain.MinFilter.linearMipmapLinear, chain.minFilter());
    }

    @Test
    void steadyStateMagnifyingSkipsGenerationAndMinifyingRegenerates() {
        MipmapChain chain = new MipmapChain();
        chain.reallocated();
        chain.generated();
        // magnified: level 0 is all the LINEAR magnifier samples — skip
        assertFalse(chain.shouldGenerate(false));
        assertEquals(
            MipmapChain.MinFilter.linearMipmapLinear,
            chain.minFilter(),
            "the existing chain keeps mipmap filtering"
        );
        // minified: the chain filters the shrink — regenerate against the
        // fresh level-0 content
        assertTrue(chain.shouldGenerate(true));
    }

    @Test
    void theFilterIsNeverMipmapWhileTheChainIsInvalid() {
        // the black-screen invariant: walk the resize/normal/magnify/minify
        // rhythm and assert the two can never combine into an incomplete
        // mipmap sample
        MipmapChain chain = new MipmapChain();
        boolean[] minifies = {false, true, false, false, true, false, true, true, false};
        boolean[] resizes = {true, false, false, true, false, false, false, true, false};
        for (int frame = 0; frame < minifies.length; frame++) {
            if (resizes[frame]) chain.reallocated();
            assertEquals(
                chain.isValid() ? MipmapChain.MinFilter.linearMipmapLinear : MipmapChain.MinFilter.linear,
                chain.minFilter(),
                "frame " + frame + ": MIN filter must match chain validity"
            );
            if (chain.shouldGenerate(minifies[frame])) {
                // UiSurface's half: generate, then flip validity — the same
                // frame may return to mipmap filtering
                chain.generated();
                assertEquals(
                    MipmapChain.MinFilter.linearMipmapLinear,
                    chain.minFilter(),
                    "frame " + frame + ": post-generation filter"
                );
            }
        }
    }

    @Test
    void resizeThenLongMagnifyingStretchRegeneratesExactlyOnce() {
        // the adaptive-ss failure mode: after a resize the panel stays
        // magnified for many frames — without the validity bit the surface
        // would never rebuild the chain and stay mipmap-incomplete forever
        MipmapChain chain = new MipmapChain();
        chain.reallocated();
        chain.generated();
        chain.reallocated();
        int generations = 0;
        for (int frame = 0; frame < 120; frame++) {
            if (chain.shouldGenerate(false)) {
                chain.generated();
                generations++;
            }
        }
        assertEquals(1, generations, "one regeneration right after the resize, then quiet");
        assertTrue(chain.isValid());
    }
}
