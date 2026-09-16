package dev.vfyjxf.cloudlib.ui.debug;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.tree.Layout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * Draws DevTools-like box-model highlights over the inspected scene, in plain
 * screen space. Rendering goes straight through {@link GuiGraphics} and never
 * touches the inspected scene's canvas, batch or widget tree.
 * <p>
 * Boxes follow the web box model: margin (orange), border (yellow),
 * padding (green) and content (blue), plus an outline and a label with the
 * widget's type name and size.
 */
final class HighlightRenderer {

    private final Scene inspected;

    HighlightRenderer(Scene inspected) {
        this.inspected = inspected;
    }

    void render(GuiGraphics graphics, @Nullable Widget hover, @Nullable Widget selected) {
        if (hover != null && hover != selected) {
            drawWidget(graphics, hover, false);
        }
        if (selected != null) {
            drawWidget(graphics, selected, true);
        }
    }

    private void drawWidget(GuiGraphics graphics, Widget widget, boolean strong) {
        if (!widget.lifecycle().mounted() || widget.scene() != inspected) return;

        FloatPos origin;
        try {
            origin = widget.localToScene(0, 0);
        } catch (RuntimeException e) {
            return;
        }
        int x = (int) Math.round(origin.x);
        int y = (int) Math.round(origin.y);
        int w = widget.width();
        int h = widget.height();
        if (w <= 0 || h <= 0) return;

        FloatRect margin = FloatRect.ZERO;
        FloatRect border = FloatRect.ZERO;
        FloatRect padding = FloatRect.ZERO;
        try {
            Layout layout = inspected.layoutTree().getLayout(widget.nodeId());
            if (layout != null) {
                if (layout.margin() != null) margin = layout.margin();
                if (layout.border() != null) border = layout.border();
                if (layout.padding() != null) padding = layout.padding();
            }
        } catch (RuntimeException ignored) {
            // node gone from the layout tree — fall back to plain bounds
        }

        int mL = Math.round(margin.left), mT = Math.round(margin.top);
        int mR = Math.round(margin.right), mB = Math.round(margin.bottom);
        int bL = Math.round(border.left), bT = Math.round(border.top);
        int bR = Math.round(border.right), bB = Math.round(border.bottom);
        int pL = Math.round(padding.left), pT = Math.round(padding.top);
        int pR = Math.round(padding.right), pB = Math.round(padding.bottom);

        // outermost first so inner boxes paint over (DevTools look)
        if (mL > 0 || mT > 0 || mR > 0 || mB > 0) {
            graphics.fill(x - mL, y - mT, x + w + mR, y + h + mB, DebugTheme.hlMargin);
        }
        if (bL > 0 || bT > 0 || bR > 0 || bB > 0) {
            graphics.fill(x, y, x + w, y + h, DebugTheme.hlBorder);
        }
        if (pL > 0 || pT > 0 || pR > 0 || pB > 0) {
            graphics.fill(x + bL, y + bT, x + w - bR, y + h - bB, DebugTheme.hlPadding);
        }
        graphics.fill(x + bL + pL, y + bT + pT, x + w - bR - pR, y + h - bB - pB, DebugTheme.hlContent);

        int outline = strong ? DebugTheme.hlOutline : DebugTheme.hlOutlineHover;
        graphics.fill(x, y, x + w, y + 1, outline);
        graphics.fill(x, y + h - 1, x + w, y + h, outline);
        graphics.fill(x, y, x + 1, y + h, outline);
        graphics.fill(x + w - 1, y, x + w, y + h, outline);

        drawLabel(graphics, widget, x, y, w, h);
    }

    private void drawLabel(GuiGraphics graphics, Widget widget, int x, int y, int w, int h) {
        var font = Minecraft.getInstance().font;
        String label = widget.inspectionTypeName() + "  " + w + " × " + h;
        int textW = font.width(label);
        int labelW = textW + 6;
        int labelH = 11;

        // above the box by default, flip inside/below when near the screen top
        int labelX = x;
        int labelY = y - labelH - 1;
        if (labelY < 0) labelY = y + h + 1;

        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if (labelX + labelW > screenW) labelX = screenW - labelW;
        if (labelX < 0) labelX = 0;

        graphics.fill(labelX, labelY, labelX + labelW, labelY + labelH, DebugTheme.hlLabelBg);
        graphics.drawString(font, label, labelX + 3, labelY + 2, DebugTheme.hlLabelText, false);
    }
}
