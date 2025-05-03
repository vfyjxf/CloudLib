package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widget.Widget;
import dev.vfyjxf.cloudlib.api.ui.widget.WidgetGroup;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.style.StyleSizeLength;

public class TextWidget extends WidgetGroup<TextWidget.InternalDisplay> {

    private final InternalDisplay display = new InternalDisplay();

    private Component text;


    public static TextWidget create(Component label) {
        return new TextWidget(label);
    }

    public static TextWidget of(LangEntry entry) {
        return new TextWidget(entry.get());
    }

    public static TextWidget of(String label) {
        return new TextWidget(Component.literal(label));
    }

    private TextWidget(Component text) {
        this.text = text;
        display.asChild(this);
        onInit((self) -> {
            //TODO:建立widget生命周期模型再改
            configureInternalDisplay();
        });
    }

    public Component text() {
        return text;
    }

    public void setText(Component text) {
        this.text = text;
        configureInternalDisplay();
    }

    private void configureInternalDisplay() {
        var font = getContext().getFont();
        int labelWidth = font.width(text);
        display.yogaNode().setWidth(StyleSizeLength.points(labelWidth));
        display.yogaNode().setHeight(StyleSizeLength.points(font.lineHeight));
    }

    protected class InternalDisplay extends Widget {
        @Override
        protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            var font = getContext().getFont();
            graphics.drawString(font, text, 0, 0, 0xffffff);
        }
    }
}
