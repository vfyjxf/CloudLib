package dev.vfyjxf.cloudlib.text;

import com.mojang.blaze3d.platform.Lighting;
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
import dev.vfyjxf.cloudlib.api.ui.canvas.EntityPreviewRenderer;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * The default {@link RichTextRenderer}: vanilla text via the batched font pipeline,
 * images via {@link SceneCanvas#texture}, items via the vanilla item renderer,
 * blocks as GUI-isometric models and entities through the inventory-entity
 * pipeline. Embedded widgets ({@link WidgetNode}) paint themselves as child
 * widgets and are skipped here.
 */
public class DefaultRichTextRenderer implements RichTextRenderer {

    private static final int fullBright = 15728880;

    @Override
    public void render(SceneCanvas canvas, LaidOutText laidOut, float x, float y, RenderOptions options) {
        if (laidOut.isEmpty()) return;
        renderHighlights(canvas, laidOut, x, y);
        renderTexts(canvas, laidOut, x, y, options);
        for (TextLine line : laidOut.lines()) {
            for (TextFragment fragment : line.fragments()) {
                if (fragment.kind() == TextFragment.Kind.object) {
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
                        Math.round(x + fragment.x()),
                        Math.round(y + fragment.y()),
                        Math.round(fragment.width()),
                        Math.round(fragment.height()),
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
                    if (fragment.kind() != TextFragment.Kind.text || fragment.text() == null) {
                        continue;
                    }
                    Style style = fragment.style().style();
                    int color = fragment.style().textColor(options.themeColors(), options.defaultColor());
                    boolean shadow = fragment.style().shadowOr(options.shadow());
                    FormattedCharSequence sequence = language.getVisualOrder(FormattedText.of(fragment.text(), style));
                    batch.drawString(
                        sequence,
                        Math.round(x + fragment.x()),
                        Math.round(y + fragment.y()),
                        color,
                        shadow
                    );
                }
            }
        });
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
                Lighting.setupFor3DItems();
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    node.state(),
                    pose,
                    graphics.bufferSource(),
                    fullBright,
                    OverlayTexture.NO_OVERLAY
                );
                graphics.flush();
            } finally {
                pose.popPose();
                Lighting.setupFor3DItems();
            }
        });
    }

    /**
     * The entity pipeline itself — scissor, follow-mouse angles, lighting — lives in
     * {@link EntityPreviewRenderer}; this adapter only resolves the node.
     */
    private void renderEntity(
        SceneCanvas canvas,
        EntityNode node,
        float x,
        float y,
        float w,
        float h,
        RenderOptions options
    ) {
        Entity entity = node.entity().get();
        if (entity == null || w <= 0 || h <= 0) return;
        EntityPreviewRenderer.render(
            canvas,
            entity,
            Math.round(x),
            Math.round(y),
            Math.round(w),
            Math.round(h),
            node.scale(),
            node.followMouse(),
            options.mouseX(),
            options.mouseY(),
            options.partialTicks()
        );
    }

    //endregion
}
