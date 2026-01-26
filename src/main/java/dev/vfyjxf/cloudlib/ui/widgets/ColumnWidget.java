package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A vertical layout container.
 * <p>
 * Arranges children vertically from top to bottom using flexbox layout.
 * Similar to Compose's Column or Flutter's Column.
 */
public class ColumnWidget extends CompositeWidget<Widget> {

    private int spacing = 0;

    public static ColumnWidget create() {
        return new ColumnWidget();
    }

    public static ColumnWidget create(int spacing) {
        return new ColumnWidget().setSpacing(spacing);
    }

    private ColumnWidget() {
        applyStyle(UIStyle.of(
            UIStyles.flexColumn()
        ));
    }

    public int spacing() {
        return spacing;
    }

    public ColumnWidget setSpacing(int spacing) {
        this.spacing = spacing;
        applyStyle(UIStyle.of(UIStyles.rowGap(spacing)));
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
