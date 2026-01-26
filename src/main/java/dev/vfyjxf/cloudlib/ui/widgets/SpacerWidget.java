package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A flexible space widget that expands to fill available space.
 * <p>
 * Used in stack layouts to push other widgets apart.
 * Similar to SwiftUI's Spacer.
 */
public class SpacerWidget extends Widget {

    private float minLength = 0;

    public static SpacerWidget create() {
        return new SpacerWidget();
    }

    public static SpacerWidget create(float minLength) {
        return new SpacerWidget().setMinLength(minLength);
    }

    /**
     * Creates a fixed-size spacer.
     */
    public static SpacerWidget fixed(float size) {
        return new SpacerWidget()
            .setMinLength(size)
            .setFlexGrow(0);
    }

    private SpacerWidget() {
        applyStyle(UIStyle.of(
            UIStyles.flexGrow(1)
        ));
    }

    public float minLength() {
        return minLength;
    }

    public SpacerWidget setMinLength(float minLength) {
        this.minLength = minLength;
        applyStyle(UIStyle.of(
            UIStyles.minWidth(minLength),
            UIStyles.minHeight(minLength)
        ));
        return this;
    }

    public SpacerWidget setFlexGrow(float grow) {
        applyStyle(UIStyle.of(UIStyles.flexGrow(grow)));
        return this;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Spacer renders nothing by default
    }
}
