package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.text.RichTexts;
import dev.vfyjxf.cloudlib.api.text.ThemeColorResolver;
import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextMeasure;
import dev.vfyjxf.cloudlib.api.text.layout.TextAlignment;
import dev.vfyjxf.cloudlib.api.text.render.RenderOptions;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Simple text label with alignment and auto-measuring.
 * <p>
 * Internally backed by the rich text pipeline; alignment is applied against the
 * widget's laid-out width. Unlike the legacy implementation, text wraps when the
 * layout imposes a width smaller than the content.
 */
public class LabelWidget extends Widget {

    // region state

    private Component text;
    private int color = 0xFFFFFF;
    private boolean shadow = true;
    private @Nullable TextAlign align = TextAlign.left;

    private @Nullable RichTextMeasure measure;
    private @Nullable TextAlignment appliedAlignment;

    // endregion

    // region types

    public enum TextAlign {
        left, CENTER, right
    }

    // endregion

    // region factory

    public static LabelWidget of(String text) {
        return new LabelWidget(Component.literal(text));
    }

    public static LabelWidget of(Component text) {
        return new LabelWidget(text);
    }

    public static LabelWidget of(LangEntry entry) {
        return new LabelWidget(entry.get());
    }

    public static LabelWidget of(LangEntry entry, Object... args) {
        return new LabelWidget(entry.get(args));
    }

    private LabelWidget(Component text) {
        this.text = text;
        this.onMount((scene, context, handle) -> {
            measure = createMeasure();
            scene.layoutTree().setMeasureFunc(nodeId(), measure);
        });
        this.onUnmount(() -> measure = null);
    }

    private RichTextMeasure createMeasure() {
        RichTextMeasure created = RichTexts.measure(RichText.of(text));
        TextAlignment alignment = mapAlignment(align);
        created.withAlignment(alignment);
        appliedAlignment = alignment;
        return created;
    }

    private static TextAlignment mapAlignment(@Nullable TextAlign align) {
        if (align == null) return TextAlignment.left;
        return switch (align) {
            case left -> TextAlignment.left;
            case CENTER -> TextAlignment.center;
            case right -> TextAlignment.right;
        };
    }

    // endregion

    // region configuration

    public Component text() {
        return text;
    }

    public LabelWidget setText(Component text) {
        this.text = text;
        if (measure != null) {
            measure = createMeasure();
            scene().layoutTree().setMeasureFunc(nodeId(), measure);
            scene().layoutTree().markDirty(nodeId());
        }
        return this;
    }

    public LabelWidget setText(LangEntry entry, Object... args) {
        return setText(entry.get(args));
    }

    public LabelWidget setText(String text) {
        return setText(Component.literal(text));
    }

    public int color() {
        return color;
    }

    public LabelWidget setColor(int color) {
        this.color = color;
        return this;
    }

    /**
     * Whether the label renders its text with a drop shadow — a themed
     * {@code text-shadow} value wins over the widget default ({@code true}).
     */
    public boolean shadow() {
        Boolean v = style().get(Styles.textShadow);
        return v != null ? v : shadow;
    }

    /** Code-level shadow override — writes the {@code text-shadow} style key. */
    public LabelWidget setShadow(boolean shadow) {
        this.shadow = shadow;
        set(Styles.textShadow, shadow);
        return this;
    }

    public @Nullable TextAlign align() {
        return align;
    }

    public LabelWidget setAlign(@Nullable TextAlign align) {
        this.align = align;
        if (measure != null) {
            TextAlignment alignment = mapAlignment(align);
            if (alignment != appliedAlignment) {
                measure.withAlignment(alignment);
                appliedAlignment = alignment;
                scene().layoutTree().markDirty(nodeId());
            }
        }
        return this;
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
        if (measure == null) return;
        LaidOutText laidOut = measure.layoutAt(Math.max(0, width()));
        RenderOptions options = RenderOptions.defaults.withDefaultColor(inkColor()).withShadow(shadow())
                .withMouse(mouseX, mouseY).withPartialTicks(partialTicks)
                .withThemeColors(ThemeColorResolver.of(style()));
        RichTexts.renderer().render(canvas, laidOut, 0, 0, options);
    }

    /**
     * The themed {@code color} property wins over the code-level color. The rich
     * text pipeline takes ARGB, hence the forced opaque alpha on the fallback.
     */
    private int inkColor() {
        Integer themed = style().visualContext().textColor();
        return themed != null ? themed : color | 0xFF000000;
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        String content = text.getString();
        if (content.length() > 30) {
            content = content.substring(0, 27) + "...";
        }
        collector.add("text", content, InspectionProperty.categoryData);
        collector.addFormatted(
            "color",
            String.format("#%06X", color & 0xFFFFFF),
            "#FFFFFF",
            InspectionProperty.categoryVisual
        );
        collector.addWithDefault("shadow", shadow, true, InspectionProperty.categoryVisual);
        collector.addWithDefault("align", align, TextAlign.left, InspectionProperty.categoryVisual);
    }

    // endregion
}
