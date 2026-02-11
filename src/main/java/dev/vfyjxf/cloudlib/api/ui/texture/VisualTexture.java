package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Base interface for all renderable textures.
 * <p>
 * Built-in texture types:
 * <ul>
 *   <li>{@link ImageTexture} - Image textures</li>
 *   <li>{@link ColorTexture} - Solid color textures</li>
 *   <li>{@link NineSliceTexture} - Nine-slice textures</li>
 *   <li>{@link SpriteTexture} - Sprite textures</li>
 *   <li>{@link FrameAnimation} - Frame animations</li>
 *  </ul>
 * </p>
 */
@FunctionalInterface
public interface VisualTexture {

    /**
     * Renders this texture at the specified position and size.
     */
    void render(GuiGraphics graphics, int x, int y, int width, int height);

    /**
     * An empty texture that renders nothing.
     */
    VisualTexture empty = (graphics, x, y, width, height) -> {};

    //region factory

    /**
     * Creates a texture from a sprite ResourceLocation.
     */
    static VisualTexture sprite(ResourceLocation spriteLocation) {
        return (graphics, x, y, w, h) -> {
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(spriteLocation);
            graphics.blit(x, y, 0, w, h, sprite);
        };
    }

    /**
     * Composites multiple textures into layers.
     */
    static VisualTexture composite(VisualTexture... layers) {
        return (graphics, x, y, width, height) -> {
            for (VisualTexture layer : layers) {
                layer.render(graphics, x, y, width, height);
            }
        };
    }

    /**
     * Applies a color tint.
     */
    static VisualTexture tinted(VisualTexture texture, int argb) {
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
    static VisualTexture tinted(VisualTexture texture, IntSupplier colorSupplier) {
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
    static VisualTexture clipped(VisualTexture texture, int clipLeft, int clipTop, int clipRight, int clipBottom) {
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
    static VisualTexture offset(VisualTexture texture, int offsetX, int offsetY) {
        return (graphics, x, y, width, height) ->
            texture.render(graphics, x + offsetX, y + offsetY, width, height);
    }

    /**
     * Adds padding around the texture.
     */
    static VisualTexture padded(VisualTexture texture, int padding) {
        return padded(texture, padding, padding, padding, padding);
    }

    /**
     * Adds custom padding around the texture.
     */
    static VisualTexture padded(VisualTexture texture, int top, int bottom, int left, int right) {
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
    static VisualTexture conditional(VisualTexture texture, Supplier<Boolean> condition) {
        return (graphics, x, y, width, height) -> {
            if (condition.get()) {
                texture.render(graphics, x, y, width, height);
            }
        };
    }

    /**
     * Wraps a texture with explicit dimensions.
     */
    static SizedTexture sized(VisualTexture texture, int width, int height) {
        return new SizedTexture() {
            @Override
            public int width() {return width;}

            @Override
            public int height() {return height;}

            @Override
            public void render(GuiGraphics graphics, int x, int y, int w, int h) {
                texture.render(graphics, x, y, w, h);
            }
        };
    }

    //endregion

}
