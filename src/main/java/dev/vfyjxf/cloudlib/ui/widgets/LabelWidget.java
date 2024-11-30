package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.UIContext;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class LabelWidget extends Widget {

    private Component label;

    public static LabelWidget create(Component label) {
        return new LabelWidget(label);
    }

    public static LabelWidget of(LangEntry entry) {
        return new LabelWidget(entry.get());
    }

    public static LabelWidget of(String label) {
        return new LabelWidget(Component.literal(label));
    }

    public LabelWidget(Component label) {
        this.label = label;
    }

    public Component getLabel() {
        return label;
    }

    public void setLabel(Component label) {
        this.label = label;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        UIContext context = getContext();
        var font = context.getFont();
        var x = (int) (posX() + (getWidth() - font.width(label)) / 2);
        var y = (int) (posY() + (getHeight() - font.lineHeight) / 2);
    }
}
