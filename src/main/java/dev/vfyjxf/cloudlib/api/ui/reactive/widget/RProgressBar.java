package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * A progress bar widget.
 */
public class RProgressBar extends RWidget {

    private float progress = 0.0f; // 0.0 to 1.0
    private String label = "";
    private boolean showLabel = true;

    // Colors
    private int backgroundColor = 0xFF333333;
    private int fillColor = 0xFF44AA44;
    private int borderColor = 0xFF555555;
    private int labelColor = 0xFFFFFFFF;

    // Direction
    private boolean vertical = false;

    public RProgressBar(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
    }

    public float getProgress() {
        return progress;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setShowLabel(boolean show) {
        this.showLabel = show;
    }

    public void setColors(int background, int fill, int border) {
        this.backgroundColor = background;
        this.fillColor = fill;
        this.borderColor = border;
    }

    public void setFillColor(int color) {
        this.fillColor = color;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Draw background
        graphics.fill(x, y, x + width, y + height, backgroundColor);

        // Draw progress fill
        if (vertical) {
            int fillHeight = (int) (height * progress);
            graphics.fill(x + 1, y + height - fillHeight, x + width - 1, y + height - 1, fillColor);
        } else {
            int fillWidth = (int) (width * progress);
            graphics.fill(x + 1, y + 1, x + fillWidth - 1, y + height - 1, fillColor);
        }

        // Draw border
        drawBorder(graphics, x, y, width, height, borderColor);

        // Draw label
        if (showLabel && !label.isEmpty()) {
            String displayLabel = label.replace("%p", String.format("%.0f%%", progress * 100));
            int textWidth = font.width(displayLabel);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - 8) / 2;
            graphics.drawString(font, displayLabel, textX, textY, labelColor);
        }
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public int[] measure(Font font) {
        return new int[]{width, height};
    }
}
