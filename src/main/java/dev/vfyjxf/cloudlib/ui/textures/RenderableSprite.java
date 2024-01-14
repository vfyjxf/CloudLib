package dev.vfyjxf.cloudlib.ui.textures;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.vfyjxf.cloudlib.api.ui.IRenderableTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class RenderableSprite implements IRenderableTexture {
    private final SpriteUploader spriteUploader;
    private final ResourceLocation location;
    private final int width;
    private final int height;
    private int trimLeft;
    private int trimRight;
    private int trimTop;
    private int trimBottom;

    public RenderableSprite(SpriteUploader spriteUploader, ResourceLocation location, int width, int height) {
        this.spriteUploader = spriteUploader;
        this.location = location;
        this.width = width;
        this.height = height;
    }

    public RenderableSprite trim(int left, int right, int top, int bottom) {
        this.trimLeft = left;
        this.trimRight = right;
        this.trimTop = top;
        this.trimBottom = bottom;
        return this;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void render(GuiGraphics poseStack, int xOffset, int yOffset) {
        render(poseStack, xOffset, yOffset, 0, 0, 0, 0);
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset, int maskTop, int maskBottom, int maskLeft, int maskRight) {
        TextureAtlasSprite sprite = spriteUploader.getSprite(location);
        int textureWidth = this.width;
        int textureHeight = this.height;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, spriteUploader.atlasLocation);

        maskTop += trimTop;
        maskBottom += trimBottom;
        maskLeft += trimLeft;
        maskRight += trimRight;

        int x = xOffset + maskLeft;
        int y = yOffset + maskTop;
        int width = textureWidth - maskRight - maskLeft;
        int height = textureHeight - maskBottom - maskTop;
        float uSize = sprite.getU1() - sprite.getU0();
        float vSize = sprite.getV1() - sprite.getV0();

        float minU = sprite.getU0() + uSize * (maskLeft / (float) textureWidth);
        float minV = sprite.getV0() + vSize * (maskTop / (float) textureHeight);
        float maxU = sprite.getU1() - uSize * (maskRight / (float) textureWidth);
        float maxV = sprite.getV1() - vSize * (maskBottom / (float) textureHeight);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix = graphics.pose().last().pose();
        bufferBuilder.vertex(matrix, x, y + height, 0)
                .uv(minU, maxV)
                .endVertex();
        bufferBuilder.vertex(matrix, x + width, y + height, 0)
                .uv(maxU, maxV)
                .endVertex();
        bufferBuilder.vertex(matrix, x + width, y, 0)
                .uv(maxU, minV)
                .endVertex();
        bufferBuilder.vertex(matrix, x, y, 0)
                .uv(minU, minV)
                .endVertex();
        tessellator.end();
    }
}
