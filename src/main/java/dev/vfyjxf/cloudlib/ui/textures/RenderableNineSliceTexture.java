package dev.vfyjxf.cloudlib.ui.textures;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class RenderableNineSliceTexture implements RenderableTexture {
    private final SpriteUploader spriteUploader;
    private final ResourceLocation location;
    private final int width;
    private final int height;
    private final int sliceLeft;
    private final int sliceRight;
    private final int sliceTop;
    private final int sliceBottom;

    public RenderableNineSliceTexture(SpriteUploader spriteUploader, ResourceLocation location, int width, int height, int left, int right, int top, int bottom) {
        this.spriteUploader = spriteUploader;
        this.location = location;

        this.width = width;
        this.height = height;
        this.sliceLeft = left;
        this.sliceRight = right;
        this.sliceTop = top;
        this.sliceBottom = bottom;
    }

    public void render(PoseStack poseStack, Rect area) {
        render(poseStack, area.x, area.y, area.width(), area.height);
    }

    public void render(PoseStack poseStack, int xOffset, int yOffset, int width, int height) {
        TextureAtlasSprite sprite = spriteUploader.getSprite(location);
        int leftWidth = sliceLeft;
        int rightWidth = sliceRight;
        int topHeight = sliceTop;
        int bottomHeight = sliceBottom;
        int textureWidth = this.width;
        int textureHeight = this.height;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, spriteUploader.atlasLocation);

        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();
        float uSize = uMax - uMin;
        float vSize = vMax - vMin;

        float uLeft = uMin + uSize * (leftWidth / (float) textureWidth);
        float uRight = uMax - uSize * (rightWidth / (float) textureWidth);
        float vTop = vMin + vSize * (topHeight / (float) textureHeight);
        float vBottom = vMax - vSize * (bottomHeight / (float) textureHeight);

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix = poseStack.last().pose();

        // left top
        render(bufferBuilder, matrix, uMin, vMin, uLeft, vTop, xOffset, yOffset, leftWidth, topHeight);
        // left bottom
        render(bufferBuilder, matrix, uMin, vBottom, uLeft, vMax, xOffset, yOffset + height - bottomHeight, leftWidth, bottomHeight);
        // right top
        render(bufferBuilder, matrix, uRight, vMin, uMax, vTop, xOffset + width - rightWidth, yOffset, rightWidth, topHeight);
        // right bottom
        render(bufferBuilder, matrix, uRight, vBottom, uMax, vMax, xOffset + width - rightWidth, yOffset + height - bottomHeight, rightWidth, bottomHeight);

        int middleWidth = textureWidth - leftWidth - rightWidth;
        int middleHeight = textureWidth - topHeight - bottomHeight;
        int tiledMiddleWidth = width - leftWidth - rightWidth;
        int tiledMiddleHeight = height - topHeight - bottomHeight;
        if (tiledMiddleWidth > 0) {
            // top edge
            drawTiled(bufferBuilder, matrix, uLeft, vMin, uRight, vTop, xOffset + leftWidth, yOffset, tiledMiddleWidth, topHeight, middleWidth, topHeight);
            // bottom edge
            drawTiled(bufferBuilder, matrix, uLeft, vBottom, uRight, vMax, xOffset + leftWidth, yOffset + height - bottomHeight, tiledMiddleWidth, bottomHeight, middleWidth, bottomHeight);
        }
        if (tiledMiddleHeight > 0) {
            // left side
            drawTiled(bufferBuilder, matrix, uMin, vTop, uLeft, vBottom, xOffset, yOffset + topHeight, leftWidth, tiledMiddleHeight, leftWidth, middleHeight);
            // right side
            drawTiled(bufferBuilder, matrix, uRight, vTop, uMax, vBottom, xOffset + width - rightWidth, yOffset + topHeight, rightWidth, tiledMiddleHeight, rightWidth, middleHeight);
        }
        if (tiledMiddleHeight > 0 && tiledMiddleWidth > 0) {
            // middle area
            drawTiled(bufferBuilder, matrix, uLeft, vTop, uRight, vBottom, xOffset + leftWidth, yOffset + topHeight, tiledMiddleWidth, tiledMiddleHeight, middleWidth, middleHeight);
        }

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static void drawTiled(BufferBuilder bufferBuilder, Matrix4f matrix, float uMin, float vMin, float uMax, float vMax, int xOffset, int yOffset, int tiledWidth, int tiledHeight, int width, int height) {
        int xTileCount = tiledWidth / width;
        int xRemainder = tiledWidth - (xTileCount * width);
        int yTileCount = tiledHeight / height;
        int yRemainder = tiledHeight - (yTileCount * height);

        int yStart = yOffset + tiledHeight;

        float uSize = uMax - uMin;
        float vSize = vMax - vMin;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int tileWidth = (xTile == xTileCount) ? xRemainder : width;
                int tileHeight = (yTile == yTileCount) ? yRemainder : height;
                int x = xOffset + (xTile * width);
                int y = yStart - ((yTile + 1) * height);
                if (tileWidth > 0 && tileHeight > 0) {
                    int maskRight = width - tileWidth;
                    int maskTop = height - tileHeight;
                    float uOffset = (maskRight / (float) width) * uSize;
                    float vOffset = (maskTop / (float) height) * vSize;

                    render(bufferBuilder, matrix, uMin, vMin + vOffset, uMax - uOffset, vMax, x, y + maskTop, tileWidth, tileHeight);
                }
            }
        }
    }

    private static void render(BufferBuilder bufferBuilder, Matrix4f matrix, float minU, double minV, float maxU, float maxV, int xOffset, int yOffset, int width, int height) {
        bufferBuilder.addVertex(matrix, xOffset, yOffset + height, 0)
                .setUv(minU, maxV);
        bufferBuilder.addVertex(matrix, xOffset + width, yOffset + height, 0)
                .setUv(maxU, maxV);
        bufferBuilder.addVertex(matrix, xOffset + width, yOffset, 0)
                .setUv(maxU, (float) minV);
        bufferBuilder.addVertex(matrix, xOffset, yOffset, 0)
                .setUv(minU, (float) minV);
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
    public void render(GuiGraphics graphics, int xOffset, int yOffset) {
//        render(poseStack, xOffset, yOffset, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset, int maskTop, int maskBottom, int maskLeft, int maskRight) {
//        render(graphics, xOffset, yOffset, width, height);
    }
}
