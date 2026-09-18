package dev.vfyjxf.cloudlib.internal.ui.inworld;

/**
 * The mip-chain validity state behind a surface texture's MIN filter.
 * {@code RenderTarget.createBuffers} allocates level 0 only, so every
 * allocation or resize leaves the texture without a chain; a texture whose
 * MIN filter is mipmap-based while the chain is missing is
 * <b>mipmap-incomplete</b> and samples as opaque black
 * {@code (0, 0, 0, 1)}. The chain is only valid after
 * {@code glGenerateMipmap} ran against the current level-0 content — this
 * machine tracks that, so the surface can pick a MIN filter that is always
 * complete ({@link MinFilter#linear} while the chain is gone) and regenerate
 * the chain on the first frame after any reallocation even when the quad
 * magnifies and {@link dev.vfyjxf.cloudlib.api.ui.inworld.render.Supersampling#needsMipmap}
 * would otherwise skip it.
 * <p>
 * Pure state (no GL) — the frame decision is unit-testable headless;
 * {@link UiSurface} applies it.
 */
public final class MipmapChain {

    /** The MIN filter the texture may carry right now — both stay complete on a level-0-only texture. */
    public enum MinFilter {
        /** Plain bilinear — the only complete choice while no chain exists. */
        linear,
        /** Trilinear mipmap filtering — valid only with a complete chain. */
        linearMipmapLinear
    }

    private boolean valid;

    /** The texture storage was (re)allocated — allocate/resize drop the chain. */
    public void reallocated() {
        valid = false;
    }

    /** {@code glGenerateMipmap} ran — the chain is complete again. */
    public void generated() {
        valid = true;
    }

    /** Whether a complete chain currently exists. */
    public boolean isValid() {
        return valid;
    }

    /**
     * The MIN filter safe to leave set on the texture this frame: mipmap
     * filtering only while a complete chain exists, plain {@code LINEAR}
     * while it does not — the texture never sits in the incomplete state
     * that samples black.
     */
    public MinFilter minFilter() {
        return valid ? MinFilter.linearMipmapLinear : MinFilter.linear;
    }

    /**
     * Whether this frame must regenerate the chain: the quad minifies the
     * surface (mipmaps are what filters the shrink) <em>or</em> the chain is
     * stale after a reallocation — so the first frame after any resize always
     * regenerates, even while magnified, before the filter returns to
     * mipmap sampling.
     */
    public boolean shouldGenerate(boolean quadMinifies) {
        return quadMinifies || !valid;
    }
}
