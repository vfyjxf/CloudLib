package dev.vfyjxf.cloudlib.api.ui.canvas;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The guide-line style uniforms, written the same way for both passes: the
 * screen path draws through {@link SceneCanvas#guideLine} and a world pass
 * through its own render types, but the stroke's width, hues, dash, fade and
 * arc window are one vocabulary — a change here lands on both, and the
 * resource test pins the names against the shaders' json.
 */
@OnlyIn(Dist.CLIENT)
public final class GuideLineUniforms {

    private GuideLineUniforms() {}

    /**
     * Writes {@code style} into the guide-line shader's style uniforms. The
     * dash duty, the fade and the arc window ride along; the geometry-sized
     * uniforms ({@code Size}, {@code Points}, the pass's screen size) stay the
     * caller's business.
     */
    public static void applyStyle(ShaderInstance shader, GuideLineStyle style) {
        uniform(shader, "LineWidth").set(style.lineWidth());
        uniform(shader, "EdgeWidth").set(style.edgeWidth());
        color(shader, "LineColor", style.lineColor());
        color(shader, "EdgeColor", style.edgeColor());
        uniform(shader, "FadeFraction").set(style.fadeFraction());
        uniform(shader, "FadeAlpha").set(style.fadeAlpha());
        uniform(shader, "DashPeriod").set(style.dashPeriodPx());
        uniform(shader, "DashDuty").set(style.dashDuty());
        uniform(shader, "DashPhase").set(style.dashPhasePx());
        uniform(shader, "ArcStart").set(style.arcStart());
        uniform(shader, "ArcEnd").set(style.arcEnd());
    }

    /** Decomposes an ARGB int into the (r, g, b, a) float quad a colour uniform takes. */
    public static void color(ShaderInstance shader, String name, int argb) {
        Uniform uniform = uniform(shader, name);
        uniform.set(
            ((argb >> 16) & 0xFF) / 255f,
            ((argb >> 8) & 0xFF) / 255f,
            (argb & 0xFF) / 255f,
            ((argb >>> 24) & 0xFF) / 255f
        );
    }

    /**
     * The named uniform of {@code shader}. A name the shader's json does not
     * declare is a broken draw, so it fails here naming the uniform instead of
     * silently setting nothing.
     */
    private static Uniform uniform(ShaderInstance shader, String name) {
        Uniform uniform = shader.getUniform(name);
        if (uniform == null) {
            throw new IllegalStateException("Shader uniform not found: " + name);
        }
        return uniform;
    }
}
