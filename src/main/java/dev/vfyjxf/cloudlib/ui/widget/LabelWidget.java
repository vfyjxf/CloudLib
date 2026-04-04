package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Simple text label with alignment and auto-measuring.
 */
public class LabelWidget extends Widget {

    //region state

    private Component text;
    private int color = 0xFFFFFF;
    private boolean shadow = true;
    private @Nullable TextAlign align = TextAlign.LEFT;

    //endregion

    //region types

    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }

    //endregion

    //region factory

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
            scene.layoutTree().setMeasureFunc(nodeId(), (style, availableSpace) -> {
                var font = context.font();
                return new FloatSize(font.width(this.text), font.lineHeight);
            });
        });
    }

    //endregion

    //region configuration

    public Component text() {
        return text;
    }

    public LabelWidget setText(Component text) {
        this.text = text;
        return this;
    }

    public LabelWidget setText(LangEntry entry, Object... args) {
        this.text = entry.get(args);
        return this;
    }

    public LabelWidget setText(String text) {
        this.text = Component.literal(text);
        return this;
    }

    public int color() {
        return color;
    }

    public LabelWidget setColor(int color) {
        this.color = color;
        return this;
    }

    public boolean shadow() {
        return shadow;
    }

    public LabelWidget setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public @Nullable TextAlign align() {
        return align;
    }

    public LabelWidget setAlign(@Nullable TextAlign align) {
        this.align = align;
        return this;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
        var font = context().font();
        int textWidth = font.width(text);

        int x = switch (align) {
            case LEFT -> 0;
            case CENTER -> (width() - textWidth) / 2;
            case RIGHT -> width() - textWidth;
            case null -> 0;
        };

        canvas.text(text, x, 0, color, shadow);
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        String content = text.getString();
        if (content.length() > 30) {
            content = content.substring(0, 27) + "...";
        }
        collector.add("text", content, InspectionProperty.categoryData);
        collector.addFormatted("color", String.format("#%06X", color & 0xFFFFFF), "#FFFFFF", InspectionProperty.categoryVisual);
        collector.addWithDefault("shadow", shadow, true, InspectionProperty.categoryVisual);
        collector.addWithDefault("align", align, TextAlign.LEFT, InspectionProperty.categoryVisual);
    }

    //endregion
}
