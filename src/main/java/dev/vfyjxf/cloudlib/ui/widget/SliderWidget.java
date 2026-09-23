package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Draggable slider for value selection.
 * <p>
 * The groove, the value's portion of it and the knob are real sub-parts —
 * {@code slider::part(track)}, {@code ::part(fill)} and
 * {@code slider::part(handle)} — each painted with its own background texture.
 * The {@code setXTexture} setters stay what they always were: the code-side
 * texture a part falls back to when the theme paints no background for it. The
 * parts are absolutely positioned by this widget, so size, layout and the whole
 * public API are unchanged.
 * <p>
 * Two theme metrics drive the geometry: {@code slider-track-size} (groove
 * thickness) and {@code slider-handle-size} (knob side) — both beat the code
 * defaults when a stylesheet declares them. A press reports {@code :pressed},
 * so {@code slider::part(handle):pressed} lights the knob up while dragging.
 */
public class SliderWidget extends CompositeWidget<Widget> {

    // region types

    public enum Orientation {
        horizontal, vertical
    }

    // endregion

    // region parts

    /** {@code ::part(track)} — the groove the handle slides in. */
    static final String partTrack = "track";
    /** {@code ::part(fill)} — the value's portion of the groove. */
    static final String partFill = "fill";
    /** {@code ::part(handle)} — the draggable knob. */
    static final String partHandle = "handle";

    /** {@code slider-track-size} — the groove thickness a theme may declare. */
    static final String propTrackSize = "slider-track-size";
    /** {@code slider-handle-size} — the knob side a theme may declare. */
    static final String propHandleSize = "slider-handle-size";

    /** Groove thickness across the sliding axis, when no theme declares one. */
    static final int defaultTrackThickness = 4;

    // endregion

    // region state

    private double value = 0.0;
    private double min = 0.0;
    private double max = 1.0;
    private double step = 0.0;
    private Orientation orientation = Orientation.horizontal;
    private boolean dragging = false;
    private @Nullable Consumer<Double> onValueChanged;

    // endregion

    // region textures

    private VisualTexture trackTexture = new ColorTexture(0xFF444444);
    private VisualTexture filledTrackTexture = new ColorTexture(0xFF00AA00);
    private VisualTexture thumbTexture = new ColorTexture(0xFFAAAAAA);
    private VisualTexture thumbHoverTexture = new ColorTexture(0xFFCCCCCC);
    private int thumbSize = 8;

    // endregion

    // region parts state

    private final WidgetPart trackPart;
    private final WidgetPart fillPart;
    private final WidgetPart handlePart;

    // endregion

    // region factory

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
        trackPart = addWidget(new WidgetPart(this, partTrack, this::trackPartBounds, () -> trackTexture));
        fillPart = addWidget(new WidgetPart(this, partFill, this::fillPartBounds, () -> filledTrackTexture));
        handlePart = addWidget(new WidgetPart(this, partHandle, this::handlePartBounds, this::handleTexture));

        onMouseClicked((input, context) -> {
            updateValueFromMouse(input.mouseX(), input.mouseY());
            setDraggingState(true);
            return EventDispatch.consumed;
        });

        onMouseReleased((input, context) -> {
            setDraggingState(false);
            return EventDispatch.consumed;
        });

