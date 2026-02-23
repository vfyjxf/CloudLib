package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

/**
 * Panel with optional title bar.
 */
public class PanelWidget extends CompositeWidget<Widget> {

    //region state

    private @Nullable String title;
    private boolean showTitleBar = true;
    private int borderWidth = 1;
    private int titleBarHeight = 16;
    private int contentPadding = 4;

    //endregion

    //region colors

    private int titleColor = 0xFFFFFF;

    //endregion

    //region textures

    private VisualTexture backgroundTexture = new ColorTexture(0xCC222222);
    private VisualTexture borderTexture = new ColorTexture(0xFF555555);
    private VisualTexture titleBarTexture = new ColorTexture(0xFF333333);

    //endregion

    //region factory

    public static PanelWidget create() {
        return new PanelWidget();
    }

    public static PanelWidget create(String title) {
        return new PanelWidget().setTitle(title);
    }

    private PanelWidget() {
    }

    //endregion

    //region configuration

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

    //endregion

    //region children

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        var graphics = canvas.graphics();
        int w = width();
        int h = height();

        // Background
        canvas.texture(backgroundTexture, 0, 0, w, h);

        // Border
        if (borderWidth > 0) {
            canvas.texture(borderTexture, 0, 0, w, borderWidth);
            canvas.texture(borderTexture, 0, h - borderWidth, w, borderWidth);
            canvas.texture(borderTexture, 0, 0, borderWidth, h);
            canvas.texture(borderTexture, w - borderWidth, 0, borderWidth, h);
        }

        // Title bar
        int contentY = borderWidth;
        if (showTitleBar && title != null) {
            canvas.texture(titleBarTexture, borderWidth, borderWidth, w - borderWidth * 2, titleBarHeight);

            var font = context().font();
            int textX = borderWidth + 4;
            int textY = borderWidth + (titleBarHeight - font.lineHeight) / 2;
            canvas.text(title, textX, textY, titleColor, true);

            contentY = borderWidth + titleBarHeight;
        }

        // Render children
        canvas.pushTransform();
        canvas.translate(borderWidth + contentPadding, contentY + contentPadding);
        int relX = mouseX - borderWidth - contentPadding;
        int relY = mouseY - contentY - contentPadding;
        canvas.renderChildren(children(), relX, relY, partialTicks);
        canvas.popTransform();
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        if (title != null) {
            collector.add("title", title, InspectionProperty.categoryData);
        }
        collector.addWithDefault("showTitleBar", showTitleBar, true, InspectionProperty.categoryVisual);
        collector.addWithDefault("borderWidth", borderWidth, 1, InspectionProperty.categoryVisual);
        collector.addWithDefault("contentPadding", contentPadding, 4, InspectionProperty.categoryLayout);
        collector.add("children", children().size(), InspectionProperty.categoryData);
    }

    //endregion
}
