package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A texture drawn through an ARGB tint — the {@code color(#hex)} css modifier.
 * <p>
 * The tint composes with whatever tint the draw already carries (a canvas
 * {@code color(...)} scope, a widget's fade), and with the wrapped texture's own
 * color for the self-colored kinds ({@link ColorTexture}, {@link GradientTexture},
 * {@link BorderTexture}, {@link RoundedRectTexture}) — see
 * {@link VisualTexture#multiply}. Batching is preserved whenever the wrapped
 * texture supports it: the composed tint is what reaches the vertex color.
 */
public record TintedTexture(VisualTexture texture, int tint) implements BatchableTexture {

    // region factory

    /** Tints {@code texture} with {@code argb} — {@code -1} is the identity tint. */
    public static TintedTexture of(VisualTexture texture, int tint) {
        return new TintedTexture(texture, tint);
    }

    // endregion

    // region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        boolean blended = (tint >>> 24) != 0xFF;
        if (blended) {
            RenderSystem.enableBlend();
        }
        RenderSystem.setShaderColor(
            ((tint >> 16) & 0xFF) / 255f,
            ((tint >> 8) & 0xFF) / 255f,
            (tint & 0xFF) / 255f,
            (tint >>> 24) / 255f
        );
        texture.render(graphics, x, y, width, height);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    // endregion

    // region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int color) {
        if (texture instanceof BatchableTexture batchable) {
            batchable.emit(emitter, x, y, width, height, VisualTexture.multiply(tint, color));
        }
    }

    @Override
    public boolean supportsBatching() {
        return texture instanceof BatchableTexture batchable && batchable.supportsBatching();
    }

    // endregion
}
