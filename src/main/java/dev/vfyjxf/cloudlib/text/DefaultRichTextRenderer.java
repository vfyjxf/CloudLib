package dev.vfyjxf.cloudlib.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.text.BlockNode;
import dev.vfyjxf.cloudlib.api.text.CustomRenderNode;
import dev.vfyjxf.cloudlib.api.text.EntityNode;
import dev.vfyjxf.cloudlib.api.text.ImageNode;
import dev.vfyjxf.cloudlib.api.text.ItemNode;
import dev.vfyjxf.cloudlib.api.text.RichNode;
import dev.vfyjxf.cloudlib.api.text.WidgetNode;
import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.TextFragment;
import dev.vfyjxf.cloudlib.api.text.layout.TextLine;
import dev.vfyjxf.cloudlib.api.text.render.RenderOptions;
import dev.vfyjxf.cloudlib.api.text.render.RichTextRenderer;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The default {@link RichTextRenderer}: vanilla text via the batched font pipeline,
 * images via {@link SceneCanvas#texture}, items via the vanilla item renderer,
 * blocks as GUI-isometric models and entities through the inventory-entity
 * pipeline. Embedded widgets ({@link WidgetNode}) paint themselves as child
 * widgets and are skipped here.
 */
public class DefaultRichTextRenderer implements RichTextRenderer {

    private static final int FULL_BRIGHT = 15728880;

    @Override
    public void render(SceneCanvas canvas, LaidOutText laidOut, float x, float y, RenderOptions options) {
        if (laidOut.isEmpty()) return;
        renderHighlights(canvas, laidOut, x, y);
        renderTexts(canvas, laidOut, x, y, options);
        for (TextLine line : laidOut.lines()) {
            for (TextFragment fragment : line.fragments()) {
                if (fragment.kind() == TextFragment.Kind.OBJECT) {
                    renderObject(canvas, fragment, x, y, options);
                }
            }
        }
    }

    //region text

    private void renderHighlights(SceneCanvas canvas, LaidOutText laidOut, float x, float y) {
        for (TextLine line : laidOut.lines()) {
            for (TextFragment fragment : line.fragments()) {
                Integer highlight = fragment.style().highlightColor();
                if (highlight != null) {
                    canvas.fill(
                            Math.round(x + fragment.x()), Math.round(y + fragment.y()),
                            Math.round(fragment.width()), Math.round(fragment.height()),
                            highlight
                    );
                }
            }
        }
    }

    private void renderTexts(SceneCanvas canvas, LaidOutText laidOut, float x, float y, RenderOptions options) {
        Language language = Language.getInstance();
        canvas.textBatch(batch -> {
            for (TextLine line : laidOut.lines()) {
                for (TextFragment fragment : line.fragments()) {
                    if (fragment.kind() != TextFragment.Kind.TEXT || fragment.text() == null) {
                        continue;
                    }
                    Style style = fragment.style().style();
                    int color = textColor(style, options.defaultColor());
                    boolean shadow = fragment.style().shadowOr(options.shadow());
                    FormattedCharSequence sequence = language.getVisualOrder(FormattedText.of(fragment.text(), style));
                    batch.drawString(
                            sequence,
                            Math.round(x + fragment.x()), Math.round(y + fragment.y()),
                            color, shadow
                    );
                }
            }
        });
    }

    private static int textColor(Style style, int defaultColor) {
        TextColor color = style.getColor();
        return color != null ? color.getValue() | 0xFF000000 : defaultColor;
    }

    //endregion

    //region objects

    private void renderObject(SceneCanvas canvas, TextFragment fragment, float x, float y, RenderOptions options) {
        Insets padding = fragment.style().paddingOr(Insets.zero);
        float cx = x + fragment.x() + padding.left();
        float cy = y + fragment.y() + padding.top();
        float cw = fragment.width() - padding.left() - padding.right();
        float ch = fragment.height() - padding.top() - padding.bottom();
        RichNode source = fragment.source();
        switch (source) {
            case ImageNode image -> {
                if (cw > 0 && ch > 0) {
                    canvas.texture(image.texture(), Math.round(cx), Math.round(cy), Math.round(cw), Math.round(ch));
                }
            }
            case ItemNode item -> renderItem(canvas, item, cx, cy, cw, ch);
            case BlockNode block -> renderBlock(canvas, block, cx, cy, cw, ch);
            case EntityNode entity -> renderEntity(canvas, entity, cx, cy, cw, ch, options);
            case CustomRenderNode custom -> {
                if (cw > 0 && ch > 0) {
                    custom.renderer().render(canvas, cx, cy, cw, ch);
                }
            }
            default -> {
                // Embedded widgets paint themselves; spacers reserve space only.
            }
        }
    }

    private void renderItem(SceneCanvas canvas, ItemNode node, float x, float y, float w, float h) {
        ItemStack stack = node.stack();
        if (stack.isEmpty()) return;
        int ix = Math.round(x);
        int iy = Math.round(y);
        if (w == 16 && h == 16) {
            canvas.renderItem(stack, ix, iy);
            if (node.showDecorations()) {
                canvas.renderItemDecorations(stack, ix, iy);
            }
            return;
        }
        canvas.pushTransform();
        try {
            canvas.translate(x, y);
            canvas.scale(w / 16f, h / 16f);
            canvas.renderItem(stack, 0, 0);
            if (node.showDecorations()) {
                canvas.renderItemDecorations(stack, 0, 0);
            }
        } finally {
            canvas.popTransform();
        }
    }

    /**
     * Renders a block as a GUI-isometric model, the same way JEI-style block
     * displays do.
     */
    private void renderBlock(SceneCanvas canvas, BlockNode node, float x, float y, float w, float h) {
        float size = Math.min(w, h);
        if (size <= 0) return;
        canvas.renderLayered(graphics -> {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            try {
                pose.translate(x + w / 2f, y + h / 2f, 100f);
                pose.scale(size, -size, size);
                pose.mulPose(Axis.XP.rotationDegrees(30f));
                pose.mulPose(Axis.YP.rotationDegrees(225f));
                pose.translate(-0.5f, -0.5f, -0.5f);
                com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                        node.state(), pose, graphics.bufferSource(), FULL_BRIGHT,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY
                );
                graphics.flush();
            } finally {
                pose.popPose();
                com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            }
        });
    }

    private void renderEntity(SceneCanvas canvas, EntityNode node, float x, float y, float w, float h, RenderOptions options) {
        Entity entity = node.entity().get();
        if (entity == null || w <= 0 || h <= 0) return;
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(w);
        int ih = Math.round(h);
        // Entities easily exceed their reserved box; clip hard to the fragment.
        canvas.withClip(ix, iy, iw, ih, () -> canvas.renderLayered(graphics -> {
            if (entity instanceof LivingEntity living) {
                renderLiving(graphics, living, node, ix, iy, iw, ih, options);
            } else {
                renderSimple(graphics, entity, node, ix, iy, iw, ih, options);
            }
        }));
    }

    private static void renderLiving(
            GuiGraphics graphics, LivingEntity entity, EntityNode node,
            int x, int y, int w, int h, RenderOptions options
    ) {
        if (node.followMouse()) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, x, y, x + w, y + h, node.scale(), 0.0625f,
                    options.mouseX(), options.mouseY(), entity
            );
        } else {
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf camera = new Quaternionf().rotateX(20f * (float) (Math.PI / 180.0));
            pose.mul(camera);
            InventoryScreen.renderEntityInInventory(
                    graphics,
                    x + w / 2f, y + h / 2f, node.scale(),
                    new Vector3f(0, entity.getBbHeight() / 2f, 0),
                    pose, camera, entity
            );
        }
    }

    private static void renderSimple(
            GuiGraphics graphics, Entity entity, EntityNode node,
            int x, int y, int w, int h, RenderOptions options
    ) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x + w / 2f, y + h / 2f + node.scale() * entity.getBbHeight() / 2f, 100f);
            pose.scale(node.scale(), node.scale(), -node.scale());
            com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(() -> dispatcher.render(
                    entity, 0, 0, 0, 0f, options.partialTicks(),
                    pose, graphics.bufferSource(), FULL_BRIGHT
            ));
            graphics.flush();
            dispatcher.setRenderShadow(true);
        } finally {
            pose.popPose();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        }
    }

    //endregion
}
