package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.widget.SliderWidget;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Blueprint for {@link SliderWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * Slider(0, 100, 50)
 * Slider(0.0, 1.0, value -> System.out.println("Value: " + value))
 * Slider(0, 10).step(1)
 * }</pre>
 */
public final class SliderBlueprint implements Blueprint<SliderWidget> {

    private double value = 0.0;
    private double min = 0.0;
    private double max = 1.0;
    private double step = 0.0;
    private SliderWidget.Orientation orientation = SliderWidget.Orientation.HORIZONTAL;
    private @Nullable Consumer<Double> onValueChanged;

    private VisualTexture trackTexture = new ColorTexture(0xFF444444);
    private VisualTexture filledTrackTexture = new ColorTexture(0xFF00AA00);
    private VisualTexture thumbTexture = new ColorTexture(0xFFAAAAAA);
    private int thumbSize = 8;

    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private SliderBlueprint(double min, double max, double value) {
        this.min = min;
        this.max = max;
        this.value = value;
    }

    //region dsl entry points

    public static SliderBlueprint Slider(double min, double max) {
        return ScopedReceiver.add(new SliderBlueprint(min, max, min));
    }

    public static SliderBlueprint Slider(double min, double max, double value) {
        return ScopedReceiver.add(new SliderBlueprint(min, max, value));
    }

    public static SliderBlueprint Slider(double min, double max, Consumer<Double> onValueChanged) {
        return ScopedReceiver.add(new SliderBlueprint(min, max, min).onValueChanged(onValueChanged));
    }

    //endregion

    //region builder methods

    public SliderBlueprint value(double value) {
        this.value = value;
        return this;
    }

    public SliderBlueprint range(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public SliderBlueprint step(double step) {
        this.step = step;
        return this;
    }

    public SliderBlueprint orientation(SliderWidget.Orientation orientation) {
        this.orientation = orientation;
        return this;
    }

    public SliderBlueprint horizontal() {
        this.orientation = SliderWidget.Orientation.HORIZONTAL;
        return this;
    }

    public SliderBlueprint vertical() {
        this.orientation = SliderWidget.Orientation.VERTICAL;
        return this;
    }

    public SliderBlueprint onValueChanged(@Nullable Consumer<Double> onValueChanged) {
        this.onValueChanged = onValueChanged;
        return this;
    }

    public SliderBlueprint colors(int track, int filled, int thumb) {
        this.trackTexture = new ColorTexture(track);
        this.filledTrackTexture = new ColorTexture(filled);
        this.thumbTexture = new ColorTexture(thumb);
        return this;
    }

    public SliderBlueprint thumbSize(int size) {
        this.thumbSize = size;
        return this;
    }

    public SliderBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public SliderBlueprint style(UIStyle style) {
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
    public SliderWidget createWidget(Scene scene, SceneContext context) {
        return SliderWidget.create();
    }

    @Override
    public void updateWidget(SliderWidget widget, Scene scene, SceneContext context) {
        widget.setRange(min, max)
              .setValue(value)
              .setStep(step)
              .setOrientation(orientation)
              .onValueChanged(onValueChanged)
              .setTrackTexture(trackTexture)
              .setFilledTrackTexture(filledTrackTexture)
              .setThumbTexture(thumbTexture)
              .setThumbSize(thumbSize)
              .useStyle(style);
    }

    //endregion
}
