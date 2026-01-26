package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A horizontal layout container.
 * <p>
 * Arranges children horizontally from left to right using flexbox layout.
 * Similar to Compose's Row or Flutter's Row.
 */
public class RowWidget extends CompositeWidget<Widget> {

    private int spacing = 0;

    public static RowWidget create() {
        return new RowWidget();
    }

    public static RowWidget create(int spacing) {
        return new RowWidget().setSpacing(spacing);
    }

    private RowWidget() {
        applyStyle(UIStyle.of(
            UIStyles.flexRow()
        ));
    }

    public int spacing() {
        return spacing;
    }

    public RowWidget setSpacing(int spacing) {
        this.spacing = spacing;
        applyStyle(UIStyle.of(UIStyles.columnGap(spacing)));
        return this;
    }

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);
    }
}
