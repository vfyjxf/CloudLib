package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.text.RichText;
import dev.vfyjxf.cloudlib.api.text.RichTexts;
import dev.vfyjxf.cloudlib.api.text.ThemeColorResolver;
import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.text.layout.RichTextMeasure;
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
 * Text display with auto-measuring for layout.
 * <p>
 * Internally backed by the rich text pipeline: the component is laid out by
 * {@link dev.vfyjxf.cloudlib.api.text.layout.RichTextLayouter} and measured through
 * taffy via {@link RichTextMeasure}. Unlike the legacy implementation, text wraps
 * when the layout imposes a width smaller than the content.
 */
public class TextWidget extends Widget {

    // region state

    private Component text;
    private int color = 0xFFFFFF;
    private boolean shadow = false;

    private @Nullable RichTextMeasure measure;

    // endregion

    // region factory

    public static TextWidget of(Component text) {
        return new TextWidget(text);
    }

    public static TextWidget of(String text) {
        return new TextWidget(Component.literal(text));
    }

    public static TextWidget of(LangEntry entry) {
        return new TextWidget(entry.get());
    }

    public static TextWidget of(LangEntry entry, Object... args) {
        return new TextWidget(entry.get(args));
    }

    private TextWidget(Component text) {
        this.text = text;
        this.onMount((scene, context, handle) -> {
            measure = createMeasure();
            scene.layoutTree().setMeasureFunc(nodeId(), measure);
        });
        this.onUnmount(() -> measure = null);
    }

    private RichTextMeasure createMeasure() {
        return RichTexts.measure(RichText.of(text));
    }

    // endregion

    // region configuration

    public Component text() {
        return text;
    }

    public TextWidget setText(Component text) {
        this.text = text;
        if (measure != null) {
            measure = createMeasure();
            scene().layoutTree().setMeasureFunc(nodeId(), measure);
            scene().layoutTree().markDirty(nodeId());
        }
        return this;
    }

    public TextWidget setText(LangEntry entry, Object... args) {
        return setText(entry.get(args));
    }

    public TextWidget setText(String text) {
        return setText(Component.literal(text));
    }

    public int color() {
        return color;
    }

    public TextWidget setColor(int color) {
        this.color = color;
        return this;
    }

    /**
     * Whether the text renders with a drop shadow — a themed
     * {@code text-shadow} value wins over the widget default ({@code false}).
     */
    public boolean shadow() {
        Boolean v = style().get(Styles.textShadow);
        return v != null ? v : shadow;
    }

    /** Code-level shadow override — writes the {@code text-shadow} style key. */
    public TextWidget setShadow(boolean shadow) {
        this.shadow = shadow;
        set(Styles.textShadow, shadow);
        return this;
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
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
        collector.addWithDefault("shadow", shadow, false, InspectionProperty.categoryVisual);
    }

    // endregion
}