        onMouseDragged((input, deltaX, deltaY, context) -> {
            if (dragging) {
                updateValueFromMouse(input.mouseX(), input.mouseY());
            }
            return EventDispatch.pass;
        });
    }

    // endregion

    // region input

    private void updateValueFromMouse(double mouseX, double mouseY) {
        // input coordinates are scene-space — convert through the transform
        // chain so the slider works inside nested/translated containers too
        var local = sceneToLocal(mouseX, mouseY);
        double ratio;
        int handle = handleSize();
        if (orientation == Orientation.horizontal) {
            int trackStart = handle / 2;
            int trackEnd = width() - handle / 2;
            ratio = (local.x() - trackStart) / (trackEnd - trackStart);
        } else {
            int trackStart = handle / 2;
            int trackEnd = height() - handle / 2;
            ratio = 1.0 - (local.y() - trackStart) / (trackEnd - trackStart);
        }

        ratio = Math.clamp(ratio, 0.0, 1.0);
        double newValue = min + ratio * (max - min);

        if (step > 0) {
            newValue = Math.round(newValue / step) * step;
        }

        setValue(newValue);
    }

    // endregion

    // region configuration

    public double value() {
        return value;
    }

    public SliderWidget setValue(double value) {
        double clamped = Math.clamp(value, min, max);
        if (this.value != clamped) {
            this.value = clamped;
            invalidateParts();
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
        invalidateParts();
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
        if (this.orientation != orientation) {
            this.orientation = orientation;
            invalidateParts();
        }
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
        if (this.thumbSize != size) {
            this.thumbSize = size;
            invalidateParts();
        }
        return this;
    }

    public SliderWidget setColors(int track, int filled, int thumb) {
        this.trackTexture = new ColorTexture(track);
        this.filledTrackTexture = new ColorTexture(filled);
        this.thumbTexture = new ColorTexture(thumb);
        return this;
    }

    // endregion

    // region geometry (pure — the headless tests drive these)

    /** How far the value sits between {@link #min()} and {@link #max()}, clamped to [0,1]. */
    public double ratio() {
        double span = max - min;
        return span == 0 ? 0.0 : Math.clamp((value - min) / span, 0.0, 1.0);
    }

    /** The groove: the whole sliding length minus half a handle at each end. */
    public static Rect trackBounds(int width, int height, int thumbSize, Orientation orientation) {
        return trackBounds(width, height, thumbSize, defaultTrackThickness, orientation);
    }

    /** The groove at an explicit thickness — what a themed {@code slider-track-size} resolves to. */
    public static Rect trackBounds(int width, int height, int thumbSize, int thickness, Orientation orientation) {
        if (orientation == Orientation.horizontal) {
            return new Rect(thumbSize / 2, (height - thickness) / 2, width - thumbSize, thickness);
        }
        return new Rect((width - thickness) / 2, thumbSize / 2, thickness, height - thumbSize);
    }

    /** The value's portion of the groove — horizontal grows right, vertical grows up. */
    public static Rect fillBounds(int width, int height, int thumbSize, double ratio, Orientation orientation) {
        return fillBounds(width, height, thumbSize, defaultTrackThickness, ratio, orientation);
    }

    /** The filled portion at an explicit groove thickness. */
    public static Rect fillBounds(
        int width,
        int height,
        int thumbSize,
        int thickness,
        double ratio,
        Orientation orientation
    ) {
        Rect track = trackBounds(width, height, thumbSize, thickness, orientation);
        double clamped = Math.clamp(ratio, 0.0, 1.0);
        if (orientation == Orientation.horizontal) {
            return new Rect(track.x(), track.y(), (int) (track.width() * clamped), track.height());
        }
        int filled = (int) (track.height() * clamped);
        return new Rect(track.x(), height - thumbSize / 2 - filled, track.width(), filled);
    }

    /** The knob — a {@code thumbSize} square riding the end of the filled portion. */
    public static Rect handleBounds(int width, int height, int thumbSize, double ratio, Orientation orientation) {
        double clamped = Math.clamp(ratio, 0.0, 1.0);
        if (orientation == Orientation.horizontal) {
            return new Rect((int) ((width - thumbSize) * clamped), (height - thumbSize) / 2, thumbSize, thumbSize);
        }
        return new Rect((width - thumbSize) / 2, (int) ((height - thumbSize) * (1.0 - clamped)), thumbSize, thumbSize);
    }

    private Rect trackPartBounds() {
        return trackBounds(width(), height(), handleSize(), trackThickness(), orientation);
    }

    private Rect fillPartBounds() {
        return fillBounds(width(), height(), handleSize(), trackThickness(), ratio(), orientation);
    }

    private Rect handlePartBounds() {
        return handleBounds(width(), height(), handleSize(), ratio(), orientation);
    }

    /**
     * The groove thickness — a theme's {@code slider-track-size} beats the code
     * default, because the metric is exactly what a stylesheet wants to own.
     */
    private int trackThickness() {
        return metric(propTrackSize, defaultTrackThickness);
    }

    /**
     * The knob's side — a theme's {@code slider-handle-size} beats
     * {@link #setThumbSize}, for the same reason.
     */
    private int handleSize() {
        return metric(propHandleSize, thumbSize);
    }

    /** A metric the current theme declared, rounded to whole px, or the code fallback. */
    private int metric(String name, int fallback) {
        Float themed = style().visualContext().getProperty(name, Float.class);
        return themed != null ? Math.max(1, Math.round(themed)) : fallback;
    }

    /** The knob's code texture — a hover or a drag lightens it. */
    private VisualTexture handleTexture() {
        return hovered() || dragging ? thumbHoverTexture : thumbTexture;
    }

    /**
     * Moves the slider in and out of its dragged state. A press is a press on
     * the handle: {@code :pressed} is what {@code slider::part(handle):pressed}
     * keys off, and the code handle texture lightens on it too.
     */
    private void setDraggingState(boolean dragging) {
        if (this.dragging == dragging) {
            return;
        }
        this.dragging = dragging;
        if (dragging) {
            addStyleState("pressed");
        } else {
            removeStyleState("pressed");
        }
    }

    // endregion

    // region hooks

    @Override
    public void render(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        // parts are positioned by this widget, not by taffy: give them their
        // current rect before the canvas translates to them
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
        collector.addWithDefault("value", value, 0.0, InspectionProperty.categoryData);
        collector.addWithDefault("min", min, 0.0, InspectionProperty.categoryData);
        collector.addWithDefault("max", max, 1.0, InspectionProperty.categoryData);
        collector.addWithDefault("step", step, 0.0, InspectionProperty.categoryData);
        collector.addWithDefault("orientation", orientation, Orientation.horizontal, InspectionProperty.categoryVisual);
        collector.addWithDefault("dragging", dragging, false, InspectionProperty.categoryState);
        collector.add("track", trackPart.bounds(), InspectionProperty.categoryLayout);
        collector.add("fill", fillPart.bounds(), InspectionProperty.categoryLayout);
        collector.add("handle", handlePart.bounds(), InspectionProperty.categoryLayout);
    }

    // endregion
}
