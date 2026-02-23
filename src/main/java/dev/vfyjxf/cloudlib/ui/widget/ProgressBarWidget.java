package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * Progress bar with direction support.
 */
public class ProgressBarWidget extends Widget {

    //region types

    public enum Direction {
        LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM, BOTTOM_TO_TOP
    }

    //endregion

    //region state

    private DoubleSupplier progressSupplier = () -> 0.0;
    private Direction direction = Direction.LEFT_TO_RIGHT;

    //endregion

    //region textures

    private @Nullable VisualTexture backgroundTexture = new ColorTexture(0xFF333333);
    private VisualTexture fillTexture = new ColorTexture(0xFF00AA00);

    //endregion

    //region factory

    public static ProgressBarWidget create() {
        return new ProgressBarWidget();
    }

    public static ProgressBarWidget create(DoubleSupplier progressSupplier) {
        return new ProgressBarWidget().setProgressSupplier(progressSupplier);
    }

    private ProgressBarWidget() {
    }

    //endregion

    //region configuration

    public double progress() {
        return Math.clamp(progressSupplier.getAsDouble(), 0.0, 1.0);
    }

    public ProgressBarWidget setProgressSupplier(DoubleSupplier supplier) {
        this.progressSupplier = supplier;
        return this;
    }

    public ProgressBarWidget setProgress(double progress) {
        this.progressSupplier = () -> progress;
        return this;
    }

    public Direction direction() {
        return direction;
    }

    public ProgressBarWidget setDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    public @Nullable VisualTexture backgroundTexture() {
        return backgroundTexture;
    }

    public ProgressBarWidget setBackgroundTexture(@Nullable VisualTexture texture) {
        this.backgroundTexture = texture;
        return this;
    }

    public VisualTexture fillTexture() {
        return fillTexture;
    }

    public ProgressBarWidget setFillTexture(VisualTexture texture) {
        this.fillTexture = texture;
        return this;
    }

    public ProgressBarWidget setColors(int background, int fill) {
        this.backgroundTexture = new ColorTexture(background);
        this.fillTexture = new ColorTexture(fill);
        return this;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);

        if (backgroundTexture != null) {
            canvas.texture(backgroundTexture, 0, 0, width(), height());
        }

        double progress = progress();
        if (progress <= 0) return;

        int fillX = 0, fillY = 0, fillW = width(), fillH = height();

        switch (direction) {
            case LEFT_TO_RIGHT -> fillW = (int) (width() * progress);
            case RIGHT_TO_LEFT -> {
                fillW = (int) (width() * progress);
                fillX = width() - fillW;
            }
            case TOP_TO_BOTTOM -> fillH = (int) (height() * progress);
            case BOTTOM_TO_TOP -> {
                fillH = (int) (height() * progress);
                fillY = height() - fillH;
            }
        }

        canvas.texture(fillTexture, fillX, fillY, fillW, fillH);
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addFormatted("progress", String.format("%.1f%%", progress() * 100), null, InspectionProperty.categoryData);
        collector.addWithDefault("direction", direction, Direction.LEFT_TO_RIGHT, InspectionProperty.categoryVisual);
    }

    //endregion
}
