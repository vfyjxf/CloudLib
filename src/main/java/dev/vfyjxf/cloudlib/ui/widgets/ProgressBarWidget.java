package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * A progress bar widget for displaying progress or loading states.
 * <p>
 * Supports horizontal and vertical orientations with customizable
 * background and fill textures.
 */
public class ProgressBarWidget extends Widget {

    public enum Direction {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP
    }

    private DoubleSupplier progressSupplier = () -> 0.0;
    private Direction direction = Direction.LEFT_TO_RIGHT;
    private @Nullable VisualTexture backgroundTexture = new ColorTexture(0xFF333333);
    private VisualTexture fillTexture = new ColorTexture(0xFF00AA00);

    public static ProgressBarWidget create() {
        return new ProgressBarWidget();
    }

    public static ProgressBarWidget create(DoubleSupplier progressSupplier) {
        return new ProgressBarWidget().setProgressSupplier(progressSupplier);
    }

    private ProgressBarWidget() {}

    public double progress() {
        return Math.clamp(progressSupplier.getAsDouble(), 0.0, 1.0);
    }

    public ProgressBarWidget setProgressSupplier(DoubleSupplier progressSupplier) {
        this.progressSupplier = progressSupplier;
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

    public ProgressBarWidget setBackgroundTexture(@Nullable VisualTexture backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
        return this;
    }

    public VisualTexture fillTexture() {
        return fillTexture;
    }

    public ProgressBarWidget setFillTexture(VisualTexture fillTexture) {
        this.fillTexture = fillTexture;
        return this;
    }

    public ProgressBarWidget setColors(int backgroundColor, int fillColor) {
        this.backgroundTexture = new ColorTexture(backgroundColor);
        this.fillTexture = new ColorTexture(fillColor);
        return this;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);

        // Render background
        if (backgroundTexture != null) {
            backgroundTexture.render(graphics, 0, 0, width(), height());
        }

        // Calculate fill area based on progress and direction
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

        fillTexture.render(graphics, fillX, fillY, fillW, fillH);
    }
}
