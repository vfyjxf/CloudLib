package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * A panel widget with optional title bar and content area.
 * <p>
 * Features:
 * <ul>
 *   <li>Optional title bar with label</li>
 *   <li>Configurable background and border</li>
 *   <li>Content padding</li>
 * </ul>
 */
public class PanelWidget extends CompositeWidget<Widget> {

    private @Nullable String title;
    private VisualTexture backgroundTexture = new ColorTexture(0xCC222222);
    private VisualTexture borderTexture = new ColorTexture(0xFF555555);
    private VisualTexture titleBarTexture = new ColorTexture(0xFF333333);
    private int titleColor = 0xFFFFFF;
    private int borderWidth = 1;
    private int titleBarHeight = 16;
    private int contentPadding = 4;
    private boolean showTitleBar = true;

    public static PanelWidget create() {
        return new PanelWidget();
    }

    public static PanelWidget create(String title) {
        return new PanelWidget().setTitle(title);
    }

    private PanelWidget() {}

    public @Nullable String title() {
        return title;
    }

    public PanelWidget setTitle(@Nullable String title) {
        this.title = title;
        return this;
    }

    public PanelWidget setBackgroundTexture(VisualTexture texture) {
        this.backgroundTexture = texture;
        return this;
    }

    public PanelWidget setBorderTexture(VisualTexture texture) {
        this.borderTexture = texture;
        return this;
    }

    public PanelWidget setTitleBarTexture(VisualTexture texture) {
        this.titleBarTexture = texture;
        return this;
    }

    public PanelWidget setTitleColor(int color) {
        this.titleColor = color;
        return this;
    }

    public PanelWidget setBorderWidth(int width) {
        this.borderWidth = width;
        return this;
    }

    public PanelWidget setTitleBarHeight(int height) {
        this.titleBarHeight = height;
        return this;
    }

    public PanelWidget setContentPadding(int padding) {
        this.contentPadding = padding;
        return this;
    }

    public PanelWidget setShowTitleBar(boolean show) {
        this.showTitleBar = show;
        return this;
    }

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int w = width();
        int h = height();

        // Background
        backgroundTexture.render(graphics, 0, 0, w, h);

        // Border
        if (borderWidth > 0) {
            borderTexture.render(graphics, 0, 0, w, borderWidth); // top
            borderTexture.render(graphics, 0, h - borderWidth, w, borderWidth); // bottom
            borderTexture.render(graphics, 0, 0, borderWidth, h); // left
            borderTexture.render(graphics, w - borderWidth, 0, borderWidth, h); // right
        }

        // Title bar
        int contentY = borderWidth;
        if (showTitleBar && title != null) {
            titleBarTexture.render(graphics, borderWidth, borderWidth, w - borderWidth * 2, titleBarHeight);

            // Title text
            var font = context().font();
            int textX = borderWidth + 4;
            int textY = borderWidth + (titleBarHeight - font.lineHeight) / 2;
            graphics.drawString(font, title, textX, textY, titleColor);

            contentY = borderWidth + titleBarHeight;
        }

        // Render children in content area
        graphics.pose().pushPose();
        graphics.pose().translate(borderWidth + contentPadding, contentY + contentPadding, 0);

        for (Widget child : children()) {
            graphics.pose().pushPose();
            {
                graphics.pose().translate(child.pos().x(), child.pos().y(), 0);
                int relativeX = mouseX - borderWidth - contentPadding - child.pos().x();
                int relativeY = mouseY - contentY - contentPadding - child.pos().y();
                child.renderWidget(graphics, relativeX, relativeY, partialTicks);
            }
            graphics.pose().popPose();
        }

        graphics.pose().popPose();
    }
}
