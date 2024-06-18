package dev.vfyjxf.cloudlib.helper;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.vfyjxf.cloudlib.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.css.Rect;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * @author KilaBash
 * From LDLib
 */
public class RenderHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void drawFluidTexture(@Nonnull GuiGraphics graphics, float xCoord, float yCoord, TextureAtlasSprite textureSprite, int maskTop, int maskRight, float zLevel, int fluidColor) {
        float uMin = textureSprite.getU0();
        float uMax = textureSprite.getU1();
        float vMin = textureSprite.getV0();
        float vMax = textureSprite.getV1();
        uMax = uMax - maskRight / 16f * (uMax - uMin);
        vMax = vMax - maskTop / 16f * (vMax - vMin);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        var mat = graphics.pose().last().pose();
        buffer.addVertex(mat, xCoord, yCoord + 16, zLevel).setUv(uMin, vMax).setColor(fluidColor);
        buffer.addVertex(mat, xCoord + 16 - maskRight, yCoord + 16, zLevel).setUv(uMax, vMax).setColor(fluidColor);
        buffer.addVertex(mat, xCoord + 16 - maskRight, yCoord + maskTop, zLevel).setUv(uMax, vMin).setColor(fluidColor);
        buffer.addVertex(mat, xCoord, yCoord + maskTop, zLevel).setUv(uMin, vMin).setColor(fluidColor);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void drawFluidForGui(@NotNull GuiGraphics graphics, FluidStack contents, long tankCapacity, int startX, int startY, int widthT, int heightT) {
        ResourceLocation LOCATION_BLOCKS_TEXTURE = InventoryMenu.BLOCK_ATLAS;
        var texture = IClientFluidTypeExtensions.of(contents.getFluid()).getStillTexture(contents);
        var fluidStillSprite = texture == null ? null : Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        if (fluidStillSprite == null) {
            fluidStillSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
            if (!FMLEnvironment.production) {
                LOGGER.error("Missing fluid texture for fluid: " + contents.getHoverName().getString());
            }
        }
        int fluidColor = IClientFluidTypeExtensions.of(contents.getFluid()).getTintColor(contents) | 0xff000000;
        int scaledAmount = (int) (contents.getAmount() * heightT / tankCapacity);
        if (contents.getAmount() > 0 && scaledAmount < 1) {
            scaledAmount = 1;
        }
        if (scaledAmount > heightT || contents.getAmount() == tankCapacity) {
            scaledAmount = heightT;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, LOCATION_BLOCKS_TEXTURE);

        final int xTileCount = widthT / 16;
        final int xRemainder = widthT - xTileCount * 16;
        final int yTileCount = scaledAmount / 16;
        final int yRemainder = scaledAmount - yTileCount * 16;

        final int yStart = startY + heightT;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int width = xTile == xTileCount ? xRemainder : 16;
                int height = yTile == yTileCount ? yRemainder : 16;
                int x = startX + xTile * 16;
                int y = yStart - (yTile + 1) * 16;
                if (width > 0 && height > 0) {
                    int maskTop = 16 - height;
                    int maskRight = 16 - width;
                    drawFluidTexture(graphics, x, y, fluidStillSprite, maskTop, maskRight, 0, fluidColor);
                }
            }
        }
        RenderSystem.enableBlend();
    }

    public static void drawBorder(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int color, int border) {
        graphics.drawManaged(() -> {
            drawSolidRect(graphics,x - border, y - border, width + 2 * border, border, color);
            drawSolidRect(graphics,x - border, y + height, width + 2 * border, border, color);
            drawSolidRect(graphics,x - border, y, border, height, color);
            drawSolidRect(graphics,x + width, y, border, height, color);
        });
    }

    public static void drawStringSized(@NotNull GuiGraphics graphics, String text, float x, float y, int color, boolean dropShadow, float scale, boolean center) {
        graphics.pose().pushPose();
        Font fontRenderer = Minecraft.getInstance().font;
        double scaledTextWidth = center ? fontRenderer.width(text) * scale : 0.0;
        graphics.pose().translate(x - scaledTextWidth / 2.0, y, 0.0f);
        graphics.pose().scale(scale, scale, scale);
        graphics.drawString(fontRenderer, text, 0, 0, color, dropShadow);
        graphics.pose().popPose();
    }

    public static void drawStringFixedCorner(@NotNull GuiGraphics graphics, String text, float x, float y, int color, boolean dropShadow, float scale) {
        Font fontRenderer = Minecraft.getInstance().font;
        float scaledWidth = fontRenderer.width(text) * scale;
        float scaledHeight = fontRenderer.lineHeight * scale;
        drawStringSized(graphics, text, x - scaledWidth, y - scaledHeight, color, dropShadow, scale, false);
    }

    public static void drawText(@NotNull GuiGraphics graphics, String text, float x, float y, float scale, int color) {
        drawText(graphics, text, x, y, scale, color, false);
    }

    public static void drawText(@NotNull GuiGraphics graphics, String text, float x, float y, float scale, int color, boolean shadow) {
        Font fontRenderer = Minecraft.getInstance().font;
        RenderSystem.disableBlend();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 0f);
        float sf = 1 / scale;
        graphics.drawString(fontRenderer, text, (int) (x * sf), (int) (y * sf), color, shadow);
        graphics.pose().popPose();
        RenderSystem.enableBlend();
    }

    public static void drawItemStack(@NotNull GuiGraphics graphics, ItemStack itemStack, int x, int y, int color, @Nullable String altTxt) {
        float a = ((color >> 24) & 0xff) / 255f;
        float r = ((color >> 16) & 0xff) / 255f;
        float g = ((color >> 8) & 0xff) / 255f;
        float b = ((color) & 0xff) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        Minecraft mc = Minecraft.getInstance();

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 232);
        graphics.renderItem(itemStack, x, y);
        graphics.renderItemDecorations(mc.font, itemStack, x, y, altTxt);
        graphics.pose().popPose();

        // clear depth buffer,it may cause some rendering issues?
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
    }

    public static List<Component> getItemToolTip(ItemStack itemStack) {
        Minecraft mc = Minecraft.getInstance();
        return Screen.getTooltipFromItem(mc, itemStack);
    }

    public static void drawSolidRect(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
        RenderSystem.enableBlend();
    }

    public static void drawSolidRect(@NotNull GuiGraphics graphics, Rectangle rect, int color) {
        drawSolidRect(graphics, rect.x, rect.y, rect.width, rect.height, color);
    }

    public static void drawRectShadow(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, int distance) {
        drawGradientRect(graphics, x + distance, y + height, width - distance, distance, 0x4f000000, 0, false);
        drawGradientRect(graphics, x + width, y + distance, distance, height - distance, 0x4f000000, 0, true);

        float startAlpha = (float) (0x4f) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        x += width;
        y += height;
        Matrix4f mat = graphics.pose().last().pose();
        buffer.addVertex(mat, x, y, 0).setColor(0, 0, 0, startAlpha);
        buffer.addVertex(mat, x, y + distance, 0).setColor(0, 0, 0, 0);
        buffer.addVertex(mat, x + distance, y + distance, 0).setColor(0, 0, 0, 0);

        buffer.addVertex(mat, x, y, 0).setColor(0, 0, 0, startAlpha);
        buffer.addVertex(mat, x + distance, y + distance, 0).setColor(0, 0, 0, 0);
        buffer.addVertex(mat, x + distance, y, 0).setColor(0, 0, 0, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void drawGradientRect(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, int startColor, int endColor) {
        drawGradientRect(graphics, x, y, width, height, startColor, endColor, false);
    }

    public static void drawGradientRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int startColor, int endColor, boolean horizontal) {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
        float startBlue  = (float)(startColor       & 255) / 255.0F;
        float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
        float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
        float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
        float endBlue    = (float)(endColor         & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Matrix4f mat = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        if (horizontal) {
            buffer.addVertex(mat,x + width, y, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            buffer.addVertex(mat,x, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y + height, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x + width, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } else {
            buffer.addVertex(mat,x + width, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            buffer.addVertex(mat,x + width, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
    }

    public static void drawLines(@Nonnull GuiGraphics graphics, List<Vec2> points, int startColor, int endColor, float width) {
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        drawColorLines(graphics.pose(), bufferbuilder, points, startColor, endColor, width);

        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    public static void drawTextureRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height) {
        Tesselator tesselator = Tesselator.getInstance();
        Matrix4f mat = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(mat, x, y + height, 0).setUv(0, 0);
        buffer.addVertex(mat, x + width, y + height, 0).setUv(1, 0);
        buffer.addVertex(mat, x + width, y, 0).setUv(1, 1);
        buffer.addVertex(mat, x, y, 0).setUv(0, 1);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void drawColorLines(@Nonnull PoseStack poseStack, VertexConsumer builder, List<Vec2> points, int setColorStart, int setColorEnd, float width) {
        if (points.size() < 2) return;
        Matrix4f mat = poseStack.last().pose();
        Vec2 lastPoint = points.get(0);
        Vec2 point = points.get(1);
        Vector3f vec = null;
        int sa = (setColorStart >> 24) & 0xff, sr = (setColorStart >> 16) & 0xff, sg = (setColorStart >> 8) & 0xff, sb = setColorStart & 0xff;
        int ea = (setColorEnd >> 24) & 0xff, er = (setColorEnd >> 16) & 0xff, eg = (setColorEnd >> 8) & 0xff, eb = setColorEnd & 0xff;
        ea = (ea - sa);
        er = (er - sr);
        eg = (eg - sg);
        eb = (eb - sb);
        for (int i = 1; i < points.size(); i++) {
            float s = (i - 1f) / points.size();
            float e = i * 1f / points.size();
            point = points.get(i);
            vec = new Vector3f(point.x - lastPoint.x, point.y - lastPoint.y, 0).rotateZ(Mth.HALF_PI).normalize().mul(-width);
            builder.addVertex(mat, lastPoint.x + vec.x, lastPoint.y + vec.y, 0)
                    .setColor((sr + er * s) / 255, (sg + eg * s) / 255, (sb + eb * s) / 255, (sa + ea * s) / 255)
            ;
            vec.mul(-1);
            builder.addVertex(mat, lastPoint.x + vec.x, lastPoint.y + vec.y, 0)
                    .setColor((sr + er * e) / 255, (sg + eg * e) / 255, (sb + eb * e) / 255, (sa + ea * e) / 255)
            ;
            lastPoint = point;
        }
        vec.mul(-1);
        builder.addVertex(mat, point.x + vec.x, point.y + vec.y, 0)
                .setColor(sr + er, sg + eg, sb + eb, sa + ea)
        ;
        vec.mul(-1);
        builder.addVertex(mat, point.x + vec.x, point.y + vec.y, 0)
                .setColor(sr + er, sg + eg, sb + eb, sa + ea)
        ;
    }

    public static void drawColorTexLines(@Nonnull PoseStack poseStack, VertexConsumer builder, List<Vec2> points, int setColorStart, int setColorEnd, float width) {
        if (points.size() < 2) return;
        Matrix4f mat = poseStack.last().pose();
        Vec2 lastPoint = points.get(0);
        Vec2 point = points.get(1);
        Vector3f vec = null;
        int sa = (setColorStart >> 24) & 0xff, sr = (setColorStart >> 16) & 0xff, sg = (setColorStart >> 8) & 0xff, sb = setColorStart & 0xff;
        int ea = (setColorEnd >> 24) & 0xff, er = (setColorEnd >> 16) & 0xff, eg = (setColorEnd >> 8) & 0xff, eb = setColorEnd & 0xff;
        ea = (ea - sa);
        er = (er - sr);
        eg = (eg - sg);
        eb = (eb - sb);
        for (int i = 1; i < points.size(); i++) {
            float s = (i - 1f) / points.size();
            float e = i * 1f / points.size();
            point = points.get(i);
            float u = (i - 1f) / points.size();
            vec = new Vector3f(point.x - lastPoint.x, point.y - lastPoint.y, 0).rotateZ(Mth.HALF_PI).normalize().mul(-width);
            builder.addVertex(mat, lastPoint.x + vec.x, lastPoint.y + vec.y, 0).setUv(u,0)
                    .setColor((sr + er * s) / 255, (sg + eg * s) / 255, (sb + eb * s) / 255, (sa + ea * s) / 255)
            ;
            vec.mul(-1);
            builder.addVertex(mat, lastPoint.x + vec.x, lastPoint.y + vec.y, 0).setUv(u,1)
                    .setColor((sr + er * e) / 255, (sg + eg * e) / 255, (sb + eb * e) / 255, (sa + ea * e) / 255)
            ;
            lastPoint = point;
        }
        vec.mul(-1);
        builder.addVertex(mat, point.x + vec.x, point.y + vec.y, 0).setUv(1,0)
                .setColor(sr + er, sg + eg, sb + eb, sa + ea)
        ;
        vec.mul(-1);
        builder.addVertex(mat, point.x + vec.x, point.y + vec.y, 0).setUv(1,1)
                .setColor(sr + er, sg + eg, sb + eb, sa + ea)
        ;
    }

    public static void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltipTexts, ItemStack tooltipStack, @Nullable TooltipComponent tooltipComponent, Font tooltipFont) {
        graphics.renderTooltip(tooltipFont, tooltipTexts, Optional.ofNullable(tooltipComponent), tooltipStack, mouseX, mouseY);
    }

    public static ClientTooltipComponent getClientTooltipComponent(TooltipComponent component) {
        return ClientTooltipComponent.create(component);
    }
}
