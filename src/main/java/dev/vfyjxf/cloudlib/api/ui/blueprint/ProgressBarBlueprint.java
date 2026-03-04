package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.widget.ProgressBarWidget;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * Blueprint for {@link ProgressBarWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * ProgressBar(() -> 0.5)
 * ProgressBar(() -> progress).direction(Direction.LEFT_TO_RIGHT)
 * ProgressBar(0.75).colors(0xFF333333, 0xFF00AA00)
 * }</pre>
 */
public final class ProgressBarBlueprint implements Blueprint<ProgressBarWidget> {

    private DoubleSupplier progressSupplier;
    private ProgressBarWidget.Direction direction = ProgressBarWidget.Direction.LEFT_TO_RIGHT;
    private VisualTexture backgroundTexture = new ColorTexture(0xFF333333);
    private VisualTexture fillTexture = new ColorTexture(0xFF00AA00);
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private ProgressBarBlueprint(DoubleSupplier progressSupplier) {
        this.progressSupplier = progressSupplier;
    }

    //region dsl entry points

    public static ProgressBarBlueprint ProgressBar(DoubleSupplier progressSupplier) {
        return ScopedReceiver.add(new ProgressBarBlueprint(progressSupplier));
    }

    public static ProgressBarBlueprint ProgressBar(double progress) {
        return ScopedReceiver.add(new ProgressBarBlueprint(() -> progress));
    }

    //endregion

    //region builder methods

    public ProgressBarBlueprint progress(DoubleSupplier supplier) {
        this.progressSupplier = supplier;
        return this;
    }

    public ProgressBarBlueprint direction(ProgressBarWidget.Direction direction) {
        this.direction = direction;
        return this;
    }

    public ProgressBarBlueprint colors(int background, int fill) {
        this.backgroundTexture = new ColorTexture(background);
        this.fillTexture = new ColorTexture(fill);
        return this;
    }

    public ProgressBarBlueprint textures(VisualTexture background, VisualTexture fill) {
        this.backgroundTexture = background;
        this.fillTexture = fill;
        return this;
    }

    public ProgressBarBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public ProgressBarBlueprint style(UIStyle style) {
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
    public ProgressBarWidget createWidget(Scene scene, SceneContext context) {
        return ProgressBarWidget.create();
    }

    @Override
    public void updateWidget(ProgressBarWidget widget, Scene scene, SceneContext context) {
        widget.setProgressSupplier(progressSupplier)
              .setDirection(direction)
              .setBackgroundTexture(backgroundTexture)
              .setFillTexture(fillTexture)
              .useStyle(style);
    }

    //endregion
}
