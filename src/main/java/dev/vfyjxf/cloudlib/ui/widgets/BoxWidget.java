package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A stack/overlay layout container.
 * <p>
 * Overlays children on top of each other. Later children are rendered
 * on top of earlier children. Similar to Compose's Box or Flutter's Stack.
 */
public class BoxWidget extends CompositeWidget<Widget> {

    public static BoxWidget create() {
        return new BoxWidget();
    }

    private BoxWidget() {}

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);
    }
}
