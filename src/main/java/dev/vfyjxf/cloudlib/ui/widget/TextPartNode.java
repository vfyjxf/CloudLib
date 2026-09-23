package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.text.RichTexts;
import dev.vfyjxf.cloudlib.api.text.ThemeColorResolver;
import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextLayouter;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextMeasure;
import dev.vfyjxf.cloudlib.api.text.layout.TextAlignment;
import dev.vfyjxf.cloudlib.api.text.render.RenderOptions;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.text.VanillaGlyphMeasurer;
import dev.vfyjxf.cloudlib.text.VanillaTranslationResolver;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TextAlign;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;

/**
 * A flowed text part — rich text that is measured and drawn by its own node, so
 * a stylesheet can address it through {@code ::part(name)}.
 * <p>
 * {@code RichTextWidget}/{@code LabelWidget} are hosts in their own right: their
 * tag is {@code rich-text}/{@code label}, so a child of them cannot be
 * {@code section-header::part(title)}. This node is the part-flavoured text
 * leaf: it reports its owner's tag ({@link PartNode}), carries the part name the
 * owner gave it, and keeps the rich text pipeline — a {@link RichTextMeasure}
 * for the intrinsic size (it is a leaf, so taffy does consult the measure
 * function), {@link RichTexts#renderer} for the glyphs.
 * <p>
 * The ink is the resolved {@code color} of the part, falling back to the code
 * color the owner chose; the horizontal alignment follows {@code text-align}
 * exactly like {@link RichTextWidget}; a themed {@code font-size} scales the
 * glyphs (and the measured size) relative to the font's line height.
 */
class TextPartNode extends PartNode {

    private RichText text;
    private int fallbackColor;
    private @Nullable StyleKey<Integer> slotFallback;
    private boolean defaultShadow;
    private float appliedScale = 1f;

    private @Nullable RichTextMeasure measure;
    private @Nullable TextAlignment appliedAlignment;

    TextPartNode(Widget owner, @Nullable String part, RichText text, int fallbackColor) {
        super(owner, part);
        this.text = text;
        this.fallbackColor = fallbackColor;

        // the mount listener runs before the widget counts as mounted, so the
        // font comes from the listener's context, not from context()
        onMount((scene, context, handle) -> {
            measure = createMeasure(context.font());
            scene.layoutTree().setMeasureFunc(nodeId(), this::measureContent);
        });
        onUnmount(() -> measure = null);

        // the alignment/scale are read from the resolved style, so a theme flip
        // has to re-run the measure
        style().addChangeListener(Styles.textAlign, (oldValue, newValue) -> onTextMetricsChanged());
        style().addChangeListener(Styles.fontSize, (oldValue, newValue) -> onTextMetricsChanged());
    }

    // region configuration

    RichText text() {
        return text;
    }

    TextPartNode setText(RichText text) {
        this.text = text;
        if (measure != null) {
            measure = createMeasure(context().font());
            scene().layoutTree().setMeasureFunc(nodeId(), this::measureContent);
            markLayoutDirty();
        }
        return this;
    }

    /** The code ink used when the theme's {@code color} is unset. */
    int fallbackColor() {
        return fallbackColor;
    }

    TextPartNode setFallbackColor(int color) {
        this.fallbackColor = color;
        return this;
    }

    /**
     * The themed slot the ink falls back to before the code color — a key/value
     * row's label reads {@code Styles.textDim}, so a panel that declares the slot
     * dims every label inside it without naming the widget.
     */
    TextPartNode setSlotFallback(@Nullable StyleKey<Integer> key) {
        this.slotFallback = key;
        return this;
    }

    boolean defaultShadow() {
        return defaultShadow;
    }

    TextPartNode setDefaultShadow(boolean shadow) {
        this.defaultShadow = shadow;
        return this;
    }

    /** The laid-out document at the node's current width — the render-time accessor. */
    LaidOutText laidOut() {
        if (measure == null) {
            return LaidOutText.empty;
        }
        return measure.layoutAt(Math.max(0, width()));
    }

    // endregion

    // region measure

    /**
     * The measure is bound to the <em>scene's</em> font rather than to the client
     * singleton {@link RichTexts#measure} reads: a node then measures with exactly
     * the font its host draws with, and a scene whose host carries a metric-only
     * font measures without a running client.
     */
    private RichTextMeasure createMeasure(Font font) {
        RichTextMeasure created = new RichTextMeasure(
            text,
            new RichTextLayouter(new VanillaGlyphMeasurer(font), VanillaTranslationResolver.instance)
        );
        TextAlignment alignment = currentAlignment();
        created.withAlignment(alignment);
        appliedAlignment = alignment;
        return created;
    }

