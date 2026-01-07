package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Utility class for texture composition and transformation.
 * <p>
 * For basic texture types, use their respective classes directly:
 * <ul>
 *   <li>{@link ImageTexture} - Image textures</li>
 *   <li>{@link ColorTexture} - Solid color textures</li>
 *   <li>{@link NineSliceTexture} - Nine-slice textures</li>
 *   <li>{@link FrameAnimation} - Frame animations</li>
 * </ul>
 */
public final class Textures {

    private Textures() {}

    /**
     * Creates a texture from a sprite ResourceLocation.
     */
    public static Texture sprite(ResourceLocation spriteLocation) {
        return (graphics, x, y, w, h) -> graphics.blitSprite(spriteLocation, x, y, w, h);
    }

    /**
     * Composites multiple textures into layers.
     */
    public static Texture composite(Texture... layers) {
        return (graphics, x, y, width, height) -> {
            for (Texture layer : layers) {
                layer.render(graphics, x, y, width, height);
            }
        };
    }

    /**
     * Applies a color tint.
     */
    public static Texture tinted(Texture texture, int argb) {
        return (graphics, x, y, width, height) -> {
            float a = ((argb >> 24) & 0xFF) / 255f;
            float r = ((argb >> 16) & 0xFF) / 255f;
            float g = ((argb >> 8) & 0xFF) / 255f;
            float b = (argb & 0xFF) / 255f;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(r, g, b, a);
            texture.render(graphics, x, y, width, height);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        };
    }

    /**
     * Applies a dynamic color tint.
     */
    public static Texture tinted(Texture texture, IntSupplier colorSupplier) {
        return (graphics, x, y, width, height) -> {
            int argb = colorSupplier.getAsInt();
            float a = ((argb >> 24) & 0xFF) / 255f;
            float r = ((argb >> 16) & 0xFF) / 255f;
            float g = ((argb >> 8) & 0xFF) / 255f;
            float b = (argb & 0xFF) / 255f;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(r, g, b, a);
            texture.render(graphics, x, y, width, height);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        };
    }

    /**
     * Clips the texture using scissor.
     */
    public static Texture clipped(Texture texture, int clipLeft, int clipTop, int clipRight, int clipBottom) {
        return (graphics, x, y, width, height) -> {
            int visibleW = width - clipLeft - clipRight;
            int visibleH = height - clipTop - clipBottom;
            if (visibleW <= 0 || visibleH <= 0) return;

            graphics.enableScissor(x + clipLeft, y + clipTop, x + width - clipRight, y + height - clipBottom);
            texture.render(graphics, x, y, width, height);
            graphics.disableScissor();
        };
    }

    /**
     * Offsets the texture position.
     */
    public static Texture offset(Texture texture, int offsetX, int offsetY) {
        return (graphics, x, y, width, height) ->
                texture.render(graphics, x + offsetX, y + offsetY, width, height);
    }

    /**
     * Adds padding around the texture.
     */
    public static Texture padded(Texture texture, int padding) {
        return padded(texture, padding, padding, padding, padding);
    }

    /**
     * Adds custom padding around the texture.
     */
    public static Texture padded(Texture texture, int top, int bottom, int left, int right) {
        return (graphics, x, y, width, height) -> {
            int innerW = width - left - right;
            int innerH = height - top - bottom;
            if (innerW > 0 && innerH > 0) {
                texture.render(graphics, x + left, y + top, innerW, innerH);
            }
        };
    }

    /**
     * Renders conditionally.
     */
    public static Texture conditional(Texture texture, Supplier<Boolean> condition) {
        return (graphics, x, y, width, height) -> {
            if (condition.get()) {
                texture.render(graphics, x, y, width, height);
            }
        };
    }

    /**
     * Wraps a texture with explicit dimensions.
     */
    public static SizedTexture sized(Texture texture, int width, int height) {
        return new SizedTexture() {
            @Override
            public int width() { return width; }
            @Override
            public int height() { return height; }
            @Override
            public void render(GuiGraphics graphics, int x, int y, int w, int h) {
                texture.render(graphics, x, y, w, h);
            }
        };
    }
}
