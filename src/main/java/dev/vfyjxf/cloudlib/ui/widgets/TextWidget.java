package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.network.chat.Component;

/**
 * Text display with auto-measuring for layout.
 */
public class TextWidget extends Widget {

    //region state

    private Component text;
    private int color = 0xFFFFFF;
    private boolean shadow = false;

    //endregion

    //region factory

    public static TextWidget of(Component text) {
        return new TextWidget(text);
    }

    public static TextWidget of(String text) {
        return new TextWidget(Component.literal(text));
    }

    public static TextWidget of(LangEntry entry) {
        return new TextWidget(entry.get());
    }

    private TextWidget(Component text) {
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

    public TextWidget setText(Component text) {
        this.text = text;
        return this;
    }

    public TextWidget setText(String text) {
        this.text = Component.literal(text);
        return this;
    }

    public int color() {
        return color;
    }

    public TextWidget setColor(int color) {
        this.color = color;
        return this;
    }

    public boolean shadow() {
        return shadow;
    }

    public TextWidget setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        canvas.text(text, 0, 0, color, shadow);
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
        collector.add("text", content, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("color", String.format("#%06X", color & 0xFFFFFF), "#FFFFFF", InspectionProperty.CATEGORY_VISUAL);
        collector.addWithDefault("shadow", shadow, false, InspectionProperty.CATEGORY_VISUAL);
    }

    //endregion
}
