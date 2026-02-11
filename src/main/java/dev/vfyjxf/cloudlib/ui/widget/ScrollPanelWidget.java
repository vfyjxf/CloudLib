package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

/**
 * Scrollable container with scrollbar.
 */
public class ScrollPanelWidget extends CompositeWidget<Widget> {

    //region types

    public enum ScrollDirection {
        VERTICAL, HORIZONTAL, BOTH
    }

    //endregion

    //region state

    private ScrollDirection direction = ScrollDirection.VERTICAL;
    private int scrollX = 0;
    private int scrollY = 0;
    private int contentWidth = 0;
    private int contentHeight = 0;
    private int scrollSpeed = 10;
    private boolean showScrollBar = true;
    private int scrollBarWidth = 6;

    //endregion

    //region textures

    private VisualTexture scrollBarBackground = new ColorTexture(0x80000000);
    private VisualTexture scrollBarThumb = new ColorTexture(0xFFAAAAAA);
    private @Nullable VisualTexture backgroundTexture = null;

    //endregion

    //region factory

    public static ScrollPanelWidget create() {
        return new ScrollPanelWidget();
    }

    public static ScrollPanelWidget vertical() {
        return new ScrollPanelWidget().setDirection(ScrollDirection.VERTICAL);
    }

    public static ScrollPanelWidget horizontal() {
        return new ScrollPanelWidget().setDirection(ScrollDirection.HORIZONTAL);
    }

    private ScrollPanelWidget() {
        onMouseDragged((input, deltaX, deltaY, context) -> EventDispatch.pass);
    }

    //endregion

    //region configuration

    public ScrollDirection direction() {
        return direction;
    }

    public ScrollPanelWidget setDirection(ScrollDirection direction) {
        this.direction = direction;
        return this;
    }

    public int scrollX() {
        return scrollX;
    }

    public ScrollPanelWidget setScrollX(int scrollX) {
        this.scrollX = clampScrollX(scrollX);
        return this;
    }

    public int scrollY() {
        return scrollY;
    }

    public ScrollPanelWidget setScrollY(int scrollY) {
        this.scrollY = clampScrollY(scrollY);
        return this;
    }

    public ScrollPanelWidget scrollBy(int deltaX, int deltaY) {
        if (direction == ScrollDirection.VERTICAL || direction == ScrollDirection.BOTH) {
            this.scrollY = clampScrollY(this.scrollY + deltaY);
        }
        if (direction == ScrollDirection.HORIZONTAL || direction == ScrollDirection.BOTH) {
            this.scrollX = clampScrollX(this.scrollX + deltaX);
        }
        return this;
    }

    public int contentWidth() {
        return contentWidth;
    }

    public ScrollPanelWidget setContentWidth(int width) {
        this.contentWidth = width;
        return this;
    }

    public int contentHeight() {
        return contentHeight;
    }

    public ScrollPanelWidget setContentHeight(int height) {
        this.contentHeight = height;
        return this;
    }

    public ScrollPanelWidget setContentSize(int width, int height) {
        this.contentWidth = width;
        this.contentHeight = height;
        return this;
    }

    public int scrollSpeed() {
        return scrollSpeed;
    }

    public ScrollPanelWidget setScrollSpeed(int speed) {
        this.scrollSpeed = speed;
        return this;
    }

    public boolean showScrollBar() {
        return showScrollBar;
    }

    public ScrollPanelWidget setShowScrollBar(boolean show) {
        this.showScrollBar = show;
        return this;
    }

    public int scrollBarWidth() {
        return scrollBarWidth;
    }

    public ScrollPanelWidget setScrollBarWidth(int width) {
        this.scrollBarWidth = width;
        return this;
    }

    public ScrollPanelWidget setScrollBarTextures(VisualTexture background, VisualTexture thumb) {
        this.scrollBarBackground = background;
        this.scrollBarThumb = thumb;
        return this;
    }

    public ScrollPanelWidget setBackgroundTexture(@Nullable VisualTexture texture) {
        this.backgroundTexture = texture;
        return this;
    }

    //endregion

    //region children

    public <T extends Widget> T addChild(T widget) {
        return addWidget(widget);
    }

    //endregion

    //region scroll helpers

    private int clampScrollX(int x) {
        int max = Math.max(0, contentWidth - width());
        return Math.clamp(x, 0, max);
    }

    private int clampScrollY(int y) {
        int max = Math.max(0, contentHeight - height());
        return Math.clamp(y, 0, max);
    }

    private boolean canScrollVertically() {
        return (direction == ScrollDirection.VERTICAL || direction == ScrollDirection.BOTH)
               && contentHeight > height();
    }

    private boolean canScrollHorizontally() {
        return (direction == ScrollDirection.HORIZONTAL || direction == ScrollDirection.BOTH)
               && contentWidth > width();
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        var graphics = canvas.graphics();

        if (backgroundTexture != null) {
            canvas.texture(backgroundTexture, 0, 0, width(), height());
        }

        int absX = absolutePos().x();
        int absY = absolutePos().y();
        canvas.pushClip(absX, absY, width(), height());

        canvas.pushTransform();
        canvas.translate(-scrollX, -scrollY);
        int relX = mouseX + scrollX;
        int relY = mouseY + scrollY;
        canvas.renderChildren(children(), relX, relY, partialTicks);
        canvas.popTransform();

        canvas.popClip();

        if (showScrollBar) {
            renderScrollBars(canvas);
        }
    }

    private void renderScrollBars(SceneCanvas canvas) {
        if (canScrollVertically()) {
            int barX = width() - scrollBarWidth;
            int barH = height();
            int thumbH = Math.max(20, (int) ((float) height() / contentHeight * barH));
            int thumbY = (int) ((float) scrollY / (contentHeight - height()) * (barH - thumbH));

            canvas.texture(scrollBarBackground, barX, 0, scrollBarWidth, barH);
            canvas.texture(scrollBarThumb, barX, thumbY, scrollBarWidth, thumbH);
        }

        if (canScrollHorizontally()) {
            int barY = height() - scrollBarWidth;
            int barW = width() - (canScrollVertically() ? scrollBarWidth : 0);
            int thumbW = Math.max(20, (int) ((float) width() / contentWidth * barW));
            int thumbX = (int) ((float) scrollX / (contentWidth - width()) * (barW - thumbW));

            canvas.texture(scrollBarBackground, 0, barY, barW, scrollBarWidth);
            canvas.texture(scrollBarThumb, thumbX, barY, thumbW, scrollBarWidth);
        }
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("scrollX", scrollX, 0, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("scrollY", scrollY, 0, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("contentW", contentWidth, 0, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("contentH", contentHeight, 0, InspectionProperty.CATEGORY_DATA);
        collector.addWithDefault("direction", direction.name(), ScrollDirection.VERTICAL.name(), InspectionProperty.CATEGORY_VISUAL);
        collector.addWithDefault("showScrollBar", showScrollBar, true, InspectionProperty.CATEGORY_VISUAL);
        collector.add("children", children().size(), InspectionProperty.CATEGORY_DATA);
    }

    //endregion
}
