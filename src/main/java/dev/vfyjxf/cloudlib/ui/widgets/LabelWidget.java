package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A simple label widget for displaying static or dynamic text.
 * <p>
 * Unlike {@link TextWidget}, LabelWidget is a leaf widget without children,
 * optimized for simple text display with optional shadow and color support.
 */
public class LabelWidget extends Widget {

    private Component text;
    private int color = 0xFFFFFF;
    private boolean shadow = true;
    private @Nullable TextAlign align = TextAlign.LEFT;

    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }

    public static LabelWidget of(String text) {
        return new LabelWidget(Component.literal(text));
    }

    public static LabelWidget of(Component text) {
        return new LabelWidget(text);
    }

    public static LabelWidget of(LangEntry entry) {
        return new LabelWidget(entry.get());
    }

    private LabelWidget(Component text) {
        this.text = text;
    }

    public Component text() {
        return text;
    }

    public LabelWidget setText(Component text) {
        this.text = text;
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

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);
        var font = context().font();
        int textWidth = font.width(text);

        int x = switch (align) {
            case LEFT -> 0;
            case CENTER -> (width() - textWidth) / 2;
            case RIGHT -> width() - textWidth;
            case null -> 0;
        };

        if (shadow) {
            graphics.drawString(font, text, x, 0, color);
        } else {
            graphics.drawString(font, text, x, 0, color, false);
        }
    }
}
