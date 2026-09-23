package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A texture drawn through a scale + translate — the {@code scale(n|w,h)} and
 * {@code translate(x,y)} css modifiers, which merge into one transform.
 * <p>
 * The scale pivots on the drawn rect's center (LDLib2's {@code Transform2D}
 * default) and the translate shifts the result in screen pixels. Batching is
 * preserved whenever the wrapped texture supports it: the transform is applied to
 * the emitted quad, which is exact for the axis-aligned scale a css modifier can
 * express.
 */
public record TransformedTexture(VisualTexture texture, float scaleX, float scaleY, float offsetX, float offsetY)
        implements
            BatchableTexture {

    // region factory

    /** Wraps {@code texture} with a uniform scale. */
    public static TransformedTexture scaled(VisualTexture texture, float scale) {
        return new TransformedTexture(texture, scale, scale, 0f, 0f);
    }

    /** Wraps {@code texture} with a non-uniform scale. */
    public static TransformedTexture scaled(VisualTexture texture, float scaleX, float scaleY) {
        return new TransformedTexture(texture, scaleX, scaleY, 0f, 0f);
    }

    /** Wraps {@code texture} with a translation. */
    public static TransformedTexture translated(VisualTexture texture, float offsetX, float offsetY) {
        return new TransformedTexture(texture, 1f, 1f, offsetX, offsetY);
    }

    /** Whether this transform draws its texture exactly as it is. */
    public boolean isIdentity() {
        return scaleX == 1f && scaleY == 1f && offsetX == 0f && offsetY == 0f;
    }

    // endregion

    // region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (isIdentity()) {
            texture.render(graphics, x, y, width, height);
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + width / 2f + offsetX, y + height / 2f + offsetY, 0f);
        pose.scale(scaleX, scaleY, 1f);
        pose.translate(-(x + width / 2f), -(y + height / 2f), 0f);
        texture.render(graphics, x, y, width, height);
        pose.popPose();
    }

    // endregion

    // region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int color) {
        if (!(texture instanceof BatchableTexture batchable)) {
            return;
        }
        float scaledWidth = width * scaleX;
        float scaledHeight = height * scaleY;
        float scaledX = x + (width - scaledWidth) / 2f + offsetX;
        float scaledY = y + (height - scaledHeight) / 2f + offsetY;
        batchable.emit(emitter, scaledX, scaledY, scaledWidth, scaledHeight, color);
    }

    @Override
    public boolean supportsBatching() {
        return texture instanceof BatchableTexture batchable && batchable.supportsBatching();
    }

    // endregion
}
