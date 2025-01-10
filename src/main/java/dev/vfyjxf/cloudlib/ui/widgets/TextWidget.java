package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.UIContext;
import dev.vfyjxf.cloudlib.api.ui.alignment.Alignment;
import dev.vfyjxf.cloudlib.api.ui.alignment.AlignmentReviver;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TextWidget extends Widget implements AlignmentReviver<TextWidget> {

    private Component text;
    private Alignment.Horizontal horizontalAlignment;
    private Alignment.Vertical verticalAlignment;

    public static TextWidget create(Component label, Alignment.Horizontal horizontal, Alignment.Vertical vertical) {
        return new TextWidget(label, horizontal, vertical);
    }

    public static TextWidget create(Component label) {
        return new TextWidget(label);
    }

    public static TextWidget of(LangEntry entry) {
        return new TextWidget(entry.get());
    }

    public static TextWidget of(String label) {
        return new TextWidget(Component.literal(label));
    }

    public TextWidget(Component text, Alignment.Horizontal horizontalAlignment, Alignment.Vertical verticalAlignment) {
        this.text = text;
        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
    }

    public TextWidget(Component text) {
        this(text, Alignment.CenterHorizontally, Alignment.CenterVertically);
    }

    public Component text() {
        return text;
    }

    public void setText(Component text) {
        this.text = text;
    }

    @Override
    public TextWidget alignment(Alignment alignment) {
        return this;
    }

    public TextWidget horizontalAlignment(Alignment.Horizontal horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
        return this;
    }

    public TextWidget verticalAlignment(Alignment.Vertical verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        return this;
    }

    public Alignment.Horizontal horizontalAlignment() {
        return horizontalAlignment;
    }

    public Alignment.Vertical verticalAlignment() {
        return verticalAlignment;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        UIContext context = getContext();
        var font = context.getFont();
        int labelWidth = font.width(text);
        int x = horizontalAlignment.align(labelWidth, getWidth());
        int y = verticalAlignment.align(font.lineHeight, getHeight()) + padding().top;
        graphics.drawString(font, text, x, y - font.lineHeight / 2, 0xffffff);
    }
}