    /**
     * The themed {@code font-size} as a glyph scale: the declared size over the
     * font's own line height, so {@code font-size: 18px} on a 9px font doubles
     * the title. 1 when the theme declares none.
     */
    float fontScale() {
        Float size = style().get(Styles.fontSize);
        if (size == null || size <= 0) {
            return 1f;
        }
        int lineHeight = mountedFontLineHeight();
        return lineHeight > 0 ? size / lineHeight : 1f;
    }

    private int mountedFontLineHeight() {
        if (!lifecycle().mounted()) {
            return 0;
        }
        return context().font().lineHeight;
    }

    /**
     * The scaled content size. The measure runs in unscaled text units — the wrap
     * width is divided by the scale and the result multiplied back — so a scaled
     * title wraps at the width it is actually given.
     */
    private FloatSize measureContent(FloatSize knownDimensions, TaffySize<AvailableSpace> availableSpace) {
        if (measure == null) {
            return FloatSize.ZERO;
        }
        float scale = fontScale();
        appliedScale = scale;
        if (scale == 1f) {
            return measure.measure(knownDimensions, availableSpace);
        }
        FloatSize scaledKnown = new FloatSize(
            divideOrNaN(knownDimensions.width, scale),
            divideOrNaN(knownDimensions.height, scale)
        );
        TaffySize<AvailableSpace> scaledSpace = availableSpace.map(space -> space.mapDefiniteValue(v -> v / scale));
        FloatSize raw = measure.measure(scaledKnown, scaledSpace);
        return new FloatSize(scaleOrNaN(raw.width, scale), scaleOrNaN(raw.height, scale));
    }

    private static float divideOrNaN(float value, float scale) {
        return Float.isNaN(value) ? value : value / scale;
    }

    private static float scaleOrNaN(float value, float scale) {
        return Float.isNaN(value) ? value : value * scale;
    }

    private void onTextMetricsChanged() {
        if (measure == null) {
            return;
        }
        TextAlignment alignment = currentAlignment();
        if (alignment != appliedAlignment) {
            measure.withAlignment(alignment);
            appliedAlignment = alignment;
        }
        markLayoutDirty();
    }

    private void markLayoutDirty() {
        if (lifecycle().mounted()) {
            scene().layoutTree().markDirty(nodeId());
        }
    }

    private TextAlignment currentAlignment() {
        TextAlign styled = style().get(Styles.textAlign);
        if (styled == null) {
            return TextAlignment.left;
        }
        return switch (styled) {
            case RIGHT -> TextAlignment.right;
            case CENTER -> TextAlignment.center;
            default -> TextAlignment.left;
        };
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        LaidOutText laidOut = laidOut();
        if (laidOut.isEmpty()) {
            return;
        }
        float scale = fontScale();
        if (scale != 1f) {
            canvas.pushTransform();
            canvas.scale(scale);
        }
        RenderOptions options = RenderOptions.defaults.withDefaultColor(inkColor()).withShadow(shadow())
                .withMouse((mouseX / scale), (mouseY / scale)).withPartialTicks(partialTicks)
                .withThemeColors(ThemeColorResolver.of(style()));
        RichTexts.renderer().render(canvas, laidOut, 0, 0, options);
        if (scale != 1f) {
            canvas.popTransform();
        }
    }

    /**
     * The themed {@code color} wins over a themed slot (a row's {@code text-dim})
     * and over the code ink; the rich text pipeline takes ARGB.
     * <p>
     * Package-private so the headless tests can read the ink that would be drawn
     * without a canvas.
     */
    int inkColor() {
        Integer themed = style().visualContext().textColor();
        if (themed != null) {
            return themed;
        }
        if (slotFallback != null) {
            Integer slot = style().get(slotFallback);
            if (slot != null) {
                return slot;
            }
        }
        return fallbackColor;
    }

    /** Whether the text carries a drop shadow — a themed {@code text-shadow} wins over the code default. */
    private boolean shadow() {
        Boolean themed = style().get(Styles.textShadow);
        return themed != null ? themed : defaultShadow;
    }

    // endregion
}
