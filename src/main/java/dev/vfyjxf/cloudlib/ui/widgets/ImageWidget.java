package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Image/texture display widget.
 */
public class ImageWidget extends Widget {

    //region state

    private @Nullable VisualTexture texture;
    private boolean preserveAspectRatio = false;

    //endregion

    //region factory

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

    //endregion

    //region configuration

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

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
        if (texture != null) {
            canvas.texture(texture, 0, 0, width(), height());
        }
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.add("hasTexture", texture != null, InspectionProperty.CATEGORY_VISUAL);
        if (texture != null) {
            collector.add("textureType", texture.getClass().getSimpleName(), InspectionProperty.CATEGORY_VISUAL);
        }
        collector.addWithDefault("preserveAspect", preserveAspectRatio, false, InspectionProperty.CATEGORY_VISUAL);
    }

    //endregion
}
