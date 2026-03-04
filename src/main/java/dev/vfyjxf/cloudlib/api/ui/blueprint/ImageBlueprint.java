package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.widget.ImageWidget;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint for {@link ImageWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Image(texture)
 * Image(ResourceLocation.of("modid:texture"), 16, 16)
 * }</pre>
 */
public final class ImageBlueprint implements Blueprint<ImageWidget> {

    private @Nullable VisualTexture texture;
    private boolean preserveAspectRatio = false;
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private ImageBlueprint(@Nullable VisualTexture texture) {
        this.texture = texture;
    }

    //region dsl entry points

    public static ImageBlueprint Image(VisualTexture texture) {
        return ScopedReceiver.add(new ImageBlueprint(texture));
    }

    public static ImageBlueprint Image(ResourceLocation location, int width, int height) {
        return ScopedReceiver.add(new ImageBlueprint(
                new dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture(location, width, height)
        ));
    }

    public static ImageBlueprint EmptyImage() {
        return ScopedReceiver.add(new ImageBlueprint(null));
    }

    //endregion

    //region builder methods

    public ImageBlueprint texture(VisualTexture texture) {
        this.texture = texture;
        return this;
    }

    public ImageBlueprint preserveAspectRatio(boolean preserve) {
        this.preserveAspectRatio = preserve;
        return this;
    }

    public ImageBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public ImageBlueprint style(UIStyle style) {
        this.style = style;
        return this;
    }

    //endregion

    //region blueprint implementation

    @Override
    public @Nullable Object key() {
        return key;
    }

    @Override
    public ImageWidget createWidget(Scene scene, SceneContext context) {
        return ImageWidget.empty();
    }

    @Override
    public void updateWidget(ImageWidget widget, Scene scene, SceneContext context) {
        widget.setTexture(texture)
              .setPreserveAspectRatio(preserveAspectRatio)
              .useStyle(style);
    }

    //endregion
}
