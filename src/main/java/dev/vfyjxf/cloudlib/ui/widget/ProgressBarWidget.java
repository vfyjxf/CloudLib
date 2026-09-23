package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
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
 * <p>
 * The trough and the filled portion are real sub-parts —
 * {@code progress-bar::part(track)} and {@code ::part(fill)} — each painted with
 * its own background texture; {@link #setBackgroundTexture} and
 * {@link #setFillTexture} stay the code-side texture a part falls back to when
 * the theme paints no background for it. The parts are absolutely positioned by
 * this widget, so the public API and the layout are unchanged.
 */
public class ProgressBarWidget extends CompositeWidget<Widget> {

    // region types

    public enum Direction {
        leftToRight, rightToLeft, topToBottom, bottomToTop
    }

    // endregion

    // region parts

    /** {@code ::part(track)} — the trough the fill grows into. */
    static final String partTrack = "track";
    /** {@code ::part(fill)} — the portion covered by {@link #progress()}. */
    static final String partFill = "fill";

    // endregion

    // region state

    private DoubleSupplier progressSupplier = () -> 0.0;
    private Direction direction = Direction.leftToRight;

    // endregion

    // region textures

    private @Nullable VisualTexture backgroundTexture = new ColorTexture(0xFF333333);
    private VisualTexture fillTexture = new ColorTexture(0xFF00AA00);

    // endregion

    // region parts state

    private final WidgetPart trackPart;
    private final WidgetPart fillPart;

    // endregion

    // region factory

    public static ProgressBarWidget create() {
        return new ProgressBarWidget();
    }

    public static ProgressBarWidget create(DoubleSupplier progressSupplier) {
        return new ProgressBarWidget().setProgressSupplier(progressSupplier);
    }

    private ProgressBarWidget() {
        trackPart = addWidget(
            new WidgetPart(this, partTrack, () -> trackBounds(width(), height()), () -> backgroundTexture)
        );
        fillPart = addWidget(
            new WidgetPart(
                this,
                partFill,
                () -> fillBounds(width(), height(), progress(), direction),
                () -> fillTexture
            )
        );
    }

    // endregion

    // region configuration

    public double progress() {
        return Math.clamp(progressSupplier.getAsDouble(), 0.0, 1.0);
    }

    public ProgressBarWidget setProgressSupplier(DoubleSupplier supplier) {
        this.progressSupplier = supplier;
        invalidateParts();
        return this;
    }

    public ProgressBarWidget setProgress(double progress) {
        this.progressSupplier = () -> progress;
        invalidateParts();
        return this;
    }

    public Direction direction() {
        return direction;
    }

    public ProgressBarWidget setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            invalidateParts();
        }
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

    // endregion

    // region geometry (pure — the headless tests drive these)

    /** The trough — the whole widget. */
    public static Rect trackBounds(int width, int height) {
        return new Rect(0, 0, width, height);
    }

    /** The filled portion for a {@code progress} in [0,1], grown from the {@code direction}'s origin. */
    public static Rect fillBounds(int width, int height, double progress, Direction direction) {
        double clamped = Math.clamp(progress, 0.0, 1.0);
        int x = 0;
        int y = 0;
        int w = width;
        int h = height;
        switch (direction) {
            case leftToRight -> w = (int) (width * clamped);
            case rightToLeft -> {
                w = (int) (width * clamped);
                x = width - w;
            }
            case topToBottom -> h = (int) (height * clamped);
            case bottomToTop -> {
                h = (int) (height * clamped);
                y = height - h;
            }
        }
        return new Rect(x, y, w, h);
    }

    // endregion

    // region hooks

    @Override
    public void render(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        // a supplier can move the fill every frame without any layout change —
        // re-read the rect before the canvas translates to the parts
        WidgetPart.syncAll(this);
        super.render(canvas, mouseX, mouseY, partialTicks);
    }

    /**
     * The parts mirror this widget's selector surface — a hover or a state flip
     * has to re-resolve them too.
     */
    @Override
    public void markStyleDirty() {
        super.markStyleDirty();
        WidgetPart.markStyleDirtyAll(this);
    }

    /** Re-runs the parts' layout handlers after a change taffy cannot see. */
    private void invalidateParts() {
        if (!lifecycle().mounted()) {
            return;
        }
        for (Widget part : children()) {
            scene().layoutTree().markDirty(part.nodeId());
        }
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addFormatted(
            "progress",
            String.format("%.1f%%", progress() * 100),
            null,
            InspectionProperty.categoryData
        );
        collector.addWithDefault("direction", direction, Direction.leftToRight, InspectionProperty.categoryVisual);
        collector.add("track", trackPart.bounds(), InspectionProperty.categoryLayout);
        collector.add("fill", fillPart.bounds(), InspectionProperty.categoryLayout);
    }

    // endregion
}
