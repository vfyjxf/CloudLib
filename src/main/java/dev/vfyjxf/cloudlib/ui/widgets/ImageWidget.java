package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A widget for displaying images or textures.
 * <p>
 * Supports various texture types including:
 * <ul>
 *   <li>{@link ImageTexture} - Standard image textures</li>
 *   <li>{@link VisualTexture} - Any renderable texture</li>
 * </ul>
 */
public class ImageWidget extends Widget {

    private @Nullable VisualTexture texture;
    private boolean preserveAspectRatio = false;

    public static ImageWidget of(VisualTexture texture) {
        return new ImageWidget(texture);
    }

    public static ImageWidget of(ResourceLocation location, int width, int height) {
        return new ImageWidget(new ImageTexture(location, width, height));
    }

    public static ImageWidget empty() {
        return new ImageWidget(null);
    }

    private ImageWidget(@Nullable VisualTexture texture) {
        this.texture = texture;
    }

    public @Nullable VisualTexture texture() {
        return texture;
    }

    public ImageWidget setTexture(@Nullable VisualTexture texture) {
        this.texture = texture;
        return this;
    }

    public boolean preserveAspectRatio() {
        return preserveAspectRatio;
    }

    public ImageWidget setPreserveAspectRatio(boolean preserveAspectRatio) {
        this.preserveAspectRatio = preserveAspectRatio;
        return this;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);
        if (texture != null) {
            texture.render(graphics, 0, 0, width(), height());
        }
    }
}
