package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TextWidget extends CompositeWidget<TextWidget.InternalDisplay> {

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
//        display.asChild(this);
//        onInit((self) -> {
//            //TODO:建立widget生命周期模型再改
//            configureInternalDisplay();
//        });
    }

    public Component text() {
        return text;
    }

    public void setText(Component text) {
        this.text = text;
        configureInternalDisplay();
    }

    private void configureInternalDisplay() {
        var font = context().font();
        int labelWidth = font.width(text);
        display.setSize(labelWidth, font.lineHeight);
    }

    protected class InternalDisplay extends Widget {
        @Override
        protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            var font = context().font();
            graphics.drawString(font, text, 0, 0, 0xffffff);
        }

        protected Widget setSize(int width, int height) {
            return super.setSize(width, height);
        }
    }
}
