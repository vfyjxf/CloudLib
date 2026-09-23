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
        shader.getUniform("LineWidth").set(style.lineWidth());
        shader.getUniform("EdgeWidth").set(style.edgeWidth());
        color(shader, "LineColor", style.lineColor());
        color(shader, "EdgeColor", style.edgeColor());
        shader.getUniform("FadeFraction").set(style.fadeFraction());
        shader.getUniform("FadeAlpha").set(style.fadeAlpha());
        shader.getUniform("DashPeriod").set(style.dashPeriodPx());
        shader.getUniform("DashDuty").set(style.dashDuty());
        shader.getUniform("DashPhase").set(style.dashPhasePx());
        shader.getUniform("ArcStart").set(style.arcStart());
        shader.getUniform("ArcEnd").set(style.arcEnd());
    }

    /** Decomposes an ARGB int into the (r, g, b, a) float quad a colour uniform takes. */
    @SuppressWarnings("DataFlowIssue")
    public static void color(ShaderInstance shader, String name, int argb) {
        Uniform uniform = shader.getUniform(name);
        uniform.set(
            ((argb >> 16) & 0xFF) / 255f,
            ((argb >> 8) & 0xFF) / 255f,
            (argb & 0xFF) / 255f,
            ((argb >>> 24) & 0xFF) / 255f
        );
    }
}
