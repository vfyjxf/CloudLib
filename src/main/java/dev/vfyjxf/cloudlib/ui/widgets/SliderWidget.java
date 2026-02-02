package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Draggable slider for value selection.
 */
public class SliderWidget extends Widget {

    //region types

    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    //endregion

    //region state

    private double value = 0.0;
    private double min = 0.0;
    private double max = 1.0;
    private double step = 0.0;
    private Orientation orientation = Orientation.HORIZONTAL;
    private boolean dragging = false;
    private @Nullable Consumer<Double> onValueChanged;

    //endregion

    //region textures

    private VisualTexture trackTexture = new ColorTexture(0xFF444444);
    private VisualTexture filledTrackTexture = new ColorTexture(0xFF00AA00);
    private VisualTexture thumbTexture = new ColorTexture(0xFFAAAAAA);
    private VisualTexture thumbHoverTexture = new ColorTexture(0xFFCCCCCC);
    private int thumbSize = 8;

    //endregion

    //region factory

    public static SliderWidget create() {
        return new SliderWidget();
    }

    public static SliderWidget create(double min, double max) {
        return new SliderWidget().setRange(min, max);
    }

    public static SliderWidget create(double min, double max, double initial) {
        return new SliderWidget().setRange(min, max).setValue(initial);
    }

    private SliderWidget() {
        onMouseClicked((input, context) -> {
            updateValueFromMouse(input.mouseX(), input.mouseY());
            dragging = true;
            return EventDispatch.consumed;
        });

        onMouseReleased((input, context) -> {
            dragging = false;
            return EventDispatch.consumed;
        });

        onMouseDragged((input, deltaX, deltaY, context) -> {
            if (dragging) {
                updateValueFromMouse(input.mouseX(), input.mouseY());
            }
            return EventDispatch.pass;
        });
    }

    //endregion

    //region input

    private void updateValueFromMouse(double mouseX, double mouseY) {
        double ratio;
        if (orientation == Orientation.HORIZONTAL) {
            int trackStart = thumbSize / 2;
            int trackEnd = width() - thumbSize / 2;
            ratio = (mouseX - trackStart) / (trackEnd - trackStart);
        } else {
            int trackStart = thumbSize / 2;
            int trackEnd = height() - thumbSize / 2;
            ratio = 1.0 - (mouseY - trackStart) / (trackEnd - trackStart);
        }

        ratio = Math.clamp(ratio, 0.0, 1.0);
        double newValue = min + ratio * (max - min);

        if (step > 0) {
            newValue = Math.round(newValue / step) * step;
        }

        setValue(newValue);
    }

    //endregion

    //region configuration

    public double value() {
        return value;
    }

    public SliderWidget setValue(double value) {
        double clamped = Math.clamp(value, min, max);
        if (this.value != clamped) {
            this.value = clamped;
            if (onValueChanged != null) {
                onValueChanged.accept(this.value);
            }
        }
        return this;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public SliderWidget setRange(double min, double max) {
        this.min = min;
        this.max = max;
        this.value = Math.clamp(this.value, min, max);
        return this;
    }

    public double step() {
        return step;
    }

    public SliderWidget setStep(double step) {
        this.step = step;
        return this;
    }

    public Orientation orientation() {
        return orientation;
    }

    public SliderWidget setOrientation(Orientation orientation) {
        this.orientation = orientation;
        return this;
    }

    public SliderWidget onValueChanged(@Nullable Consumer<Double> callback) {
        this.onValueChanged = callback;
        return this;
    }

    public SliderWidget setTrackTexture(VisualTexture texture) {
        this.trackTexture = texture;
        return this;
    }

    public SliderWidget setFilledTrackTexture(VisualTexture texture) {
        this.filledTrackTexture = texture;
        return this;
    }

    public SliderWidget setThumbTexture(VisualTexture texture) {
        this.thumbTexture = texture;
        return this;
    }

    public SliderWidget setThumbHoverTexture(VisualTexture texture) {
        this.thumbHoverTexture = texture;
        return this;
    }

    public SliderWidget setThumbSize(int size) {
        this.thumbSize = size;
        return this;
    }

    public SliderWidget setColors(int track, int filled, int thumb) {
        this.trackTexture = new ColorTexture(track);
        this.filledTrackTexture = new ColorTexture(filled);
        this.thumbTexture = new ColorTexture(thumb);
        return this;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
        double ratio = (value - min) / (max - min);

        if (orientation == Orientation.HORIZONTAL) {
            renderHorizontal(canvas, ratio);
        } else {
            renderVertical(canvas, ratio);
        }
    }

    private void renderHorizontal(SceneCanvas canvas, double ratio) {
        int trackHeight = 4;
        int trackY = (height() - trackHeight) / 2;

        canvas.texture(trackTexture, thumbSize / 2, trackY, width() - thumbSize, trackHeight);

        int filledWidth = (int) ((width() - thumbSize) * ratio);
        canvas.texture(filledTrackTexture, thumbSize / 2, trackY, filledWidth, trackHeight);

        int thumbX = (int) ((width() - thumbSize) * ratio);
        int thumbY = (height() - thumbSize) / 2;
        VisualTexture currentThumb = (hovered() || dragging) ? thumbHoverTexture : thumbTexture;
        canvas.texture(currentThumb, thumbX, thumbY, thumbSize, thumbSize);
    }

    private void renderVertical(SceneCanvas canvas, double ratio) {
        int trackWidth = 4;
        int trackX = (width() - trackWidth) / 2;

        canvas.texture(trackTexture, trackX, thumbSize / 2, trackWidth, height() - thumbSize);

        int filledHeight = (int) ((height() - thumbSize) * ratio);
        int filledY = height() - thumbSize / 2 - filledHeight;
        canvas.texture(filledTrackTexture, trackX, filledY, trackWidth, filledHeight);

        int thumbX = (width() - thumbSize) / 2;
        int thumbY = (int) ((height() - thumbSize) * (1.0 - ratio));
        VisualTexture currentThumb = (hovered() || dragging) ? thumbHoverTexture : thumbTexture;
        canvas.texture(currentThumb, thumbX, thumbY, thumbSize, thumbSize);
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("value", value, 0.0, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("min", min, 0.0, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("max", max, 1.0, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("step", step, 0.0, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("orientation", orientation.name(), Orientation.HORIZONTAL.name(), InspectionProperty.CATEGORY_VISUAL);
        collector.addWithDefault("dragging", dragging, false, InspectionProperty.CATEGORY_STATE);
    }

    //endregion
}
